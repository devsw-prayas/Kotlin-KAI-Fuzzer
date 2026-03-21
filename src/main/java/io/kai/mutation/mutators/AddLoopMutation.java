package io.kai.mutation.mutators;

import io.kai.builders.BranchBuilder;
import io.kai.builders.FunctionBuilder;
import io.kai.builders.LoopBuilder;
import io.kai.builders.expressions.BinaryOpBuilder;
import io.kai.builders.expressions.IntLiteralBuilder;
import io.kai.contracts.capability.IBranchContainer;
import io.kai.contracts.IBuilder;
import io.kai.contracts.capability.IContainer;
import io.kai.contracts.capability.IExpressionBuilder;
import io.kai.contracts.capability.ILocalScopeBuilder;
import io.kai.mutation.IMutationPolicy;
import io.kai.mutation.MutationContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class AddLoopMutation implements IMutationPolicy {
    private boolean isReturn(IBuilder node) {
        return node.build(0).trim().startsWith("return");
    }

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

        IExpressionBuilder range = new IntLiteralBuilder(builder.getRegistry(), "0..10");
        IExpressionBuilder cond = new BinaryOpBuilder(builder.getRegistry(), ">",
                new IntLiteralBuilder(builder.getRegistry(), "0"),
                new IntLiteralBuilder(builder.getRegistry(), "0"));

        if (builder instanceof IBranchContainer<?> branched) {
            int branch = ctx.rng().nextInt(branched.branchLength());
            List<ILocalScopeBuilder> branchChildren = new ArrayList<>((Collection<? extends ILocalScopeBuilder>) branched.getBranch(branch));
            List<ILocalScopeBuilder> loopBody = new ArrayList<>();
            List<ILocalScopeBuilder> after = new ArrayList<>();

            for (ILocalScopeBuilder child : branchChildren) {
                if (isReturn(child)) after.add(child);
                else loopBody.add(child);
            }

            LoopBuilder newLoop = new LoopBuilder(builder.getRegistry(), type,
                    type == LoopBuilder.LoopType.FOR_EACH ? range : cond, loopBody);
            branched.clear(branch);
            branched.addChildRaw(newLoop, branch);
            after.forEach(r -> branched.addChildRaw(r, branch));

        } else if (builder instanceof IContainer<?> container) {
            List<ILocalScopeBuilder> children = new ArrayList<>((List<ILocalScopeBuilder>) builder.children());
            List<ILocalScopeBuilder> loopBody = new ArrayList<>();
            List<ILocalScopeBuilder> after = new ArrayList<>();

            for (ILocalScopeBuilder child : children) {
                if (isReturn(child)) after.add(child);
                else loopBody.add(child);
            }

            LoopBuilder newLoop = new LoopBuilder(builder.getRegistry(), type,
                    type == LoopBuilder.LoopType.FOR_EACH ? range : cond, loopBody);
            container.clear();
            container.addChildRaw(newLoop);
            after.forEach(container::addChildRaw);
        }
        return builder;
    }
}

