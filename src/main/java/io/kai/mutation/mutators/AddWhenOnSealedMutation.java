package io.kai.mutation.mutators;

import io.kai.builders.FunctionBuilder;
import io.kai.builders.SealedClassBuilder;
import io.kai.builders.WhenBuilder;
import io.kai.builders.expressions.FunctionCallBuilder;
import io.kai.builders.expressions.NullLiteralBuilder;
import io.kai.contracts.IBuilder;
import io.kai.contracts.Parameter;
import io.kai.mutation.IMutationPolicy;
import io.kai.mutation.MutationContext;
import io.kai.mutation.MutationUtility;
import io.kai.mutation.ScopeContextBuilder;
import io.kai.mutation.context.SymbolTable;

import java.util.List;
import java.util.Set;

// Adds a when expression over a sealed class if one exists in scope
public class AddWhenOnSealedMutation implements IMutationPolicy {
    @Override public Set<Class<? extends IBuilder>> targetTypes() { return Set.of(FunctionBuilder.class); }
    @Override public String id() { return "add_when_on_sealed"; }
    @Override
    public boolean compatibleWith(IBuilder b) {
        if (!(b instanceof FunctionBuilder fn)) return false;
        return !fn.isOperator();
    }
    @Override
    public IBuilder apply(IBuilder builder, MutationContext ctx) {
        FunctionBuilder fn = (FunctionBuilder) builder;
        var scope = ScopeContextBuilder.buildFor(ctx.root(), builder.id());
        List<SymbolTable.ClassMeta> sealed = scope.symbols().getSealedClasses();
        if (sealed.isEmpty()) return builder;

        SymbolTable.ClassMeta target = sealed.get(ctx.rng().nextInt(sealed.size()));

        // Find existing param of sealed type, or add one
        String paramName = fn.getParameters().stream()
                .filter(p -> p.type().equals(target.name()))
                .map(Parameter::name)
                .findFirst()
                .orElse(null);

        if (paramName == null) {
            paramName = fn.getRegistry().next("x");
            fn.addParam(io.kai.contracts.Parameter.simple(paramName, target.name()));
        }

        var subject = new io.kai.builders.expressions.VariableRefBuilder(
                fn.getRegistry(), paramName, target.name());

        WhenBuilder when = new WhenBuilder(fn.getRegistry(), subject);
        when.addBranch(new WhenBuilder.WhenBranch(
                new NullLiteralBuilder(fn.getRegistry()),
                List.of(),
                true
        ));
        MutationUtility.addChildSmart(fn, when);
        return builder;
    }
}
