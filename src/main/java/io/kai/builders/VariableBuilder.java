package io.kai.builders;

import io.kai.builders.expressions.IntLiteralBuilder;
import io.kai.contracts.*;
import io.kai.contracts.capability.IExpressionBuilder;
import io.kai.contracts.capability.ILocalScopeBuilder;
import io.kai.contracts.capability.IMemberBuilder;
import io.kai.contracts.capability.ITopLevelBuilder;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class VariableBuilder implements ITopLevelBuilder, IMemberBuilder, ILocalScopeBuilder {

    private final String id;
    private final NameRegistry registry;
    private final boolean isMutable;
    private final IExpressionBuilder initializer;
    private final boolean nullable;
    private final boolean isLateinit;
    private final String customGetter;
    private final String customSetter;
    private final Supplier<String> typeSupplier;
    private final String explicitBackingFieldType;
    private final String explicitBackingFieldExpr;
    private final String customGetterBlock;

    // Main public constructor
    public VariableBuilder(NameRegistry registry, boolean isMutable,
                           IExpressionBuilder initializer, boolean nullable, String type) {
        this(registry, registry.next("var"), isMutable, () -> type,
                initializer, nullable, false, null, null, null, null, null);

    }

    // Private constructor for withoutChild and copies
    private VariableBuilder(NameRegistry registry, String id, boolean isMutable,
                            Supplier<String> typeSupplier, IExpressionBuilder initializer,
                            boolean nullable, boolean isLateinit,
                            String customGetter, String customSetter,
                            String explicitBackingFieldType, String explicitBackingFieldExpr,
                            String customGetterBlock) {
        this.registry = registry;
        this.id = id;
        this.isMutable = isMutable;
        this.typeSupplier = typeSupplier;
        this.initializer = initializer;
        this.nullable = nullable;
        this.isLateinit = isLateinit;
        this.customGetter = customGetter;
        this.customSetter = customSetter;
        this.explicitBackingFieldType = explicitBackingFieldType;
        this.explicitBackingFieldExpr = explicitBackingFieldExpr;
        this.customGetterBlock = customGetterBlock;
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
                .append(": ").append(typeSupplier.get()).append(nullable ? "?" : "");

        // lateinit vars can't have initializers
        if (!isLateinit) {
            if (explicitBackingFieldType != null) {
                sb.append("\n").append(indent(indentLevel + 1))
                        .append("field = ").append(explicitBackingFieldExpr);
            }
            if (customGetterBlock != null) {
                sb.append("\n").append(indent(indentLevel + 1)).append("get() {\n")
                        .append(indent(indentLevel + 2)).append(customGetterBlock).append("\n")
                        .append(indent(indentLevel + 1)).append("}");
            } else if (customGetter != null) {
                sb.append("\n").append(indent(indentLevel + 1))
                        .append("get() = ").append(customGetter);
            } else if (explicitBackingFieldType == null) {
                sb.append(" = ").append(initializer.build(indentLevel));
            }
            if (customSetter != null) {
                sb.append("\n").append(indent(indentLevel + 1))
                        .append("set(value) { ").append(customSetter).append(" }");
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
            return new VariableBuilder(registry, id, isMutable, typeSupplier,
                    defaultExpr, nullable, isLateinit, customGetter, customSetter,
                    explicitBackingFieldType, explicitBackingFieldExpr, customGetterBlock);
        }
        return this;
    }

    // Getters
    public boolean isMutable() { return isMutable; }
    public String getType() { return typeSupplier.get(); }
    public IExpressionBuilder getInitializer() { return initializer; }
    public boolean isLateinit() { return isLateinit; }
    public String getCustomGetter() { return customGetter; }
    public String getCustomSetter() { return customSetter; }

    // Setters — return new instance (immutable style)
    public VariableBuilder withLateinit() {
        return new VariableBuilder(registry, id, true, typeSupplier,
                initializer, false, true, customGetter, customSetter,
                explicitBackingFieldType, explicitBackingFieldExpr, customGetterBlock);
    }

    public VariableBuilder withGetter(String getter) {
        return new VariableBuilder(registry, id, isMutable, typeSupplier,
                initializer, nullable, isLateinit, getter, customSetter,
                explicitBackingFieldType, explicitBackingFieldExpr, customGetterBlock);
    }

    public VariableBuilder withSetter(String setter) {
        return new VariableBuilder(registry, id, isMutable, typeSupplier,
                initializer, nullable, isLateinit, customGetter, setter,
                explicitBackingFieldType, explicitBackingFieldExpr, customGetterBlock);
    }

    public VariableBuilder withExplicitBackingField(String fieldType, String fieldExpr) {
        return new VariableBuilder(registry, id, isMutable, typeSupplier,
                initializer, nullable, isLateinit, customGetter, customSetter,
                fieldType, fieldExpr, customGetterBlock);
    }

    public VariableBuilder withGetterBlock(String block) {
        return new VariableBuilder(registry, id, isMutable, typeSupplier,
                initializer, nullable, isLateinit, customGetter, customSetter,
                explicitBackingFieldType, explicitBackingFieldExpr, block);
    }

    @Override
    public NameRegistry getRegistry() { return registry; }

    public boolean isNullable() { return nullable; }

    public static VariableBuilder ofClass(NameRegistry registry, boolean isMutable,
                                          IExpressionBuilder initializer, boolean nullable,
                                          ClassBuilder classRef) {
        Supplier<String> typeSupplier = () -> {
            String id = classRef.id();
            if (!classRef.getTypeParams().isEmpty()) {
                id += "<" + classRef.getTypeParams().keySet().stream()
                        .map(p -> "*")
                        .collect(Collectors.joining(", ")) + ">";
            }
            return id;
        };
        return new VariableBuilder(
                registry, registry.next("var"), isMutable,
                typeSupplier, initializer, nullable, false, null, null,
                null, null, null);
    }
}