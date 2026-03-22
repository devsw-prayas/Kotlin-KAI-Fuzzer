package io.kai.destabilizer.destabilizers;

import io.kai.builders.FunctionBuilder;
import io.kai.builders.RawStatementBuilder;
import io.kai.builders.TryCatchBuilder;
import io.kai.builders.VariableBuilder;
import io.kai.builders.expressions.IntLiteralBuilder;
import io.kai.contracts.IBuilder;
import io.kai.contracts.NameRegistry;
import io.kai.contracts.Parameter;
import io.kai.destabilizer.DestabilizerTreeWalker;
import io.kai.destabilizer.IDestabilizer;

import java.util.List;
import java.util.Random;

public class ReifiedCatchSuspendDestabilizer implements IDestabilizer {

    @Override
    public String id() { return "reified_catch_suspend"; }

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
        if (!fn.isInline() || !fn.isSuspend()) return false;
        // Guard: skip if already injected
        return fn.children().stream()
                .filter(c -> c instanceof TryCatchBuilder)
                .map(c -> (TryCatchBuilder) c)
                .noneMatch(tc -> tc.getExceptionType().startsWith("T_")
                        && fn.getTypeParams().containsKey(tc.getExceptionType()));
    }

    private void inject(FunctionBuilder fn) {
        NameRegistry reg = fn.getRegistry();

        // Add a fresh reified T : Throwable param dedicated to the catch clause
        // "reified Throwable" convention → emits: reified T_N : Throwable
        String T = reg.next("T");
        fn.addBoundedTypeParam(T, "reified Throwable");

        // Ensure function has a () -> Unit param to call in the try body
        boolean hasBlockParam = fn.getParameters().stream()
                .anyMatch(p -> p.type().equals("() -> Unit"));
        if (!hasBlockParam) {
            fn.addParam(Parameter.simple(reg.next("block"), "() -> Unit"));
        }

        String blockCall = fn.getParameters().stream()
                .filter(p -> p.type().equals("() -> Unit"))
                .map(Parameter::name)
                .findFirst()
                .orElse(null);

        // Build: try { block() } catch (e: T) { val _cls = T::class.java }
        TryCatchBuilder tc = new TryCatchBuilder(reg, T);

        // try body
        if (blockCall != null) {
            tc.addChild(new RawStatementBuilder(reg, blockCall + "()"), 0);
        }

        // catch body: force reified materialisation inside the handler
        tc.addChild(new VariableBuilder(reg, false,
                new IntLiteralBuilder(reg, T + "::class.java"),
                false, "Class<" + T + ">"), 1);

        fn.addChild(tc);
    }
}