package io.kai.mutation.mutators;

import io.kai.builders.BranchBuilder;
import io.kai.builders.ExpressionBuilder;
import io.kai.builders.FunctionBuilder;
import io.kai.builders.LoopBuilder;
import io.kai.contracts.capability.IBranchContainer;
import io.kai.contracts.IBuilder;
import io.kai.contracts.capability.IContainer;
import io.kai.contracts.capability.ILocalScopeBuilder;
import io.kai.mutation.IMutationPolicy;
import io.kai.mutation.MutationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AddLoopMutation implements IMutationPolicy {

    @Override
    public Set<Class<? extends IBuilder>> targetTypes() {
        return Set.of(FunctionBuilder.class, BranchBuilder.class);
    }

    @Override
    public String id() {
        return "add_loop";
    }

    @Override
    public boolean compatibleWith(IBuilder builder) {
        return targetTypes().contains(builder.getClass());
    }

    @Override
    @SuppressWarnings("unchecked")
    public IBuilder apply(IBuilder builder, MutationContext ctx) {
        LoopBuilder.LoopType type = ctx.rng().nextInt(2) == 0
                ? LoopBuilder.LoopType.FOR_EACH : LoopBuilder.LoopType.WHILE;
        List<? extends IBuilder> children;
        ExpressionBuilder cond = new ExpressionBuilder(ctx.registry(),
                ExpressionBuilder.ExpressionType.BOOL_LITERAL, "true");

        if(builder instanceof IBranchContainer<?> branched) {
            int branch = ctx.rng().nextInt(branched.branchLength());
            children = new ArrayList<>(branched.getBranch(branch));
            LoopBuilder newLoop = new LoopBuilder(ctx.registry(), type, cond, (List<ILocalScopeBuilder>) children);
            branched.clear(branch);
            branched.addChildRaw(newLoop, branch);

        }else if(builder instanceof IContainer<?> container){
            children = new ArrayList<>(builder.children());
            LoopBuilder newLoop = new LoopBuilder(ctx.registry(), type, cond, (List<ILocalScopeBuilder>) children);
            container.clear();
            container.addChildRaw(newLoop);
        }
        return builder;
    }
}

