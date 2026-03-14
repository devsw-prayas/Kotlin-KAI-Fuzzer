package io.kai.contracts;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class NameRegistry {
    private final ConcurrentHashMap<String, AtomicInteger> registry = new ConcurrentHashMap<>();

    public Map<String, Integer> snapshot(){
        HashMap<String, Integer> outputMap = new HashMap<>();
        for (String s : registry.keySet())
            outputMap.put(s, registry.get(s).get());
        return outputMap;
    }

    public void restore(Map<String, Integer> snapshot){
        registry.clear();
        for(String s : snapshot.keySet()){
            registry.put(s, new AtomicInteger(snapshot.get(s)));
        }
    }

    public String next(String prefix){
            return prefix + "_" + registry.computeIfAbsent(prefix, (n) ->{
                return new AtomicInteger(0);
        }).getAndIncrement();
    }
}
