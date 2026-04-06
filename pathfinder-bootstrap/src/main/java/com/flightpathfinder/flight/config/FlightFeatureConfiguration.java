package com.flightpathfinder.flight.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
/**
 * 航班功能配置模型。
 */

@Configuration
@EnableConfigurationProperties(GraphSnapshotProperties.class)
public class FlightFeatureConfiguration {
}

