package io.kai.builders;

import io.kai.contracts.IBuilder;
import io.kai.contracts.IBuilderVisitor;
import io.kai.contracts.NameRegistry;
import io.kai.contracts.capability.ILocalScopeBuilder;

import java.util.List;

public class RawStatementBuilder implements ILocalScopeBuilder {

    private final String id;
    private final NameRegistry registry;
    private final String statement;

    public RawStatementBuilder(NameRegistry registry, String statement) {
        this.registry = registry;
        this.id = registry.next("raw");
        this.statement = statement;
    }

    @Override
    public String id() { return id; }

    @Override
    public String build(int indentLevel) { return statement; }

    @Override
    public List<? extends IBuilder> children() { return List.of(); }

    @Override
    public void accept(IBuilderVisitor visitor) { visitor.visit(this); }

    @Override
    public IBuilder withoutChild(IBuilder builder) { return this; }

    @Override
    public NameRegistry getRegistry() { return registry; }
}