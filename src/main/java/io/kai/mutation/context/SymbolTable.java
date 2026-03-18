package io.kai.mutation.context;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SymbolTable {

    public record FunctionMeta(
            String name,
            Map<String, String> typeParams,
            boolean isSuspend,
            boolean isInline
    ) {}

    public record ClassMeta(
            String name,
            Map<String, String> typeParams,
            boolean isSealed,
            boolean isData
    ) {}

    private final Map<String, FunctionMeta> functions = new LinkedHashMap<>();
    private final Map<String, ClassMeta> classes = new LinkedHashMap<>();

    public void declareFunction(FunctionMeta meta) {
        functions.putIfAbsent(meta.name(), meta);
    }

    public void declareClass(ClassMeta meta) {
        classes.putIfAbsent(meta.name(), meta);
    }

    public FunctionMeta getFunction(String name) {
        return functions.get(name);
    }

    public ClassMeta getClass(String name) {
        return classes.get(name);
    }

    public List<FunctionMeta> getAllFunctions() {
        return List.copyOf(functions.values());
    }

    public List<ClassMeta> getAllClasses() {
        return List.copyOf(classes.values());
    }

    public List<FunctionMeta> getGenericFunctions() {
        return functions.values().stream()
                .filter(f -> !f.typeParams().isEmpty())
                .collect(Collectors.toList());
    }

    public List<FunctionMeta> getSuspendFunctions() {
        return functions.values().stream()
                .filter(FunctionMeta::isSuspend)
                .collect(Collectors.toList());
    }

    public List<FunctionMeta> getInlineFunctions() {
        return functions.values().stream()
                .filter(FunctionMeta::isInline)
                .collect(Collectors.toList());
    }

    public List<ClassMeta> getSealedClasses() {
        return classes.values().stream()
                .filter(ClassMeta::isSealed)
                .collect(Collectors.toList());
    }

    public List<ClassMeta> getGenericClasses() {
        return classes.values().stream()
                .filter(c -> !c.typeParams().isEmpty())
                .collect(Collectors.toList());
    }
}