package io.kai.compiler.oracles;

import io.kai.compiler.CompilerResult;
import io.kai.compiler.FindingType;
import io.kai.compiler.IOracle;
import io.kai.compiler.OracleVerdict;

import java.util.regex.Pattern;

public class CrashOracle implements IOracle {
    private static final String ICE_MARKER = "error: internal error:";

    private static final Pattern JVM_CRASH = Pattern.compile(
            "^(Exception in thread|A fatal error|java\\.lang\\.|# A fatal error)",
            Pattern.MULTILINE
    );

    @Override
    public OracleVerdict evaluate(CompilerResult result) {
        if (result.timedOut())
            return new OracleVerdict.Finding(FindingType.HANG, "Compilation Timeout");

        if (result.stderr().contains(ICE_MARKER))
            return new OracleVerdict.Clean();

        boolean abnormalExit = result.exitCode() >= 2;
        boolean jvmCrash = JVM_CRASH.matcher(result.stderr()).find();

        if (abnormalExit || jvmCrash)
            return new OracleVerdict.Finding(FindingType.CRASH, "Compiler Crashed");

        return new OracleVerdict.Clean();
    }
}
