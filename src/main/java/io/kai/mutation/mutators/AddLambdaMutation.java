package io.kai.mutation.mutators;

import io.kai.builders.FunctionBuilder;
import io.kai.builders.VariableBuilder;
import io.kai.builders.expressions.LambdaBuilder;
import io.kai.contracts.IBuilder;
import io.kai.contracts.capability.IContainer;
import io.kai.mutation.IMutationPolicy;
import io.kai.mutation.MutationContext;
import io.kai.mutation.MutationUtility;

import java.util.Set;

public class AddLambdaMutation implements IMutationPolicy {
    @Override public Set<Class<? extends IBuilder>> targetTypes() { return Set.of(FunctionBuilder.class); }
    @Override public String id() { return "add_lambda"; }
    @Override public boolean compatibleWith(IBuilder b) { return b instanceof FunctionBuilder; }

    @Override
    public IBuilder apply(IBuilder builder, MutationContext ctx) {
        LambdaBuilder lambda = new LambdaBuilder(builder.getRegistry());
        VariableBuilder var = new VariableBuilder(builder.getRegistry(), false, lambda, false, "() -> Unit");
        if (builder instanceof IContainer<?> c) MutationUtility.addChildSmart(c, var);
        return builder;
    }
}