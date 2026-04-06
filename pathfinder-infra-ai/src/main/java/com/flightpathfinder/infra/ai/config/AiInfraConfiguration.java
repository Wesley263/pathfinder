package com.flightpathfinder.infra.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
/**
 * 面向 AI 基础设施的自动装配配置。
 */

@Configuration
@EnableConfigurationProperties(AiModelProperties.class)
public class AiInfraConfiguration {
}


