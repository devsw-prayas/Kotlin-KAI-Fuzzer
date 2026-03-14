package io.kai.builders;

import io.kai.contracts.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ClassBuilder implements ITopLevelBuilder {
    private final List<IMemberBuilder> builders;
    private final String id;
    private final NameRegistry registry;

    public ClassBuilder(NameRegistry registry){
        this.builders = new ArrayList<>();
        this.registry = registry;
        this.id = registry.next("class");
    }

    private ClassBuilder(NameRegistry registry, String id, List<IMemberBuilder> builders){
        this.registry = registry;
        this.id = id;
        this.builders = builders;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String build(BuildContext ctx) {
        return "class " + id + " {\n" + (builders.stream()
                .map((child) -> indent(ctx.indentLevel()) + 1 + child.build(new BuildContext(
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
        return new ClassBuilder(registry, id, list);
    }
}
