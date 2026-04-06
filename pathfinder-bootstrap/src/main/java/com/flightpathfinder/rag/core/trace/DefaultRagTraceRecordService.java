package com.flightpathfinder.rag.core.trace;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightpathfinder.framework.trace.TraceNode;
import com.flightpathfinder.framework.trace.TraceRoot;
import com.flightpathfinder.rag.core.trace.repository.PersistedRagTraceNodeRecord;
import com.flightpathfinder.rag.core.trace.repository.PersistedRagTraceRunRecord;
import com.flightpathfinder.rag.core.trace.repository.PersistedRagTraceToolRecord;
import com.flightpathfinder.rag.core.trace.repository.RagTraceNodeRepository;
import com.flightpathfinder.rag.core.trace.repository.RagTraceRunRepository;
import com.flightpathfinder.rag.core.trace.repository.RagTraceToolRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 说明。
 *
 * 说明。
 * 说明。
 */
@Service
public class DefaultRagTraceRecordService implements RagTraceRecordService {

    private final RagTraceRunRepository ragTraceRunRepository;
    private final RagTraceNodeRepository ragTraceNodeRepository;
    private final RagTraceToolRepository ragTraceToolRepository;
    private final ObjectMapper objectMapper;

    public DefaultRagTraceRecordService(RagTraceRunRepository ragTraceRunRepository,
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
     * @param traceResult 参数说明。
     */
    @Override
    @Transactional
    public void persist(RagTraceResult traceResult) {
        if (traceResult == null || traceResult.traceId().isBlank()) {
            return;
        }
        List<PersistedRagTraceNodeRecord> nodeRecords = toNodeRecords(traceResult);
        List<PersistedRagTraceToolRecord> toolRecords = toToolRecords(traceResult);
        ragTraceRunRepository.upsert(toRunRecord(traceResult, nodeRecords.size(), toolRecords.size()));
        ragTraceNodeRepository.replaceForTrace(traceResult.traceId(), nodeRecords);
        ragTraceToolRepository.replaceForTrace(traceResult.traceId(), toolRecords);
    }

    private PersistedRagTraceRunRecord toRunRecord(RagTraceResult traceResult, int nodeCount, int toolCount) {
        Instant startedAt = traceResult.root() == null ? Instant.now() : traceResult.root().startedAt();
        Instant finishedAt = resolveFinishedAt(traceResult, startedAt);
        return new PersistedRagTraceRunRecord(
                traceResult.traceId(),
                traceResult.requestId(),
                traceResult.conversationId(),
                traceResult.scene(),
                traceResult.overallStatus(),
                traceResult.snapshotMissOccurred(),
                startedAt,
                finishedAt,
                nodeCount,
                toolCount);
    }

    private List<PersistedRagTraceNodeRecord> toNodeRecords(RagTraceResult traceResult) {
        TraceRoot root = traceResult.root();
        if (root == null || root.nodes() == null || root.nodes().isEmpty()) {
            return List.of();
        }
        List<PersistedRagTraceNodeRecord> records = new ArrayList<>();
        for (int index = 0; index < root.nodes().size(); index++) {
            TraceNode node = root.nodes().get(index);
            Map<String, Object> attributes = node.attributes() == null ? Map.of() : node.attributes();
            records.add(new PersistedRagTraceNodeRecord(
                    traceResult.traceId(),
                    index + 1,
                    node.name(),
                    resolveNodeType(node.name()),
                    stringValue(attributes.get("status"), "UNKNOWN"),
                    resolveNodeSummary(node.name(), attributes),
                    node.startedAt(),
                    node.finishedAt(),
                    toJson(attributes)));
        }
        return List.copyOf(records);
    }

    private List<PersistedRagTraceToolRecord> toToolRecords(RagTraceResult traceResult) {
        List<PersistedRagTraceToolRecord> records = new ArrayList<>();
        for (int index = 0; index < traceResult.mcpToolSummaries().size(); index++) {
            RagTraceToolSummary toolSummary = traceResult.mcpToolSummaries().get(index);
            records.add(new PersistedRagTraceToolRecord(
                    traceResult.traceId(),
                    index + 1,
                    toolSummary.toolId(),
                    toolSummary.status(),
                    toolSummary.message(),
                    toolSummary.snapshotMiss()));
        }
        return List.copyOf(records);
    }

    private Instant resolveFinishedAt(RagTraceResult traceResult, Instant fallback) {
        Instant finishedAt = fallback;
        if (traceResult.root() != null && traceResult.root().nodes() != null) {
            for (TraceNode node : traceResult.root().nodes()) {
                if (node != null && node.finishedAt() != null && node.finishedAt().isAfter(finishedAt)) {
                    finishedAt = node.finishedAt();
                }
            }
        }
        return finishedAt;
    }

    private String resolveNodeType(String nodeName) {
        return "mcp-execution".equals(nodeName) ? "INTERNAL" : "STAGE";
    }

    private String resolveNodeSummary(String nodeName, Map<String, Object> attributes) {
        String summary = stringValue(attributes.get("summary"), "");
        if (!summary.isBlank()) {
            return summary;
        }
        if ("mcp-execution".equals(nodeName)) {
            // 工具执行归属检索阶段，因此以内部节点形式持久化；
            // 说明。
            Object toolCount = attributes.get("toolCount");
            return toolCount == null ? "" : "toolCount=" + toolCount;
        }
        return "";
    }

    private String toJson(Map<String, Object> attributes) {
        try {
            return objectMapper.writeValueAsString(new LinkedHashMap<>(attributes));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize rag trace node attributes", exception);
        }
    }

    private String stringValue(Object value, String defaultValue) {
        String text = value == null ? "" : String.valueOf(value).trim();
        return text.isBlank() ? defaultValue : text;
    }
}
