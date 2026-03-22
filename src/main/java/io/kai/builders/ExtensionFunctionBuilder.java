package io.kai.builders;

import io.kai.contracts.IBuilder;
import io.kai.contracts.IBuilderVisitor;
import io.kai.contracts.NameRegistry;
import io.kai.contracts.Parameter;
import io.kai.contracts.capability.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ExtensionFunctionBuilder implements ITopLevelBuilder,
        IContainer<ILocalScopeBuilder>, IFirstStatement, IGeneric {
    private final String id;
    private final NameRegistry registry;
    private final Supplier<String> receiverType;
    private final List<Parameter> parameters;
    private final String returnType;
    private final List<ILocalScopeBuilder> body;
    private final List<String> annotations = new ArrayList<>();
    private Supplier<String> firstStatement = null;
    private final Map<String, String> typeParams = new LinkedHashMap<>();

    public ExtensionFunctionBuilder(NameRegistry registry, String receiverType) {
        this(registry, () -> receiverType, "Unit");
    }

    public ExtensionFunctionBuilder(NameRegistry registry, String receiverType, String returnType) {
        this(registry, () -> receiverType, returnType);
    }

    public ExtensionFunctionBuilder(NameRegistry registry, Supplier<String> receiverType, String returnType) {
        this.registry = registry;
        this.id = registry.next("ext_fun");
        this.receiverType = receiverType;
        this.parameters = new ArrayList<>();
        this.returnType = returnType;
        this.body = new ArrayList<>();
    }

    private ExtensionFunctionBuilder(NameRegistry registry, String id, Supplier<String> receiverType,
                                     List<Parameter> parameters, String returnType,
                                     List<ILocalScopeBuilder> body) {
        this.registry = registry;
        this.id = id;
        this.receiverType = receiverType;
        this.parameters = new ArrayList<>(parameters);
        this.returnType = returnType;
        this.body = new ArrayList<>(body);
    }

    @Override
    public String id() { return id; }

    @Override
    public String build(int indentLevel) {
        String indent = indent(indentLevel);
        String paramsStr = parameters.stream()
                .map(p -> p.name() + ": " + p.type())
                .collect(Collectors.joining(", "));
        StringBuilder bodyBuilder = new StringBuilder();
        if (firstStatement != null) {
            bodyBuilder.append(indent(indentLevel + 1))
                    .append(firstStatement.get()).append("\n");
        }
        bodyBuilder.append(body.stream()
                .map(s -> indent(indentLevel + 1) + s.build(indentLevel + 1))
                .collect(Collectors.joining("\n")));
        String bodyStr = bodyBuilder.toString();

        StringBuilder sb = new StringBuilder();
        for (String ann : annotations) {
            sb.append(indent).append(ann).append("\n");
        }
        sb.append(indent);
        String typeParamStr = buildTypeParams();
        sb.append("fun ");
        if (!typeParamStr.isEmpty()) sb.append(typeParamStr).append(" ");
        sb.append(receiverType.get()).append(".").append(id)
                .append("(").append(paramsStr).append("): ").append(returnType)
                .append(" {\n");
        if (!bodyStr.isEmpty()) sb.append(bodyStr).append("\n");
        sb.append(indent).append("}");
        return sb.toString();
    }

    @Override
    public List<? extends IBuilder> children() { return body; }

    @Override
    public void accept(IBuilderVisitor visitor) {
        visitor.visit(this);
        for (var s : body) s.accept(visitor);
    }

    @Override
    @SuppressWarnings("SuspiciousMethodCalls")
    public IBuilder withoutChild(IBuilder builder) {
        List<ILocalScopeBuilder> newBody = new ArrayList<>(body);
        newBody.remove(builder);
        ExtensionFunctionBuilder copy = new ExtensionFunctionBuilder(registry, id, receiverType,
                parameters, returnType, newBody);
        annotations.forEach(copy::addAnnotation);
        copy.setFirstStatementLazy(firstStatement);
        typeParams.forEach(copy::addBoundedTypeParam);
        return copy;
    }

    @Override
    public boolean addChild(ILocalScopeBuilder builder) {
        if (builder == null) return false;
        return body.add(builder);
    }

    @Override
    public void clear() { body.clear(); }

    public void addParam(Parameter p) {
        if (p != null) parameters.add(p);
    }

    @Override
    public NameRegistry getRegistry() { return registry; }

    public String getReceiverType() { return receiverType.get(); }
    public String getReturnType() { return returnType; }
    public List<Parameter> getParameters() { return parameters; }

    public void addAnnotation(String raw) {
        if (raw != null && !annotations.contains(raw)) annotations.add(raw);
    }

    public List<String> getAnnotations() { return annotations; }

    @Override
    public void setFirstStatement(String stmt) {
        this.firstStatement = stmt == null ? null : () -> stmt;
    }

    @Override
    public void setFirstStatementLazy(Supplier<String> stmt) {
        this.firstStatement = stmt;
    }

    @Override
    public String getFirstStatement() {
        return firstStatement == null ? null : firstStatement.get();
    }

    @Override
    public boolean hasFirstStatement() { return firstStatement != null; }

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
}