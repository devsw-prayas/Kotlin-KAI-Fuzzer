package io.kai.compiler.oracles;

import io.kai.compiler.CompilerResult;
import io.kai.compiler.IOracle;
import io.kai.compiler.OracleVerdict;

import java.util.List;

public class CompositeOracle implements IOracle {
    private final List<IOracle> chain;

    public CompositeOracle(List<IOracle> oracles){
        this.chain = oracles;
    }

    @Override
    public OracleVerdict evaluate(CompilerResult result){
        for (IOracle oracle : chain) {
            OracleVerdict verdict = oracle.evaluate(result);
            if (verdict instanceof OracleVerdict.Finding) return verdict;
        }
        return new OracleVerdict.Clean();
    }
}
