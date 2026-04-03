package com.flightpathfinder.framework.trace;

import java.time.Instant;
import java.util.List;

public record TraceRoot(String traceId, String scene, Instant startedAt, List<TraceNode> nodes) {
}

