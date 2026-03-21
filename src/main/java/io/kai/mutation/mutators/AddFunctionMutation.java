package io.kai.mutation.mutators;

import io.kai.builders.ClassBuilder;
import io.kai.builders.FunctionBuilder;
import io.kai.builders.ProgramBuilder;
import io.kai.contracts.IBuilder;
import io.kai.contracts.capability.IContainer;
import io.kai.mutation.IMutationPolicy;
import io.kai.mutation.MutationContext;
import io.kai.mutation.MutationUtility;

import java.util.Set;

public class AddFunctionMutation implements IMutationPolicy {
    @Override
    public Set<Class<? extends IBuilder>> targetTypes() {
        return Set.of(ClassBuilder.class, ProgramBuilder.class);
    }

    @Override
    public String id() {
        return "add_function";
    }

    @Override
    public boolean compatibleWith(IBuilder b) {
        if (b instanceof ProgramBuilder pb)
            return MutationUtility.countChildrenOfType(pb, FunctionBuilder.class) < 3;
        return b instanceof ClassBuilder;
    }
    @Override
    public IBuilder apply(IBuilder builder, MutationContext ctx) {
        FunctionBuilder newFunc = new FunctionBuilder(builder.getRegistry());
        if(builder instanceof IContainer<?> container){
            container.addChildRaw(newFunc);
        }
        return builder;
    }
}
