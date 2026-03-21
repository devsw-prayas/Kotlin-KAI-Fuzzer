package io.kai.mutation.mutators;

import io.kai.builders.*;
import io.kai.builders.expressions.LambdaBuilder;
import io.kai.builders.expressions.NullLiteralBuilder;
import io.kai.contracts.capability.IBranchContainer;
import io.kai.contracts.IBuilder;
import io.kai.contracts.capability.IContainer;
import io.kai.contracts.capability.IExpressionBuilder;
import io.kai.mutation.IMutationPolicy;
import io.kai.mutation.MutationContext;
import io.kai.mutation.MutationUtility;
import io.kai.mutation.ScopeContextBuilder;
import io.kai.mutation.context.ScopeContext;

import java.util.Set;

public class InjectNullCheckMutation implements IMutationPolicy {

    @Override
    public String id() {
        return "inject_null_check";
    }

    @Override
    public Set<Class<? extends IBuilder>> targetTypes() {
        return Set.of(FunctionBuilder.class, BranchBuilder.class, LambdaBuilder.class);
    }

    @Override
    public boolean compatibleWith(IBuilder b) {
        return b instanceof FunctionBuilder
                || b instanceof BranchBuilder
                || b instanceof LambdaBuilder;
    }
    @Override
    public IBuilder apply(IBuilder builder, MutationContext ctx) {
        ScopeContext scope = ScopeContextBuilder.buildFor(ctx.root(), builder.id());

        VariableBuilder nullVar;
        ClassBuilder classRef = MutationUtility.pickClassRef(scope, ctx.rng());

        if (classRef != null && ctx.rng().nextInt(3) == 0) {
            nullVar = VariableBuilder.ofClass(builder.getRegistry(), false,
                    new NullLiteralBuilder(builder.getRegistry()), true, classRef);
        } else {
            String type = MutationUtility.pickType(scope, ctx.rng());
            nullVar = new VariableBuilder(builder.getRegistry(), false,
                    new NullLiteralBuilder(builder.getRegistry()), true, type);
        }

        if (builder instanceof IBranchContainer<?> bc) {
            int branch = ctx.rng().nextInt(bc.branchLength());
            bc.addChildRaw(nullVar, branch);
        } else if (builder instanceof IContainer<?> c) {
            MutationUtility.addChildSmart(c, nullVar);
        }
        return builder;
    }
}