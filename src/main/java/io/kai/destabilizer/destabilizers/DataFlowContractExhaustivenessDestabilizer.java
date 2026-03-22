package io.kai.destabilizer.destabilizers;

import io.kai.builders.*;
import io.kai.contracts.IBuilder;
import io.kai.contracts.NameRegistry;
import io.kai.contracts.Parameter;
import io.kai.destabilizer.DestabilizerTreeWalker;
import io.kai.destabilizer.IDestabilizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DataFlowContractExhaustivenessDestabilizer implements IDestabilizer {

    @Override
    public String id() { return "dataflow_contract_exhaustiveness"; }

    @Override
    public List<String> requiredFlags() {
        return List.of(
                "-Xdata-flow-based-exhaustiveness",
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
                .anyMatch(c -> c.build(0).contains("DestabDFSentinel_"));
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
        List<String> remainingSubs = sc.getSubclasses().stream()
                .skip(1)
                .map(s -> sealedId + "." + s.id())
                .toList();

        if (remainingSubs.isEmpty()) return;

        String T = reg.next("T");
        String extT = reg.next("T");
        String shapeParam = reg.next("shape");

        // Generic box class
        ClassBuilder box = new ClassBuilder(reg);
        box.addBoundedTypeParam(T, sealedId);
        box.addConstructorParam(Parameter.simple("shape", T));

        // Extension function
        ExtensionFunctionBuilder ext = new ExtensionFunctionBuilder(
                reg,
                () -> {
                    List<String> keys = new ArrayList<>(box.getTypeParams().keySet());
                    if (keys.isEmpty()) return box.id();
                    return box.id() + "<" + extT +
                            ", *".repeat(keys.size() - 1) +
                            ">";
                },
                "String");

        ext.addBoundedTypeParam(extT, sealedId);
        ext.addAnnotation("@OptIn(kotlin.contracts.ExperimentalContracts::class)");

        // shape as explicit value parameter — contract can reference it
        ext.addParam(Parameter.simple(shapeParam, extT));

        // Contract — DFA uses this to eliminate sub0 from when exhaustiveness
        ext.setFirstStatementLazy(() ->
                "kotlin.contracts.contract { " +
                        "returns() implies (" + shapeParam + " !is " + sub0 + ") }");

        // Early return on sub0
        ext.addChild(new RawStatementBuilder(reg,
                "if (" + shapeParam + " is " + sub0 + ") return \"" + sub0 + "\""));

        // Return when — exhaustive without else branch
        // DFA proves sub0 eliminated by contract + early return
        StringBuilder whenRaw = new StringBuilder("return when (")
                .append(shapeParam).append(") {\n");
        for (String sub : remainingSubs) {
            whenRaw.append("        is ").append(sub)
                    .append(" -> \"").append(sub).append("\"\n");
        }
        whenRaw.append("    }");
        ext.addChild(new RawStatementBuilder(reg, whenRaw.toString()));

        pb.addChild(box);
        pb.addChild(ext);
        pb.addChild(new TypeAliasBuilder(reg,
                "DestabDFSentinel_" + sc.id(), "Boolean"));
    }
}