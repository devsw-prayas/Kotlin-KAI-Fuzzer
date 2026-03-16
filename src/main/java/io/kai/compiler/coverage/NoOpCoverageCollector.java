package io.kai.compiler.coverage;

import io.kai.compiler.CompilerResult;

public class NoOpCoverageCollector implements ICoverageCollector{
    @Override
    public ProcessBuilder attach(ProcessBuilder builder) {
        return builder;
    }

    @Override
    public CoverageSnapshot collect(CompilerResult result) {
        return CoverageSnapshot.empty();
    }

    @Override
    public boolean isNovel(CoverageSnapshot snapshot) {
        return false;
    }
}
