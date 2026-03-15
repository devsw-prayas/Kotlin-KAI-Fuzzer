package io.kai.mutation;

import io.kai.contracts.NameRegistry;
import io.kai.contracts.TypeScope;

import java.util.Random;

public record MutationContext(Random rng, TypeScope scope, int depth, MutationStats stats, NameRegistry registry) { }
