package io.kai.builders;

import io.kai.contracts.IBuilder;
import io.kai.contracts.IBuilderVisitor;
import io.kai.contracts.NameRegistry;
import io.kai.contracts.capability.IBranchContainer;
import io.kai.contracts.capability.IExpressionBuilder;
import io.kai.contracts.capability.ILocalScopeBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WhenBuilder implements ILocalScopeBuilder, IBranchContainer<ILocalScopeBuilder> {

    public record WhenBranch(
            IExpressionBuilder condition,
            List<ILocalScopeBuilder> body,
            boolean isElse
    ) {}

    private final String id;
    private final NameRegistry registry;
    private final IExpressionBuilder subject; // null = when without subject
    private final List<WhenBranch> branches;

    public WhenBuilder(NameRegistry registry, IExpressionBuilder subject) {
        this.registry = registry;
        this.id = registry.next("when");
        this.subject = subject;
        this.branches = new ArrayList<>();
    }

    private WhenBuilder(NameRegistry registry, String id,
                        IExpressionBuilder subject, List<WhenBranch> branches) {
        this.registry = registry;
        this.id = id;
        this.subject = subject;
        this.branches = new ArrayList<>(branches);
    }

    public void addBranch(WhenBranch branch) {
        branches.add(branch);
    }

    @Override
    public String id() { return id; }

    @Override
    public String build(int indentLevel) {
        String indent = indent(indentLevel);
        String inner = indent(indentLevel + 1);

        StringBuilder sb = new StringBuilder(indent);
        sb.append("when");
        if (subject != null) sb.append(" (").append(subject.build(indentLevel)).append(")");
        sb.append(" {\n");

        for (WhenBranch branch : branches) {
            sb.append(inner);
            if (branch.isElse()) {
                sb.append("else");
            } else {
                sb.append(branch.condition().build(indentLevel + 1));
            }
            sb.append(" -> {\n");
            for (ILocalScopeBuilder stmt : branch.body()) {
                sb.append(indent(indentLevel + 2)).append(stmt.build(indentLevel + 2)).append("\n");
            }
            sb.append(inner).append("}\n");
        }

        sb.append(indent).append("}");
        return sb.toString();
    }

    @Override
    public List<? extends IBuilder> children() {
        List<IBuilder> all = new ArrayList<>();
        if (subject != null) all.add(subject);
        for (WhenBranch b : branches) all.addAll(b.body());
        return all;
    }

    @Override
    public void accept(IBuilderVisitor visitor) {
        visitor.visit(this);
        if (subject != null) subject.accept(visitor);
        for (WhenBranch b : branches) for (var s : b.body()) s.accept(visitor);
    }

    @Override
    @SuppressWarnings("SuspiciousMethodCalls")
    public IBuilder withoutChild(IBuilder builder) {
        List<WhenBranch> newBranches = new ArrayList<>();
        for (WhenBranch b : branches) {
            List<ILocalScopeBuilder> newBody = new ArrayList<>(b.body());
            newBody.remove(builder);
            newBranches.add(new WhenBranch(b.condition(), newBody, b.isElse()));
        }
        return new WhenBuilder(registry, id, subject, newBranches);
    }

    @Override
    public boolean addChild(ILocalScopeBuilder builder, int branchIndex) {
        if (builder == null || branchIndex < 0 || branchIndex >= branches.size()) return false;
        WhenBranch old = branches.get(branchIndex);
        List<ILocalScopeBuilder> newBody = new ArrayList<>(old.body());
        newBody.add(builder);
        branches.set(branchIndex, new WhenBranch(old.condition(), newBody, old.isElse()));
        return true;
    }

    @Override
    public int branchLength() { return branches.size(); }

    @Override
    public List<ILocalScopeBuilder> getBranch(int branchIndex) {
        if (branchIndex < 0 || branchIndex >= branches.size()) return List.of();
        return branches.get(branchIndex).body();
    }

    @Override
    public void clear(int branchIndex) {
        if (branchIndex < 0 || branchIndex >= branches.size()) return;
        WhenBranch old = branches.get(branchIndex);
        branches.set(branchIndex, new WhenBranch(old.condition(), new ArrayList<>(), old.isElse()));
    }

    @Override
    public NameRegistry getRegistry() { return registry; }

    public IExpressionBuilder getSubject() { return subject; }
    public List<WhenBranch> getBranches() { return branches; }
}