package io.kai.destabilizer.destabilizers;

import io.kai.builders.ClassBuilder;
import io.kai.builders.VariableBuilder;
import io.kai.builders.expressions.IntLiteralBuilder;
import io.kai.contracts.IBuilder;
import io.kai.contracts.NameRegistry;
import io.kai.destabilizer.DestabilizerTreeWalker;
import io.kai.destabilizer.IDestabilizer;

import java.util.List;
import java.util.Random;

public class ExplicitBackingFieldContractDestabilizer implements IDestabilizer {

    @Override
    public String id() { return "explicit_backing_field_contract"; }

    @Override
    public List<String> requiredFlags() {
        return List.of(
                "-Xexplicit-backing-fields",
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
        if (cb.getTypeParams().isEmpty()) return false;
        // Already has backing field injection
        if (cb.children().stream()
                .anyMatch(c -> c.build(0).contains("field = null as"))) return false;
        // Already has contract getter
        return cb.children().stream()
                .noneMatch(c -> c.build(0).contains("returns(true) implies"));
    }

    private void inject(ClassBuilder cb) {
        NameRegistry reg = cb.getRegistry();
        String T = cb.getTypeParams().keySet().iterator().next();

        VariableBuilder backingField = new VariableBuilder(
                reg, false,
                new IntLiteralBuilder(reg, "null"),
                true, "Any")
                .withExplicitBackingField(T + "?", "null as " + T + "?");

        String fieldId = backingField.id();

        String className = cb.id();
        VariableBuilder contractProp = new VariableBuilder(
                reg, false,
                new IntLiteralBuilder(reg, "null"),
                false, "Boolean")
                .withGetterBlock(
                        "kotlin.contracts.contract { " +
                                "returns(true) implies (this@" + className + " is " + className + ") }\n" +
                                "            return true");

        cb.addChild(backingField);
        cb.addChild(contractProp);
    }
}