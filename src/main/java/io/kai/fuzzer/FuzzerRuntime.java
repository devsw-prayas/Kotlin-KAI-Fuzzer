package io.kai.fuzzer;

import io.kai.builders.BranchBuilder;
import io.kai.builders.ClassBuilder;
import io.kai.builders.FunctionBuilder;
import io.kai.builders.LoopBuilder;
import io.kai.builders.expressions.*;
import io.kai.contracts.IBuilder;
import io.kai.contracts.NameRegistry;
import io.kai.mutation.IMutationPolicy;
import io.kai.mutation.MutationRegistry;
import io.kai.mutation.mutators.*;

import java.util.*;

public final class FuzzerRuntime {
    private final List<IMutationPolicy> policies;
    private final Map<IBuilder, Double> prototypeWeights;
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

        // Tier 1
        policies.add(new AddReifiedInlineMutation());
        policies.add(new AddCrossinlineMutation());
        policies.add(new AddNoinlineMutation());
        policies.add(new AddReifiedClassCheckMutation());
        policies.add(new AddReifiedNewInstanceMutation());

        // Tier 2
        policies.add(new AddRecursiveGenericBoundMutation());
        policies.add(new AddDeepGenericNestingMutation());
        policies.add(new AddSelfReferentialTypeAliasMutation());
        policies.add(new AddContravariantBoundMutation());
        policies.add(new AddMultipleUpperBoundMutation());

        // Tier 3-5
        policies.add(new AddSuspendFunctionMutation());
        policies.add(new AddSealedClassMutation());
        policies.add(new WrapInTryCatchMutation());
        policies.add(new AddTypeAliasMutation());
        policies.add(new AddCompanionObjectMutation());
        policies.add(new AddLambdaMutation());
        policies.add(new AddOperatorOverloadMutation());
        policies.add(new AddWhenOnSealedMutation());
    }

    private void initBuilderWeights() {
        NameRegistry dummy = new NameRegistry();

        // FunctionBuilder configurations
        FunctionBuilder reifiedInline = new FunctionBuilder(dummy);
        reifiedInline.setInline(true);
        reifiedInline.addBoundedTypeParam("T", "reified");
        prototypeWeights.put(reifiedInline, 0.95);

        FunctionBuilder suspendGeneric = new FunctionBuilder(dummy);
        suspendGeneric.setSuspend(true);
        suspendGeneric.addBoundedTypeParam("T", "");
        prototypeWeights.put(suspendGeneric, 0.88);

        FunctionBuilder inlineGeneric = new FunctionBuilder(dummy);
        inlineGeneric.setInline(true);
        inlineGeneric.addBoundedTypeParam("T", "");
        prototypeWeights.put(inlineGeneric, 0.85);

        FunctionBuilder suspend = new FunctionBuilder(dummy);
        suspend.setSuspend(true);
        prototypeWeights.put(suspend, 0.82);

        FunctionBuilder generic = new FunctionBuilder(dummy);
        generic.addBoundedTypeParam("T", "");
        prototypeWeights.put(generic, 0.75);

        FunctionBuilder operator = new FunctionBuilder(dummy);
        operator.setOperator(true);
        prototypeWeights.put(operator, 0.65);

        FunctionBuilder plain = new FunctionBuilder(dummy);
        prototypeWeights.put(plain, 0.55);

        // ClassBuilder configurations
        ClassBuilder sealedGeneric = new ClassBuilder(dummy);
        sealedGeneric.setSealed(true);
        sealedGeneric.addBoundedTypeParam("T", "");
        prototypeWeights.put(sealedGeneric, 0.88);

        ClassBuilder sealed = new ClassBuilder(dummy);
        sealed.setSealed(true);
        prototypeWeights.put(sealed, 0.80);

        ClassBuilder dataGeneric = new ClassBuilder(dummy);
        dataGeneric.setData(true);
        dataGeneric.addBoundedTypeParam("T", "");
        prototypeWeights.put(dataGeneric, 0.72);

        ClassBuilder genericClass = new ClassBuilder(dummy);
        genericClass.addBoundedTypeParam("T", "");
        prototypeWeights.put(genericClass, 0.70);

        ClassBuilder data = new ClassBuilder(dummy);
        data.setData(true);
        prototypeWeights.put(data, 0.65);

        ClassBuilder plainClass = new ClassBuilder(dummy);
        prototypeWeights.put(plainClass, 0.60);

        // Other builders — class match only, no flags
        prototypeWeights.put(new LambdaBuilder(dummy), 0.68);
        prototypeWeights.put(new BranchBuilder(dummy, new NullLiteralBuilder(dummy),
                List.of(), List.of()), 0.45);
        prototypeWeights.put(new LoopBuilder(dummy,
                LoopBuilder.LoopType.WHILE, new NullLiteralBuilder(dummy)), 0.35);
        prototypeWeights.put(new BinaryOpBuilder(dummy, "+",
                new NullLiteralBuilder(dummy), new NullLiteralBuilder(dummy)), 0.30);
        prototypeWeights.put(new VariableRefBuilder(dummy, "x", "Int"), 0.25);
        prototypeWeights.put(new IntLiteralBuilder(dummy, "0"), 0.15);
        prototypeWeights.put(new StringLiteralBuilder(dummy, ""), 0.15);
        prototypeWeights.put(new BoolLiteralBuilder(dummy, "true"), 0.15);
        prototypeWeights.put(new NullLiteralBuilder(dummy), 0.10);
    }

    private void initMutationNastiness() {
        // Tier 1 — reified/inline (nastiest)
        mutationNastiness.put("add_reified_inline", 0.10);
        mutationNastiness.put("add_crossinline", 0.12);
        mutationNastiness.put("add_noinline", 0.15);
        mutationNastiness.put("add_reified_class_check", 0.13);
        mutationNastiness.put("add_reified_new_instance", 0.11);

        // Tier 2 — recursive generics
        mutationNastiness.put("add_recursive_generic_bound", 0.15);
        mutationNastiness.put("add_deep_generic_nesting", 0.18);
        mutationNastiness.put("add_self_referential_typealias", 0.13);
        mutationNastiness.put("add_contravariant_bound", 0.20);
        mutationNastiness.put("add_multiple_upper_bound", 0.22);

        // Tier 3 — coroutines
        mutationNastiness.put("add_suspend_function", 0.25);

        // Tier 4 — type system
        mutationNastiness.put("add_type_alias", 0.35);

        // Tier 5 — sealed/when
        mutationNastiness.put("add_sealed_class", 0.28);
        mutationNastiness.put("add_when_on_sealed", 0.30);

        // Tier 6 — operators
        mutationNastiness.put("add_operator_overload", 0.45);

        // Tier 7 — lambda
        mutationNastiness.put("add_lambda", 0.50);

        // Misc
        mutationNastiness.put("wrap_in_try_catch", 0.35);
        mutationNastiness.put("add_companion_object", 0.55);

        // MVP-1 originals
        mutationNastiness.put("add_variable", 0.80);
        mutationNastiness.put("add_loop", 0.70);
        mutationNastiness.put("expand_expression", 0.75);
        mutationNastiness.put("add_function", 0.72);
        mutationNastiness.put("add_generic", 0.60);
        mutationNastiness.put("inject_null_check", 0.65);
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
        prototypeWeights = new LinkedHashMap<>();
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

    public double builderWeight(IBuilder type) {
        return prototypeWeights.getOrDefault(type, 0.5);
    }

    public double nastiness(String policyId) {
        return mutationNastiness.getOrDefault(policyId, 0.5);
    }

    public List<IMutationPolicy> policies() {
        return policies;
    }
}