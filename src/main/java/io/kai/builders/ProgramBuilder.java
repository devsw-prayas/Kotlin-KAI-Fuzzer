package io.kai.builders;

import io.kai.contracts.*;
import io.kai.contracts.capability.IContainer;
import io.kai.contracts.capability.ITopLevelBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProgramBuilder implements IBuilder, IContainer<ITopLevelBuilder> {
    private List<ITopLevelBuilder> builders = new ArrayList<>();
    private final String id;
    private final NameRegistry registry;

    private ProgramBuilder(NameRegistry registry, List<ITopLevelBuilder> builders, String id){
        this.registry = registry;
        this.id = id;
        this.builders = builders;
    }
    public ProgramBuilder(NameRegistry registry){
        this.registry = registry;
        id = registry.next("program");
    }

    @Override
    public String id() {
        return  id;
    }

    @Override
    public String build(BuildContext ctx) {
        return builders.stream()
                .map((child) -> child.build(ctx))
                .collect(Collectors.joining("\n"));
    }

    @Override
    public List<? extends IBuilder> children() {
        return builders;
    }

    @Override
    public void accept(IBuilderVisitor visitor) {
        visitor.visit(this);
        for(var child : builders) {
            child.accept(visitor);
        }
    }


    @Override
    @SuppressWarnings("SuspiciousMethodCalls")
    public IBuilder withoutChild(IBuilder builder) {
        ArrayList<ITopLevelBuilder> list = new ArrayList<>(builders);
        list.remove(builder);
        return new ProgramBuilder(registry, list, id);
    }

    @Override
    public boolean addChild(ITopLevelBuilder builder) {
        if(builder == null) return false;
        return builders.add(builder);
    }

    @Override
    public boolean addChildren(List<ITopLevelBuilder> children) {
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
