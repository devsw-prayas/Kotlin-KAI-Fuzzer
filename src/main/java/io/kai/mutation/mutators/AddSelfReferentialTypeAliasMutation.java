package io.kai.mutation.mutators;

import io.kai.builders.ProgramBuilder;
import io.kai.builders.TypeAliasBuilder;
import io.kai.contracts.IBuilder;
import io.kai.mutation.IMutationPolicy;
import io.kai.mutation.MutationContext;

import java.util.Set;

public class AddSelfReferentialTypeAliasMutation implements IMutationPolicy {

    private static final String[][] ALIAS_TEMPLATES = {
            {"MyList",   "List<T>"},
            {"MyMap",    "Map<T, String>"},
            {"MySet",    "Set<T>"},
            {"MyPair",   "Pair<T, T>"},
            {"Mapper",   "Function1<T, T>"},
            {"Predicate","Function1<T, Boolean>"},
    };

    @Override public Set<Class<? extends IBuilder>> targetTypes() { return Set.of(ProgramBuilder.class); }
    @Override public String id() { return "add_self_referential_typealias"; }
    @Override public boolean compatibleWith(IBuilder b) { return b instanceof ProgramBuilder; }

    @Override
    public IBuilder apply(IBuilder builder, MutationContext ctx) {
        ProgramBuilder pb = (ProgramBuilder) builder;
        String[] template = ALIAS_TEMPLATES[ctx.rng().nextInt(ALIAS_TEMPLATES.length)];
        String aliasName = template[0] + "_" + ctx.registry().next("alias");
        String targetType = template[1];
        // Emit: typealias MyList_alias_0<T> = List<T>
        var alias = new TypeAliasBuilder(builder.getRegistry(),
                aliasName + "<T>", targetType);
        pb.addChild(alias);
        return builder;
    }
}