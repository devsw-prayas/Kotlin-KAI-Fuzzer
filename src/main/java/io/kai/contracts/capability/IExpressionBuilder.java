package io.kai.contracts.capability;

public interface IExpressionBuilder extends ILocalScopeBuilder{
    default String getValue(){
        return null;
    }
}
