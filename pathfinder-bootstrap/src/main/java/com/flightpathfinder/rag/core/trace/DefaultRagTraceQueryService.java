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
 * Rag 追踪查询服务默认实现。
 *
 * 聚合 run/node/tool 三类持久化记录，生成管理端可展示的追踪摘要与详情视图。
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
        * 查询单条 trace 的完整详情。
     *
        * @param traceId 链路追踪标识
        * @return 命中时返回聚合详情
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
                        // 便于前端同时支持阶段摘要视图与逐节点排障视图。
                    return new RagTraceDetailResult(toRunSummary(runRecord), stages, nodes, tools);
                });
    }

    /**
                 * 查询近期运行记录列表。
     *
     * @param requestId 可选请求标识过滤
     * @param conversationId 可选会话标识过滤
     * @param limit 最大返回条数
                 * @return 运行摘要列表
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
            // 属性 JSON 损坏时降级返回原始文本，避免查询接口因单条脏数据失败。
            return Map.of("rawAttributes", attributesJson == null ? "" : attributesJson);
        }
    }
}
