package io.kai.mutation.mutators;

import io.kai.builders.*;
import io.kai.builders.expressions.IntLiteralBuilder;
import io.kai.contracts.capability.IBranchContainer;
import io.kai.contracts.IBuilder;
import io.kai.contracts.capability.IContainer;
import io.kai.contracts.capability.IExpressionBuilder;
import io.kai.mutation.IMutationPolicy;
import io.kai.mutation.MutationContext;

import java.util.Set;

public class AddVariableMutation implements IMutationPolicy {
    @Override
    public Set<Class<? extends IBuilder>> targetTypes() {
        return Set.of(FunctionBuilder.class, ClassBuilder.class, BranchBuilder.class);
    }

    @Override
    public String id() {
        return "add_variable";
    }

    @Override
    public boolean compatibleWith(IBuilder builder) {
        return targetTypes().contains(builder.getClass());
    }

    @Override
    public IBuilder apply(IBuilder builder, MutationContext ctx) {
        IExpressionBuilder exp = new IntLiteralBuilder(builder.getRegistry(), "100");
        VariableBuilder newVar = new VariableBuilder(builder.getRegistry(), true, exp, true, "Int");

        if(builder instanceof IBranchContainer<?> bc){
            int branch = ctx.rng().nextInt(bc.branchLength());
            bc.addChildRaw(newVar, branch);
        }else if(builder instanceof IContainer<?> c)
            c.addChildRaw(newVar);
        return builder;
    }
}
