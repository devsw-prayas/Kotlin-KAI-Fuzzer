package io.kai.mutation;

import io.kai.contracts.IBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MutationRegistry {
    private final HashMap<Class<? extends IBuilder>, List<IMutationPolicy>> mapping;

    public MutationRegistry(){
        mapping = new HashMap<>();
    }

    public void register(Class<? extends IBuilder> nodeType, IMutationPolicy policy){
        mapping.computeIfAbsent(nodeType, (n) -> new ArrayList<>()).add(policy);
    }

    public List<IMutationPolicy> policiesFor(IBuilder node) {
        List<IMutationPolicy> mutators = mapping.get(node.getClass());
        if (mutators == null) return List.of();
        return mutators.stream()
                .filter(p -> p.compatibleWith(node))
                .toList();
    }
}
