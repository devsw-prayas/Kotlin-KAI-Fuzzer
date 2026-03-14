package io.kai.contracts;

import java.util.List;

public interface IBuilder {
    String id();
    String build(BuildContext ctx);
    List<? extends IBuilder> children();
    void accept(IBuilderVisitor visitor);
    IBuilder withoutChild(IBuilder builder);
    default String indent(int level) {
        return "    ".repeat(level);
    }
}
