package io.kai.compiler.coverage;

import io.kai.compiler.CompilerResult;

public interface ICoverageCollector {
    ProcessBuilder attach(ProcessBuilder builder);
    CoverageSnapshot collect(CompilerResult result);
    boolean isNovel(CoverageSnapshot snapshot);
}
