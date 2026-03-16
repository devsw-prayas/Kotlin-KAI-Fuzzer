package io.kai.llm;

public record MutationSuggestion(
        String description,
        String targetNodeType,
        String rationale
) {}