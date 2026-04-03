package com.flightpathfinder.infra.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AiModelProperties.class)
public class AiInfraConfiguration {
}
