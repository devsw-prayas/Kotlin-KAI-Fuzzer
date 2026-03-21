package io.kai.mutation.mutators;

import io.kai.builders.FunctionBuilder;
import io.kai.builders.VariableBuilder;
import io.kai.builders.expressions.IntLiteralBuilder;
import io.kai.contracts.IBuilder;
import io.kai.mutation.IMutationPolicy;
import io.kai.mutation.MutationContext;

import java.util.Set;

// Adds: fun <T> fun_0() where T: Runnable, T: Cloneable
// Emitted as a comment-style annotation on the function via raw expression in body
public class AddMultipleUpperBoundMutation implements IMutationPolicy {
    private static final String[] BOUNDS = {
            "Runnable", "Comparable<" + "T" + ">",
            "java.io.Serializable", "Cloneable"
    };

    @Override public Set<Class<? extends IBuilder>> targetTypes() { return Set.of(FunctionBuilder.class); }
    @Override public String id() { return "add_multiple_upper_bound"; }
    @Override public boolean compatibleWith(IBuilder b) { return b instanceof FunctionBuilder; }
    @Override public IBuilder apply(IBuilder builder, MutationContext ctx) {
        FunctionBuilder fn = (FunctionBuilder) builder;
        String bound = BOUNDS[ctx.rng().nextInt(BOUNDS.length)];
        String tName = ctx.registry().next("T");
        String resolvedBound = bound.replace("T", tName);
        fn.addBoundedTypeParam(tName, resolvedBound);
        return fn;
    }
}
