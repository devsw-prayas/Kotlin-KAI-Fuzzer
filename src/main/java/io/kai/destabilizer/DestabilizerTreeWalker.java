package io.kai.destabilizer;

import io.kai.contracts.IBuilder;

import java.util.ArrayList;
import java.util.List;


public class DestabilizerTreeWalker {

    public static <T extends IBuilder> List<T> findAll(IBuilder root, Class<T> type) {
        List<T> result = new ArrayList<>();
        walk(root, type, result);
        return result;
    }

    private static <T> void walk(IBuilder node, Class<T> type, List<T> acc) {
        if (type.isInstance(node)) acc.add(type.cast(node));
        for (IBuilder child : node.childrenRaw()) {
            walk(child, type, acc);
        }
    }
}
