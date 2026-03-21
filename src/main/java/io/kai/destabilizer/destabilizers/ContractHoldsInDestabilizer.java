package io.kai.destabilizer.destabilizers;

import io.kai.builders.FunctionBuilder;
import io.kai.builders.RawStatementBuilder;
import io.kai.builders.VariableBuilder;
import io.kai.builders.expressions.IntLiteralBuilder;
import io.kai.contracts.IBuilder;
import io.kai.contracts.Parameter;
import io.kai.contracts.NameRegistry;
import io.kai.contracts.capability.ILocalScopeBuilder;
import io.kai.destabilizer.DestabilizerTreeWalker;
import io.kai.destabilizer.IDestabilizer;

import java.util.List;
import java.util.Random;

public class ContractHoldsInDestabilizer implements IDestabilizer {

    @Override
    public String id() { return "contract_holdsin"; }

    @Override
    public List<String> requiredFlags() {
        return List.of(
                "-Xallow-holdsin-contract",
                "-opt-in=kotlin.contracts.ExperimentalContracts",
                "-opt-in=kotlin.contracts.ExperimentalExtendedContracts"
        );
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
        if (fn.isOperator()) return false;
        if (fn.hasFirstStatement()) return false; // already injected
        if (fn.getParameters().size() > 4) return false;
        if (fn.getParameters().stream()
                .anyMatch(p -> p.name().startsWith("cond_"))) return false;
        return fn.getBody().stream()
                .noneMatch(c -> c.build(0).contains("holdsIn"));
    }

    private void inject(FunctionBuilder fn) {
        NameRegistry reg = fn.getRegistry();

        fn.addAnnotation(
                "@OptIn(kotlin.contracts.ExperimentalContracts::class, " +
                        "kotlin.contracts.ExperimentalExtendedContracts::class)"
        );

        String condName  = reg.next("cond");
        String blockName = reg.next("block");

        fn.addParam(Parameter.simple(condName, "Boolean"));
        fn.addParam(new Parameter(blockName, "() -> Unit",
                null, false, false, true));

        fn.setFirstStatement(
                "kotlin.contracts.contract { " +
                        condName + ".holdsIn<Unit>(" + blockName + ") }");

        fn.getBody().add(new RawStatementBuilder(reg, blockName + "()"));
    }
}