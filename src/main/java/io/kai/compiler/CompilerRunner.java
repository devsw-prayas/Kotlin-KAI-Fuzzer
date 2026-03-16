package io.kai.compiler;

import io.kai.compiler.coverage.ICoverageCollector;
import io.kai.mutation.chain.MutationChainLog;
import io.kai.compiler.CompilerResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class CompilerRunner {
    private final Path kotlincPath;
    private final long timeoutMs;
    private final ICoverageCollector coverageCollector;

    public CompilerRunner(Path kotlincPath, long timeoutMs, ICoverageCollector coverageCollector) {
        this.kotlincPath = kotlincPath;
        this.timeoutMs = timeoutMs;
        this.coverageCollector = coverageCollector;
    }

    public CompilerResult compile(String source, MutationChainLog chainLog) throws IOException {
        Path tmpFile = writeTempFile(source);

        ProcessBuilder pb = new ProcessBuilder(kotlincPath.toString(), tmpFile.toString());
        pb.redirectErrorStream(false);
        coverageCollector.attach(pb);

        long start = System.currentTimeMillis();
        Process process = pb.start();

        boolean timedOut = false;
        try {
            timedOut = !process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (timedOut) process.destroyForcibly();

        long duration = System.currentTimeMillis() - start;

        String stdout = new String(process.getInputStream().readAllBytes());
        String stderr = new String(process.getErrorStream().readAllBytes());
        int exitCode = timedOut ? -1 : process.exitValue();

        // Clean up temp file
        Files.deleteIfExists(tmpFile);

        return new CompilerResult(exitCode, stdout, stderr, duration, timedOut, source, chainLog);
    }

    private Path writeTempFile(String source) throws IOException {
        Path tmpDir = Files.createTempDirectory("kai_");
        Path tmpFile = tmpDir.resolve("program.kt");
        Files.writeString(tmpFile, source);
        return tmpFile;
    }
}