package io.kai.fuzzer;

import io.kai.contracts.IBuilder;
import io.kai.mutation.IMutationPolicy;
import io.kai.mutation.MutationRegistry;
import io.kai.mutation.mutators.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FuzzerRuntime {
    private final List<IMutationPolicy> policies;
    private final Map<Class<? extends IBuilder>, Double> builderWeights;
    private final Map<String, Double> mutationNastiness;
    private final MutationRegistry registry;
    private static final FuzzerRuntime INSTANCE = new FuzzerRuntime();

    private void initPolicies() {
        // MVP-1
        policies.add(new AddLoopMutation());
        policies.add(new AddVariableMutation());
        policies.add(new InjectNullCheckMutation());
        policies.add(new ExpandExpressionMutation());
        policies.add(new GenericMutation());
        policies.add(new AddFunctionMutation());
    }

    private void initBuilderWeights() {
        // MVP-2 values from spec
    }

    private void initMutationNastiness() {
        // MVP-2 values from spec
    }

    private void initRegistry() {
        for (IMutationPolicy policy : policies) {
            for (Class<? extends IBuilder> type : policy.targetTypes()) {
                registry.register(type, policy);
            }
        }
    }

    private FuzzerRuntime() {
        policies = new ArrayList<>();
        builderWeights = new HashMap<>();
        mutationNastiness = new HashMap<>();
        registry = new MutationRegistry();

        initPolicies();
        initBuilderWeights();
        initMutationNastiness();
        initRegistry();
    }

    public static FuzzerRuntime get() {
        return INSTANCE;
    }

    public MutationRegistry registry() {
        return registry;
    }

    public double builderWeight(Class<? extends IBuilder> type) {
        return builderWeights.getOrDefault(type, 0.5);
    }

    public double nastiness(String policyId) {
        return mutationNastiness.getOrDefault(policyId, 0.5);
    }

    public List<IMutationPolicy> policies() {
        return policies;
    }
}