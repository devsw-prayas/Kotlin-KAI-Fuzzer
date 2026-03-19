package io.kai.mutation.chain;

import io.kai.contracts.IBuilder;
import io.kai.contracts.capability.IBranchContainer;
import io.kai.contracts.capability.IContainer;
import io.kai.mutation.*;
import io.kai.mutation.context.ScopeContext;

import java.util.List;

public record MutationChain(List<MutationStep> steps) {
    public IBuilder applyTo(IBuilder root, MutationRegistry registry, MutationContext ctx) {
        IBuilder current = root;

        for (MutationStep step : steps) {
            // 1. Find the target node
            IBuilder target = MutationUtility.findById(current, step.nodeID());
            if (target == null) continue;

            // 2. Find the policy by id
            IMutationPolicy policy = registry.policiesFor(target)
                    .stream()
                    .filter(p -> p.id().equals(step.policyID()))
                    .findFirst()
                    .orElse(null);
            if (policy == null) continue;

            ScopeContext scope = ScopeContextBuilder.buildFor(current, target.id());
            MutationContext stepCtx = new MutationContext(
                    ctx.rng(), scope, ctx.depth(), ctx.stats(), ctx.registry(), ctx.root()
            );

            // 4. Apply the policy — get the mutated node
            IBuilder mutated = policy.apply(target, ctx);

            // 5. If the policy returned the same node (in-place mutation like addChild),
            //    nothing more to do — the list was already mutated.
            //    If it returned a NEW node (like ExpandExpressionMutation),
            //    we need to swap it into the parent.
            if (mutated != target) {
                IBuilder parent = MutationUtility.findParent(current, target.id());
                if (parent == null) {
                    // target was the root itself — replace root
                    current = mutated;
                } else {
                    swapChild(parent, target, mutated);
                }
            }

            // 6. Update stats
            ctx.stats().increment(policy.id());
        }

        return current;
    }

    /**
     * Returns the policy id of the last step, for scheduler feedback.
     */
    public String lastPolicy() {
        if (steps.isEmpty()) return "";
        return steps.getLast().policyID();
    }

    /**
     * Swaps oldChild for newChild in the parent's mutable list.
     * Works for both IContainer and IBranchContainer parents.
     */
    @SuppressWarnings("unchecked")
    private void swapChild(IBuilder parent, IBuilder oldChild, IBuilder newChild) {
        if (parent instanceof IBranchContainer<?> bc) {
            // Find which branch contains the old child and swap there
            for (int i = 0; i < bc.branchLength(); i++) {
                List<IBuilder> branch = (List<IBuilder>) bc.getBranch(i);
                int idx = branch.indexOf(oldChild);
                if (idx >= 0) {
                    branch.set(idx, newChild);
                    return;
                }
            }
        } else if (parent instanceof IContainer<?> c) {
            // Get the underlying list via children() — it's the same ArrayList
            // so mutating it mutates the parent directly
            List<IBuilder> list = (List<IBuilder>) parent.children();
            int idx = list.indexOf(oldChild);
            if (idx >= 0) list.set(idx, newChild);
        }
    }
}