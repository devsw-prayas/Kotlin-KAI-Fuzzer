package io.kai.builders;

import io.kai.contracts.*;
import io.kai.contracts.capability.IContainer;
import io.kai.contracts.capability.IGeneric;
import io.kai.contracts.capability.IMemberBuilder;
import io.kai.contracts.capability.ITopLevelBuilder;

import java.util.*;
import java.util.stream.Collectors;

public class ClassBuilder implements ITopLevelBuilder, IContainer<IMemberBuilder>, IGeneric {
    private final List<IMemberBuilder> builders;
    private final String id;
    private final NameRegistry registry;
    private final Map<String, String> typeParams;
    private boolean isSealed;
    private boolean isData;

    private boolean isAbstract;
    private boolean isOpen;
    private boolean isObject;
    private final List<String> superTypes;
    private final List<Parameter> primaryConstructorParams;
    private Set<String> usedOperators = new HashSet<>();

    public ClassBuilder(NameRegistry registry) {
        this.builders = new ArrayList<>();
        this.registry = registry;
        this.id = registry.next("class");
        this.typeParams = new LinkedHashMap<>();
        this.isSealed = false;
        this.isData = false;
        this.isAbstract = false;
        this.isOpen = false;
        this.isObject = false;
        this.superTypes = new ArrayList<>();
        this.primaryConstructorParams = new ArrayList<>();
    }

    private ClassBuilder(NameRegistry registry, String id, List<IMemberBuilder> builders,
                         Map<String, String> typeParams, boolean isSealed, boolean isData,
                         boolean isAbstract, boolean isOpen, boolean isObject,
                         List<String> superTypes, List<Parameter> primaryConstructorParams,
                         Set<String> usedOperators) {
        this.registry = registry;
        this.id = id;
        this.builders = builders;
        this.typeParams = typeParams;
        this.isSealed = isSealed;
        this.isData = isData;
        this.isAbstract = isAbstract;
        this.isOpen = isOpen;
        this.isObject = isObject;
        this.superTypes = new ArrayList<>(superTypes);
        this.primaryConstructorParams = new ArrayList<>(primaryConstructorParams);
        this.usedOperators = new LinkedHashSet<>(usedOperators);
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String build(int indentLevel) {
        String indent = indent(indentLevel);
        String body = builders.stream()
                .map(child -> child.build(indentLevel + 1))
                .collect(Collectors.joining("\n"));

        StringBuilder prefix = new StringBuilder(indent);

        // Modifiers
        if (isAbstract) prefix.append("abstract ");
        if (isOpen) prefix.append("open ");
        if (isSealed) prefix.append("sealed ");
        if (isData) prefix.append("data ");
        prefix.append(isObject ? "object " : "class ").append(id);

        // Type params (objects can't have type params)
        if (!isObject) {
            String tp = buildTypeParams();
            if (!tp.isEmpty()) prefix.append(tp);
        }

        // Primary constructor params
        if (!primaryConstructorParams.isEmpty()) {
            String ctorParams = primaryConstructorParams.stream()
                    .map(p -> "val " + p.name() + ": " + p.type())
                    .collect(Collectors.joining(", "));
            prefix.append("(").append(ctorParams).append(")");
        }

        // Super types
        if (!superTypes.isEmpty()) {
            prefix.append(" : ").append(String.join(", ", superTypes));
        }

        prefix.append(" {\n")
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

        // If removing an operator function, unregister its operator name
        Set<String> newRegisteredOperators = new LinkedHashSet<>(usedOperators);
        if (builder instanceof FunctionBuilder fn && fn.isOperator()
                && fn.getOperatorName() != null) {
            newRegisteredOperators.remove(fn.getOperatorName());
        }

        return new ClassBuilder(registry, id, list,
                new LinkedHashMap<>(typeParams), isSealed, isData,
                isAbstract, isOpen, isObject,
                new ArrayList<>(superTypes),
                new ArrayList<>(primaryConstructorParams),
                newRegisteredOperators);
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

    public void setAbstract(boolean v) { isAbstract = v; }
    public void setOpen(boolean v) { isOpen = v; }
    public void setObject(boolean v) { isObject = v; }
    public void addSuperType(String t) { superTypes.add(t); }
    public void addConstructorParam(Parameter p) { primaryConstructorParams.add(p); }
    public boolean isAbstract() { return isAbstract; }
    public boolean isOpen() { return isOpen; }
    public boolean isObject() { return isObject; }
    public List<String> getSuperTypes() { return superTypes; }
    public List<Parameter> getPrimaryConstructorParams() { return primaryConstructorParams; }

    public boolean hasOperator(String name) {
        return usedOperators.contains(name);
    }

    public void registerOperator(String name) {
        usedOperators.add(name);
    }
}