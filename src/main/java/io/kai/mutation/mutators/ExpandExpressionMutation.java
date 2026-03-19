package io.kai.mutation.mutators;

import io.kai.builders.expressions.*;
import io.kai.contracts.IBuilder;
import io.kai.contracts.capability.IExpressionBuilder;
import io.kai.mutation.IMutationPolicy;
import io.kai.mutation.MutationContext;

import java.util.Set;

public class ExpandExpressionMutation implements IMutationPolicy {

    private static final String[] OPERATORS = {"+", "-", "*", "/", ">", "<", "=="};

    @Override
    public Set<Class<? extends IBuilder>> targetTypes() {
        return Set.of(IntLiteralBuilder.class, BoolLiteralBuilder.class, StringLiteralBuilder.class);
    }

    @Override
    public String id() {
        return "expand_expression";
    }

    @Override
    public boolean compatibleWith(IBuilder builder) {
        return builder instanceof IntLiteralBuilder
                || builder instanceof BoolLiteralBuilder
                || builder instanceof StringLiteralBuilder;
    }

    @Override
    public IBuilder apply(IBuilder builder, MutationContext ctx) {
        String operator = OPERATORS[ctx.rng().nextInt(OPERATORS.length)];

        IExpressionBuilder left = switch (builder) {
            case IntLiteralBuilder b -> new IntLiteralBuilder(builder.getRegistry(), b.getValue());
            case BoolLiteralBuilder b -> new BoolLiteralBuilder(builder.getRegistry(), b.getValue());
            case StringLiteralBuilder b -> new StringLiteralBuilder(builder.getRegistry(), b.getValue());
            default -> new IntLiteralBuilder(builder.getRegistry(), "0");
        };

        IExpressionBuilder right = new IntLiteralBuilder(
                builder.getRegistry(),
                String.valueOf(ctx.rng().nextInt(100))
        );

        return new BinaryOpBuilder(builder.getRegistry(), operator, left, right);
    }
}