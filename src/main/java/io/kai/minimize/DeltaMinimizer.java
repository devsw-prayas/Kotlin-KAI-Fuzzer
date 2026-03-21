package io.kai.minimize;

import io.kai.compiler.CompilerRunner;
import io.kai.compiler.IOracle;
import io.kai.compiler.OracleVerdict;
import io.kai.contracts.IBuilder;

public class DeltaMinimizer implements IMinimizer {

    @Override
    public IBuilder minimize(IBuilder program, OracleVerdict verdict,
                             IOracle oracle, CompilerRunner runner) {
        IBuilder current = program;
        boolean changed = true;

        while (changed) {
            changed = false;
            for (IBuilder child : current.children()) {
                IBuilder candidate = current.withoutChild(child);
                try {
                    var result = runner.compile(
                            candidate.build(0), null);
                    if (oracle.evaluate(result) instanceof OracleVerdict.Finding) {
                        current = candidate;
                        changed = true;
                        break;
                    }
                } catch (Exception ignored) {
                    // if compile throws, skip this candidate
                }
            }
        }
        return current;
    }
}
