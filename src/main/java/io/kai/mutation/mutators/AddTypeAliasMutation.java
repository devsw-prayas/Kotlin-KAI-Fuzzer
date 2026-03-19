package io.kai.mutation.mutators;

import io.kai.builders.ProgramBuilder;
import io.kai.builders.TypeAliasBuilder;
import io.kai.contracts.IBuilder;
import io.kai.mutation.IMutationPolicy;
import io.kai.mutation.MutationContext;

import java.util.Set;

public class AddTypeAliasMutation implements IMutationPolicy {
    private static final String[] TARGET_TYPES = {
        "List<Int>", "Map<String, Int>", "Set<String>",
        "List<Map<String, Int>>", "Map<String, List<Int>>",
        "() -> Unit", "(Int) -> Boolean", "suspend () -> Unit"
    };

    @Override public Set<Class<? extends IBuilder>> targetTypes() { return Set.of(ProgramBuilder.class); }
    @Override public String id() { return "add_type_alias"; }
    @Override public boolean compatibleWith(IBuilder b) { return b instanceof ProgramBuilder; }
    @Override public IBuilder apply(IBuilder builder, MutationContext ctx) {
        String targetType = TARGET_TYPES[ctx.rng().nextInt(TARGET_TYPES.length)];
        String aliasName = "Alias_" + ctx.registry().next("alias");
        TypeAliasBuilder alias = new TypeAliasBuilder(builder.getRegistry(), aliasName, targetType);
        ((ProgramBuilder) builder).addChild(alias);
        return builder;
    }
}
