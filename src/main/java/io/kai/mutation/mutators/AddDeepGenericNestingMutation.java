package io.kai.mutation.mutators;

import io.kai.builders.FunctionBuilder;
import io.kai.builders.VariableBuilder;
import io.kai.builders.expressions.NullLiteralBuilder;
import io.kai.contracts.IBuilder;
import io.kai.mutation.IMutationPolicy;
import io.kai.mutation.MutationContext;

import java.util.Set;

// Adds a variable with deeply nested generic type: Map<List<Set<Int>>, String>
public class AddDeepGenericNestingMutation implements IMutationPolicy {
    private static final String[] DEEP_TYPES = {
        "Map<List<Set<Int>>, String>",
        "List<Map<String, List<Int>>>",
        "Set<Map<List<String>, Map<Int, Boolean>>>",
        "Map<List<Map<String, Int>>, Set<List<Boolean>>>",
        "List<Set<Map<List<Int>, Map<String, Boolean>>>>"
    };

    @Override public Set<Class<? extends IBuilder>> targetTypes() { return Set.of(FunctionBuilder.class); }
    @Override public String id() { return "add_deep_generic_nesting"; }
    @Override public boolean compatibleWith(IBuilder b) { return b instanceof FunctionBuilder; }
    @Override public IBuilder apply(IBuilder builder, MutationContext ctx) {
        FunctionBuilder fn = (FunctionBuilder) builder;
        String deepType = DEEP_TYPES[ctx.rng().nextInt(DEEP_TYPES.length)];
        var nullLit = new NullLiteralBuilder(builder.getRegistry());
        var deepVar = new VariableBuilder(builder.getRegistry(), false, nullLit, true);
        // Override type via raw expression trick — emit "null as? DeepType"
        var castLit = new io.kai.builders.expressions.IntLiteralBuilder(
                builder.getRegistry(), "null as? " + deepType);
        var typedVar = new VariableBuilder(builder.getRegistry(), false, castLit, true);
        fn.addChild(typedVar);
        return fn;
    }
}
