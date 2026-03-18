package io.kai.builders;

import io.kai.contracts.*;
import io.kai.contracts.capability.IContainer;
import io.kai.contracts.capability.IGeneric;
import io.kai.contracts.capability.IMemberBuilder;
import io.kai.contracts.capability.ITopLevelBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ClassBuilder implements ITopLevelBuilder, IContainer<IMemberBuilder>, IGeneric {
    private final List<IMemberBuilder> builders;
    private final String id;
    private final NameRegistry registry;
    private final Map<String, String> typeParams;
    private boolean isSealed;
    private boolean isData;

    public ClassBuilder(NameRegistry registry) {
        this.builders = new ArrayList<>();
        this.registry = registry;
        this.id = registry.next("class");
        this.typeParams = new LinkedHashMap<>();
        this.isSealed = false;
        this.isData = false;
    }

    private ClassBuilder(NameRegistry registry, String id, List<IMemberBuilder> builders,
                         Map<String, String> typeParams, boolean isSealed, boolean isData) {
        this.registry = registry;
        this.id = id;
        this.builders = builders;
        this.typeParams = typeParams;
        this.isSealed = isSealed;
        this.isData = isData;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String build(int indentLevel) {
        String indent = indent(indentLevel);
        String body = builders.stream()
                .map(child -> indent(indentLevel + 1) + child.build(indentLevel + 1))
                .collect(Collectors.joining("\n"));

        StringBuilder prefix = new StringBuilder(indent);
        if (isSealed) prefix.append("sealed ");
        if (isData) prefix.append("data ");
        prefix.append("class ").append(id).append(buildTypeParams()).append(" {\n")
                .append(body).append("\n")
                .append(indent).append("}");

        return prefix.toString();
    }

    @Override
    public List<? extends IBuilder> children() {
        return builders;
    }

    @Override
    public void accept(IBuilderVisitor visitor) {
        visitor.visit(this);
        for (var child : builders) child.accept(visitor);
    }

    @Override
    @SuppressWarnings("SuspiciousMethodCalls")
    public IBuilder withoutChild(IBuilder builder) {
        ArrayList<IMemberBuilder> list = new ArrayList<>(builders);
        list.remove(builder);
        return new ClassBuilder(registry, id, list,
                new LinkedHashMap<>(typeParams), isSealed, isData);
    }

    @Override
    public boolean addChild(IMemberBuilder builder) {
        if (builder == null) return false;
        return builders.add(builder);
    }

    // --- IGeneric ---

    @Override
    public boolean addTypeParam() {
        String name = registry.next("T");
        typeParams.put(name, "");
        return true;
    }

    @Override
    public boolean addBoundedTypeParam(String name, String bound) {
        if (typeParams.containsKey(name)) return false;
        typeParams.put(name, bound);
        return true;
    }

    @Override
    public Map<String, String> getTypeParams() {
        return typeParams;
    }

    @Override
    public boolean removeParam(String param) {
        return typeParams.remove(param) != null;
    }

    @Override
    public void clearParams() {
        typeParams.clear();
    }

    // --- Modifiers ---

    public void setSealed(boolean sealed) { this.isSealed = sealed; }
    public void setData(boolean data) { this.isData = data; }
    public boolean isSealed() { return isSealed; }
    public boolean isData() { return isData; }

    @Override
    public NameRegistry getRegistry() {
        return registry;
    }
}