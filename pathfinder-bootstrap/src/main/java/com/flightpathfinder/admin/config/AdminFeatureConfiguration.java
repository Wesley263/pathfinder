package com.flightpathfinder.admin.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 启用管理端功能域所需的配置。
 *
 * 说明。
 */
@Configuration
@EnableConfigurationProperties(AdminDataEtlProperties.class)
public class AdminFeatureConfiguration {
}
