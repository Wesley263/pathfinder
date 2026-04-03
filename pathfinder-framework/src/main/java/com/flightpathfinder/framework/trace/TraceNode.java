package com.flightpathfinder.framework.trace;

import java.time.Instant;
import java.util.Map;

public record TraceNode(String name, Instant startedAt, Instant finishedAt, Map<String, Object> attributes) {
}

