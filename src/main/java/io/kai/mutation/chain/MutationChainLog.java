package io.kai.mutation.chain;

import java.util.Map;

public record MutationChainLog(
        String seedId,
        long rngSeed,
        Map<String, Integer> registrySnapshot,
        MutationChain chain,
        long timestamp
) {}