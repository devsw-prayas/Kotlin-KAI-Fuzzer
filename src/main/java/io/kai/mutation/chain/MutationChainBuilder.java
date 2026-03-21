package io.kai.mutation.chain;

import io.kai.contracts.IBuilder;
import io.kai.mutation.IMutationPolicy;
import io.kai.mutation.MutationContext;
import io.kai.mutation.MutationRegistry;
import io.kai.scheduler.IScheduler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MutationChainBuilder {
    private final MutationRegistry registry;
    private final int maxDepth;
    private final IScheduler scheduler;

    public MutationChainBuilder(MutationRegistry registry, int maxDepth, IScheduler scheduler) {
        this.registry = registry;
        this.maxDepth = maxDepth;
        this.scheduler = scheduler;
    }

    public MutationChain build(IBuilder root, MutationContext ctx) {
        List<MutationStep> steps = new ArrayList<>();
        Set<String> mutatedNodes = new HashSet<>();

        for (int i = 0; i < maxDepth; i++) {
            // Pick a node that hasn't been mutated yet in this chain
            IBuilder node = selectNode(root, ctx, mutatedNodes);
            if (node == null) break; // all nodes exhausted

            // Get compatible policies for that node
            List<IMutationPolicy> candidates = registry.policiesFor(node);
            if (candidates.isEmpty()) continue;

            // Update -> Uses the new nastiness based selector
            IMutationPolicy policy = scheduler.selectPolicy(node, candidates, ctx.stats());

            // Record the step and mark node as used
            steps.add(new MutationStep(node.id(), policy.id()));
            mutatedNodes.add(node.id());
        }

        return new MutationChain(List.copyOf(steps));
    }

    private IBuilder selectNode(IBuilder root, MutationContext ctx, Set<String> excluded) {
        List<IBuilder> all = new ArrayList<>();
        collectAll(root, all);
        // Filter out already mutated nodes and nodes with no compatible policies
        List<IBuilder> candidates = all.stream()
                .filter(n -> !excluded.contains(n.id()))
                .filter(n -> !registry.policiesFor(n).isEmpty())
                .toList();
        if (candidates.isEmpty()) return null;
        return candidates.get(ctx.rng().nextInt(candidates.size()));
    }

    private void collectAll(IBuilder node, List<IBuilder> acc) {
        acc.add(node);
        for (var child : node.children()) {
            collectAll(child, acc);
        }
    }
}