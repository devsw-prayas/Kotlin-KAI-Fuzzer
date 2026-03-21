package io.kai.contracts;

import java.util.List;

public interface IBuilder {
    String id();
    String build(int indentLevel);
    List<? extends IBuilder> children();
    void accept(IBuilderVisitor visitor);
    IBuilder withoutChild(IBuilder builder);
    default String indent(int level) {
        return "    ".repeat(level);
    }
    default NameRegistry getRegistry() {
        return null;
    }
}
