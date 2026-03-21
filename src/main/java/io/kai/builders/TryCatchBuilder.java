package io.kai.builders;

import io.kai.contracts.IBuilder;
import io.kai.contracts.IBuilderVisitor;
import io.kai.contracts.NameRegistry;
import io.kai.contracts.capability.IBranchContainer;
import io.kai.contracts.capability.ILocalScopeBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TryCatchBuilder implements ILocalScopeBuilder, IBranchContainer<ILocalScopeBuilder> {

    private final String id;
    private final NameRegistry registry;
    private final List<ILocalScopeBuilder> tryBody;
    private final List<ILocalScopeBuilder> catchBody;
    private final List<ILocalScopeBuilder> finallyBody;
    private final String exceptionType;

    public TryCatchBuilder(NameRegistry registry) {
        this(registry, "Throwable");
    }

    public TryCatchBuilder(NameRegistry registry, String exceptionType) {
        this.registry = registry;
        this.id = registry.next("try_catch");
        this.exceptionType = exceptionType;
        this.tryBody = new ArrayList<>();
        this.catchBody = new ArrayList<>();
        this.finallyBody = new ArrayList<>();
    }

    private TryCatchBuilder(NameRegistry registry, String id, String exceptionType,
                            List<ILocalScopeBuilder> tryBody,
                            List<ILocalScopeBuilder> catchBody,
                            List<ILocalScopeBuilder> finallyBody) {
        this.registry = registry;
        this.id = id;
        this.exceptionType = exceptionType;
        this.tryBody = new ArrayList<>(tryBody);
        this.catchBody = new ArrayList<>(catchBody);
        this.finallyBody = new ArrayList<>(finallyBody);
    }

    @Override
    public String id() { return id; }

    @Override
    public String build(int indentLevel) {
        String indent = indent(indentLevel);
        String inner = indent(indentLevel + 1);

        String tryStr = tryBody.stream()
                .map(s -> s.build(indentLevel + 1))
                .collect(Collectors.joining("\n"));
        String catchStr = catchBody.stream()
                .map(s -> s.build(indentLevel + 1))
                .collect(Collectors.joining("\n"));
        String finallyStr = finallyBody.stream()
                .map(s -> s.build(indentLevel + 1))
                .collect(Collectors.joining("\n"));

        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("try {\n");
        if (!tryStr.isEmpty()) sb.append(tryStr).append("\n");
        sb.append(indent).append("} catch (e: ").append(exceptionType).append(") {\n");
        if (!catchStr.isEmpty()) sb.append(catchStr).append("\n");
        sb.append(indent).append("}");

        if (!finallyBody.isEmpty()) {
            sb.append(" finally {\n");
            if (!finallyStr.isEmpty()) sb.append(finallyStr).append("\n");
            sb.append(indent).append("}");
        }

        return sb.toString();
    }

    @Override
    public List<? extends IBuilder> children() {
        List<IBuilder> all = new ArrayList<>();
        all.addAll(tryBody);
        all.addAll(catchBody);
        all.addAll(finallyBody);
        return all;
    }

    @Override
    public void accept(IBuilderVisitor visitor) {
        visitor.visit(this);
        for (var s : tryBody) s.accept(visitor);
        for (var s : catchBody) s.accept(visitor);
        for (var s : finallyBody) s.accept(visitor);
    }

    @Override
    @SuppressWarnings("SuspiciousMethodCalls")
    public IBuilder withoutChild(IBuilder builder) {
        List<ILocalScopeBuilder> newTry = new ArrayList<>(tryBody);
        List<ILocalScopeBuilder> newCatch = new ArrayList<>(catchBody);
        List<ILocalScopeBuilder> newFinally = new ArrayList<>(finallyBody);
        newTry.remove(builder);
        newCatch.remove(builder);
        newFinally.remove(builder);
        return new TryCatchBuilder(registry, id, exceptionType, newTry, newCatch, newFinally);
    }

    @Override
    public boolean addChild(ILocalScopeBuilder builder, int branchIndex) {
        if (builder == null) return false;
        return switch (branchIndex) {
            case 0 -> tryBody.add(builder);
            case 1 -> catchBody.add(builder);
            case 2 -> finallyBody.add(builder);
            default -> false;
        };
    }

    @Override
    public int branchLength() { return 3; }

    @Override
    public List<ILocalScopeBuilder> getBranch(int branchIndex) {
        return switch (branchIndex) {
            case 0 -> tryBody;
            case 1 -> catchBody;
            case 2 -> finallyBody;
            default -> List.of();
        };
    }

    @Override
    public void clear(int branchIndex) {
        switch (branchIndex) {
            case 0 -> { tryBody.clear(); }
            case 1 -> { catchBody.clear(); }
            case 2 -> { finallyBody.clear();}
            default -> {
                return;
            }
        };
    }

    @Override
    public NameRegistry getRegistry() { return registry; }

    public String getExceptionType() { return exceptionType; }
    public List<ILocalScopeBuilder> getTryBody() { return tryBody; }
    public List<ILocalScopeBuilder> getCatchBody() { return catchBody; }
    public List<ILocalScopeBuilder> getFinallyBody() { return finallyBody; }
}