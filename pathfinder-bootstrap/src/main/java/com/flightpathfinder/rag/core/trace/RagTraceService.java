package com.flightpathfinder.rag.core.trace;

import com.flightpathfinder.rag.core.answer.AnswerResult;
import com.flightpathfinder.rag.core.pipeline.StageOneRagResult;
import com.flightpathfinder.rag.core.retrieve.RetrievalResult;
import java.time.Instant;

/**
 * 说明。
 *
 * 说明。
 * 应用层因此可以记录阶段事实，而无需让各阶段自行管理持久化与上下文生命周期。
 */
public interface RagTraceService {

    /**
     * 说明。
     *
     * @param requestId 当前查询请求标识
     * @param conversationId 与请求关联的会话标识（可选）
     * @return 返回结果。
     */
    RagTraceSession startQueryTrace(String requestId, String conversationId);

    /**
     * 说明。
     *
     * @param session 参数说明。
     * @param startedAt 编排层捕获的阶段开始时间
     * @param stageOneResult 参数说明。
     */
    void recordStageOne(RagTraceSession session, Instant startedAt, StageOneRagResult stageOneResult);

    /**
     * 说明。
     *
     * @param session 参数说明。
     * @param startedAt 编排层捕获的阶段开始时间
     * @param retrievalResult 参数说明。
     */
    void recordRetrieval(RagTraceSession session, Instant startedAt, RetrievalResult retrievalResult);

    /**
     * 说明。
     *
     * @param session 参数说明。
     * @param startedAt 编排层捕获的阶段开始时间
     * @param answerResult 参数说明。
     */
    void recordFinalAnswer(RagTraceSession session, Instant startedAt, AnswerResult answerResult);

    /**
     * 说明。
     *
     * @param session 参数说明。
     * @param stageName 失败阶段名称
     * @param startedAt 编排层捕获的阶段开始时间
     * @param throwable 终止阶段的异常
     */
    void recordFailure(RagTraceSession session, String stageName, Instant startedAt, Throwable throwable);

    /**
     * 说明。
     *
     * @param session 参数说明。
     * @param overallStatus 编排结束后的整体请求状态
     * @param snapshotMissOccurred 是否发生快照缺失并影响请求结果
     * @return 返回结果。
     */
    RagTraceResult finish(RagTraceSession session, String overallStatus, boolean snapshotMissOccurred);

    /**
     * 说明。
     */
    void clear();
}
