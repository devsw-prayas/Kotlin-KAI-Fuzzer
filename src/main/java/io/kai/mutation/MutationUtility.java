package io.kai.mutation;

import io.kai.builders.ClassBuilder;
import io.kai.builders.expressions.LambdaBuilder;
import io.kai.contracts.IBuilder;
import io.kai.contracts.capability.IContainer;
import io.kai.mutation.context.ScopeContext;
import io.kai.mutation.context.SymbolTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class MutationUtility {
    public static IBuilder findById(IBuilder root, String id) {
        if (root.id().equals(id)) return root;
        for (var child : root.children()) {
            IBuilder result = findById(child, id);
            if (result != null) return result;
        }
        return null;
    }

    public static IBuilder findParent(IBuilder root, String childId) {
        for (var child : root.children()) {
            if (child.id().equals(childId)) return root; // root is the parent
            IBuilder result = findParent(child, childId);
            if (result != null) return result;
        }
        return null;
    }

    public static void addChildSmart(IContainer<?> container, IBuilder newChild) {
        List<IBuilder> snapshot = ((IBuilder) container).childrenRaw();
        boolean isLambda = container instanceof LambdaBuilder;
        int returnIdx = -1;

        for (int i = 0; i < snapshot.size(); i++) {
            String built = snapshot.get(i).build(0).trim();
            boolean isReturnStmt = built.startsWith("return");
            boolean isImplicitReturn = isLambda && i == snapshot.size() - 1 && !isReturnStmt;
            if (isReturnStmt || isImplicitReturn) {
                returnIdx = i;
                break;
            }
        }

        if (returnIdx == -1) {
            container.addChildRaw(newChild);
        } else {
            container.clear();
            for (int i = 0; i < returnIdx; i++) container.addChildRaw(snapshot.get(i));
            container.addChildRaw(newChild);
            for (int i = returnIdx; i < snapshot.size(); i++) container.addChildRaw(snapshot.get(i));
        }
    }

    public static long countChildrenOfType(IBuilder node, Class<?> type) {
        return node.children().stream()
                .filter(type::isInstance)
                .count();
    }

    public static String pickType(ScopeContext scope, Random rng) {
        List<String> types = new ArrayList<>(scope.getTypeParams());
        types.remove("Unit");

        // Non-generic classes only as plain strings
        for (SymbolTable.ClassMeta cls : scope.symbols().getAllClasses()) {
            if (cls.typeParams().isEmpty()) {
                types.add(cls.name());
            }
            // Generic classes handled via pickClassRef() + VariableBuilder.ofClass()
        }

        if (types.isEmpty()) return "Int";
        return types.get(rng.nextInt(types.size()));
    }

    public static String pickVar(ScopeContext scope, Random rng, String requiredType) {
        List<String> vars = scope.getVars().entrySet().stream()
                .filter(e -> !e.getValue().contains("->"))
                .filter(e -> !e.getValue().contains("<"))
                .filter(e -> requiredType == null || e.getValue().equals(requiredType))
                .map(Map.Entry::getKey)
                .toList();
        if (vars.isEmpty()) return null;
        return vars.get(rng.nextInt(vars.size()));
    }

    public static String pickVar(ScopeContext scope, Random rng) {
        return pickVar(scope, rng, null);
    }

    public static String pickNullableType(ScopeContext scope, Random rng) {
        return pickType(scope, rng) + "?";
    }

    public static ClassBuilder pickClassRef(ScopeContext scope, Random rng) {
        List<SymbolTable.ClassMeta> classes = scope.symbols().getAllClasses();
        if (classes.isEmpty()) return null;
        return classes.get(rng.nextInt(classes.size())).classRef();
    }
}