package io.kai.artifact;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.kai.contracts.IBuilder;
import io.kai.compiler.CompilerResult;
import io.kai.compiler.OracleVerdict;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

public class ArtifactStore {
    private final Path outputDir;
    private final ObjectMapper mapper;
    private final AtomicInteger counter = new AtomicInteger(0);

    public ArtifactStore(Path outputDir) throws IOException {
        this.outputDir = outputDir;
        this.mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        Files.createDirectories(outputDir);
    }

    public void save(IBuilder program, CompilerResult result, OracleVerdict.Finding verdict) {
        try {
            String dirName = String.format("crash_%04d", counter.getAndIncrement());
            Path crashDir = outputDir.resolve(dirName);
            Files.createDirectories(crashDir);

            String source = program.build(0);
            Files.writeString(crashDir.resolve("program.kt"), source);

            Files.writeString(crashDir.resolve("stderr.log"), result.stderr());

            if (result.chainLog() != null) {
                mapper.writeValue(crashDir.resolve("chain.json").toFile(), result.chainLog());
            }

            mapper.writeValue(crashDir.resolve("verdict.json").toFile(), verdict);

        } catch (IOException e) {
            System.err.println("[ArtifactStore] Failed to save artifact: " + e.getMessage());
        }
    }
}