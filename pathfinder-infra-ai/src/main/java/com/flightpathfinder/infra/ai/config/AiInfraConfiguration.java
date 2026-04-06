package com.flightpathfinder.infra.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
/**
 * AI 基础设施配置入口。
 *
 * 负责注册模型路由相关配置属性绑定。
 */

@Configuration
@EnableConfigurationProperties(AiModelProperties.class)
public class AiInfraConfiguration {
}


