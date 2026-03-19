package io.kai.fuzzer;

import io.kai.contracts.IBuilder;
import io.kai.corpus.CorpusMeta;
import io.kai.contracts.visitor.StructuralHasher;
import io.kai.compiler.coverage.CoverageSnapshot;
import io.kai.mutation.chain.MutationChain;
import io.kai.mutation.chain.MutationChainLog;
import io.kai.mutation.MutationContext;
import io.kai.compiler.CompilerResult;
import io.kai.compiler.OracleVerdict;
import io.kai.mutation.context.ScopeContext;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FuzzerEngine {
    private final FuzzerContext ctx;

    public FuzzerEngine(FuzzerContext ctx) {
        this.ctx = ctx;
    }

    public void run() throws InterruptedException {
        var executor = Executors.newWorkStealingPool(ctx.config().threadCount());
        for (int i = 0; i < ctx.config().threadCount(); i++) {
            executor.submit(this::workerLoop);
        }
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
    }

    private void workerLoop() {
        while (true) {
            try {
                // 1. Bootstrap corpus if empty
                if (ctx.manager().size() == 0) {
                    IBuilder initial = ctx.seeder().next();
                    ctx.manager().add(initial, CorpusMeta.fresh(StructuralHasher.hash(initial)));
                }

                // 2. Select seed from corpus
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

                // 4. Apply chain -> emit source
                IBuilder mutated = chain.applyTo(seed, FuzzerRuntime.get().registry(), mutCtx);
                String source = mutated.build(0);
                MutationChainLog chainLog = new MutationChainLog(
                        seed.id(),
                        rngSeed,
                        mutCtx.registry().snapshot(),
                        chain,
                        System.currentTimeMillis()
                );

                // 5. Compile
                CompilerResult result = ctx.runner().compile(source, chainLog);

                // DEBUG
                System.out.println("[Run] exit=" + result.exitCode() + " timedOut=" + result.timedOut() + " stderr=" +
                        result.stderr().substring(0, Math.min(100, result.stderr().length())));

                // 6. Collect coverage
                CoverageSnapshot coverage = ctx.collector().collect(result);

                // 7. Evaluate oracle
                OracleVerdict verdict = ctx.oracle().evaluate(result);

                // 8. Handle finding
                if (verdict instanceof OracleVerdict.Finding f) {
                    IBuilder minimal = ctx.minimizer().minimize(mutated, f, ctx.oracle(), ctx.runner());
                    ctx.store().save(minimal, result, f);
                    System.out.println("[FINDING] " + f.type() + ": " + f.description());
                }

                // 9. Update corpus if novel coverage
                if (ctx.collector().isNovel(coverage)) {
                    ctx.manager().add(mutated, CorpusMeta.fresh(StructuralHasher.hash(mutated)));
                }

                // 10. Update scheduler
                String lastPolicyId = chain.lastPolicy();
                if (lastPolicyId != null && !lastPolicyId.isEmpty()) {
                    FuzzerRuntime.get().registry().policiesFor(seed)
                            .stream()
                            .filter(p -> p.id().equals(lastPolicyId))
                            .findFirst()
                            .ifPresent(policy -> ctx.scheduler().onResult(seed, policy, verdict));
                }

            } catch (Exception e) {
                // Never let a worker die from a single iteration failure
                System.err.println("[Worker] Error in fuzzing loop: " + e.getMessage());
            }
        }
    }
}