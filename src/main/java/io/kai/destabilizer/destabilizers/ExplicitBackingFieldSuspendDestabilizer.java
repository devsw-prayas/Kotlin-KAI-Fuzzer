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

public class ExplicitBackingFieldSuspendDestabilizer implements IDestabilizer {

    @Override
    public String id() { return "explicit_backing_field_suspend"; }

    @Override
    public List<String> requiredFlags() {
        return List.of("-Xexplicit-backing-fields");
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
        // Skip if already injected
        return cb.children().stream()
                .noneMatch(c -> c.build(0).contains("field = suspend {"));
    }

    private void inject(ClassBuilder cb) {
        NameRegistry reg = cb.getRegistry();

        VariableBuilder vb = new VariableBuilder(
                reg, false,
                new IntLiteralBuilder(reg, "null"),
                false, "Any")  // Any is supertype of everything — valid
                .withExplicitBackingField("suspend () -> Unit", "suspend { }");

        cb.addChild(vb);
    }
}