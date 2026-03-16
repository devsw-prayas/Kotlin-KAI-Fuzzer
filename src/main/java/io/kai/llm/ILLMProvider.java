package io.kai.llm;

import io.kai.contracts.IBuilder;

import java.util.List;

public interface ILLMProvider {
    List<String> generateSeedPrograms(int count, SeedHints hints);
    List<MutationSuggestion> suggestMutations(String treeDescription);
    List<SeedScore> rankSeeds(List<IBuilder> programs);
}