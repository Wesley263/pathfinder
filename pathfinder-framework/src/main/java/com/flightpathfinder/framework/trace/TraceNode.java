package com.flightpathfinder.framework.trace;

import java.time.Instant;
import java.util.Map;
/**
 * 链路追踪基础模型。
 */
public record TraceNode(String name, Instant startedAt, Instant finishedAt, Map<String, Object> attributes) {
}


