package io.kai.mutation.context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ScopeContext {
    private final TypeScope typeScope;
    private final ValueScope valueScope;
    private final SymbolTable symbols;
    private final ScopeContext parent;
    private final boolean isolated;

    public ScopeContext(ScopeContext parent) {
        this(parent, false);
    }

    public ScopeContext(ScopeContext parent, boolean isolated) {
        this.typeScope = new TypeScope();
        this.valueScope = new ValueScope();
        this.parent = parent;
        this.isolated = isolated;
        this.symbols = parent == null ? new SymbolTable() : parent.symbols;
        if (parent == null) {
            typeScope.declare("Int");
            typeScope.declare("String");
            typeScope.declare("Boolean");
            typeScope.declare("Unit");
        }
    }

    public ScopeContext enter() { return new ScopeContext(this, false); }
    public ScopeContext enterIsolated() { return new ScopeContext(this, true); }

    public ScopeContext exit() {
        return parent;
    }

    // Walks chain to root, collects all type params
    public List<String> getTypeParams() {
        List<String> result = new ArrayList<>(typeScope.getParams());
        if (parent != null && !isolated) result.addAll(parent.getTypeParams());
        return result;
    }

    // Walks chain to root, collects all vars
    public Map<String, String> getVars() {
        Map<String, String> result = new LinkedHashMap<>();
        if (parent != null) result.putAll(parent.getVars());
        result.putAll(valueScope.getVars()); // inner scope overrides outer
        return result;
    }

    public TypeScope typeScope() { return typeScope; }
    public ValueScope valueScope() { return valueScope; }
    public SymbolTable symbols() { return symbols; }
    public ScopeContext parent() { return parent; }
}
