package io.kai.destabilizer.destabilizers;

import io.kai.builders.FunctionBuilder;
import io.kai.builders.RawStatementBuilder;
import io.kai.builders.SealedClassBuilder;
import io.kai.builders.WhenBuilder;
import io.kai.builders.expressions.IntLiteralBuilder;
import io.kai.builders.expressions.VariableRefBuilder;
import io.kai.contracts.IBuilder;
import io.kai.contracts.NameRegistry;
import io.kai.contracts.Parameter;
import io.kai.destabilizer.DestabilizerTreeWalker;
import io.kai.destabilizer.IDestabilizer;

import java.util.List;
import java.util.Random;

public class DataFlowExhaustivenessReifiedDestabilizer implements IDestabilizer {

    @Override
    public String id() { return "dataflow_exhaustiveness_reified"; }

    @Override
    public List<String> requiredFlags() {
        return List.of("-Xdata-flow-based-exhaustiveness");
    }

    @Override
    public boolean canApply(IBuilder root) {
        boolean hasEligibleFn = DestabilizerTreeWalker
                .findAll(root, FunctionBuilder.class)
                .stream()
                .anyMatch(this::isEligible);
        boolean hasSealedClass = !DestabilizerTreeWalker
                .findAll(root, SealedClassBuilder.class)
                .isEmpty();
        return hasEligibleFn && hasSealedClass;
    }

    @Override
    public void destabilize(IBuilder root, Random rng) {
        List<SealedClassBuilder> sealedClasses = DestabilizerTreeWalker
                .findAll(root, SealedClassBuilder.class);
        if (sealedClasses.isEmpty()) return;

        List<FunctionBuilder> targets = DestabilizerTreeWalker
                .findAll(root, FunctionBuilder.class)
                .stream()
                .filter(this::isEligible)
                .toList();
        if (targets.isEmpty()) return;

        // Pick sealed class with most subclasses for maximum exhaustiveness work
        SealedClassBuilder sealed = sealedClasses.stream()
                .filter(sc -> sc.getSubclasses().size() >= 2)
                .findFirst()
                .orElse(null);
        if (sealed == null) return;

        FunctionBuilder target = targets.get(rng.nextInt(targets.size()));
        inject(target, sealed);
    }

    private boolean isEligible(FunctionBuilder fn) {
        if (fn.isOperator()) return false;
        if (!fn.getReturnType().equals("Unit")) return false; // early return only safe in Unit functions
        return fn.children().stream()
                .noneMatch(c -> c instanceof WhenBuilder);
    }

    private void inject(FunctionBuilder fn, SealedClassBuilder sealed) {
        NameRegistry reg = fn.getRegistry();

        String sealedId = sealed.id();
        List<String> subIds = sealed.getSubclasses().stream()
                .map(sc -> sealedId + "." + sc.id())
                .toList();

        if (subIds.size() < 2) return;

        // Add parameter x of the sealed type
        String paramName = reg.next("x");
        fn.addParam(Parameter.simple(paramName, sealedId));

        // Early return on first subclass — gives DFA the condition to eliminate
        // format: if (x is SubClass0) return
        fn.addChild(new RawStatementBuilder(reg,
                "if (" + paramName + " is " + subIds.getFirst() + ") return"));

        // When on remaining subclasses — no else branch forces exhaustiveness check
        // DFA must prove that after the early return, only remaining subclasses exist
        WhenBuilder when = new WhenBuilder(reg,
                new VariableRefBuilder(reg, paramName, sealedId));

        for (int i = 1; i < subIds.size(); i++) {
            String subId = subIds.get(i);
            // condition: is SubClassN
            IntLiteralBuilder condition = new IntLiteralBuilder(reg, "is " + subId);
            List<IBuilder> body = List.of(
                    new RawStatementBuilder(reg, "\"" + subId + "\"")
            );
            when.addBranch(new WhenBuilder.WhenBranch(
                    condition,
                    List.of(new RawStatementBuilder(reg, "Unit")),
                    false
            ));
        }

        fn.addChild(when);
    }
}