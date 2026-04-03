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
 * Default query service for persisted trace runs.
 *
 * <p>This service rebuilds audit-friendly detail views from run, node and tool repositories. It intentionally
 * does not depend on request-scoped trace context, which keeps read queries independent from live requests.
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
     * Loads a detailed persisted trace view for the given trace id.
     *
     * @param traceId unique trace identifier
     * @return detailed trace result when the trace exists
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
                    // The detail view keeps both "stages" and "all nodes" so admin callers can show a clean
                    // top-level timeline without losing internal nodes such as MCP execution summaries.
                    return new RagTraceDetailResult(toRunSummary(runRecord), stages, nodes, tools);
                });
    }

    /**
     * Lists recent trace runs filtered by request or conversation identifiers.
     *
     * @param requestId optional request id filter
     * @param conversationId optional conversation id filter
     * @param limit maximum number of trace runs to return
     * @return recent trace run summaries
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
            // Query must remain resilient even when historical attribute JSON cannot be parsed perfectly.
            return Map.of("rawAttributes", attributesJson == null ? "" : attributesJson);
        }
    }
}
