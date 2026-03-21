package io.kai.mutation;

import io.kai.builders.*;
import io.kai.builders.expressions.LambdaBuilder;
import io.kai.contracts.IBuilder;

import io.kai.mutation.context.*;

import java.util.LinkedHashMap;
import java.util.List;

public class ScopeContextBuilder {
    public static ScopeContext buildFor(IBuilder root, String targetId) {
        ScopeContext rootCtx = new ScopeContext(null);
        ScopeContext[] result = new ScopeContext[1];
        walk(root, targetId, rootCtx, result);
        return result[0] != null ? result[0] : rootCtx;
    }

    private static boolean walk(IBuilder node, String targetId,
                                ScopeContext current, ScopeContext[] result) {
        // Determine if this node introduces a new scope level FIRST
        ScopeContext next;
        if (node instanceof ObjectBuilder ob && ob.isCompanion()) {
            next = current.enterIsolated();
        } else {
            next = introducesScope(node) ? current.enter() : current;
        }

        // Variables: walk children BEFORE registering
        // prevents self-reference (var_5 referencing var_5 in its own initializer)
        if (node instanceof VariableBuilder vb) {
            if (node.id().equals(targetId)) {
                result[0] = next;
                return true;
            }
            for (IBuilder child : node.children()) {
                if (walk(child, targetId, current, result)) return true;
            }
            String fullType = vb.getType() + (vb.isNullable() ? "?" : "");
            current.valueScope().declareVar(vb.id(), fullType);
            return false;
        }

        // Register into the correct scope
        register(node, current, next);

        // Check if this is the target
        if (node.id().equals(targetId)) {
            result[0] = next;
            return true;
        }

        // Walk children in the next scope
        for (IBuilder child : node.children()) {
            if (walk(child, targetId, next, result)) return true;
        }

        return false;
    }
    private static void register(IBuilder node, ScopeContext current, ScopeContext next) {
        if (node instanceof FunctionBuilder fb) {
            // Function declared in current scope (siblings can see it)
            current.symbols().declareFunction(new SymbolTable.FunctionMeta(
                    fb.id(),
                    new LinkedHashMap<>(fb.getTypeParams()),
                    fb.isSuspend(),
                    fb.isInline()
            ));
            // Type params scoped to the function's own scope (next)
            for (String tp : fb.getTypeParams().keySet()) {
                next.typeScope().declare(tp);
            }
        } else if (node instanceof ClassBuilder cb) {
            // Class declared in current scope
            current.symbols().declareClass(new SymbolTable.ClassMeta(
                    cb.id(),
                    new LinkedHashMap<>(cb.getTypeParams()),
                    cb.isSealed(),
                    cb.isData(),
                    cb.isAbstract(),
                    cb.isOpen(),
                    cb.isObject(),
                    cb
            ));
            // Type params scoped to the class's own scope (next)
            for (String tp : cb.getTypeParams().keySet()) {
                next.typeScope().declare(tp);
            }
        }
    }

    private static boolean introducesScope(IBuilder node) {
        return node instanceof FunctionBuilder
                || node instanceof ClassBuilder
                || node instanceof LambdaBuilder
                || node instanceof ObjectBuilder
                || node instanceof TryCatchBuilder;
    }
}