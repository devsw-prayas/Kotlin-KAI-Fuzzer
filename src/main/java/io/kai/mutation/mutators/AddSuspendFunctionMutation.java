package io.kai.mutation.mutators;

import io.kai.builders.ClassBuilder;
import io.kai.builders.FunctionBuilder;
import io.kai.builders.ProgramBuilder;
import io.kai.contracts.IBuilder;
import io.kai.contracts.capability.IContainer;
import io.kai.mutation.IMutationPolicy;
import io.kai.mutation.MutationContext;

import java.util.Set;

// Adds a new suspend fun to the target container
public class AddSuspendFunctionMutation implements IMutationPolicy {
    @Override public Set<Class<? extends IBuilder>> targetTypes() {
        return Set.of(ClassBuilder.class, ProgramBuilder.class);
    }
    @Override public String id() { return "add_suspend_function"; }
    @Override public boolean compatibleWith(IBuilder b) {
        return b instanceof ClassBuilder || b instanceof ProgramBuilder;
    }
    @Override public IBuilder apply(IBuilder builder, MutationContext ctx) {
        FunctionBuilder fn = new FunctionBuilder(builder.getRegistry());
        fn.setSuspend(true);
        if (builder instanceof IContainer<?> c) c.addChildRaw(fn);
        return builder;
    }
}
