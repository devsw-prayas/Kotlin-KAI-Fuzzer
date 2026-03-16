package io.kai;

import io.kai.artifact.ArtifactStore;
import io.kai.compiler.CompilerRunner;
import io.kai.compiler.IOracle;
import io.kai.compiler.coverage.ICoverageCollector;
import io.kai.compiler.coverage.NoOpCoverageCollector;
import io.kai.compiler.oracles.CompositeOracle;
import io.kai.compiler.oracles.CrashOracle;
import io.kai.compiler.oracles.IceOracle;
import io.kai.contracts.NameRegistry;
import io.kai.corpus.ICorpusManager;
import io.kai.corpus.SimpleCorpusManager;
import io.kai.fuzzer.FuzzerConfig;
import io.kai.fuzzer.FuzzerContext;
import io.kai.fuzzer.FuzzerEngine;
import io.kai.llm.ILLMProvider;
import io.kai.llm.NoOpLLMProvider;
import io.kai.minimize.IMinimizer;
import io.kai.minimize.NoOpMinimizer;
import io.kai.mutation.MutationRegistry;
import io.kai.mutation.MutationStats;
import io.kai.mutation.chain.MutationChainBuilder;
import io.kai.mutation.mutators.*;
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
        // --- Mutation registry ---
        MutationRegistry mutationRegistry = new MutationRegistry();
        registerDefaultPolicies(mutationRegistry);

        // --- Core components ---
        NameRegistry nameRegistry = new NameRegistry();
        MutationStats stats = new MutationStats();
        Random rng = new Random();

        ICoverageCollector coverage = new NoOpCoverageCollector();
        ICorpusManager corpus = new SimpleCorpusManager();
        ISeedProvider seeder = new SyntheticSeedProvider();
        IScheduler scheduler = new RandomScheduler(rng);
        IMinimizer minimizer = new NoOpMinimizer();
        ILLMProvider llm = new NoOpLLMProvider();

        IOracle oracle = new CompositeOracle(List.of(new IceOracle(), new CrashOracle()));

        CompilerRunner runner = new CompilerRunner(kotlincPath, timeoutMs, coverage);
        ArtifactStore store = new ArtifactStore(logDir);

        MutationChainBuilder chainBuilder = new MutationChainBuilder(mutationRegistry, maxDepth);

        FuzzerConfig config = new FuzzerConfig(batchSize, maxDepth, threadCount, timeoutMs, logDir, kotlincPath);

        FuzzerContext ctx = new FuzzerContext(
                config, corpus, scheduler, chainBuilder, runner,
                oracle, coverage, minimizer, llm, store,
                stats, seeder, mutationRegistry, nameRegistry, rng
        );

        System.out.println("[Kai] Starting fuzzer with " + threadCount + " thread(s)");
        System.out.println("[Kai] kotlinc: " + kotlincPath);
        System.out.println("[Kai] Artifacts: " + logDir);

        new FuzzerEngine(ctx).run();
        return 0;
    }

    private void registerDefaultPolicies(MutationRegistry registry) {
        AddVariableMutation addVar = new AddVariableMutation();
        AddLoopMutation addLoop = new AddLoopMutation();
        AddFunctionMutation addFunc = new AddFunctionMutation();
        GenericMutation addGeneric = new GenericMutation();
        ExpandExpressionMutation expandExpr = new ExpandExpressionMutation();
        InjectNullCheckMutation nullCheck = new InjectNullCheckMutation();

        // Register each policy for its target node types
        for (var targetType : addVar.targetTypes()) registry.register(targetType, addVar);
        for (var targetType : addLoop.targetTypes()) registry.register(targetType, addLoop);
        for (var targetType : addFunc.targetTypes()) registry.register(targetType, addFunc);
        for (var targetType : addGeneric.targetTypes()) registry.register(targetType, addGeneric);
        for (var targetType : expandExpr.targetTypes()) registry.register(targetType, expandExpr);
        for (var targetType : nullCheck.targetTypes()) registry.register(targetType, nullCheck);
    }
}