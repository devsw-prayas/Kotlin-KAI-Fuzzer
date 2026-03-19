package io.kai.mutation;

import io.kai.builders.ClassBuilder;
import io.kai.builders.FunctionBuilder;
import io.kai.builders.VariableBuilder;
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
        // Register this node into current scope
        register(node, current);

        // Check if this is the target
        if (node.id().equals(targetId)) {
            result[0] = current;
            return true;
        }

        // Determine if this node introduces a new scope level
        ScopeContext next = introducesScope(node) ? current.enter() : current;

        // Walk children
        for (IBuilder child : node.children()) {
            if (walk(child, targetId, next, result)) return true;
        }

        return false;
    }

    private static void register(IBuilder node, ScopeContext ctx) {
        if (node instanceof FunctionBuilder fb) {
            ctx.symbols().declareFunction(new SymbolTable.FunctionMeta(
                    fb.id(),
                    new LinkedHashMap<>(fb.getTypeParams()),
                    fb.isSuspend(),
                    fb.isInline()
            ));
            for (String tp : fb.getTypeParams().keySet()) {
                ctx.typeScope().declare(tp);
            }
        } else if (node instanceof ClassBuilder cb) {
            ctx.symbols().declareClass(new SymbolTable.ClassMeta(
                    cb.id(),
                    new LinkedHashMap<>(cb.getTypeParams()),
                    cb.isSealed(),
                    cb.isData()
            ));
            for (String tp : cb.getTypeParams().keySet()) {
                ctx.typeScope().declare(tp);
            }
        } else if (node instanceof VariableBuilder vb) {
            ctx.valueScope().declareVar(vb.id(), vb.getType());
        }
    }

    private static boolean introducesScope(IBuilder node) {
        return node instanceof FunctionBuilder
                || node instanceof ClassBuilder;
    }
}