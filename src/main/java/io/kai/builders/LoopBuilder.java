package io.kai.builders;

import io.kai.contracts.*;
import io.kai.contracts.capability.IContainer;
import io.kai.contracts.capability.ILocalScopeBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LoopBuilder implements ILocalScopeBuilder, IContainer<ILocalScopeBuilder> {
    private final NameRegistry registry;
    private final String id;
    private final List<ILocalScopeBuilder> builders;
    private final ExpressionBuilder condition;

    public enum LoopType{
        FOR_EACH,
        WHILE
    }

    private final LoopType type;

    private LoopBuilder(NameRegistry registry, String id, ExpressionBuilder condition,LoopType type, List<ILocalScopeBuilder> builders){
        this.registry = registry;
        this.id = id;
        this.condition = condition;
        this.type = type;
        this.builders = builders;
    }

    public LoopBuilder(NameRegistry registry, LoopType type, ExpressionBuilder condition){
        this(registry, registry.next("loop"), condition, type, new ArrayList<>());
    }

    public LoopBuilder(NameRegistry registry, LoopType type, ExpressionBuilder condition, List<ILocalScopeBuilder> builders){
        this(registry, registry.next("loop"), condition, type, builders);

    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String build(BuildContext ctx) {
        String s = "{\n" +  builders.stream().map((child) -> indent(ctx.indentLevel() + 1) +
                child.build(new BuildContext(ctx.indentLevel() + 1,
                        ctx.nameRegistry(), ctx.typeScope()))).collect(Collectors.joining("\n")) + "\n}";
        String cond = condition.build(new BuildContext(ctx.indentLevel() + 1,
                ctx.nameRegistry(), ctx.typeScope()));
        s = switch (type) {
            case FOR_EACH -> "for(" + "i in 0..10" + ")" + s;
            case WHILE -> "while(" + cond + ")" + s;
        };
        return s;
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
}
