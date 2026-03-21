package io.kai.mutation.mutators;

import io.kai.builders.FunctionBuilder;
import io.kai.builders.VariableBuilder;
import io.kai.builders.expressions.IntLiteralBuilder;
import io.kai.contracts.IBuilder;
import io.kai.mutation.IMutationPolicy;
import io.kai.mutation.MutationContext;
import io.kai.mutation.MutationUtility;

import java.util.Set;

// Adds: inline fun <reified T: Any> fun_0() { val cls = T::class.java }
public class AddReifiedNewInstanceMutation implements IMutationPolicy {
    @Override public Set<Class<? extends IBuilder>> targetTypes() { return Set.of(FunctionBuilder.class); }
    @Override public String id() { return "add_reified_new_instance"; }
    @Override public boolean compatibleWith(IBuilder b) { return b instanceof FunctionBuilder; }
    @Override public IBuilder apply(IBuilder builder, MutationContext ctx) {
        FunctionBuilder fn = (FunctionBuilder) builder;
        fn.setInline(true);
        String tName = ctx.registry().next("T");
        fn.addBoundedTypeParam(tName, "reified");
        // emit: val cls_0 = T::class.java
        var lit = new IntLiteralBuilder(ctx.registry(), tName + "::class.java");
        var clsVar = new VariableBuilder(ctx.registry(), false, lit, false, "Class<" + tName + ">");
        MutationUtility.addChildSmart(fn, clsVar);
        return fn;
    }
}
