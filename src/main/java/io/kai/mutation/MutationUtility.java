package io.kai.mutation;

import io.kai.contracts.IBuilder;

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
}