package io.kai.builders;

import io.kai.contracts.IBuilder;
import io.kai.contracts.IBuilderVisitor;
import io.kai.contracts.NameRegistry;
import io.kai.contracts.capability.ITopLevelBuilder;

import java.util.List;

public class TypeAliasBuilder implements ITopLevelBuilder {

    private final String id;
    private final NameRegistry registry;
    private final String aliasName;
    private final String targetType;

    public TypeAliasBuilder(NameRegistry registry, String aliasName, String targetType) {
        this.registry = registry;
        this.id = registry.next("typealias");
        this.aliasName = aliasName;
        this.targetType = targetType;
    }

    @Override
    public String id() { return id; }

    @Override
    public String build(int indentLevel) {
        return indent(indentLevel) + "typealias " + aliasName + " = " + targetType;
    }

    @Override
    public List<? extends IBuilder> children() { return List.of(); }

    @Override
    public void accept(IBuilderVisitor visitor) { visitor.visit(this); }

    @Override
    public IBuilder withoutChild(IBuilder builder) { return this; }

    @Override
    public NameRegistry getRegistry() { return registry; }

    public String getAliasName() { return aliasName; }
    public String getTargetType() { return targetType; }
}