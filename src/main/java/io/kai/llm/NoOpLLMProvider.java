package io.kai.llm;

import io.kai.contracts.IBuilder;

import java.util.List;

public class NoOpLLMProvider implements ILLMProvider {

    @Override
    public List<String> generateSeedPrograms(int count, SeedHints hints) {
        return List.of();
    }

    @Override
    public List<MutationSuggestion> suggestMutations(String treeDescription) {
        return List.of();
    }

    @Override
    public List<SeedScore> rankSeeds(List<IBuilder> programs) {
        return List.of();
    }
}