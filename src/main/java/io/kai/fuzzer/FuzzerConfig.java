package io.kai.fuzzer;

import java.nio.file.Path;

public record FuzzerConfig(int batchSize, int maxDepth, int threadCount,
                           long timeoutMs, Path logDir, Path kotlincPath, long maxIter) {
}
