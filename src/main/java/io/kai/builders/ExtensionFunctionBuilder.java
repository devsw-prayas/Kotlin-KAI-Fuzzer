package io.kai.builders;

import io.kai.contracts.IBuilder;
import io.kai.contracts.IBuilderVisitor;
import io.kai.contracts.NameRegistry;
import io.kai.contracts.Parameter;
import io.kai.contracts.capability.IContainer;
import io.kai.contracts.capability.ILocalScopeBuilder;
import io.kai.contracts.capability.ITopLevelBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ExtensionFunctionBuilder implements ITopLevelBuilder, IContainer<ILocalScopeBuilder> {

    private final String id;
    private final NameRegistry registry;
    private final String receiverType;
    private final List<Parameter> parameters;
    private final String returnType;
    private final List<ILocalScopeBuilder> body;

    public ExtensionFunctionBuilder(NameRegistry registry, String receiverType) {
        this.registry = registry;
        this.id = registry.next("ext_fun");
        this.receiverType = receiverType;
        this.parameters = new ArrayList<>();
        this.returnType = "Unit";
        this.body = new ArrayList<>();
    }

    private ExtensionFunctionBuilder(NameRegistry registry, String id, String receiverType,
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
        String bodyStr = body.stream()
                .map(s -> indent(indentLevel + 1) + s.build(indentLevel + 1))
                .collect(Collectors.joining("\n"));

        StringBuilder sb = new StringBuilder(indent);
        sb.append("fun ").append(receiverType).append(".").append(id)
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
        return new ExtensionFunctionBuilder(registry, id, receiverType,
                parameters, returnType, newBody);
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

    public String getReceiverType() { return receiverType; }
    public String getReturnType() { return returnType; }
    public List<Parameter> getParameters() { return parameters; }
}