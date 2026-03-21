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
    private final String type;
    private final IExpressionBuilder initializer;
    private final boolean nullable;
    private final boolean isLateinit;
    private final String customGetter;
    private final String customSetter;

    // Main public constructor
    public VariableBuilder(NameRegistry registry, boolean isMutable,
                           IExpressionBuilder initializer, boolean nullable, String type) {
        this.registry = registry;
        this.id = registry.next("var");
        this.isMutable = isMutable;
        this.type = type;
        this.initializer = initializer;
        this.nullable = nullable;
        this.isLateinit = false;
        this.customGetter = null;
        this.customSetter = null;
    }

    // Private constructor for withoutChild and copies
    private VariableBuilder(NameRegistry registry, String id, boolean isMutable,
                            String type, IExpressionBuilder initializer, boolean nullable,
                            boolean isLateinit, String customGetter, String customSetter) {
        this.registry = registry;
        this.id = id;
        this.isMutable = isMutable;
        this.type = type;
        this.initializer = initializer;
        this.nullable = nullable;
        this.isLateinit = isLateinit;
        this.customGetter = customGetter;
        this.customSetter = customSetter;
    }

    @Override
    public String id() { return id; }

    @Override
    public String build(int indentLevel) {
        String keyword = isMutable ? "var" : "val";
        String indent = indent(indentLevel);
        StringBuilder sb = new StringBuilder(indent);

        if (isLateinit) sb.append("lateinit ");
        sb.append(keyword).append(" ").append(id)
                .append(": ").append(type).append(nullable ? "?" : "");

        // lateinit vars can't have initializers
        if (!isLateinit) {
            if (customGetter == null && customSetter == null) {
                sb.append(" = ").append(initializer.build(indentLevel));
            } else {
                // custom getter/setter — no initializer
                if (customGetter != null) {
                    sb.append("\n").append(indent(indentLevel + 1))
                            .append("get() = ").append(customGetter);
                }
                if (customSetter != null) {
                    sb.append("\n").append(indent(indentLevel + 1))
                            .append("set(value) { ").append(customSetter).append(" }");
                }
            }
        }

        return sb.toString();
    }

    @Override
    public List<? extends IBuilder> children() {
        if (isLateinit || customGetter != null) return List.of();
        return List.of(initializer);
    }

    @Override
    public void accept(IBuilderVisitor visitor) {
        visitor.visit(this);
        if (!isLateinit && customGetter == null) initializer.accept(visitor);
    }

    @Override
    public IBuilder withoutChild(IBuilder builder) {
        if (builder.equals(initializer)) {
            IExpressionBuilder defaultExpr = new IntLiteralBuilder(registry, "200");
            return new VariableBuilder(registry, id, isMutable, type,
                    defaultExpr, nullable, isLateinit, customGetter, customSetter);
        }
        return this;
    }

    // Getters
    public boolean isMutable() { return isMutable; }
    public String getType() { return type; }
    public IExpressionBuilder getInitializer() { return initializer; }
    public boolean isLateinit() { return isLateinit; }
    public String getCustomGetter() { return customGetter; }
    public String getCustomSetter() { return customSetter; }

    // Setters — return new instance (immutable style)
    public VariableBuilder withLateinit() {
        return new VariableBuilder(registry, id, true, type,
                initializer, false, true, customGetter, customSetter);
    }

    public VariableBuilder withGetter(String getter) {
        return new VariableBuilder(registry, id, isMutable, type,
                initializer, nullable, isLateinit, getter, customSetter);
    }

    public VariableBuilder withSetter(String setter) {
        return new VariableBuilder(registry, id, isMutable, type,
                initializer, nullable, isLateinit, customGetter, setter);
    }

    @Override
    public NameRegistry getRegistry() { return registry; }
}