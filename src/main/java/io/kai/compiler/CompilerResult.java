package io.kai.compiler;

import io.kai.mutation.chain.MutationChainLog;

public record CompilerResult(int exitCode, String stdout, String stderr, long durationMs, boolean timedOut,
                             String sourceProgram, MutationChainLog chainLog) {
}
