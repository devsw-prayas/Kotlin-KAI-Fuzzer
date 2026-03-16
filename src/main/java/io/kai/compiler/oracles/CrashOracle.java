package io.kai.compiler.oracles;

import io.kai.compiler.CompilerResult;
import io.kai.compiler.FindingType;
import io.kai.compiler.IOracle;
import io.kai.compiler.OracleVerdict;

public class CrashOracle implements IOracle {
    private static final String ICE_MARKER = "error: internal error:";

    @Override
    public OracleVerdict evaluate(CompilerResult result) {
        if(result.timedOut()) return new OracleVerdict.Finding(FindingType.HANG, "Compilation Timeout");
        if(result.exitCode() != 0 && !result.stderr().contains(ICE_MARKER))
            return new OracleVerdict.Finding(FindingType.CRASH, "Compiler Crashed");
        return new OracleVerdict.Clean();
    }
}
