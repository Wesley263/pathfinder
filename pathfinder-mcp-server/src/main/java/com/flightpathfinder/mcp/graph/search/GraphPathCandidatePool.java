package com.flightpathfinder.mcp.graph.search;

import com.flightpathfinder.mcp.graph.model.RestoredCandidatePath;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;

/**
 * Bounded pool for completed candidate paths.
 *
 * <p>The pool exists so completed paths can compete against frontier states immediately,
 * which enables early-stop and admission decisions without keeping every candidate forever.</p>
 */
final class GraphPathCandidatePool {

    private static final Comparator<RankedCandidate> ORDER = Comparator
            .comparingDouble(RankedCandidate::score)
            .thenComparingLong(RankedCandidate::sequence);

    private final int maxCandidateCount;
    private final NavigableSet<RankedCandidate> candidates = new TreeSet<>(ORDER);
    private long sequence = 0L;

    GraphPathCandidatePool(int maxCandidateCount) {
        this.maxCandidateCount = maxCandidateCount;
    }

    /**
     * Admits a completed candidate into the bounded pool, evicting the current worst entry when needed.
     */
    void admit(RestoredCandidatePath candidate, double score) {
        candidates.add(new RankedCandidate(candidate, score, sequence++));
        if (candidates.size() > maxCandidateCount) {
            candidates.pollFirst();
        }
    }

    /**
     * Checks whether a partial state's optimistic score is already too weak to enter the pool.
     */
    boolean shouldRejectPotential(double optimisticScore, double slack) {
        return candidates.size() >= maxCandidateCount
                && optimisticScore + slack < worstScore();
    }

    /**
     * Checks whether frontier exploration can stop because the best remaining frontier node
     * can no longer beat the pool's current floor by the configured closure slack.
     */
    boolean shouldStop(double bestFrontierScore, GraphPathFrontierPolicy policy) {
        return candidates.size() >= policy.earlyStopCandidateCount()
                && bestFrontierScore + policy.frontierClosureSlack() < worstScore();
    }

    List<RestoredCandidatePath> admittedCandidates() {
        List<RestoredCandidatePath> admitted = new ArrayList<>(candidates.size());
        for (RankedCandidate candidate : candidates.descendingSet()) {
            admitted.add(candidate.path());
        }
        return List.copyOf(admitted);
    }

    private double worstScore() {
        if (candidates.isEmpty()) {
            return Double.NEGATIVE_INFINITY;
        }
        return candidates.first().score();
    }

    private record RankedCandidate(RestoredCandidatePath path, double score, long sequence) {
    }
}
