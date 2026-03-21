package io.kai.mutation.mutators;

import io.kai.builders.ClassBuilder;
import io.kai.builders.FunctionBuilder;
import io.kai.builders.expressions.IntLiteralBuilder;
import io.kai.contracts.IBuilder;
import io.kai.contracts.Parameter;
import io.kai.mutation.IMutationPolicy;
import io.kai.mutation.MutationContext;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class AddOperatorOverloadMutation implements IMutationPolicy {

    private record OpSpec(String name, String returnType, boolean isBinary) {}

    private static final OpSpec[] OPERATORS = {
            new OpSpec("plus",       "Int",     true),
            new OpSpec("minus",      "Int",     true),
            new OpSpec("times",      "Int",     true),
            new OpSpec("div",        "Int",     true),
            new OpSpec("rem",        "Int",     true),
            new OpSpec("compareTo",  "Int",     true),
            new OpSpec("unaryMinus", "Int",     false),
            new OpSpec("not",        "Boolean", false),
    };

    @Override public Set<Class<? extends IBuilder>> targetTypes() { return Set.of(ClassBuilder.class); }
    @Override public String id() { return "add_operator_overload"; }
    @Override
    public boolean compatibleWith(IBuilder b) {
        if (!(b instanceof ClassBuilder cb)) return false;
        return Arrays.stream(OPERATORS)
                .anyMatch(op -> !cb.hasOperator(op.name()));
    }

    @Override
    public IBuilder apply(IBuilder builder, MutationContext ctx) {
        ClassBuilder cb = (ClassBuilder) builder;

        List<OpSpec> available = Arrays.stream(OPERATORS)
                .filter(op -> !cb.hasOperator(op.name()))
                .toList();
        if (available.isEmpty()) return builder;

        OpSpec op = available.get(ctx.rng().nextInt(available.size()));
        cb.registerOperator(op.name());

        FunctionBuilder fn = new FunctionBuilder(builder.getRegistry());
        fn.setOperator(true);
        fn.setReturnType(op.returnType());
        fn.setOperatorName(op.name());

        if (op.isBinary()) {
            fn.addParam(Parameter.ofClass("other", cb));
        }

        // Return statement — always last, never inside a block
        String returnVal = op.returnType().equals("Boolean") ? "true" : "0";
        fn.addChild(new ReturnLiteralBuilder(builder.getRegistry(), returnVal));

        cb.addChild(fn);
        return builder;
    }

    // Minimal builder that emits "return <value>"
    private static class ReturnLiteralBuilder extends IntLiteralBuilder {
        private final String value;
        ReturnLiteralBuilder(io.kai.contracts.NameRegistry r, String value) {
            super(r, value);
            this.value = value;
        }
        @Override
        public String build(int indentLevel) {
            return indent(indentLevel) + "return " + value;
        }
    }
}