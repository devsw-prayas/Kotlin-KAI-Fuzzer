package io.kai.fuzzer;

import io.kai.artifact.ArtifactStore;
import io.kai.compiler.CompilerRunner;
import io.kai.compiler.IOracle;
import io.kai.compiler.coverage.ICoverageCollector;
import io.kai.contracts.NameRegistry;
import io.kai.corpus.ICorpusManager;
import io.kai.llm.ILLMProvider;
import io.kai.minimize.IMinimizer;
import io.kai.mutation.MutationRegistry;
import io.kai.mutation.MutationStats;
import io.kai.mutation.chain.MutationChainBuilder;
import io.kai.scheduler.IScheduler;
import io.kai.seed.ISeedProvider;

import java.util.Random;

public record FuzzerContext(FuzzerConfig config, ICorpusManager manager, IScheduler scheduler,
                            MutationChainBuilder builder, CompilerRunner runner, IOracle oracle,
                            ICoverageCollector collector, IMinimizer minimizer, ILLMProvider llm,
                            ArtifactStore store, MutationStats stats, ISeedProvider seeder,
                            MutationRegistry mutationRegistry, NameRegistry registry, Random rng) {
}
