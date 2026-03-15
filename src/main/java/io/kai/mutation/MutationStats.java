package io.kai.mutation;

import java.util.HashMap;

public class MutationStats {
    private final HashMap<String, Integer> appliedPolicies;

    public MutationStats(){
        appliedPolicies = new HashMap<>();
    }

    public void increment(String policy){
        appliedPolicies.merge(policy, 1, Integer::sum);
    }

    public Integer getCount(String policy){
        return appliedPolicies.getOrDefault(policy, 0);
    }
}
