package io.kai.mutation.mutators;

import io.kai.builders.ClassBuilder;
import io.kai.builders.FunctionBuilder;
import io.kai.contracts.IBuilder;
import io.kai.contracts.capability.IGeneric;
import io.kai.mutation.IMutationPolicy;
import io.kai.mutation.MutationContext;

import java.util.Set;

public class GenericMutation implements IMutationPolicy {
    @Override
    public Set<Class<? extends IBuilder>> targetTypes() {
        return Set.of(ClassBuilder.class, FunctionBuilder.class);
    }

    @Override
    public String id() {
        return "add_generic";
    }

    @Override
    public boolean compatibleWith(IBuilder builder) {
        return targetTypes().contains(builder.getClass());
    }

    @Override
    public IBuilder apply(IBuilder builder, MutationContext ctx) {
        if(builder instanceof IGeneric generic){
            generic.addTypeParam();
        }
        return builder;
    }
}
