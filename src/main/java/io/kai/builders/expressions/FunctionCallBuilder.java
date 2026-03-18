package io.kai.builders.expressions;

import io.kai.contracts.IBuilder;
import io.kai.contracts.IBuilderVisitor;
import io.kai.contracts.NameRegistry;
import io.kai.contracts.capability.IExpressionBuilder;

import java.util.List;

public class FunctionCallBuilder implements IExpressionBuilder {
    private final String id;
    private final NameRegistry registry;
    private final String funcName;
    private final List<String> typeArgs;

    public FunctionCallBuilder(NameRegistry registry, String funcName) {
        this(registry, funcName, List.of());
    }

    public FunctionCallBuilder(NameRegistry registry, String funcName, List<String> typeArgs) {
        this.registry = registry;
        this.id = registry.next("fun_call");
        this.funcName = funcName;
        this.typeArgs = List.copyOf(typeArgs);
    }

    @Override
    public String id() { return id; }

    @Override
    public String build(int indentLevel) {
        String typeArgStr = typeArgs.isEmpty() ? ""
                : "<" + String.join(", ", typeArgs) + ">";
        return funcName + typeArgStr + "()";
    }

    @Override
    public List<? extends IBuilder> children() { return List.of(); }

    @Override
    public void accept(IBuilderVisitor visitor) { visitor.visit(this); }

    @Override
    public IBuilder withoutChild(IBuilder builder) { return this; }

    @Override
    public NameRegistry getRegistry() { return registry; }

    public String getFuncName() { return funcName; }
    public List<String> getTypeArgs() { return typeArgs; }
}