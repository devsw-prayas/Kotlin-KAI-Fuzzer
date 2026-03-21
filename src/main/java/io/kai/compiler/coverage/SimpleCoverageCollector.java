package io.kai.compiler.coverage;

import io.kai.compiler.CompilerResult;
import io.kai.contracts.IBuilder;
import io.kai.contracts.visitor.StructuralHasher;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SimpleCoverageCollector implements ICoverageCollector {

    private final Set<Long> seenHashes = Collections.synchronizedSet(new HashSet<>());

    @Override
    public ProcessBuilder attach(ProcessBuilder builder) {
        return builder; // no instrumentation needed
    }

    @Override
    public CoverageSnapshot collect(CompilerResult result) {
        // Use exit code as a proxy signal — clean compile is more interesting
        return result.exitCode() == 0
                ? CoverageSnapshot.clean()
                : CoverageSnapshot.empty();
    }

    @Override
    public boolean isNovel(CoverageSnapshot snapshot) {
        // Novel = clean compile we haven't structurally seen before
        // Actual hash check happens in FuzzerEngine via add() dedup
        return snapshot.isClean();
    }
}