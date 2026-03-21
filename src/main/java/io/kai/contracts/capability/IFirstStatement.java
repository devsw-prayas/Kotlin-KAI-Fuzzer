package io.kai.contracts.capability;

import java.util.function.Supplier;

public interface IFirstStatement {
    void setFirstStatement(String stmt);
    void setFirstStatementLazy(Supplier<String> stmt);
    String getFirstStatement();
    boolean hasFirstStatement();
}