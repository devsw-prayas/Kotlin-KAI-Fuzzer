package io.kai.fuzzer;

import io.kai.artifact.FindingDeduplicator;
import io.kai.contracts.IBuilder;
import io.kai.corpus.CorpusMeta;
import io.kai.contracts.visitor.StructuralHasher;
import io.kai.compiler.coverage.CoverageSnapshot;
import io.kai.destabilizer.DestabilizerRunner;
import io.kai.mutation.MutationContext;
import io.kai.mutation.chain.MutationChain;
import io.kai.mutation.chain.MutationChainLog;
import io.kai.compiler.CompilerResult;
import io.kai.compiler.OracleVerdict;
import io.kai.mutation.chain.MutationStep;
import io.kai.mutation.context.ScopeContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FuzzerEngine {
    private final FuzzerContext ctx;
    private final FuzzerStats stats;
    private final FindingDeduplicator deduplicator;
    private volatile boolean running = true;
    private final long maxIterations; // 0 = unlimited
    private final DestabilizerRunner destabilizerRunner; // null = disabled
    private final Random destabRng = new Random();

    public FuzzerEngine(FuzzerContext ctx, long maxIterations,
                        DestabilizerRunner destabilizerRunner) {
        this.ctx = ctx;
        this.maxIterations = maxIterations;
        this.stats = new FuzzerStats(ctx.stats());
        this.deduplicator = new FindingDeduplicator();
        this.destabilizerRunner = destabilizerRunner;
    }
    public void run() throws InterruptedException {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[Kai] Shutting down gracefully...");
            running = false;
            stats.stopReporting();
        }));

        stats.startReporting(10);

        ExecutorService executor = Executors.newWorkStealingPool(ctx.config().threadCount());
        for (int i = 0; i < ctx.config().threadCount(); i++) {
            executor.submit(this::workerLoop);
        }

        // Start destabilizer as separate thread if enabled
        if (destabilizerRunner != null) {
            ExecutorService destabExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "kai-destabilizer");
                t.setDaemon(true);
                return t;
            });
            destabExecutor.submit(this::destabilizerLoop);
            System.out.println("[Kai] Destabilizer pass enabled");
        }

        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
    }

    private void workerLoop() {
        while (running) {
            if (maxIterations > 0 && stats.getTotalIterations() >= maxIterations) {
                running = false;
                break;
            }
            try {
                // 1. Bootstrap corpus if empty
                if (ctx.manager().size() == 0) {
                    IBuilder initial = ctx.seeder().next();
                    ctx.manager().add(initial, CorpusMeta.fresh(StructuralHasher.hash(initial)));
                }

                // 2. Select seed
                IBuilder seed = ctx.scheduler().selectSeed(ctx.manager().all());

                // 3. Build mutation chain
                long rngSeed = ctx.rng().nextLong();
                MutationContext mutCtx = new MutationContext(
                        new Random(rngSeed),
                        new ScopeContext(null),
                        0,
                        ctx.stats(),
                        ctx.seeder().getRegistry(),
                        seed
                );
                MutationChain chain = ctx.builder().build(seed, mutCtx);

                // Verbose output
                if (FuzzerRuntime.isVerbose()) {
                    StringBuilder sb = new StringBuilder("[Chain] seed=")
                            .append(seed.id()).append(" steps=").append(chain.steps().size()).append("\n");
                    for (int i = 0; i < chain.steps().size(); i++) {
                        MutationStep step = chain.steps().get(i);
                        sb.append("  step ").append(i + 1).append(": ")
                                .append(step.policyID())
                                .append(" → node(").append(step.nodeID()).append(")");
                        if (i < chain.steps().size() - 1) sb.append("\n");
                    }
                    System.out.println(sb);
                }

                // 4. Apply chain → emit source
                IBuilder mutated;
                String source;

                // Keep it synchronized, destabilizer will be pulling the entry as well
                synchronized (seed) {
                    mutated = chain.applyTo(seed, ctx.mutationRegistry(), mutCtx);
                    source = mutated.build(0);
                }

                // Output
                if (FuzzerRuntime.isVerbose()) {
                    try {
                        Files.createDirectories(Path.of("./debug"));
                        Files.writeString(Path.of("./debug/program_" + System.nanoTime() + ".kt"), source);
                    } catch (Exception ignored) {}
                }

                MutationChainLog chainLog = new MutationChainLog(
                        seed.id(), rngSeed,
                        mutCtx.registry().snapshot(),
                        chain, System.currentTimeMillis()
                );

                // 5. Compile
                CompilerResult result = ctx.runner().compile(source, chainLog,
                        FuzzerRuntime.get().globalFlags());

                // Verbose output for compilation
                if (FuzzerRuntime.isVerbose()) {
                    System.out.println("[Compile] exit=" + result.exitCode()
                            + " duration=" + result.durationMs() + "ms"
                            + " stderr=" + result.stderr().length() + "b");
                    if (!result.stderr().isBlank())
                        System.out.println("[STDERR]\n" + result.stderr());
                }

                // 6. Collect coverage
                CoverageSnapshot coverage = ctx.collector().collect(result);

                // 7. Evaluate oracle
                OracleVerdict verdict = ctx.oracle().evaluate(result);

                // 8. Handle finding — deduplicate, minimize, save
                if (verdict instanceof OracleVerdict.Finding f) {
                    if (deduplicator.isNew(result.stderr())) {
                        IBuilder minimal = ctx.minimizer().minimize(mutated, f, ctx.oracle(), ctx.runner());
                        ctx.store().save(minimal, result, f);
                        stats.recordFinding();
                        System.out.println("[FINDING #" + stats.getTotalFindings()
                                + "] " + f.type() + ": " + f.description());
                    } else {
                        System.out.println("[DUPLICATE] " + f.type() + " — skipped");
                    }
                }

                // 9. Update corpus if novel coverage
                if (ctx.collector().isNovel(coverage)) {
                    ctx.manager().add(mutated, CorpusMeta.fresh(StructuralHasher.hash(mutated)));
                }

                // 10. Update scheduler
                String lastPolicyId = chain.lastPolicy();
                if (lastPolicyId != null && !lastPolicyId.isEmpty()) {
                    ctx.mutationRegistry().policiesFor(seed).stream()
                            .filter(p -> p.id().equals(lastPolicyId))
                            .findFirst()
                            .ifPresent(p -> ctx.scheduler().onResult(seed, p, verdict));
                }

                // 11. Record stats
                stats.recordIteration();
                stats.setCorpusSize(ctx.manager().size());

            } catch (Exception e) {
                System.err.println("[Worker] Error: " + e.getMessage());
            }
        }
    }

    private void destabilizerLoop() {
        while (running) {
            try {
                if (ctx.manager().size() == 0) {
                    Thread.sleep(1000);
                    continue;
                }
                if (destabRng.nextDouble() < 0.30) {
                    IBuilder seed = ctx.scheduler().selectSeed(ctx.manager().all());
                    boolean found = destabilizerRunner.run(seed, destabRng);
                    if (found) stats.recordFinding();
                }
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("[DestabLoop] " + e.getClass().getSimpleName()
                        + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
