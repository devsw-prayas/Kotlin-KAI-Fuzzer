package io.kai.mutation.mutators;

import io.kai.builders.FunctionBuilder;
import io.kai.builders.expressions.IntLiteralBuilder;
import io.kai.builders.VariableBuilder;
import io.kai.contracts.IBuilder;
import io.kai.mutation.IMutationPolicy;
import io.kai.mutation.MutationContext;

import java.util.Set;

// Adds: inline fun <reified T> fun_0() { val x = (42 is T) }
public class AddReifiedClassCheckMutation implements IMutationPolicy {
    @Override public Set<Class<? extends IBuilder>> targetTypes() { return Set.of(FunctionBuilder.class); }
    @Override public String id() { return "add_reified_class_check"; }
    @Override public boolean compatibleWith(IBuilder b) { return b instanceof FunctionBuilder; }
    @Override public IBuilder apply(IBuilder builder, MutationContext ctx) {
        FunctionBuilder fn = (FunctionBuilder) builder;
        fn.setInline(true);
        String tName = ctx.registry().next("T");
        fn.addBoundedTypeParam(tName, "reified");
        var lit = new IntLiteralBuilder(ctx.registry(), "(42 is " + tName + ")");
        var checkVar = new VariableBuilder(ctx.registry(), false, lit, false, "Boolean");
        fn.addChild(checkVar);
        return fn;
    }
}
