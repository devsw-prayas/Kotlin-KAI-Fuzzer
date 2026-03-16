package io.kai.corpus;

import io.kai.compiler.coverage.CoverageSnapshot;

public record CorpusMeta(
        long structuralHash,
        CoverageSnapshot coverageSnapshot,
        long addedAt,
        int hitCount
) {
    public static CorpusMeta fresh(long hash) {
        return new CorpusMeta(hash, null, System.currentTimeMillis(), 0);
    }
}
