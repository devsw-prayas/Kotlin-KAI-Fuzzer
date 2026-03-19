package io.kai.builders.expressions;

import io.kai.contracts.IBuilder;
import io.kai.contracts.IBuilderVisitor;
import io.kai.contracts.NameRegistry;
import io.kai.contracts.capability.IExpressionBuilder;

import java.util.List;

public class NullLiteralBuilder implements IExpressionBuilder {
    private final String id;
    private final NameRegistry registry;

    public NullLiteralBuilder(NameRegistry registry){
        this.registry = registry;
        this.id = registry.next("null_lit");
    }
    @Override
    public String id() {
        return id;
    }

    @Override
    public String build(int indentLevel) {
        return "null";
    }

    @Override
    public List<? extends IBuilder> children() {
        return List.of();
    }

    @Override
    public void accept(IBuilderVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public IBuilder withoutChild(IBuilder builder) {
        return this;
    }

    @Override
    public NameRegistry getRegistry() {
        return registry;
    }

    @Override
    public String getValue(){
        return "null";
    }
}
