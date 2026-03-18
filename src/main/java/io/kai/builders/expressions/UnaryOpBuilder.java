package io.kai.builders.expressions;

import io.kai.contracts.IBuilder;
import io.kai.contracts.IBuilderVisitor;
import io.kai.contracts.NameRegistry;
import io.kai.contracts.capability.IExpressionBuilder;

import java.util.List;

public class UnaryOpBuilder implements IExpressionBuilder {
    private final IExpressionBuilder operand;
    private final String op;
    private final NameRegistry registry;
    private final String id;

    private UnaryOpBuilder(IExpressionBuilder operand, String op, NameRegistry registry, String id){
        this.operand = operand;
        this.op = op;
        this.registry = registry;
        this.id = id;
    }

    public UnaryOpBuilder(NameRegistry registry, String op, IExpressionBuilder operand){
        this.registry = registry;
        this.id = registry.next("un_op");
        this.op = op;
        this.operand = operand;
    }


    @Override
    public String id() {
        return id;
    }

    @Override
    public String build(int indentLevel) {
        return op + operand.build(indentLevel);
    }

    @Override
    public List<? extends IBuilder> children() {
        return List.of(operand);
    }

    @Override
    public void accept(IBuilderVisitor visitor) {
        visitor.visit(this);
        operand.accept(visitor);
    }

    @Override
    public IBuilder withoutChild(IBuilder builder) {
        NullLiteralBuilder builder1 = new NullLiteralBuilder(registry);
        if(builder.equals(operand)){
            return new UnaryOpBuilder(builder1, op, registry, id);
        } else return this;
    }

    @Override
    public NameRegistry getRegistry() {
        return registry;
    }
}
