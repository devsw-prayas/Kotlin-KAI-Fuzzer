package io.kai.mutation.mutators;

import io.kai.builders.ClassBuilder;
import io.kai.builders.FunctionBuilder;
import io.kai.builders.expressions.IntLiteralBuilder;
import io.kai.contracts.IBuilder;
import io.kai.contracts.Parameter;
import io.kai.mutation.IMutationPolicy;
import io.kai.mutation.MutationContext;

import java.util.Set;
import java.util.stream.Collectors;

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
    @Override public boolean compatibleWith(IBuilder b) { return b instanceof ClassBuilder; }

    @Override
    public IBuilder apply(IBuilder builder, MutationContext ctx) {
        ClassBuilder cb = (ClassBuilder) builder;
        OpSpec op = OPERATORS[ctx.rng().nextInt(OPERATORS.length)];

        FunctionBuilder fn = new FunctionBuilder(builder.getRegistry());
        fn.setOperator(true);
        fn.setReturnType(op.returnType());
        fn.setOperatorName(op.name());

        if (op.isBinary()) {
            // Build full parameterized type — class_0<T_0, T_1> not bare class_0
            String paramType = cb.id();
            if (!cb.getTypeParams().isEmpty()) {
                paramType += "<" + String.join(", ", cb.getTypeParams().keySet()) + ">";
            }
            fn.addParam(Parameter.simple("other", paramType));
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