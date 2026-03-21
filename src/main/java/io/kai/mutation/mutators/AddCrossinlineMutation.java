package io.kai.mutation.mutators;

import io.kai.builders.FunctionBuilder;
import io.kai.contracts.IBuilder;
import io.kai.contracts.Parameter;
import io.kai.mutation.IMutationPolicy;
import io.kai.mutation.MutationContext;

import java.util.Set;

public class AddCrossinlineMutation implements IMutationPolicy {
    @Override public Set<Class<? extends IBuilder>> targetTypes() { return Set.of(FunctionBuilder.class); }
    @Override public String id() { return "add_crossinline"; }
    @Override
    public boolean compatibleWith(IBuilder b) {
        return b instanceof FunctionBuilder fn && !fn.isOperator();
    }
    @Override public IBuilder apply(IBuilder builder, MutationContext ctx) {
        FunctionBuilder fn = (FunctionBuilder) builder;
        fn.setInline(true);
        String paramName = ctx.registry().next("block");
        fn.addParam(new Parameter(paramName, "() -> Unit", null, false, true, false));
        return fn;
    }
}
