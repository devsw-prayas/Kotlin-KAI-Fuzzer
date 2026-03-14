package io.kai.builders;

import io.kai.contracts.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FunctionBuilder implements ITopLevelBuilder, IMemberBuilder {
    private final List<ILocalScopeBuilder> builders;
    private final String returnType = "Unit";
    private final String id;
    private final NameRegistry registry;

    private FunctionBuilder(List<ILocalScopeBuilder> builders, NameRegistry registry, String id){
        this.builders = builders;
        this.registry = registry;
        this.id = id;
    }

    public FunctionBuilder(NameRegistry registry){
        this.id = registry.next("fun");
        this.registry = registry;
        builders = new ArrayList<>();
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String build(BuildContext ctx) {
        return "fun " + id + "() {\n " + builders.stream()
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
        return new FunctionBuilder(list, registry, id);
    }
}
