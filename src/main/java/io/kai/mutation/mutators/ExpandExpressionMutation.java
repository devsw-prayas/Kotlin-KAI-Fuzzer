package io.kai.mutation.mutators;

import io.kai.builders.expressions.*;
import io.kai.contracts.IBuilder;
import io.kai.contracts.capability.IExpressionBuilder;
import io.kai.mutation.IMutationPolicy;
import io.kai.mutation.MutationContext;
import io.kai.mutation.MutationUtility;
import io.kai.mutation.ScopeContextBuilder;
import io.kai.mutation.context.ScopeContext;

import java.util.Set;

public class ExpandExpressionMutation implements IMutationPolicy {
    private static final String[] INT_OPERATORS  = {"+", "-", "*", "/"};
    private static final String[] BOOL_OPERATORS = {"==", "&&", "||", ">", "<"};
    private static final String[] STR_OPERATORS  = {"+"};
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
        ScopeContext scope = ScopeContextBuilder.buildFor(ctx.root(), builder.id());

        String operator = switch (builder) {
            case BoolLiteralBuilder b ->
                    BOOL_OPERATORS[ctx.rng().nextInt(BOOL_OPERATORS.length)];
            case StringLiteralBuilder b ->
                    STR_OPERATORS[ctx.rng().nextInt(STR_OPERATORS.length)];
            default ->
                    INT_OPERATORS[ctx.rng().nextInt(INT_OPERATORS.length)];
        };

        IExpressionBuilder left = switch (builder) {
            case IntLiteralBuilder b -> new IntLiteralBuilder(builder.getRegistry(), b.getValue());
            case BoolLiteralBuilder b -> new BoolLiteralBuilder(builder.getRegistry(), b.getValue());
            case StringLiteralBuilder b -> new StringLiteralBuilder(builder.getRegistry(), b.getValue());
            default -> new IntLiteralBuilder(builder.getRegistry(), "0");
        };

        // Pick var matching the left operand type
        String requiredType = switch (builder) {
            case BoolLiteralBuilder b -> "Boolean";
            case StringLiteralBuilder b -> "String";
            default -> "Int";
        };

        String varName = MutationUtility.pickVar(scope, ctx.rng(), requiredType);
        IExpressionBuilder right;
        if (varName != null) {
            right = new VariableRefBuilder(builder.getRegistry(), varName,
                    scope.getVars().getOrDefault(varName, requiredType));
        } else {
            // Fallback literal matching the required type
            right = switch (requiredType) {
                case "Boolean" -> new BoolLiteralBuilder(builder.getRegistry(), "false");
                case "String"  -> new StringLiteralBuilder(builder.getRegistry(), "\"\"");
                default        -> new IntLiteralBuilder(builder.getRegistry(),
                        String.valueOf(ctx.rng().nextInt(100)));
            };
        }

        return new BinaryOpBuilder(builder.getRegistry(), operator, left, right);
    }
}