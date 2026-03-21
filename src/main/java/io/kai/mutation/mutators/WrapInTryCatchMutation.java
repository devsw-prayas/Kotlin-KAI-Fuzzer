package io.kai.mutation.mutators;

import io.kai.builders.FunctionBuilder;
import io.kai.builders.TryCatchBuilder;
import io.kai.builders.expressions.LambdaBuilder;
import io.kai.contracts.IBuilder;
import io.kai.contracts.capability.IContainer;
import io.kai.contracts.capability.ILocalScopeBuilder;
import io.kai.mutation.IMutationPolicy;
import io.kai.mutation.MutationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class WrapInTryCatchMutation implements IMutationPolicy {
    private static final String[] EXCEPTION_TYPES = {
            "Throwable", "Exception", "RuntimeException",
            "IllegalStateException", "IllegalArgumentException"
    };

    @Override
    public Set<Class<? extends IBuilder>> targetTypes() {
        return Set.of(FunctionBuilder.class, LambdaBuilder.class);
    }

    @Override
    public String id() { return "wrap_in_try_catch"; }

    @Override
    public boolean compatibleWith(IBuilder b) {
        if (!(b instanceof FunctionBuilder || b instanceof LambdaBuilder)) return false;
        if (b instanceof FunctionBuilder fn && fn.isOperator()) return false;
        // Don't wrap if already contains a try/catch
        return b.children().stream()
                .noneMatch(c -> c instanceof TryCatchBuilder);
    }

    @Override
    public IBuilder apply(IBuilder builder, MutationContext ctx) {
        IContainer<?> container = (IContainer<?>) builder;
        List<? extends IBuilder> existing = new ArrayList<>(builder.children());
        String exType = EXCEPTION_TYPES[ctx.rng().nextInt(EXCEPTION_TYPES.length)];
        TryCatchBuilder tryCatch = new TryCatchBuilder(builder.getRegistry(), exType);

        boolean isLambda = builder instanceof LambdaBuilder;
        List<IBuilder> returns = new ArrayList<>();

        for (int i = 0; i < existing.size(); i++) {
            IBuilder child = existing.get(i);
            boolean isLast = i == existing.size() - 1;
            boolean isReturnStmt = child.build(0).trim().startsWith("return");
            boolean isImplicitReturn = isLambda && isLast && !isReturnStmt;

            if (isReturnStmt || isImplicitReturn) {
                returns.add(child);
            } else if (child instanceof ILocalScopeBuilder ls) {
                tryCatch.addChild(ls, 0);
            }
        }

        container.clear();
        container.addChildRaw(tryCatch);
        for (IBuilder r : returns) container.addChildRaw(r);
        return builder;
    }
}