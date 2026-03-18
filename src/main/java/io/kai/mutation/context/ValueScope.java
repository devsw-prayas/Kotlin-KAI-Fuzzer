package io.kai.mutation.context;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ValueScope {
    private final Map<String, String> vars = new LinkedHashMap<>();

    public void declareVar(String name, String type) {
        vars.putIfAbsent(name, type);
    }

    public Map<String, String> getVars() {
        return Collections.unmodifiableMap(vars);
    }
}
