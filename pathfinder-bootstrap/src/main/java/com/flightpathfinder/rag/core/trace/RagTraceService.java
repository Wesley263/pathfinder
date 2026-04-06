package com.flightpathfinder.rag.core.trace;

import com.flightpathfinder.rag.core.answer.AnswerResult;
import com.flightpathfinder.rag.core.pipeline.StageOneRagResult;
import com.flightpathfinder.rag.core.retrieve.RetrievalResult;
import java.time.Instant;

/**
 * 请求级 RAG 跟踪生命周期服务。
 *
 * <p>该抽象将 trace 编排从 stage one、检索与最终回答等业务服务中解耦。
 * 应用层因此可以记录阶段事实，而无需让各阶段自行管理持久化与上下文生命周期。
 */
public interface RagTraceService {

    /**
     * 启动新的请求级 trace 会话。
     *
     * @param requestId 当前查询请求标识
     * @param conversationId 与请求关联的会话标识（可选）
     * @return 贯穿当前查询生命周期的可变 trace 会话
     */
    RagTraceSession startQueryTrace(String requestId, String conversationId);

    /**
     * 将 stage-one 结果记录到当前 trace 会话。
     *
     * @param session 当前 trace 会话
     * @param startedAt 编排层捕获的阶段开始时间
     * @param stageOneResult 需要汇总入 trace 的 stage-one 输出
     */
    void recordStageOne(RagTraceSession session, Instant startedAt, StageOneRagResult stageOneResult);

    /**
     * 将检索阶段事实记录到当前 trace 会话。
     *
     * @param session 当前 trace 会话
     * @param startedAt 编排层捕获的阶段开始时间
     * @param retrievalResult 包含 KB 与 MCP 汇总的检索输出
     */
    void recordRetrieval(RagTraceSession session, Instant startedAt, RetrievalResult retrievalResult);

    /**
     * 将最终回答阶段记录到当前 trace 会话。
     *
     * @param session 当前 trace 会话
     * @param startedAt 编排层捕获的阶段开始时间
     * @param answerResult 需要汇总入 trace 的最终回答结果
     */
    void recordFinalAnswer(RagTraceSession session, Instant startedAt, AnswerResult answerResult);

    /**
     * 在 trace 完成前记录异常阶段失败信息。
     *
     * @param session 当前 trace 会话
     * @param stageName 失败阶段名称
     * @param startedAt 编排层捕获的阶段开始时间
     * @param throwable 终止阶段的异常
     */
    void recordFailure(RagTraceSession session, String stageName, Instant startedAt, Throwable throwable);

    /**
     * 完成 trace 收尾并尝试持久化。
     *
     * @param session 当前 trace 会话
     * @param overallStatus 编排结束后的整体请求状态
     * @param snapshotMissOccurred 是否发生快照缺失并影响请求结果
     * @return 用于响应审计与可选持久化的最终 trace 结果
     */
    RagTraceResult finish(RagTraceSession session, String overallStatus, boolean snapshotMissOccurred);

    /**
     * 在编排结束后清理请求级 trace 上下文。
     */
    void clear();
}
