package io.kai.scheduler;

import io.kai.compiler.OracleVerdict;
import io.kai.contracts.IBuilder;
import io.kai.mutation.IMutationPolicy;
import io.kai.mutation.MutationStats;

import java.util.List;
import java.util.Random;

public class RandomScheduler implements IScheduler{
    private final Random rng;
    public RandomScheduler(Random rng){
        this.rng = rng;
    }
    @Override
    public IBuilder selectSeed(List<IBuilder> corpus) {
        return corpus.get(rng.nextInt(corpus.size()));
    }

    @Override
    public IMutationPolicy selectPolicy(IBuilder node, List<IMutationPolicy> candidates, MutationStats stats) {
        return candidates.get(rng.nextInt(candidates.size()));
    }

    @Override
    public void onResult(IBuilder seed, IMutationPolicy policy, OracleVerdict verdict) {

    }
}
