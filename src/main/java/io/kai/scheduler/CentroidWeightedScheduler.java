package io.kai.scheduler;

import io.kai.contracts.IBuilder;
import io.kai.compiler.OracleVerdict;
import io.kai.fuzzer.FuzzerRuntime;
import io.kai.mutation.IMutationPolicy;
import io.kai.mutation.MutationStats;

import java.util.*;

public class CentroidWeightedScheduler implements IScheduler {

    private final Random rng;
    private final int maxDepth;

    public CentroidWeightedScheduler(Random rng, int maxDepth) {
        this.rng = rng;
        this.maxDepth = maxDepth;
    }


    @Override
    public IBuilder selectSeed(List<IBuilder> corpus) {
        if (corpus.isEmpty()) throw new IllegalStateException("Corpus is empty");
        if (corpus.size() == 1) return corpus.get(0);

        // Weight each seed by the interest score of its root node
        double[] weights = new double[corpus.size()];
        double total = 0;
        for (int i = 0; i < corpus.size(); i++) {
            double w = FuzzerRuntime.get().builderWeight(corpus.get(i));
            weights[i] = w;
            total += w;
        }
        return corpus.get(weightedRandom(weights, total));
    }


    /**
     * Collects all nodes in the tree with their depths, then picks one via
     * weighted random where weight = interestScore + depthJitter.
     * Shallower nodes get larger jitter range — biases toward structural nodes.
     */
    public IBuilder selectNode(IBuilder root) {
        List<IBuilder> nodes = new ArrayList<>();
        List<Integer> depths = new ArrayList<>();
        collectAll(root, 0, nodes, depths);

        if (nodes.isEmpty()) return root;

        double[] weights = new double[nodes.size()];
        double total = 0;
        for (int i = 0; i < nodes.size(); i++) {
            int depth = depths.get(i);
            int jitterRange = Math.max(1, maxDepth - depth + 1);
            double jitter = (rng.nextDouble() * 2 - 1) * jitterRange; // [-jitterRange, +jitterRange]
            double weight = Math.max(0.01, FuzzerRuntime.get().builderWeight(nodes.get(i)) + jitter);
            weights[i] = weight;
            total += weight;
        }
        return nodes.get(weightedRandom(weights, total));
    }


    /**
     * Computes effectiveWeight for the node, then picks the policy whose
     * nastiness is closest to targetNastiness = 1.0 - effectiveWeight.
     * High interest node → low nastiness target → nastiest mutation applied.
     */
    @Override
    public IMutationPolicy selectPolicy(IBuilder node,
                                        List<IMutationPolicy> candidates,
                                        MutationStats stats) {
        if (candidates.isEmpty()) throw new IllegalStateException("No candidates");
        if (candidates.size() == 1) return candidates.get(0);

        double interestScore = FuzzerRuntime.get().builderWeight(node);
        int jitter = rng.nextInt(Math.max(1, maxDepth));
        double effectiveWeight = Math.min(1.0, interestScore + (jitter / (double) maxDepth));
        double targetNastiness = 1.0 - effectiveWeight;

        // Pick policy whose nastiness is closest to targetNastiness
        IMutationPolicy best = candidates.get(0);
        double bestDiff = Math.abs(FuzzerRuntime.get().nastiness(best.id()) - targetNastiness);

        for (IMutationPolicy candidate : candidates) {
            double diff = Math.abs(FuzzerRuntime.get().nastiness(candidate.id()) - targetNastiness);
            if (diff < bestDiff) {
                bestDiff = diff;
                best = candidate;
            }
        }
        return best;
    }

    @Override
    public void onResult(IBuilder seed, IMutationPolicy policy, OracleVerdict verdict) {
        // MVP-2: no-op — LLM score tuning deferred to MVP-3
    }


    private void collectAll(IBuilder node, int depth,
                             List<IBuilder> nodes, List<Integer> depths) {
        nodes.add(node);
        depths.add(depth);
        for (var child : node.children()) {
            collectAll(child, depth + 1, nodes, depths);
        }
    }

    /**
     * Weighted random selection — returns index.
     */
    private int weightedRandom(double[] weights, double total) {
        double r = rng.nextDouble() * total;
        double cumulative = 0;
        for (int i = 0; i < weights.length; i++) {
            cumulative += weights[i];
            if (r <= cumulative) return i;
        }
        return weights.length - 1;
    }
}
