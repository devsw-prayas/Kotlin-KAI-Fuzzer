package io.kai.builders;

import io.kai.contracts.IBuilder;
import io.kai.contracts.IBuilderVisitor;
import io.kai.contracts.NameRegistry;
import io.kai.contracts.capability.IContainer;
import io.kai.contracts.capability.IMemberBuilder;
import io.kai.contracts.capability.ITopLevelBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ObjectBuilder implements ITopLevelBuilder, IMemberBuilder, IContainer<IMemberBuilder> {

    private final String id;
    private final NameRegistry registry;
    private final boolean isCompanion;
    private final List<IMemberBuilder> members;

    public ObjectBuilder(NameRegistry registry, boolean isCompanion) {
        this.registry = registry;
        this.id = registry.next("obj");
        this.isCompanion = isCompanion;
        this.members = new ArrayList<>();
    }

    private ObjectBuilder(NameRegistry registry, String id,
                          boolean isCompanion, List<IMemberBuilder> members) {
        this.registry = registry;
        this.id = id;
        this.isCompanion = isCompanion;
        this.members = new ArrayList<>(members);
    }

    @Override
    public String id() { return id; }

    @Override
    public String build(int indentLevel) {
        String indent = indent(indentLevel);
        String body = members.stream()
                .map(m -> indent(indentLevel + 1) + m.build(indentLevel + 1))
                .collect(Collectors.joining("\n"));

        StringBuilder sb = new StringBuilder(indent);
        if (isCompanion) {
            sb.append("companion object");
        } else {
            sb.append("object ").append(id);
        }
        sb.append(" {\n");
        if (!body.isEmpty()) sb.append(body).append("\n");
        sb.append(indent).append("}");
        return sb.toString();
    }

    @Override
    public List<? extends IBuilder> children() { return members; }

    @Override
    public void accept(IBuilderVisitor visitor) {
        visitor.visit(this);
        for (var m : members) m.accept(visitor);
    }

    @Override
    @SuppressWarnings("SuspiciousMethodCalls")
    public IBuilder withoutChild(IBuilder builder) {
        List<IMemberBuilder> newMembers = new ArrayList<>(members);
        newMembers.remove(builder);
        return new ObjectBuilder(registry, id, isCompanion, newMembers);
    }

    @Override
    public boolean addChild(IMemberBuilder builder) {
        if (builder == null) return false;
        return members.add(builder);
    }

    @Override
    public void clear() { members.clear(); }

    @Override
    public NameRegistry getRegistry() { return registry; }

    public boolean isCompanion() { return isCompanion; }
    public List<IMemberBuilder> getMembers() { return members; }
}