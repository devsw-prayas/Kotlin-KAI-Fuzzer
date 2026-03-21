package io.kai.mutation.mutators;

import io.kai.builders.FunctionBuilder;
import io.kai.builders.VariableBuilder;
import io.kai.builders.expressions.IntLiteralBuilder;
import io.kai.builders.expressions.NullLiteralBuilder;
import io.kai.contracts.IBuilder;
import io.kai.mutation.IMutationPolicy;
import io.kai.mutation.MutationContext;
import io.kai.mutation.MutationUtility;
import io.kai.mutation.ScopeContextBuilder;
import io.kai.mutation.context.ScopeContext;

import java.util.List;
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
    @Override
    public IBuilder apply(IBuilder builder, MutationContext ctx) {
        FunctionBuilder fn = (FunctionBuilder) builder;
        ScopeContext scope = ScopeContextBuilder.buildFor(ctx.root(), builder.id());
        List<String> typeParams = scope.getTypeParams();

        // Pick a base deep type and inject in-scope type params where possible
        String deepType = DEEP_TYPES[ctx.rng().nextInt(DEEP_TYPES.length)];

        // If there are in-scope type params, substitute one in for a concrete type
        if (!typeParams.isEmpty()) {
            String tp = typeParams.stream()
                    .filter(t -> !t.equals("Boolean") && !t.equals("Unit"))
                    .findFirst().orElse(null);
            if (tp != null) {
                deepType = deepType
                        .replaceFirst("\\bInt\\b", tp)
                        .replaceFirst("\\bString\\b", tp);
            }
        }

        var castLit = new IntLiteralBuilder(builder.getRegistry(), "null as? " + deepType);
        var typedVar = new VariableBuilder(builder.getRegistry(), false, castLit, true, deepType);
        MutationUtility.addChildSmart(fn, typedVar);
        return fn;
    }
}
