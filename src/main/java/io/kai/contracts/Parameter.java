package io.kai.contracts;

import io.kai.builders.ClassBuilder;

import java.util.function.Supplier;

public class Parameter {
    private final String name;
    private final Supplier<String> typeSupplier;
    private final String defaultVal;
    private final boolean isVarargs;
    private final boolean isCrossInline;
    private final boolean isNoInline;

    // Eager constructor — normal params
    public Parameter(String name, String type, String defaultVal,
                     boolean isVarargs, boolean isCrossInline, boolean isNoInline) {
        this.name = name;
        this.typeSupplier = () -> type;
        this.defaultVal = defaultVal;
        this.isVarargs = isVarargs;
        this.isCrossInline = isCrossInline;
        this.isNoInline = isNoInline;
    }

    // Lazy constructor — type resolved at build time
    public Parameter(String name, Supplier<String> typeSupplier, String defaultVal,
                     boolean isVarargs, boolean isCrossInline, boolean isNoInline) {
        this.name = name;
        this.typeSupplier = typeSupplier;
        this.defaultVal = defaultVal;
        this.isVarargs = isVarargs;
        this.isCrossInline = isCrossInline;
        this.isNoInline = isNoInline;
    }

    public String name() { return name; }
    public String type() { return typeSupplier.get(); }
    public String defaultVal() { return defaultVal; }
    public boolean isVarargs() { return isVarargs; }
    public boolean isCrossInline() { return isCrossInline; }
    public boolean isNoInline() { return isNoInline; }

    public static Parameter simple(String name, String type) {
        return new Parameter(name, type, null, false, false, false);
    }

    public static Parameter ofClass(String name, ClassBuilder cb) {
        return new Parameter(name, () -> {
            String id = cb.id();
            if (!cb.getTypeParams().isEmpty()) {
                id += "<" + String.join(", ", cb.getTypeParams().keySet()) + ">";
            }
            return id;
        }, null, false, false, false);
    }
}