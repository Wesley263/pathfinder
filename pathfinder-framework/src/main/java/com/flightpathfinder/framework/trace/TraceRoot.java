package com.flightpathfinder.framework.trace;

import java.time.Instant;
import java.util.List;
/**
 * 链路追踪基础模型。
 */
public record TraceRoot(String traceId, String scene, Instant startedAt, List<TraceNode> nodes) {
}


