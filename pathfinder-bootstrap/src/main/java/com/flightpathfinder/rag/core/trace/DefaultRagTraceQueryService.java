package com.flightpathfinder.rag.core.trace;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightpathfinder.rag.core.trace.repository.PersistedRagTraceNodeRecord;
import com.flightpathfinder.rag.core.trace.repository.PersistedRagTraceRunRecord;
import com.flightpathfinder.rag.core.trace.repository.PersistedRagTraceToolRecord;
import com.flightpathfinder.rag.core.trace.repository.RagTraceNodeRepository;
import com.flightpathfinder.rag.core.trace.repository.RagTraceRunRepository;
import com.flightpathfinder.rag.core.trace.repository.RagTraceToolRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * 持久化 trace 运行记录的默认查询服务。
 *
 * <p>该服务从 run/node/tool 仓储重建面向审计的详情视图。
 * 它有意不依赖请求作用域 trace 上下文，保证读查询独立于在线请求执行。
 */
@Service
public class DefaultRagTraceQueryService implements RagTraceQueryService {

    private final RagTraceRunRepository ragTraceRunRepository;
    private final RagTraceNodeRepository ragTraceNodeRepository;
    private final RagTraceToolRepository ragTraceToolRepository;
    private final ObjectMapper objectMapper;

    public DefaultRagTraceQueryService(RagTraceRunRepository ragTraceRunRepository,
                                       RagTraceNodeRepository ragTraceNodeRepository,
                                       RagTraceToolRepository ragTraceToolRepository,
                                       ObjectMapper objectMapper) {
        this.ragTraceRunRepository = ragTraceRunRepository;
        this.ragTraceNodeRepository = ragTraceNodeRepository;
        this.ragTraceToolRepository = ragTraceToolRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 加载指定 traceId 的持久化详情视图。
     *
     * @param traceId 唯一 trace 标识
     * @return trace 存在时返回详情结果
     */
    @Override
    public Optional<RagTraceDetailResult> findDetail(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            return Optional.empty();
        }
        return ragTraceRunRepository.findByTraceId(traceId)
                .map(runRecord -> {
                    List<RagTraceNodeDetail> nodes = ragTraceNodeRepository.findByTraceId(traceId).stream()
                            .map(this::toNodeDetail)
                            .toList();
                    List<RagTraceToolSummary> tools = ragTraceToolRepository.findByTraceId(traceId).stream()
                            .map(this::toToolSummary)
                            .toList();
                    List<RagTraceNodeDetail> stages = nodes.stream()
                            .filter(node -> "STAGE".equals(node.nodeType()))
                            .toList();
                    // 详情视图同时保留“阶段节点”和“全部节点”，
                    // 便于管理侧展示清晰主时间线，同时不丢失 MCP 执行摘要等内部节点。
                    return new RagTraceDetailResult(toRunSummary(runRecord), stages, nodes, tools);
                });
    }

    /**
     * 按请求或会话标识过滤并列出近期 trace 运行记录。
     *
     * @param requestId 可选请求标识过滤
     * @param conversationId 可选会话标识过滤
     * @param limit 最大返回条数
     * @return 近期 trace 运行摘要
     */
    @Override
    public List<RagTraceRunSummary> listRuns(String requestId, String conversationId, int limit) {
        return ragTraceRunRepository.findRecent(requestId, conversationId, limit).stream()
                .map(this::toRunSummary)
                .toList();
    }

    private RagTraceRunSummary toRunSummary(PersistedRagTraceRunRecord record) {
        return new RagTraceRunSummary(
                record.traceId(),
                record.requestId(),
                record.conversationId(),
                record.scene(),
                record.overallStatus(),
                record.snapshotMissOccurred(),
                record.startedAt(),
                record.finishedAt(),
                record.nodeCount(),
                record.toolCount());
    }

    private RagTraceNodeDetail toNodeDetail(PersistedRagTraceNodeRecord record) {
        return new RagTraceNodeDetail(
                record.nodeName(),
                record.nodeType(),
                record.status(),
                record.summary(),
                record.startedAt(),
                record.finishedAt(),
                parseAttributes(record.attributesJson()));
    }

    private RagTraceToolSummary toToolSummary(PersistedRagTraceToolRecord record) {
        return new RagTraceToolSummary(
                record.toolId(),
                record.status(),
                record.message(),
                record.snapshotMiss());
    }

    private Map<String, Object> parseAttributes(String attributesJson) {
        try {
            return objectMapper.readValue(
                    attributesJson == null || attributesJson.isBlank() ? "{}" : attributesJson,
                    new TypeReference<Map<String, Object>>() {
                    });
        } catch (Exception exception) {
            // 即使历史属性 JSON 解析不完整，查询链路也必须保持韧性。
            return Map.of("rawAttributes", attributesJson == null ? "" : attributesJson);
        }
    }
}
