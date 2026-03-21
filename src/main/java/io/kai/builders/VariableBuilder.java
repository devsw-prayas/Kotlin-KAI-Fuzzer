package io.kai.builders;

import io.kai.builders.expressions.IntLiteralBuilder;
import io.kai.contracts.*;
import io.kai.contracts.capability.IExpressionBuilder;
import io.kai.contracts.capability.ILocalScopeBuilder;
import io.kai.contracts.capability.IMemberBuilder;
import io.kai.contracts.capability.ITopLevelBuilder;

import java.util.List;

public class VariableBuilder implements ITopLevelBuilder, IMemberBuilder, ILocalScopeBuilder {

    private final String id;
    private final NameRegistry registry;
    private final boolean isMutable;
    private final String type;               // hardcoded "Int" for MVP
    private final IExpressionBuilder initializer;
    private final boolean nullable;

    public VariableBuilder(NameRegistry registry, boolean isMutable, IExpressionBuilder initializer, boolean nullable, String type) {
        this.registry = registry;
        this.id = registry.next("var");
        this.isMutable = isMutable;
        this.type = type;
        this.initializer = initializer;
        this.nullable = nullable;
    }

    // Private constructor for withoutChild
    private VariableBuilder(NameRegistry registry, String id, boolean isMutable,
                            String type, IExpressionBuilder initializer, boolean nullable) {
        this.registry = registry;
        this.id = id;
        this.isMutable = isMutable;
        this.type = type;
        this.initializer = initializer;
        this.nullable = nullable;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String build(int indentLevel) {
        String keyword = isMutable ? "var" : "val";
        String indent = indent(indentLevel);
        return indent + keyword + " " + id + ": " + type + (nullable ? "?": "") +" = " + initializer.build(indentLevel);
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
            IExpressionBuilder defaultExpr = new IntLiteralBuilder(registry, "200");
            return new VariableBuilder(registry, id, isMutable, type, defaultExpr, nullable);
        }
        return this;
    }

    // Getters for mutation policies
    public boolean isMutable() { return isMutable; }
    public String getType() { return type; }
    public IExpressionBuilder getInitializer() { return initializer; }

    @Override
    public NameRegistry getRegistry() {
        return registry;
    }
}