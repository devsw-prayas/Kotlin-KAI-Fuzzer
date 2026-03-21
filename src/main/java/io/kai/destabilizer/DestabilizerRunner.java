package io.kai.destabilizer;

import io.kai.artifact.ArtifactStore;
import io.kai.artifact.FindingDeduplicator;
import io.kai.compiler.CompilerResult;
import io.kai.compiler.CompilerRunner;
import io.kai.compiler.IOracle;
import io.kai.compiler.OracleVerdict;
import io.kai.contracts.IBuilder;
import io.kai.fuzzer.FuzzerRuntime;
import io.kai.minimize.IMinimizer;

import java.util.List;
import java.util.Random;


public class DestabilizerRunner {

    private final List<IDestabilizer> destabilizers;
    private final CompilerRunner compiler;
    private final IOracle oracle;
    private final IMinimizer minimizer;
    private final ArtifactStore store;
    private final FindingDeduplicator deduplicator;

    public DestabilizerRunner(
            List<IDestabilizer> destabilizers,
            CompilerRunner compiler,
            IOracle oracle,
            IMinimizer minimizer,
            ArtifactStore store,
            FindingDeduplicator deduplicator
    ) {
        this.destabilizers = destabilizers;
        this.compiler = compiler;
        this.oracle = oracle;
        this.minimizer = minimizer;
        this.store = store;
        this.deduplicator = deduplicator;
    }


    public boolean run(IBuilder program, Random rng) {
        // Filter to destabilizers that can apply to this program
        List<IDestabilizer> applicable = destabilizers.stream()
                .filter(d -> d.canApply(program))
                .toList();

        if (applicable.isEmpty()) return false;

        // Pick a random applicable destabilizer
        IDestabilizer chosen = applicable.get(rng.nextInt(applicable.size()));

        // Apply in-place — destabilize mutates the tree directly
        // The caller is responsible for passing a copy if needed
        chosen.destabilize(program, rng);

        // Emit and compile
        String source = program.build(0);
        try {
            System.out.println("[DESTAB] chosen=" + chosen.id() + " flags=" + chosen.requiredFlags());
            CompilerResult result = compiler.compile(source, null,
                    FuzzerRuntime.get().globalFlags());
            OracleVerdict verdict = oracle.evaluate(result);

            if (verdict instanceof OracleVerdict.Finding f) {
                if (deduplicator.isNew(result.stderr())) {
                    IBuilder minimal = minimizer.minimize(program, f, oracle, compiler);
                    store.save(minimal, result, f);
                    System.out.println("[DESTABILIZER] " + chosen.id()
                            + " → FINDING: " + f.type() + " — " + f.description());
                    return true;
                } else {
                    System.out.println("[DESTABILIZER] " + chosen.id() + " → duplicate finding");
                }
            } else {
                System.out.println("[DESTABILIZER] " + chosen.id() + " → clean");
            }
        } catch (Exception e) {
            System.err.println("[DESTABILIZER] compile error: " + e.getMessage());
        }

        return false;
    }

    public List<IDestabilizer> getDestabilizers() {
        return destabilizers;
    }
}
