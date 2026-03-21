package io.kai;

import io.kai.artifact.ArtifactStore;
import io.kai.compiler.CompilerRunner;
import io.kai.compiler.IOracle;
import io.kai.compiler.coverage.ICoverageCollector;
import io.kai.compiler.coverage.NoOpCoverageCollector;
import io.kai.compiler.coverage.SimpleCoverageCollector;
import io.kai.compiler.oracles.CompositeOracle;
import io.kai.compiler.oracles.CrashOracle;
import io.kai.compiler.oracles.IceOracle;
import io.kai.corpus.ICorpusManager;
import io.kai.corpus.SimpleCorpusManager;
import io.kai.fuzzer.FuzzerConfig;
import io.kai.fuzzer.FuzzerContext;
import io.kai.fuzzer.FuzzerEngine;
import io.kai.fuzzer.FuzzerRuntime;
import io.kai.llm.ILLMProvider;
import io.kai.llm.NoOpLLMProvider;
import io.kai.minimize.DeltaMinimizer;
import io.kai.minimize.IMinimizer;
import io.kai.minimize.NoOpMinimizer;
import io.kai.mutation.MutationRegistry;
import io.kai.mutation.MutationStats;
import io.kai.mutation.chain.MutationChainBuilder;
import io.kai.scheduler.CentroidWeightedScheduler;
import io.kai.scheduler.IScheduler;
import io.kai.scheduler.RandomScheduler;
import io.kai.seed.ISeedProvider;
import io.kai.seed.SyntheticSeedProvider;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

@Command(
        name = "kai",
        mixinStandardHelpOptions = true,
        version = "Kai 1.0",
        description = "Kotlin compiler fuzzer"
)
public class CLI implements Callable<Integer> {

    @Option(names = "-b", description = "Batch size per thread", defaultValue = "50")
    int batchSize;

    @Option(names = "-mdepth", description = "Max mutation chain depth", defaultValue = "5")
    int maxDepth;

    @Option(names = "-smt", description = "Thread count", defaultValue = "1")
    int threadCount;

    @Option(names = "-maxiter", description = "Max iterations (0 = unlimited)", defaultValue = "0")
    long maxIterations;

    @Option(names = "-timeout", description = "Per-compilation timeout in ms", defaultValue = "30000")
    long timeoutMs;

    @Option(names = "-log", description = "Output directory for logs and artifacts", defaultValue = "./logs")
    Path logDir;

    @Option(names = "-kotlinc", description = "Path to kotlinc binary", required = true)
    Path kotlincPath;

    public static void main(String[] args) {
        int exit = new CommandLine(new CLI()).execute(args);
        System.exit(exit);
    }

    @Override
    public Integer call() throws Exception {
        MutationStats stats = new MutationStats();
        Random rng = new Random();

        ICoverageCollector coverage = new SimpleCoverageCollector();
        ICorpusManager corpus = new SimpleCorpusManager();
        ISeedProvider seeder = new SyntheticSeedProvider();
        IScheduler scheduler = new CentroidWeightedScheduler(rng, maxDepth);
        IMinimizer minimizer = new DeltaMinimizer();
        ILLMProvider llm = new NoOpLLMProvider();

        IOracle oracle = new CompositeOracle(List.of(new IceOracle(), new CrashOracle()));
        CompilerRunner runner = new CompilerRunner(kotlincPath, timeoutMs, coverage);
        ArtifactStore store = new ArtifactStore(logDir);

        MutationChainBuilder chainBuilder = new MutationChainBuilder(FuzzerRuntime.get().registry(), maxDepth, scheduler);
        FuzzerConfig config = new FuzzerConfig(batchSize, maxDepth, threadCount, timeoutMs, logDir, kotlincPath, maxIterations);

        FuzzerContext ctx = new FuzzerContext(
                config, corpus, scheduler, chainBuilder, runner,
                oracle, coverage, minimizer, llm, store,
                stats, seeder, rng, FuzzerRuntime.get().registry()
        );

        System.out.println("[Kai] Starting fuzzer with " + threadCount + " thread(s)");
        System.out.println("[Kai] kotlinc: " + kotlincPath);
        System.out.println("[Kai] Artifacts: " + logDir);

        new FuzzerEngine(ctx, maxIterations).run();
        return 0;
    }
}