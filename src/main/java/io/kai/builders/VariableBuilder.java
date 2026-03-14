package io.kai.builders;

import io.kai.contracts.*;

import java.util.List;

public class VariableBuilder implements ITopLevelBuilder, IMemberBuilder, ILocalScopeBuilder {

    private final String id;
    private final NameRegistry registry;
    private final boolean isMutable;
    private final String type;               // hardcoded "Int" for MVP
    private final ExpressionBuilder initializer;

    public VariableBuilder(NameRegistry registry, boolean isMutable, ExpressionBuilder initializer) {
        this.registry = registry;
        this.id = registry.next("var");
        this.isMutable = isMutable;
        this.type = "Int";
        this.initializer = initializer;
    }

    // Private constructor for withoutChild
    private VariableBuilder(NameRegistry registry, String id, boolean isMutable,
                            String type, ExpressionBuilder initializer) {
        this.registry = registry;
        this.id = id;
        this.isMutable = isMutable;
        this.type = type;
        this.initializer = initializer;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String build(BuildContext ctx) {
        String keyword = isMutable ? "var" : "val";
        String indent = indent(ctx.indentLevel());
        return indent + keyword + " " + id + ": " + type + " = " + initializer.build(ctx);
    }

    @Override
    public List<? extends IBuilder> children() {
        return List.of(initializer);
    }

    @Override
    public void accept(IBuilderVisitor visitor) {
        visitor.visit(this);
        initializer.accept(visitor);
    }

    @Override
    public IBuilder withoutChild(IBuilder builder) {
        // The initializer is the only child — if removed, replace with a default literal
        if (builder.equals(initializer)) {
            ExpressionBuilder defaultExpr = new ExpressionBuilder(
                    registry, ExpressionBuilder.ExpressionType.INT_LITERAL, "0");
            return new VariableBuilder(registry, id, isMutable, type, defaultExpr);
        }
        return this;
    }

    // Getters for mutation policies
    public boolean isMutable() { return isMutable; }
    public String getType() { return type; }
    public ExpressionBuilder getInitializer() { return initializer; }
}