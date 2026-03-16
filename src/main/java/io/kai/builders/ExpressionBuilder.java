package io.kai.builders;

import io.kai.contracts.*;
import io.kai.contracts.capability.ILocalScopeBuilder;

import java.util.List;

public class ExpressionBuilder implements ILocalScopeBuilder {

    public enum ExpressionType {
        INT_LITERAL,
        STRING_LITERAL,
        BOOL_LITERAL,
        NULL_LITERAL,
        BINARY_OP,
        FUNCTION_CALL
    }

    private final String id;
    private final NameRegistry registry;
    private final ExpressionType type;
    private final String value;           // literal value or operator symbol
    private final ExpressionBuilder left;  // only for BINARY_OP
    private final ExpressionBuilder right; // only for BINARY_OP

    // Constructor for literals and function calls
    public ExpressionBuilder(NameRegistry registry, ExpressionType type, String value) {
        this.registry = registry;
        this.id = registry.next("expr");
        this.type = type;
        this.value = value;
        this.left = null;
        this.right = null;
    }

    // Constructor for binary ops
    public ExpressionBuilder(NameRegistry registry, String operator,
                             ExpressionBuilder left, ExpressionBuilder right) {
        this.registry = registry;
        this.id = registry.next("expr");
        this.type = ExpressionType.BINARY_OP;
        this.value = operator;
        this.left = left;
        this.right = right;
    }

    // Private constructor for withoutChild
    private ExpressionBuilder(NameRegistry registry, String id, ExpressionType type,
                              String value, ExpressionBuilder left, ExpressionBuilder right) {
        this.registry = registry;
        this.id = id;
        this.type = type;
        this.value = value;
        this.left = left;
        this.right = right;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String build(BuildContext ctx) {
        return switch (type) {
            case INT_LITERAL    -> value;
            case STRING_LITERAL -> "\"" + value + "\"";
            case BOOL_LITERAL   -> value;
            case NULL_LITERAL   -> "null";
            case FUNCTION_CALL  -> value + "()";
            case BINARY_OP      -> "(" + left.build(ctx) + " " + value + " " + right.build(ctx) + ")";
        };
    }

    @Override
    public List<? extends IBuilder> children() {
        if (type == ExpressionType.BINARY_OP && left != null && right != null) {
            return List.of(left, right);
        }
        return List.of();
    }

    @Override
    public void accept(IBuilderVisitor visitor) {
        visitor.visit(this);
        for (var child : children()) {
            child.accept(visitor);
        }
    }

    @Override
    public IBuilder withoutChild(IBuilder builder) {
        // Removing a child from a binary op degrades it to a null literal
        if (type == ExpressionType.BINARY_OP) {
            if (builder.equals(left)) {
                return new ExpressionBuilder(registry, id, ExpressionType.NULL_LITERAL,
                        "null", null, null);
            }
            if (builder.equals(right)) {
                return new ExpressionBuilder(registry, id, ExpressionType.NULL_LITERAL,
                        "null", null, null);
            }
        }
        return this; // leaf nodes have no children to remove
    }

    // Getters for use by mutation policies
    public ExpressionType getType() { return type; }
    public String getValue() { return value; }
    public ExpressionBuilder getLeft() { return left; }
    public ExpressionBuilder getRight() { return right; }

    @Override
    public NameRegistry getRegistry() {
        return registry;
    }
}