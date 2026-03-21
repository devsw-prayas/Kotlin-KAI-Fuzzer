package io.kai.compiler.coverage;

import java.util.Set;

public record CoverageSnapshot(boolean isClean) {
    public static CoverageSnapshot empty() { return new CoverageSnapshot(false); }
    public static CoverageSnapshot clean() { return new CoverageSnapshot(true); }
    public boolean isClean() { return isClean; }
}