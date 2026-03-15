package io.kai.mutation.mutators;

import io.kai.builders.ExpressionBuilder;
import io.kai.contracts.IBuilder;
import io.kai.mutation.IMutationPolicy;
import io.kai.mutation.MutationContext;

import java.util.Set;

public class ExpandExpressionMutation implements IMutationPolicy {

    private static final String[] OPERATORS = {"+", "-", "*", "/", ">", "<", "=="};

    @Override
    public Set<Class<? extends IBuilder>> targetTypes() {
        return Set.of(ExpressionBuilder.class);
    }

    @Override
    public String id() {
        return "expand_expression";
    }

    @Override
    public boolean compatibleWith(IBuilder builder) {
        if (!(builder instanceof ExpressionBuilder expr)) return false;
        return switch (expr.getType()) {
            case INT_LITERAL, BOOL_LITERAL, STRING_LITERAL -> true;
            default -> false;
        };
    }

    @Override
    public IBuilder apply(IBuilder builder, MutationContext ctx) {
        ExpressionBuilder original = (ExpressionBuilder) builder;
        String operator = OPERATORS[ctx.rng().nextInt(OPERATORS.length)];

        ExpressionBuilder left = new ExpressionBuilder(
                ctx.registry(),
                original.getType(),
                original.getValue()
        );

        ExpressionBuilder right = new ExpressionBuilder(
                ctx.registry(),
                ExpressionBuilder.ExpressionType.INT_LITERAL,
                String.valueOf(ctx.rng().nextInt(100))
        );

        return new ExpressionBuilder(ctx.registry(), operator, left, right);
    }
}