package io.kai.mutation;

import io.kai.contracts.IBuilder;
import io.kai.contracts.capability.IContainer;

import java.util.List;

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
        int returnIdx = -1;
        for (int i = 0; i < snapshot.size(); i++) {
            if (snapshot.get(i).build(0).trim().startsWith("return")) {
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
}