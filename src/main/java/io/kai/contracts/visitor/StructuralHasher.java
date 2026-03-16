package io.kai.contracts.visitor;

import io.kai.contracts.IBuilder;
import io.kai.contracts.IBuilderVisitor;

public class StructuralHasher implements IBuilderVisitor {
    private long hash = 1L;

    public void visit(IBuilder node) {
        hash = hash * 31 + node.getClass().getSimpleName().hashCode();
    }

    public long result() { return hash; }

    public static long hash(IBuilder root) {
        StructuralHasher h = new StructuralHasher();
        root.accept(h);
        return h.result();
    }
}