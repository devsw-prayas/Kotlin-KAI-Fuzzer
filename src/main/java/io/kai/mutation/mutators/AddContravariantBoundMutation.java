package io.kai.mutation.mutators;

import io.kai.builders.ClassBuilder;
import io.kai.builders.FunctionBuilder;
import io.kai.contracts.IBuilder;
import io.kai.mutation.IMutationPolicy;
import io.kai.mutation.MutationContext;

import java.util.Set;

// Adds: <T: Comparable<T>> bound
public class AddContravariantBoundMutation implements IMutationPolicy {
    @Override public Set<Class<? extends IBuilder>> targetTypes() {
        return Set.of(ClassBuilder.class, FunctionBuilder.class);
    }
    @Override public String id() { return "add_contravariant_bound"; }
    @Override public boolean compatibleWith(IBuilder b) {
        return b instanceof ClassBuilder || b instanceof FunctionBuilder;
    }
    @Override public IBuilder apply(IBuilder builder, MutationContext ctx) {
        String tName = ctx.registry().next("T");
        if (builder instanceof ClassBuilder cb) {
            cb.addBoundedTypeParam(tName, "Comparable<" + tName + ">");
        } else if (builder instanceof FunctionBuilder fn) {
            fn.addBoundedTypeParam(tName, "Comparable<" + tName + ">");
        }
        return builder;
    }
}
