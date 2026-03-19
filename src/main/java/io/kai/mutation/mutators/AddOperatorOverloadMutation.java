package io.kai.mutation.mutators;

import io.kai.builders.ClassBuilder;
import io.kai.builders.FunctionBuilder;
import io.kai.contracts.IBuilder;
import io.kai.contracts.Parameter;
import io.kai.mutation.IMutationPolicy;
import io.kai.mutation.MutationContext;

import java.util.Set;

// Adds an operator overload to a class
public class AddOperatorOverloadMutation implements IMutationPolicy {
    private static final String[][] OPERATORS = {
        {"plus", "Int"},
        {"minus", "Int"},
        {"times", "Int"},
        {"div", "Int"},
        {"rem", "Int"},
        {"compareTo", "Int"},
        {"contains", "Boolean"},
    };

    @Override public Set<Class<? extends IBuilder>> targetTypes() { return Set.of(ClassBuilder.class); }
    @Override public String id() { return "add_operator_overload"; }
    @Override public boolean compatibleWith(IBuilder b) { return b instanceof ClassBuilder; }
    @Override public IBuilder apply(IBuilder builder, MutationContext ctx) {
        ClassBuilder cb = (ClassBuilder) builder;
        String[] op = OPERATORS[ctx.rng().nextInt(OPERATORS.length)];
        FunctionBuilder fn = new FunctionBuilder(builder.getRegistry());
        fn.setOperator(true);
        fn.setReturnType(op[1]);
        fn.addParam(Parameter.simple("other", cb.id()));
        cb.addChild(fn);
        return builder;
    }
}
