package com.flightpathfinder.admin.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 启用管理端功能域所需的配置。
 *
 * 用于定义当前类型或方法在模块内的职责边界。
 */
@Configuration
@EnableConfigurationProperties(AdminDataEtlProperties.class)
public class AdminFeatureConfiguration {
}


