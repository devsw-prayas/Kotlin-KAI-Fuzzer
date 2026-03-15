package io.kai.mutation;

import io.kai.contracts.IBuilder;

import java.util.Set;

public interface IMutationPolicy {
    public Set<Class<? extends IBuilder>> targetTypes();
    String id();
    boolean compatibleWith(IBuilder builder);
    IBuilder apply(IBuilder builder, MutationContext ctx);
}
