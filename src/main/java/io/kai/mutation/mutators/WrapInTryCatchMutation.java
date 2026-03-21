package io.kai.mutation.mutators;

import io.kai.builders.FunctionBuilder;
import io.kai.builders.TryCatchBuilder;
import io.kai.contracts.IBuilder;
import io.kai.contracts.capability.ILocalScopeBuilder;
import io.kai.mutation.IMutationPolicy;
import io.kai.mutation.MutationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// Wraps function body in try/catch
public class WrapInTryCatchMutation implements IMutationPolicy {
    private static final String[] EXCEPTION_TYPES = {
        "Throwable", "Exception", "RuntimeException",
        "IllegalStateException", "IllegalArgumentException"
    };

    @Override public Set<Class<? extends IBuilder>> targetTypes() { return Set.of(FunctionBuilder.class); }
    @Override public String id() { return "wrap_in_try_catch"; }
    @Override public boolean compatibleWith(IBuilder b) { return b instanceof FunctionBuilder; }
    @Override
    public IBuilder apply(IBuilder builder, MutationContext ctx) {
        FunctionBuilder fn = (FunctionBuilder) builder;
        List<? extends IBuilder> existing = new ArrayList<>(fn.children());
        String exType = EXCEPTION_TYPES[ctx.rng().nextInt(EXCEPTION_TYPES.length)];
        TryCatchBuilder tryCatch = new TryCatchBuilder(builder.getRegistry(), exType);

        List<IBuilder> returns = new ArrayList<>();
        for (IBuilder child : existing) {
            if (child.build(0).trim().startsWith("return")) {
                returns.add(child);
            } else if (child instanceof ILocalScopeBuilder ls) {
                tryCatch.addChild(ls, 0);
            }
        }

        fn.clear();
        fn.addChild(tryCatch);
        // Re-add return statements after the try/catch
        for (IBuilder r : returns) {
            fn.addChildRaw(r);
        }
        return fn;
    }
}
