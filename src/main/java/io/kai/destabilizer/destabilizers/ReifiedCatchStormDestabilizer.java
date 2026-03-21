package io.kai.destabilizer.destabilizers;

import io.kai.builders.FunctionBuilder;
import io.kai.builders.RawStatementBuilder;
import io.kai.builders.TryCatchBuilder;
import io.kai.builders.VariableBuilder;
import io.kai.builders.expressions.IntLiteralBuilder;
import io.kai.contracts.IBuilder;
import io.kai.contracts.NameRegistry;
import io.kai.destabilizer.DestabilizerTreeWalker;
import io.kai.destabilizer.IDestabilizer;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class ReifiedCatchStormDestabilizer implements IDestabilizer {

    @Override
    public String id() { return "reified_catch_storm"; }

    @Override
    public List<String> requiredFlags() {
        return List.of("-Xallow-reified-type-in-catch");
    }

    @Override
    public boolean canApply(IBuilder root) {
        return DestabilizerTreeWalker.findAll(root, FunctionBuilder.class)
                .stream()
                .anyMatch(this::isEligible);
    }

    @Override
    public void destabilize(IBuilder root, Random rng) {
        List<FunctionBuilder> targets = DestabilizerTreeWalker
                .findAll(root, FunctionBuilder.class)
                .stream()
                .filter(this::isEligible)
                .toList();

        if (targets.isEmpty()) return;

        FunctionBuilder target = targets.get(rng.nextInt(targets.size()));
        inject(target);
    }

    private boolean isEligible(FunctionBuilder fn) {
        if (!fn.isInline()) return false;
        // Guard: skip if already has both storm ops and a reified catch
        boolean hasStorm = fn.children().stream()
                .anyMatch(c -> c.build(0).contains("::class.java"));
        boolean hasCatch = fn.children().stream()
                .filter(c -> c instanceof TryCatchBuilder)
                .map(c -> (TryCatchBuilder) c)
                .anyMatch(tc -> fn.getTypeParams().containsKey(tc.getExceptionType()));
        return !(hasStorm && hasCatch);
    }

    private void inject(FunctionBuilder fn) {
        NameRegistry reg = fn.getRegistry();

        // Add a fresh reified T : Throwable param — serves double duty:
        // storm ops use it for reified substitution AND catch uses it as
        // the exception type. Maximum substitution work from one param.
        String T = reg.next("T");
        fn.addBoundedTypeParam(T, "reified Throwable");

        String existing = fn.children().stream()
                .map(c -> c.build(0))
                .reduce("", String::concat);

        // Storm ops — inject whichever are missing
        if (!existing.contains(T + "::class.java")) {
            fn.addChild(new VariableBuilder(reg, false,
                    new IntLiteralBuilder(reg, T + "::class.java"),
                    false, "Class<" + T + ">"));
        }

        if (!existing.contains("(42 is " + T + ")")) {
            fn.addChild(new VariableBuilder(reg, false,
                    new IntLiteralBuilder(reg, "(42 is " + T + ")"),
                    false, "Boolean"));
        }

        if (!existing.contains("listOf<" + T + ">()")) {
            fn.addChild(new VariableBuilder(reg, false,
                    new IntLiteralBuilder(reg, "listOf<" + T + ">()"),
                    false, "List<" + T + ">"));
        }

        // Reified catch — try body has a nullable T? var, catch forces
        // T::class.java again inside the handler
        TryCatchBuilder tc = new TryCatchBuilder(reg, T);

        tc.addChild(new VariableBuilder(reg, false,
                new IntLiteralBuilder(reg, "null"),
                true, T), 0);

        tc.addChild(new RawStatementBuilder(reg,
                "val _catch_cls = " + T + "::class.java"), 1);

        fn.addChild(tc);
    }
}