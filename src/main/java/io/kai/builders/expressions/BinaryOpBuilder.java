package io.kai.builders.expressions;

import io.kai.contracts.IBuilder;
import io.kai.contracts.IBuilderVisitor;
import io.kai.contracts.NameRegistry;
import io.kai.contracts.capability.IExpressionBuilder;

import java.util.List;

public class BinaryOpBuilder implements IExpressionBuilder {
    private final IExpressionBuilder left;
    private final IExpressionBuilder right;
    private final String op;
    private final String id;
    private final NameRegistry registry;

    private BinaryOpBuilder(NameRegistry registry, String id, String op, IExpressionBuilder left, IExpressionBuilder right) {
        this.registry = registry;
        this.id = id;
        this.op = op;
        this.left = left;
        this.right = right;
    }

    public BinaryOpBuilder(NameRegistry registry, String op, IExpressionBuilder left, IExpressionBuilder right){
        this.left = left;
        this.right = right;
        this.op = op;
        this.registry = registry;
        this.id = registry.next("bin_op");
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String build(int indentLevel) {
        return "("+ left.build(indentLevel + 1) + op + right.build(indentLevel + 1)+ ")";
    }

    @Override
    public List<? extends IBuilder> children() {
        return List.of(left, right);
    }

    @Override
    public void accept(IBuilderVisitor visitor) {
        visitor.visit(this);
        left.accept(visitor);
        right.accept(visitor);
    }

    @Override
    public IBuilder withoutChild(IBuilder builder) {
        NullLiteralBuilder nullLit = new NullLiteralBuilder(registry);
        if (builder.equals(left))
            return new BinaryOpBuilder(registry, id, op, nullLit, right);
        if (builder.equals(right))
            return new BinaryOpBuilder(registry, id, op, left, nullLit);
        return this;
    }

    @Override
    public NameRegistry getRegistry() {
        return registry;
    }
}
