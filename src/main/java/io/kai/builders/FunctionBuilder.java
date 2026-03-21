package io.kai.builders;

import io.kai.contracts.*;
import io.kai.contracts.capability.*;

import java.util.*;
import java.util.stream.Collectors;

public class FunctionBuilder implements ITopLevelBuilder, IMemberBuilder,
        ILocalScopeBuilder, IContainer<ILocalScopeBuilder>, IGeneric {

    private final List<ILocalScopeBuilder> builders;
    private final String id;
    private final NameRegistry registry;
    private final Map<String, String> typeParams;
    private boolean isInline;
    private boolean isSuspend;
    private boolean isOperator;
    private final List<Parameter> parameters;
    private String returnType;
    private String operatorName = null;

    private FunctionBuilder(List<ILocalScopeBuilder> builders, NameRegistry registry,
                            String id, Map<String, String> typeParams,
                            boolean isInline, boolean isSuspend, boolean isOperator,
                            List<Parameter> parameters, String returnType, String operatorName) {
        this.builders = builders;
        this.registry = registry;
        this.id = id;
        this.typeParams = typeParams;
        this.isInline = isInline;
        this.isSuspend = isSuspend;
        this.isOperator = isOperator;
        this.parameters = parameters;
        this.returnType = returnType;
        this.operatorName = operatorName;
    }

    public FunctionBuilder(NameRegistry registry) {
        this(new ArrayList<>(), registry, registry.next("fun"), new LinkedHashMap<>(),
                false, false, false, new ArrayList<>(), "Unit", null);
    }

    @Override
    public String id() { return id; }

    @Override
    public String build(int indentLevel) {
        String indent = indent(indentLevel);
        String body = builders.stream()
                .map(child -> indent(indentLevel + 1) + child.build(indentLevel + 1))
                .collect(Collectors.joining("\n"));

        String paramsStr = parameters.stream()
                .map(this::emitParam)
                .collect(Collectors.joining(", "));

        StringBuilder prefix = new StringBuilder(indent);
        if (isSuspend) prefix.append("suspend ");
        if (isInline) prefix.append("inline ");
        if (isOperator) prefix.append("operator ");
        prefix.append("fun ");

        String typeParamStr = buildTypeParams();
        if (!typeParamStr.isEmpty()) prefix.append(typeParamStr).append(" ");

        prefix.append(operatorName != null ? operatorName : id)
                .append("(").append(paramsStr).append(")")
                .append(": ").append(returnType)
                .append(" {\n")
                .append(body).append("\n")
                .append(indent).append("}");

        return prefix.toString();
    }

    private String emitParam(Parameter p) {
        StringBuilder sb = new StringBuilder();
        if (p.isCrossInline()) sb.append("crossinline ");
        if (p.isNoInline()) sb.append("noinline ");
        if (p.isVarargs()) sb.append("vararg ");
        sb.append(p.name()).append(": ").append(p.type());
        if (p.defaultVal() != null) sb.append(" = ").append(p.defaultVal());
        return sb.toString();
    }

    @Override
    public List<? extends IBuilder> children() { return builders; }

    @Override
    public void accept(IBuilderVisitor visitor) {
        visitor.visit(this);
        for (var child : builders) child.accept(visitor);
    }

    @Override
    @SuppressWarnings("SuspiciousMethodCalls")
    public IBuilder withoutChild(IBuilder builder) {
        ArrayList<ILocalScopeBuilder> list = new ArrayList<>(builders);
        list.remove(builder);
        return new FunctionBuilder(list, registry, id,
                new LinkedHashMap<>(typeParams), isInline, isSuspend, isOperator,
                new ArrayList<>(parameters), returnType, operatorName);
    }

    @Override
    public boolean addChild(ILocalScopeBuilder builder) {
        if (builder == null) return false;
        return builders.add(builder);
    }

    @Override
    public boolean addChildren(List<ILocalScopeBuilder> children) {
        if (children.isEmpty()) return false;
        return builders.addAll(children);
    }

    @Override
    public void clear() { builders.clear(); }

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
    public Map<String, String> getTypeParams() { return typeParams; }

    @Override
    public boolean removeParam(String param) {
        return typeParams.remove(param) != null;
    }

    @Override
    public void clearParams() { typeParams.clear(); }

    // --- Modifiers ---

    public void setInline(boolean inline) { this.isInline = inline; }
    public void setSuspend(boolean suspend) { this.isSuspend = suspend; }
    public void setOperator(boolean operator) { this.isOperator = operator; }
    public void setReturnType(String type) { this.returnType = type; }
    public boolean isInline() { return isInline; }
    public boolean isSuspend() { return isSuspend; }
    public boolean isOperator() { return isOperator; }
    public String getReturnType() { return returnType; }
    public List<Parameter> getParameters() { return parameters; }

    public void addParam(Parameter p) {
        if (p != null && !parameters.contains(p)) parameters.add(p);
    }

    @Override
    public NameRegistry getRegistry() { return registry; }

    public void setOperatorName(String name) {
        operatorName = name;
    }
}