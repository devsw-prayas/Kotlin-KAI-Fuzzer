package io.kai.compiler.coverage;

import java.util.Set;

public record CoverageSnapshot(Set<Long> coveredEdges, long timestamp) {
    public static CoverageSnapshot empty(){
        return new CoverageSnapshot(Set.of(), System.currentTimeMillis());
    }
}
