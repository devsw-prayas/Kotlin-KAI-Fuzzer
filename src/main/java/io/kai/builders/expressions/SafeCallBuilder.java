package io.kai.builders.expressions;

import io.kai.contracts.IBuilder;
import io.kai.contracts.IBuilderVisitor;
import io.kai.contracts.NameRegistry;
import io.kai.contracts.capability.IExpressionBuilder;

import java.util.List;

public class SafeCallBuilder implements IExpressionBuilder {
    private final String id;
    private final NameRegistry registry;
    private final IExpressionBuilder receiver;
    private final String member;

    public SafeCallBuilder(NameRegistry registry, IExpressionBuilder receiver, String member) {
        this.registry = registry;
        this.id = registry.next("safe_call");
        this.receiver = receiver;
        this.member = member;
    }

    private SafeCallBuilder(NameRegistry registry, String id,
                            IExpressionBuilder receiver, String member) {
        this.registry = registry;
        this.id = id;
        this.receiver = receiver;
        this.member = member;
    }

    @Override
    public String id() { return id; }

    @Override
    public String build(int indentLevel) {
        return receiver.build(indentLevel) + "?." + member;
    }

    @Override
    public List<? extends IBuilder> children() { return List.of(receiver); }

    @Override
    public void accept(IBuilderVisitor visitor) {
        visitor.visit(this);
        receiver.accept(visitor);
    }

    @Override
    public IBuilder withoutChild(IBuilder builder) {
        if (builder.equals(receiver))
            return new SafeCallBuilder(registry, id, new NullLiteralBuilder(registry), member);
        return this;
    }

    @Override
    public NameRegistry getRegistry() { return registry; }

    public IExpressionBuilder getReceiver() { return receiver; }
    public String getMember() { return member; }
}