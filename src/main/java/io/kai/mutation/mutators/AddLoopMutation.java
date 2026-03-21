package io.kai.mutation.mutators;

import io.kai.builders.BranchBuilder;
import io.kai.builders.FunctionBuilder;
import io.kai.builders.LoopBuilder;
import io.kai.builders.expressions.BinaryOpBuilder;
import io.kai.builders.expressions.IntLiteralBuilder;
import io.kai.builders.expressions.LambdaBuilder;
import io.kai.contracts.capability.IBranchContainer;
import io.kai.contracts.IBuilder;
import io.kai.contracts.capability.IContainer;
import io.kai.contracts.capability.IExpressionBuilder;
import io.kai.contracts.capability.ILocalScopeBuilder;
import io.kai.mutation.IMutationPolicy;
import io.kai.mutation.MutationContext;
import io.kai.mutation.ScopeContextBuilder;
import io.kai.mutation.context.ScopeContext;

import java.util.*;

public class AddLoopMutation implements IMutationPolicy {
    private boolean isReturn(IBuilder node) {
        return node.build(0).trim().startsWith("return");
    }

    @Override
    public String id() {
        return "add_loop";
    }

    @Override
    public Set<Class<? extends IBuilder>> targetTypes() {
        return Set.of(FunctionBuilder.class, BranchBuilder.class, LambdaBuilder.class);
    }

    @Override
    public boolean compatibleWith(IBuilder builder) {
        return builder instanceof FunctionBuilder
                || builder instanceof BranchBuilder
                || builder instanceof LambdaBuilder;
    }

    @Override
    @SuppressWarnings("unchecked")
    public IBuilder apply(IBuilder builder, MutationContext ctx) {
        ScopeContext scope = ScopeContextBuilder.buildFor(ctx.root(), builder.id());
        LoopBuilder.LoopType type = ctx.rng().nextInt(2) == 0
                ? LoopBuilder.LoopType.FOR_EACH : LoopBuilder.LoopType.WHILE;

        // Use in-scope Int var as bound if available
        String intVar = scope.getVars().entrySet().stream()
                .filter(e -> e.getValue().equals("Int"))
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);

        IExpressionBuilder range = intVar != null
                ? new IntLiteralBuilder(builder.getRegistry(), "0.." + intVar)
                : new IntLiteralBuilder(builder.getRegistry(), "0..10");

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

