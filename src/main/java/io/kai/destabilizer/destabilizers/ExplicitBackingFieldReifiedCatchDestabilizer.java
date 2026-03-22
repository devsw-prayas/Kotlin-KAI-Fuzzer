package io.kai.destabilizer.destabilizers;

import io.kai.builders.*;
import io.kai.builders.expressions.IntLiteralBuilder;
import io.kai.contracts.IBuilder;
import io.kai.contracts.NameRegistry;
import io.kai.contracts.Parameter;
import io.kai.destabilizer.DestabilizerTreeWalker;
import io.kai.destabilizer.IDestabilizer;

import java.util.List;
import java.util.Random;

public class ExplicitBackingFieldReifiedCatchDestabilizer implements IDestabilizer {

    @Override
    public String id() { return "explicit_backing_field_reified_catch"; }

    @Override
    public List<String> requiredFlags() {
        return List.of(
                "-Xexplicit-backing-fields",
                "-Xallow-reified-type-in-catch",
                "-Xallow-contracts-on-more-functions",
                "-opt-in=kotlin.contracts.ExperimentalContracts"
        );
    }

    @Override
    public boolean canApply(IBuilder root) {
        return DestabilizerTreeWalker.findAll(root, ClassBuilder.class)
                .stream()
                .anyMatch(this::isEligible);
    }

    @Override
    public void destabilize(IBuilder root, Random rng) {
        List<ClassBuilder> targets = DestabilizerTreeWalker
                .findAll(root, ClassBuilder.class)
                .stream()
                .filter(this::isEligible)
                .toList();

        if (targets.isEmpty()) return;

        ClassBuilder target = targets.get(rng.nextInt(targets.size()));
        inject(target);
    }

    private boolean isEligible(ClassBuilder cb) {
        if (cb.isObject()) return false;
        // Guard — already injected
        return cb.children().stream()
                .noneMatch(c -> c.build(0).contains("field = RuntimeException()"));
    }

    private void inject(ClassBuilder cb) {
        NameRegistry reg = cb.getRegistry();
        String className = cb.id();

        VariableBuilder errorField = new VariableBuilder(
                reg, false,
                new IntLiteralBuilder(reg, "null"),
                false, "Throwable")
                .withExplicitBackingField("Throwable", "RuntimeException()");

        String errorId = errorField.id();
        VariableBuilder contractProp = new VariableBuilder(
                reg, false,
                new IntLiteralBuilder(reg, "null"),
                false, "Boolean")
                .withGetterBlock(
                        "kotlin.contracts.contract { " +
                                "returns(true) implies (this@" + className + " is " + className + ") }\n" +
                                "            return " + errorId + " is RuntimeException");

        String T = reg.next("T");
        FunctionBuilder safeFn = new FunctionBuilder(reg);
        safeFn.setInline(true);
        safeFn.addBoundedTypeParam(T, "reified Throwable");
        safeFn.addParam(Parameter.simple(reg.next("block"), "() -> Unit"));

        TryCatchBuilder tc = new TryCatchBuilder(reg, T);
        tc.addChild(new RawStatementBuilder(reg,
                safeFn.getParameters().getFirst().name() + "()"), 0);
        tc.addChild(new VariableBuilder(reg, false,
                new IntLiteralBuilder(reg, T + "::class.java"),
                false, "Class<" + T + ">"), 1);

        safeFn.addChild(tc);

        cb.addChild(errorField);
        cb.addChild(contractProp);
        cb.addChild(safeFn);
    }
}