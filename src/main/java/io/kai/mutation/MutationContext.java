package io.kai.mutation;

import io.kai.contracts.NameRegistry;
import io.kai.mutation.context.ScopeContext;

import java.util.Random;

public record MutationContext(Random rng, ScopeContext scope, int depth, MutationStats stats, NameRegistry registry) { }
