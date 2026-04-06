package com.flightpathfinder.rag.core.trace;

import com.flightpathfinder.rag.core.answer.AnswerResult;
import com.flightpathfinder.rag.core.pipeline.StageOneRagResult;
import com.flightpathfinder.rag.core.retrieve.RetrievalResult;
import java.time.Instant;

/**
 * RAG 查询链路追踪服务。
 *
 * 统一管理 trace 的开始、分阶段记录、失败记录、结束持久化与上下文清理。
 * 应用层因此可以记录阶段事实，而无需让各阶段自行管理持久化与上下文生命周期。
 */
public interface RagTraceService {

    /**
     * 启动一次查询 trace。
     *
     * @param requestId 当前查询请求标识
     * @param conversationId 与请求关联的会话标识（可选）
     * @return trace 会话对象
     */
    RagTraceSession startQueryTrace(String requestId, String conversationId);

    /**
     * 记录第一阶段执行结果。
     *
     * @param session trace 会话
     * @param startedAt 编排层捕获的阶段开始时间
     * @param stageOneResult 第一阶段结果
     */
    void recordStageOne(RagTraceSession session, Instant startedAt, StageOneRagResult stageOneResult);

    /**
     * 记录检索阶段执行结果。
     *
     * @param session trace 会话
     * @param startedAt 编排层捕获的阶段开始时间
     * @param retrievalResult 检索阶段结果
     */
    void recordRetrieval(RagTraceSession session, Instant startedAt, RetrievalResult retrievalResult);

    /**
     * 记录最终回答阶段执行结果。
     *
     * @param session trace 会话
     * @param startedAt 编排层捕获的阶段开始时间
     * @param answerResult 最终回答结果
     */
    void recordFinalAnswer(RagTraceSession session, Instant startedAt, AnswerResult answerResult);

    /**
     * 记录阶段失败信息。
     *
     * @param session trace 会话
     * @param stageName 失败阶段名称
     * @param startedAt 编排层捕获的阶段开始时间
     * @param throwable 终止阶段的异常
     */
    void recordFailure(RagTraceSession session, String stageName, Instant startedAt, Throwable throwable);

    /**
     * 结束 trace 并返回汇总结果。
     *
     * @param session trace 会话
     * @param overallStatus 编排结束后的整体请求状态
     * @param snapshotMissOccurred 是否发生快照缺失并影响请求结果
     * @return trace 汇总结果
     */
    RagTraceResult finish(RagTraceSession session, String overallStatus, boolean snapshotMissOccurred);

    /**
     * 清理当前线程 trace 上下文。
     */
    void clear();
}
