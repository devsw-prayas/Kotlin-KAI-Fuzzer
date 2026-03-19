package io.kai.mutation.mutators;

import io.kai.builders.FunctionBuilder;
import io.kai.contracts.IBuilder;
import io.kai.mutation.IMutationPolicy;
import io.kai.mutation.MutationContext;

import java.util.Set;

public class AddReifiedInlineMutation implements IMutationPolicy {
    @Override public Set<Class<? extends IBuilder>> targetTypes() {
        return Set.of(FunctionBuilder.class);
    }
    @Override public String id() { return "add_reified_inline"; }
    @Override public boolean compatibleWith(IBuilder b) { return b instanceof FunctionBuilder; }
    @Override public IBuilder apply(IBuilder builder, MutationContext ctx) {
        FunctionBuilder fn = (FunctionBuilder) builder;
        fn.setInline(true);
        fn.addBoundedTypeParam(ctx.registry().next("T"), "reified");
        return fn;
    }
}
