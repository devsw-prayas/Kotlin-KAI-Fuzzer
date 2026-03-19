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

public class SealedClassBuilder implements ITopLevelBuilder, IContainer<IMemberBuilder> {

    private final String id;
    private final NameRegistry registry;
    private final List<ClassBuilder> subclasses;
    private final List<IMemberBuilder> members;

    public SealedClassBuilder(NameRegistry registry) {
        this.registry = registry;
        this.id = registry.next("sealed");
        this.subclasses = new ArrayList<>();
        this.members = new ArrayList<>();
    }

    private SealedClassBuilder(NameRegistry registry, String id,
                               List<ClassBuilder> subclasses, List<IMemberBuilder> members) {
        this.registry = registry;
        this.id = id;
        this.subclasses = new ArrayList<>(subclasses);
        this.members = new ArrayList<>(members);
    }

    public void addSubclass(ClassBuilder subclass) {
        subclasses.add(subclass);
    }

    @Override
    public String id() { return id; }

    @Override
    public String build(int indentLevel) {
        String indent = indent(indentLevel);
        String inner = indent(indentLevel + 1);

        // Build members
        String membersStr = members.stream()
                .map(m -> inner + m.build(indentLevel + 1))
                .collect(Collectors.joining("\n"));

        // Build subclasses as nested classes
        String subclassStr = subclasses.stream()
                .map(sc -> inner + "class " + sc.id() + " : " + id + "()")
                .collect(Collectors.joining("\n"));

        StringBuilder sb = new StringBuilder(indent);
        sb.append("sealed class ").append(id).append(" {\n");
        if (!membersStr.isEmpty()) sb.append(membersStr).append("\n");
        if (!subclassStr.isEmpty()) sb.append(subclassStr).append("\n");
        sb.append(indent).append("}");
        return sb.toString();
    }

    @Override
    public List<? extends IBuilder> children() {
        List<IBuilder> all = new ArrayList<>(members);
        all.addAll(subclasses);
        return all;
    }

    @Override
    public void accept(IBuilderVisitor visitor) {
        visitor.visit(this);
        for (var m : members) m.accept(visitor);
        for (var sc : subclasses) sc.accept(visitor);
    }

    @Override
    @SuppressWarnings("SuspiciousMethodCalls")
    public IBuilder withoutChild(IBuilder builder) {
        List<IMemberBuilder> newMembers = new ArrayList<>(members);
        List<ClassBuilder> newSubs = new ArrayList<>(subclasses);
        newMembers.remove(builder);
        newSubs.remove(builder);
        return new SealedClassBuilder(registry, id, newSubs, newMembers);
    }

    @Override
    public boolean addChild(IMemberBuilder builder) {
        if (builder == null) return false;
        return members.add(builder);
    }

    @Override
    public void clear() {
        members.clear();
        subclasses.clear();
    }

    @Override
    public NameRegistry getRegistry() { return registry; }

    public List<ClassBuilder> getSubclasses() { return subclasses; }
    public List<IMemberBuilder> getMembers() { return members; }
}