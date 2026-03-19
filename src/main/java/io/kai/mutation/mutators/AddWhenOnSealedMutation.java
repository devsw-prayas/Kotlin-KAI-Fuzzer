package io.kai.mutation.mutators;

import io.kai.builders.FunctionBuilder;
import io.kai.builders.SealedClassBuilder;
import io.kai.builders.WhenBuilder;
import io.kai.builders.expressions.FunctionCallBuilder;
import io.kai.builders.expressions.NullLiteralBuilder;
import io.kai.contracts.IBuilder;
import io.kai.mutation.IMutationPolicy;
import io.kai.mutation.MutationContext;
import io.kai.mutation.ScopeContextBuilder;
import io.kai.mutation.context.SymbolTable;

import java.util.List;
import java.util.Set;

// Adds a when expression over a sealed class if one exists in scope
public class AddWhenOnSealedMutation implements IMutationPolicy {
    @Override public Set<Class<? extends IBuilder>> targetTypes() { return Set.of(FunctionBuilder.class); }
    @Override public String id() { return "add_when_on_sealed"; }
    @Override public boolean compatibleWith(IBuilder b) { return b instanceof FunctionBuilder; }
    @Override public IBuilder apply(IBuilder builder, MutationContext ctx) {
        FunctionBuilder fn = (FunctionBuilder) builder;
        var scope = ScopeContextBuilder.buildFor(ctx.root(), builder.id());
        List<SymbolTable.ClassMeta> sealed = scope.symbols().getSealedClasses();
        if (sealed.isEmpty()) return builder; // no sealed class in scope — skip

        SymbolTable.ClassMeta target = sealed.get(ctx.rng().nextInt(sealed.size()));
        var subject = new FunctionCallBuilder(builder.getRegistry(), target.name());
        WhenBuilder when = new WhenBuilder(builder.getRegistry(), subject);

        // Add else branch
        when.addBranch(new WhenBuilder.WhenBranch(
                new NullLiteralBuilder(builder.getRegistry()),
                List.of(),
                true
        ));
        fn.addChild(when);
        return builder;
    }
}
