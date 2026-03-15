package io.kai.builders;

import io.kai.contracts.*;
import io.kai.contracts.capability.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FunctionBuilder implements ITopLevelBuilder, IMemberBuilder,
        ILocalScopeBuilder, IContainer<ILocalScopeBuilder>, IGeneric {
    private final List<ILocalScopeBuilder> builders;
    private final String returnType = "Unit";
    private final String id;
    private final NameRegistry registry;
    private final List<String> genParams;

    private FunctionBuilder(List<ILocalScopeBuilder> builders, NameRegistry registry, String id, List<String> genParams){
        this.builders = builders;
        this.registry = registry;
        this.id = id;
        this.genParams = genParams;
    }

    public FunctionBuilder(NameRegistry registry){
        this.id = registry.next("fun");
        this.registry = registry;
        builders = new ArrayList<>();
        genParams = new ArrayList<>();
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String build(BuildContext ctx) {
        return "fun " + id +  getTypeParams() +"() {\n " + builders.stream()
                .map((child) -> indent(ctx.indentLevel() + 1) +
                        child.build(new BuildContext(ctx.indentLevel() + 1, ctx.nameRegistry(), ctx.typeScope())))
                .collect(Collectors.joining("\n")) + "\n}";
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
        ArrayList<ILocalScopeBuilder> list = new ArrayList<>(builders);
        list.remove(builder);
        return new FunctionBuilder(list, registry, id, genParams);
    }

    @Override
    public boolean addChild(ILocalScopeBuilder builder) {
        if(builder == null) return false;
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
