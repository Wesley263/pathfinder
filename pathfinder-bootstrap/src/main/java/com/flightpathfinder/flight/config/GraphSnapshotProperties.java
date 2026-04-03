package com.flightpathfinder.flight.config;

import com.flightpathfinder.framework.readmodel.graph.GraphSnapshotRedisKeys;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pathfinder.flight.graph.snapshot")
public class GraphSnapshotProperties {

    private String defaultGraphKey = GraphSnapshotRedisKeys.DEFAULT_GRAPH_KEY;
    private Duration ttl = GraphSnapshotRedisKeys.DEFAULT_TTL;

    public String getDefaultGraphKey() {
        return defaultGraphKey;
    }

    public void setDefaultGraphKey(String defaultGraphKey) {
        this.defaultGraphKey = defaultGraphKey;
    }

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }
}
