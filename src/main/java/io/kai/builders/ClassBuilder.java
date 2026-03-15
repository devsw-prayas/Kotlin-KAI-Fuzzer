package io.kai.builders;

import io.kai.contracts.*;
import io.kai.contracts.capability.IContainer;
import io.kai.contracts.capability.IGeneric;
import io.kai.contracts.capability.IMemberBuilder;
import io.kai.contracts.capability.ITopLevelBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ClassBuilder implements ITopLevelBuilder, IContainer<IMemberBuilder>, IGeneric {
    private final List<IMemberBuilder> builders;
    private final String id;
    private final NameRegistry registry;
    private final List<String> genParams;

    public ClassBuilder(NameRegistry registry){
        this.builders = new ArrayList<>();
        this.registry = registry;
        this.id = registry.next("class");
        this.genParams = new ArrayList<>();
    }

    private ClassBuilder(NameRegistry registry, String id, List<IMemberBuilder> builders, List<String> params){
        this.registry = registry;
        this.id = id;
        this.builders = builders;
        this.genParams = params;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String build(BuildContext ctx) {
        return "class " + id + buildTypeParams() + " {\n" + (builders.stream()
                .map((child) -> indent(ctx.indentLevel() + 1) + child.build(new BuildContext(
                        ctx.indentLevel() + 1, ctx.nameRegistry(), ctx.typeScope())))
                .collect(Collectors.joining("\n")) + "\n}");
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
        ArrayList<IMemberBuilder> list = new ArrayList<>(builders);
        list.remove(builder);
        return new ClassBuilder(registry, id, list, genParams);
    }

    @Override
    public boolean addChild(IMemberBuilder builder) {
        if(builder == null) return false;
        return builders.add(builder);
    }

    @Override
    public boolean addTypeParam() {
        return genParams.add(registry.next("T"));
    }

    @Override
    public List<String> getTypeParams() {
        return genParams;
    }

    @Override
    public boolean removeParam(String param) {
        return genParams.remove(param);
    }

    @Override
    public void clearParams() {
        genParams.clear();
    }
}
