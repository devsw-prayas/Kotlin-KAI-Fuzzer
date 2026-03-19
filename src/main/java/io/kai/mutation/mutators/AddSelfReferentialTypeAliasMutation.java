package io.kai.mutation.mutators;

import io.kai.builders.ProgramBuilder;
import io.kai.builders.TypeAliasBuilder;
import io.kai.contracts.IBuilder;
import io.kai.mutation.IMutationPolicy;
import io.kai.mutation.MutationContext;

import java.util.Set;

// Adds: typealias Recursive_0 = List<Recursive_0>
public class AddSelfReferentialTypeAliasMutation implements IMutationPolicy {
    @Override public Set<Class<? extends IBuilder>> targetTypes() { return Set.of(ProgramBuilder.class); }
    @Override public String id() { return "add_self_referential_typealias"; }
    @Override public boolean compatibleWith(IBuilder b) { return b instanceof ProgramBuilder; }
    @Override public IBuilder apply(IBuilder builder, MutationContext ctx) {
        String aliasName = "Recursive_" + ctx.registry().next("alias");
        var alias = new TypeAliasBuilder(builder.getRegistry(), aliasName, "List<" + aliasName + ">");
        builder.getRegistry(); // ensure registry is used
        ((ProgramBuilder) builder).addChild(alias);
        return builder;
    }
}
