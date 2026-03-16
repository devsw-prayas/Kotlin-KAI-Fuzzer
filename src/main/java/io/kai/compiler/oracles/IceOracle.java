package io.kai.compiler.oracles;

import io.kai.compiler.CompilerResult;
import io.kai.compiler.FindingType;
import io.kai.compiler.IOracle;
import io.kai.compiler.OracleVerdict;

import java.util.regex.Pattern;

public class IceOracle implements IOracle {
    private static final String ICE_MARKER = "error: internal error:";
    private static final Pattern KT_FRAME = Pattern.compile("^\\s+at org\\.jetbrains\\.kotlin\\..*");

    private String extractMessage(String message){
        return message.lines().filter(line -> line.contains(ICE_MARKER))
                .findFirst().orElse("Unknown ICE");
    }

    @Override
    public OracleVerdict evaluate(CompilerResult result) {
        if(result.exitCode() == 0 ) return new OracleVerdict.Clean();
        if(!result.stderr().contains(ICE_MARKER)) return new OracleVerdict.Clean();
        if(result.stderr().lines().noneMatch(line -> KT_FRAME.matcher(line).matches()))
            return new  OracleVerdict.Clean();
        return new OracleVerdict.Finding(FindingType.ICE, extractMessage(result.stderr()));
    }
}
