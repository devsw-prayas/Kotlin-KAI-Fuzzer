package io.kai.mutation.mutators;

import io.kai.builders.ClassBuilder;
import io.kai.contracts.IBuilder;
import io.kai.mutation.IMutationPolicy;
import io.kai.mutation.MutationContext;

import java.util.Set;

// Adds: class class_0<T: class_0<T>>
public class AddRecursiveGenericBoundMutation implements IMutationPolicy {
    @Override public Set<Class<? extends IBuilder>> targetTypes() { return Set.of(ClassBuilder.class); }
    @Override public String id() { return "add_recursive_generic_bound"; }
    @Override public boolean compatibleWith(IBuilder b) { return b instanceof ClassBuilder; }
    @Override public IBuilder apply(IBuilder builder, MutationContext ctx) {
        ClassBuilder cb = (ClassBuilder) builder;
        String tName = ctx.registry().next("T");
        cb.addBoundedTypeParam(tName, cb.id() + "<" + tName + ">");
        return cb;
    }
}
