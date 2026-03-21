package io.kai.mutation.mutators;

import io.kai.builders.*;
import io.kai.builders.expressions.IntLiteralBuilder;
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

public class AddVariableMutation implements IMutationPolicy {
    @Override
    public String id() {
        return "add_variable";
    }

    @Override
    public Set<Class<? extends IBuilder>> targetTypes() {
        return Set.of(FunctionBuilder.class, ClassBuilder.class,
                BranchBuilder.class, LambdaBuilder.class);
    }

    @Override
    public boolean compatibleWith(IBuilder builder) {
        return builder instanceof FunctionBuilder
                || builder instanceof ClassBuilder
                || builder instanceof BranchBuilder
                || builder instanceof LambdaBuilder;
    }
    @Override
    public IBuilder apply(IBuilder builder, MutationContext ctx) {
        ScopeContext scope = ScopeContextBuilder.buildFor(ctx.root(), builder.id());
        boolean nullable = ctx.rng().nextBoolean();

        VariableBuilder newVar;
        ClassBuilder classRef = MutationUtility.pickClassRef(scope, ctx.rng());

        if (classRef != null && ctx.rng().nextInt(3) == 0) {
            // Lazy class reference — type resolves at build() time
            newVar = VariableBuilder.ofClass(builder.getRegistry(), false,
                    new NullLiteralBuilder(builder.getRegistry()), true, classRef);
        } else {
            String type = MutationUtility.pickType(scope, ctx.rng());
            IExpressionBuilder exp = nullable
                    ? new NullLiteralBuilder(builder.getRegistry())
                    : new IntLiteralBuilder(builder.getRegistry(), "100");
            newVar = new VariableBuilder(builder.getRegistry(),
                    ctx.rng().nextBoolean(), exp, nullable, type);
        }

        if (builder instanceof IBranchContainer<?> bc) {
            int branch = ctx.rng().nextInt(bc.branchLength());
            bc.addChildRaw(newVar, branch);
        } else if (builder instanceof IContainer<?> c) {
            MutationUtility.addChildSmart(c, newVar);
        }
        return builder;
    }
}
