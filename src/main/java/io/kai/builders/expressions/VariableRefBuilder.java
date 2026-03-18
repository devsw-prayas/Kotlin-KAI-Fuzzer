package io.kai.builders.expressions;

import io.kai.contracts.IBuilder;
import io.kai.contracts.IBuilderVisitor;
import io.kai.contracts.NameRegistry;
import io.kai.contracts.capability.IExpressionBuilder;

import java.util.List;

public class VariableRefBuilder implements IExpressionBuilder {
    private final String id;
    private final NameRegistry registry;
    private final String varName;
    private final String type;

    public VariableRefBuilder(NameRegistry registry, String varName, String type) {
        this.registry = registry;
        this.id = registry.next("var_ref");
        this.varName = varName;
        this.type = type;
    }

    @Override
    public String id() { return id; }

    @Override
    public String build(int indentLevel) { return varName; }

    @Override
    public List<? extends IBuilder> children() { return List.of(); }

    @Override
    public void accept(IBuilderVisitor visitor) { visitor.visit(this); }

    @Override
    public IBuilder withoutChild(IBuilder builder) { return this; }

    @Override
    public NameRegistry getRegistry() { return registry; }

    public String getVarName() { return varName; }
    public String getType() { return type; }
}