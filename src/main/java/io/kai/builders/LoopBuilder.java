package io.kai.builders;

import io.kai.contracts.*;
import io.kai.contracts.capability.IContainer;
import io.kai.contracts.capability.IExpressionBuilder;
import io.kai.contracts.capability.ILocalScopeBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LoopBuilder implements ILocalScopeBuilder, IContainer<ILocalScopeBuilder> {
    private final NameRegistry registry;
    private final String id;
    private final List<ILocalScopeBuilder> builders;
    private final IExpressionBuilder condition;

    public enum LoopType{
        FOR_EACH,
        WHILE
    }

    private final LoopType type;

    private LoopBuilder(NameRegistry registry, String id, IExpressionBuilder condition,LoopType type, List<ILocalScopeBuilder> builders){
        this.registry = registry;
        this.id = id;
        this.condition = condition;
        this.type = type;
        this.builders = builders;
    }

    public LoopBuilder(NameRegistry registry, LoopType type, IExpressionBuilder condition){
        this(registry, registry.next("loop"), condition, type, new ArrayList<>());
    }

    public LoopBuilder(NameRegistry registry, LoopType type, IExpressionBuilder condition, List<ILocalScopeBuilder> builders){
        this(registry, registry.next("loop"), condition, type, builders);

    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String build(int indentLevel) {
        String indent = indent(indentLevel);
        String body = builders.stream()
                .map(child -> child.build(indentLevel + 1))
                .collect(Collectors.joining("\n"));
        String cond = condition.build(indentLevel);
        return switch (type) {
            case FOR_EACH -> indent + "for(i in " + cond + ") {\n" + body + "\n" + indent + "}";
            case WHILE    -> indent + "while(" + cond + ") {\n" + body + "\n" + indent + "}";
        };
    }

    @Override
    public List<? extends IBuilder> children() {
        return builders;
    }

    @Override
    public void accept(IBuilderVisitor visitor) {
        visitor.visit(this);
        for(var child : builders){
            child.accept(visitor);
        }
    }

    @Override
    @SuppressWarnings("SuspiciousMethodCalls")
    public IBuilder withoutChild(IBuilder builder) {
        List<ILocalScopeBuilder> newBuilders = new ArrayList<>(builders);
        newBuilders.remove(builder);
        return new LoopBuilder(registry, id, condition, type, newBuilders);
    }

    @Override
    public boolean addChild(ILocalScopeBuilder builder) {
        return builders.add(builder);
    }

    @Override
    public boolean addChildren(List<ILocalScopeBuilder> children) {
        if(children.isEmpty()) return false;
        return builders.addAll(children);
    }

    @Override
    public void clear() {
        builders.clear();
    }

    @Override
    public NameRegistry getRegistry() {
        return registry;
    }
}
