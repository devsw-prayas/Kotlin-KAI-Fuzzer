package io.kai.mutation.chain;

import io.kai.contracts.IBuilder;
import io.kai.mutation.IMutationPolicy;
import io.kai.mutation.MutationContext;
import io.kai.mutation.MutationRegistry;

import java.util.ArrayList;
import java.util.List;

public class MutationChainBuilder {
    private final MutationRegistry registry;
    private final int maxDepth;

    public MutationChainBuilder(MutationRegistry registry, int maxDepth) {
        this.registry = registry;
        this.maxDepth = maxDepth;
    }

    public MutationChain build(IBuilder root, MutationContext ctx) {
        List<MutationStep> steps = new ArrayList<>();
        IBuilder current = root;

        for (int i = 0; i < maxDepth; i++) {
            // Pick a random node from the tree
            IBuilder node = selectNode(current, ctx);
            if (node == null) continue;

            // Get compatible policies for that node
            List<IMutationPolicy> candidates = registry.policiesFor(node);
            if (candidates.isEmpty()) continue;

            // Pick a random policy
            IMutationPolicy policy = candidates.get(ctx.rng().nextInt(candidates.size()));

            // Record the step
            steps.add(new MutationStep(node.id(), policy.id()));
        }

        return new MutationChain(List.copyOf(steps));
    }

    /**
     * Collects all nodes in the tree via DFS, then picks one at random.
     */
    private IBuilder selectNode(IBuilder root, MutationContext ctx) {
        List<IBuilder> all = new ArrayList<>();
        collectAll(root, all);
        if (all.isEmpty()) return null;
        return all.get(ctx.rng().nextInt(all.size()));
    }

    private void collectAll(IBuilder node, List<IBuilder> acc) {
        acc.add(node);
        for (var child : node.children()) {
            collectAll(child, acc);
        }
    }
}