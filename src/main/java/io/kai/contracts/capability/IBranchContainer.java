package io.kai.contracts.capability;

import io.kai.contracts.IBuilder;

import java.util.List;

public interface IBranchContainer<R extends IBuilder>{
    default boolean supportsBranching(){
        return true;
    }
    boolean addChild(R builder, int branch);

    default boolean addChildren(List<R> children, int branch) {
        return false;
    }

    default void clear(int branch){}

    List<R> getBranch(int branch);

    @SuppressWarnings("unchecked")
    default boolean addChildRaw(IBuilder builder, int branch){
        return addChild((R)builder, branch);
    }

    int branchLength();
}
