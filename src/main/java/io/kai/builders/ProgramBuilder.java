package io.kai.builders;

import io.kai.contracts.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProgramBuilder implements IBuilder {
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
}
