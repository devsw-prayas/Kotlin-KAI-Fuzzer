package io.kai.llm;

import java.util.List;

public record SeedHints(
        List<String> targetFeatures,
        List<String> avoidPatterns
) {}