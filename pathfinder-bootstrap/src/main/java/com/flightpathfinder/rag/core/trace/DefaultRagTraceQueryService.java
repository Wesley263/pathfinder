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
 * 说明。
 *
 * 说明。
 * 说明。
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
     * 说明。
     *
     * @param traceId 参数说明。
     * @return 返回结果。
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
                    // 说明。
                    return new RagTraceDetailResult(toRunSummary(runRecord), stages, nodes, tools);
                });
    }

    /**
     * 说明。
     *
     * @param requestId 可选请求标识过滤
     * @param conversationId 可选会话标识过滤
     * @param limit 最大返回条数
     * @return 返回结果。
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
            // 说明。
            return Map.of("rawAttributes", attributesJson == null ? "" : attributesJson);
        }
    }
}
