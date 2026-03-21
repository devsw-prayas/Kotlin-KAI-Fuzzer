package io.kai.mutation.mutators;

import io.kai.builders.ClassBuilder;
import io.kai.builders.FunctionBuilder;
import io.kai.builders.ObjectBuilder;
import io.kai.contracts.IBuilder;
import io.kai.mutation.IMutationPolicy;
import io.kai.mutation.MutationContext;

import java.util.Set;

// Adds a companion object with a factory function to a class
public class AddCompanionObjectMutation implements IMutationPolicy {
    @Override public Set<Class<? extends IBuilder>> targetTypes() { return Set.of(ClassBuilder.class); }
    @Override public String id() { return "add_companion_object"; }
    @Override public boolean compatibleWith(IBuilder b) { return b instanceof ClassBuilder; }
    @Override
    public IBuilder apply(IBuilder builder, MutationContext ctx) {
        ClassBuilder cb = (ClassBuilder) builder;

        boolean hasCompanion = cb.children().stream()
                .anyMatch(c -> c instanceof ObjectBuilder ob && ob.isCompanion());
        if (hasCompanion) return builder;

        ObjectBuilder companion = new ObjectBuilder(builder.getRegistry(), true);
        FunctionBuilder factory = new FunctionBuilder(builder.getRegistry());
        companion.addChild(factory);
        cb.addChild(companion);
        return builder;
    }
}
