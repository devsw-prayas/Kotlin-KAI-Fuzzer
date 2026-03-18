package io.kai.builders.expressions;

import io.kai.contracts.IBuilder;
import io.kai.contracts.IBuilderVisitor;
import io.kai.contracts.NameRegistry;
import io.kai.contracts.capability.IContainer;
import io.kai.contracts.capability.IExpressionBuilder;
import io.kai.contracts.capability.ILocalScopeBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LambdaBuilder implements IExpressionBuilder, IContainer<ILocalScopeBuilder> {

    public record Parameter(String name, String type) {}

    private final String id;
    private final NameRegistry registry;
    private final List<Parameter> parameters;
    private final List<ILocalScopeBuilder> body;
    private final String returnType; // nullable — null means inferred

    public LambdaBuilder(NameRegistry registry, List<Parameter> parameters, String returnType) {
        this.registry = registry;
        this.id = registry.next("lambda");
        this.parameters = new ArrayList<>(parameters);
        this.body = new ArrayList<>();
        this.returnType = returnType;
    }

    // no-param lambda
    public LambdaBuilder(NameRegistry registry) {
        this(registry, List.of(), null);
    }

    private LambdaBuilder(NameRegistry registry, String id, List<Parameter> parameters,
                          List<ILocalScopeBuilder> body, String returnType) {
        this.registry = registry;
        this.id = id;
        this.parameters = parameters;
        this.body = body;
        this.returnType = returnType;
    }

    @Override
    public String id() { return id; }

    @Override
    public String build(int indentLevel) {
        String params = parameters.stream()
                .map(p -> p.name() + ": " + p.type())
                .collect(Collectors.joining(", "));

        String bodyStr = body.stream()
                .map(s -> indent(indentLevel + 1) + s.build(indentLevel + 1))
                .collect(Collectors.joining("\n"));

        StringBuilder sb = new StringBuilder("{ ");
        if (!parameters.isEmpty()) {
            sb.append(params);
            if (returnType != null) sb.append(": ").append(returnType);
            sb.append(" ->\n");
        }
        if (!bodyStr.isEmpty()) sb.append(bodyStr).append("\n");
        sb.append(indent(indentLevel)).append("}");
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
        ArrayList<ILocalScopeBuilder> newBody = new ArrayList<>(body);
        newBody.remove(builder);
        return new LambdaBuilder(registry, id, parameters, newBody, returnType);
    }

    @Override
    public boolean addChild(ILocalScopeBuilder builder) {
        if (builder == null) return false;
        return body.add(builder);
    }

    @Override
    public boolean addChildren(List<ILocalScopeBuilder> children) {
        if (children.isEmpty()) return false;
        return body.addAll(children);
    }

    @Override
    public void clear() { body.clear(); }

    @Override
    public NameRegistry getRegistry() { return registry; }

    public List<Parameter> getParameters() { return parameters; }
    public String getReturnType() { return returnType; }
}