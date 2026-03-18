package io.kai.builders.expressions;

import io.kai.contracts.IBuilder;
import io.kai.contracts.IBuilderVisitor;
import io.kai.contracts.NameRegistry;
import io.kai.contracts.capability.IExpressionBuilder;

import java.util.List;

public class IntLiteralBuilder implements IExpressionBuilder {
    private final String id;
    private final NameRegistry registry;
    private final String value;

    public IntLiteralBuilder(NameRegistry registry, String val){
        this.registry = registry;
        this.id = registry.next("int_lit");
        this.value = val;
    }
    @Override
    public String id() {
        return id;
    }

    @Override
    public String build(int indentLevel) {
        return value;
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
        return value;
    }

}
