package io.kai.minimize;

import io.kai.compiler.CompilerRunner;
import io.kai.compiler.IOracle;
import io.kai.compiler.OracleVerdict;
import io.kai.contracts.IBuilder;

public class NoOpMinimizer implements IMinimizer{
    @Override
    public IBuilder minimize(IBuilder program, OracleVerdict verdict, IOracle oracle, CompilerRunner runner) {
        return program;
    }
}
