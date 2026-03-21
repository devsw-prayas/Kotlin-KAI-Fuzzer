package io.kai.builders;

import io.kai.builders.expressions.BoolLiteralBuilder;
import io.kai.contracts.*;
import io.kai.contracts.capability.IBranchContainer;
import io.kai.contracts.capability.IExpressionBuilder;
import io.kai.contracts.capability.ILocalScopeBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BranchBuilder implements ILocalScopeBuilder, IBranchContainer<ILocalScopeBuilder> {

    private final String id;
    private final NameRegistry registry;
    private final IExpressionBuilder condition;
    private final List<ILocalScopeBuilder> thenBranch;
    private final List<ILocalScopeBuilder> elseBranch;

    public BranchBuilder(NameRegistry registry, IExpressionBuilder condition,
                         List<ILocalScopeBuilder> thenBranch,
                         List<ILocalScopeBuilder> elseBranch) {
        this.registry = registry;
        this.id = registry.next("branch");
        this.condition = condition;
        this.thenBranch = new ArrayList<>(thenBranch);
        this.elseBranch = new ArrayList<>(elseBranch);
    }

    // Private constructor for withoutChild
    private BranchBuilder(NameRegistry registry, String id, IExpressionBuilder condition,
                          List<ILocalScopeBuilder> thenBranch,
                          List<ILocalScopeBuilder> elseBranch) {
        this.registry = registry;
        this.id = id;
        this.condition = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String build(int indentLevel) {
        String indent = indent(indentLevel);

        String thenBody = thenBranch.stream()
                .map(c -> c.build(indentLevel + 1))
                .collect(Collectors.joining("\n"));

        String elseBody = elseBranch.stream()
                .map(c -> c.build(indentLevel + 1))
                .collect(Collectors.joining("\n"));

        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("if (").append(condition.build(indentLevel)).append(") {\n");
        if (!thenBody.isEmpty()) sb.append(thenBody).append("\n");
        sb.append(indent).append("}");

        if (!elseBranch.isEmpty()) {
            sb.append(" else {\n");
            if (!elseBody.isEmpty()) sb.append(elseBody).append("\n");
            sb.append(indent).append("}");
        }

        return sb.toString();
    }

    @Override
    public List<? extends IBuilder> children() {
        List<IBuilder> all = new ArrayList<>();
        all.add(condition);
        all.addAll(thenBranch);
        all.addAll(elseBranch);
        return all;
    }

    @Override
    public void accept(IBuilderVisitor visitor) {
        visitor.visit(this);
        condition.accept(visitor);
        for (var c : thenBranch) c.accept(visitor);
        for (var c : elseBranch) c.accept(visitor);
    }

    @Override
    @SuppressWarnings("SuspiciousMethodCalls")
    public IBuilder withoutChild(IBuilder builder) {
        if (builder.equals(condition)) {
            // Replace condition with a default true literal
            return new BranchBuilder(registry, id, new BoolLiteralBuilder(registry, "true"),
                    new ArrayList<>(thenBranch), new ArrayList<>(elseBranch));
        }
        ArrayList<ILocalScopeBuilder> newThen = new ArrayList<>(thenBranch);
        ArrayList<ILocalScopeBuilder> newElse = new ArrayList<>(elseBranch);
        newThen.remove(builder);
        newElse.remove(builder);
        return new BranchBuilder(registry, id, condition, newThen, newElse);
    }


    // Getters for mutation policies
    public IExpressionBuilder getCondition() { return condition; }
    public List<ILocalScopeBuilder> getThenBranch() { return thenBranch; }
    public List<ILocalScopeBuilder> getElseBranch() { return elseBranch; }

    @Override
    public boolean addChild(ILocalScopeBuilder builder, int branch) {
        if(builder == null) return false;
        return switch (branch) {
            case 0 -> {
                thenBranch.add(builder);
                yield true;
            }
            case 1 -> {
                elseBranch.add(builder);
                yield true;
            }
            default -> false;
        };
    }

    @Override
    public int branchLength() {
        return 2;
    }


    @Override
    public boolean addChildren(List<ILocalScopeBuilder> children, int branch) {
        if(children.isEmpty() || branch > branchLength() || branch < 0) return false;
        switch (branch){
            case 0 -> {
                thenBranch.addAll(children);
                return true;
            }
            case 1 -> {
                elseBranch.addAll(children);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    public void clear(int branch) {
        if(!(branch < 0 || branch > branchLength())) {
            switch (branch){
                case 0 -> thenBranch.clear();
                case 1 -> elseBranch.clear();
            }
        }
    }

    @Override
    public List<ILocalScopeBuilder> getBranch(int branch) {
        return switch (branch){
            case 0 ->  thenBranch;
            case 1 -> elseBranch;
            default ->  List.of();
        };
    }

    @Override
    public NameRegistry getRegistry() {
        return registry;
    }
}