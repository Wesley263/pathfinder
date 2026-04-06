package com.flightpathfinder.mcp.graph.search;

import com.flightpathfinder.mcp.graph.model.RestoredCandidatePath;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;

/**
 * 已完成候选路径的有界池。
 *
 * <p>候选池让已完成路径可以立即与 frontier 状态竞争，
 * 从而支持提前收敛与准入决策，而不必永久保留全部候选。</p>
 */
final class GraphPathCandidatePool {

    /** 候选排序规则：分数优先，序列号打破平分。 */
    private static final Comparator<RankedCandidate> ORDER = Comparator
            .comparingDouble(RankedCandidate::score)
            .thenComparingLong(RankedCandidate::sequence);

    /** 候选池最大容量。 */
    private final int maxCandidateCount;
    /** 当前候选集合。 */
    private final NavigableSet<RankedCandidate> candidates = new TreeSet<>(ORDER);
    /** 入池序列号。 */
    private long sequence = 0L;

    /**
     * 构造有界候选池。
     *
     * @param maxCandidateCount 最大候选数
     */
    GraphPathCandidatePool(int maxCandidateCount) {
        this.maxCandidateCount = maxCandidateCount;
    }

    /**
     * 把一个完成候选加入有界池；超出容量时淘汰最差候选。
     */
    void admit(RestoredCandidatePath candidate, double score) {
        candidates.add(new RankedCandidate(candidate, score, sequence++));
        if (candidates.size() > maxCandidateCount) {
            candidates.pollFirst();
        }
    }

    /**
     * 判断部分状态的乐观分是否已不足以进入候选池。
     */
    boolean shouldRejectPotential(double optimisticScore, double slack) {
        return candidates.size() >= maxCandidateCount
                && optimisticScore + slack < worstScore();
    }

    /**
     * 判断是否可提前停止 frontier 扩展。
     *
     * <p>当 frontier 最优分加上收敛松弛后仍无法超过当前候选池下限时，可停止扩展。</p>
     */
    boolean shouldStop(double bestFrontierScore, GraphPathFrontierPolicy policy) {
        return candidates.size() >= policy.earlyStopCandidateCount()
                && bestFrontierScore + policy.frontierClosureSlack() < worstScore();
    }

    /**
     * 返回按分数降序排列的候选路径。
     *
     * @return 已准入候选路径
     */
    List<RestoredCandidatePath> admittedCandidates() {
        List<RestoredCandidatePath> admitted = new ArrayList<>(candidates.size());
        for (RankedCandidate candidate : candidates.descendingSet()) {
            admitted.add(candidate.path());
        }
        return List.copyOf(admitted);
    }

    /**
     * 返回候选池当前最差分。
     *
     * @return 最差分
     */
    private double worstScore() {
        if (candidates.isEmpty()) {
            return Double.NEGATIVE_INFINITY;
        }
        return candidates.first().score();
    }

    /**
     * 候选路径及其排序信息。
     */
    private record RankedCandidate(RestoredCandidatePath path, double score, long sequence) {
    }
}
