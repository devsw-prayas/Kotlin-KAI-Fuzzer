package io.kai.builders.expressions;

import io.kai.contracts.IBuilder;
import io.kai.contracts.IBuilderVisitor;
import io.kai.contracts.NameRegistry;
import io.kai.contracts.capability.IExpressionBuilder;

import java.util.List;

public class ElvisBuilder implements IExpressionBuilder {
    private final String id;
    private final NameRegistry registry;
    private final IExpressionBuilder left;
    private final IExpressionBuilder right;

    public ElvisBuilder(NameRegistry registry, IExpressionBuilder left, IExpressionBuilder right) {
        this.registry = registry;
        this.id = registry.next("elvis");
        this.left = left;
        this.right = right;
    }

    private ElvisBuilder(NameRegistry registry, String id,
                         IExpressionBuilder left, IExpressionBuilder right) {
        this.registry = registry;
        this.id = id;
        this.left = left;
        this.right = right;
    }

    @Override
    public String id() { return id; }

    @Override
    public String build(int indentLevel) {
        return left.build(indentLevel) + " ?: " + right.build(indentLevel);
    }

    @Override
    public List<? extends IBuilder> children() { return List.of(left, right); }

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
            return new ElvisBuilder(registry, id, nullLit, right);
        if (builder.equals(right))
            return new ElvisBuilder(registry, id, left, nullLit);
        return this;
    }

    @Override
    public NameRegistry getRegistry() { return registry; }

    public IExpressionBuilder getLeft() { return left; }
    public IExpressionBuilder getRight() { return right; }
}