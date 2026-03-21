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

    // Centroid state
    private double centroidDepth = 0.0;
    private double centroidSiblingIndex = 0.0;
    private int stepCount = 0;

    private static final double ALPHA = 0.12;
    private static final double REPULSION_STRENGTH = 0.15;
    private static final int WARMUP_STEPS = 3;

    // Lightweight spatial coordinate
    private record NodePosition(int depth, int siblingIndex) {}

    public CentroidWeightedScheduler(Random rng, int maxDepth) {
        this.rng = rng;
        this.maxDepth = maxDepth;
    }

    @Override
    public IBuilder selectSeed(List<IBuilder> corpus) {
        if (corpus.isEmpty()) throw new IllegalStateException("Corpus is empty");
        if (corpus.size() == 1) return corpus.get(0);

        double[] weights = new double[corpus.size()];
        double total = 0;
        for (int i = 0; i < corpus.size(); i++) {
            double w = FuzzerRuntime.get().builderWeight(corpus.get(i));
            weights[i] = w;
            total += w;
        }
        // Reset centroid on new seed selection
        centroidDepth = 0.0;
        centroidSiblingIndex = 0.0;
        stepCount = 0;
        return corpus.get(weightedRandom(weights, total));
    }

    public IBuilder selectNode(IBuilder root) {
        List<IBuilder> nodes = new ArrayList<>();
        List<NodePosition> positions = new ArrayList<>();
        collectAll(root, 0, 0, nodes, positions);

        if (nodes.isEmpty()) return root;

        double[] weights = new double[nodes.size()];
        double total = 0;
        for (int i = 0; i < nodes.size(); i++) {
            int depth = positions.get(i).depth();
            int jitterRange = Math.min(2, Math.max(1, maxDepth - depth + 1));
            double depthJitter = (rng.nextDouble() * 2 - 1) * jitterRange;
            double repulsion = computeRepulsion(positions.get(i));
            double weight = Math.max(0.01,
                    FuzzerRuntime.get().builderWeight(nodes.get(i))
                            + depthJitter + repulsion);
            weights[i] = weight;
            total += weight;
        }

        int idx = weightedRandom(weights, total);
        updateCentroid(positions.get(idx));
        return nodes.get(idx);
    }

    @Override
    public IMutationPolicy selectPolicy(IBuilder node,
                                        List<IMutationPolicy> candidates,
                                        MutationStats stats) {
        if (candidates.isEmpty()) throw new IllegalStateException("No candidates");
        if (candidates.size() == 1) return candidates.get(0);

        double interestScore = FuzzerRuntime.get().builderWeight(node);
        double jitter = (rng.nextDouble() * 2 - 1) * (1.0 / maxDepth);
        double effectiveWeight = Math.max(0.01, Math.min(1.0, interestScore + jitter));
        double targetNastiness = 1.0 - effectiveWeight;

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

    private void updateCentroid(NodePosition pos) {
        centroidDepth = ALPHA * pos.depth() + (1 - ALPHA) * centroidDepth;
        centroidSiblingIndex = ALPHA * pos.siblingIndex() + (1 - ALPHA) * centroidSiblingIndex;
        stepCount++;
    }

    private double computeRepulsion(NodePosition pos) {
        if (stepCount < WARMUP_STEPS) return 0.0;
        double dDepth = pos.depth() - centroidDepth;
        double dSibling = pos.siblingIndex() - centroidSiblingIndex;
        double distance = Math.sqrt(dDepth * dDepth + dSibling * dSibling);
        return distance * REPULSION_STRENGTH;
    }

    private void collectAll(IBuilder node, int depth, int siblingIndex,
                            List<IBuilder> nodes, List<NodePosition> positions) {
        nodes.add(node);
        positions.add(new NodePosition(depth, siblingIndex));
        List<? extends IBuilder> children = node.children();
        for (int i = 0; i < children.size(); i++) {
            collectAll(children.get(i), depth + 1, i, nodes, positions);
        }
    }

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