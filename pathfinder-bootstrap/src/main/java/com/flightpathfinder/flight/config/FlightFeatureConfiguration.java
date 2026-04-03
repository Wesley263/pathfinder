package com.flightpathfinder.flight.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GraphSnapshotProperties.class)
public class FlightFeatureConfiguration {
}
