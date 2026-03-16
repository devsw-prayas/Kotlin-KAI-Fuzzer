package io.kai.scheduler;

import io.kai.compiler.OracleVerdict;
import io.kai.contracts.IBuilder;
import io.kai.mutation.IMutationPolicy;
import io.kai.mutation.MutationStats;

import java.util.List;

public interface IScheduler {
    IBuilder selectSeed(List<IBuilder> corpus);
    IMutationPolicy selectPolicy(IBuilder node, List<IMutationPolicy> candidates, MutationStats stats);
    void onResult(IBuilder seed, IMutationPolicy policy, OracleVerdict verdict);
}
