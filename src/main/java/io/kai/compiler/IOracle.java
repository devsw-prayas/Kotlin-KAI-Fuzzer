package io.kai.compiler;

@FunctionalInterface
public interface IOracle {
    OracleVerdict evaluate(CompilerResult result);
}
