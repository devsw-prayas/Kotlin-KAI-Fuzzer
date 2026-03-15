package io.kai.contracts.capability;


import io.kai.contracts.IBuilder;

import java.util.List;

public interface IContainer <R extends IBuilder>{
    boolean addChild(R builder);

    default boolean addChildren(List<R> children){
        return false;
    }

    default void clear(){}

    @SuppressWarnings("unchecked")
    default boolean addChildRaw(IBuilder builder){
        return addChild((R)builder);
    }
}
