package io.kai.destabilizer.destabilizers;

import io.kai.builders.*;
import io.kai.builders.expressions.IntLiteralBuilder;
import io.kai.contracts.IBuilder;
import io.kai.contracts.NameRegistry;
import io.kai.contracts.Parameter;
import io.kai.destabilizer.DestabilizerTreeWalker;
import io.kai.destabilizer.IDestabilizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ContextSensitiveContractDestabilizer implements IDestabilizer {

    @Override
    public String id() { return "context_sensitive_contract"; }

    @Override
    public List<String> requiredFlags() {
        return List.of(
                "-Xallow-contracts-on-more-functions",
                "-opt-in=kotlin.contracts.ExperimentalContracts"
        );
    }

    @Override
    public boolean canApply(IBuilder root) {
        if (!(root instanceof ProgramBuilder)) return false;
        boolean hasSealed = DestabilizerTreeWalker
                .findAll(root, SealedClassBuilder.class)
                .stream()
                .anyMatch(s -> s.getSubclasses().size() >= 2);
        boolean alreadyInjected = root.children().stream()
                .anyMatch(c -> c.build(0).contains("DestabSentinel_"));
        return hasSealed && !alreadyInjected;
    }

    @Override
    public void destabilize(IBuilder root, Random rng) {
        List<SealedClassBuilder> sealedList = DestabilizerTreeWalker
                .findAll(root, SealedClassBuilder.class)
                .stream()
                .filter(s -> s.getSubclasses().size() >= 2)
                .toList();
        if (sealedList.isEmpty()) return;

        SealedClassBuilder sc = sealedList.get(rng.nextInt(sealedList.size()));
        ProgramBuilder pb = (ProgramBuilder) root;
        inject(pb, sc);
    }

    private void inject(ProgramBuilder pb, SealedClassBuilder sc) {
        NameRegistry reg = pb.getRegistry();

        String sealedId = sc.id();
        String sub0 = sealedId + "." + sc.getSubclasses().getFirst().id();
        String T = reg.next("T");
        String extT = reg.next("T");

        // Generic result wrapper — construction mutations may add more type params
        ClassBuilder result = new ClassBuilder(reg);
        result.addBoundedTypeParam(T, sealedId);
        result.addConstructorParam(Parameter.simple("value", T));

        // Lazy receiver — evaluated at build() time when result's typeParams are final
        ExtensionFunctionBuilder ext = new ExtensionFunctionBuilder(
                reg,
                () -> {
                    List<String> keys = new ArrayList<>(result.getTypeParams().keySet());
                    if (keys.isEmpty()) return result.id();
                    return result.id() + "<" + extT +
                            ", *".repeat(keys.size() - 1) +
                            ">";
                },
                "Boolean");

        ext.addBoundedTypeParam(extT, sealedId);
        ext.addAnnotation("@OptIn(kotlin.contracts.ExperimentalContracts::class)");

        ext.setFirstStatementLazy(() -> {
            List<String> keys = new ArrayList<>(result.getTypeParams().keySet());
            StringBuilder stars = new StringBuilder("<").append(sub0);
            for (int i = 1; i < keys.size(); i++) stars.append(", *");
            stars.append(">");
            return "kotlin.contracts.contract { " +
                    "returns(true) implies (this@" + ext.id() +
                    " is " + result.id() + stars + ") }";
        });

        ext.addChild(new RawStatementBuilder(reg,
                "return this.value is " + sub0));

        pb.addChild(result);
        pb.addChild(ext);
        pb.addChild(new TypeAliasBuilder(reg,
                "DestabSentinel_" + sc.id(), "Boolean"));
    }
}