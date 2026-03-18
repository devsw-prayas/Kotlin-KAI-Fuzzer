package io.kai.mutation.mutators;

import io.kai.builders.*;
import io.kai.builders.expressions.NullLiteralBuilder;
import io.kai.contracts.capability.IBranchContainer;
import io.kai.contracts.IBuilder;
import io.kai.contracts.capability.IContainer;
import io.kai.contracts.capability.IExpressionBuilder;
import io.kai.mutation.IMutationPolicy;
import io.kai.mutation.MutationContext;

import java.util.Set;

public class InjectNullCheckMutation implements IMutationPolicy {

    @Override
    public Set<Class<? extends IBuilder>> targetTypes() {
        return Set.of(FunctionBuilder.class, BranchBuilder.class);
    }

    @Override
    public String id() {
        return "inject_null_check";
    }

    @Override
    public boolean compatibleWith(IBuilder builder) {
        return targetTypes().contains(builder.getClass());
    }

    @Override
    public IBuilder apply(IBuilder builder, MutationContext ctx) {
        IExpressionBuilder nullExpr = new NullLiteralBuilder(builder.getRegistry());
        VariableBuilder nullVar = new VariableBuilder(
                builder.getRegistry(), false, nullExpr, true // nullable = true
        );

        if (builder instanceof IBranchContainer<?> bc) {
            int branch = ctx.rng().nextInt(bc.branchLength());
            bc.addChildRaw(nullVar, branch);
        } else if (builder instanceof IContainer<?> c) {
            c.addChildRaw(nullVar);
        }
        return builder;
    }
}