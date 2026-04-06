package com.flightpathfinder.admin.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 启用管理端功能域所需的配置。
 *
 * <p>管理端独立维护 ETL 相关属性，用于数据导入与运维治理，不归属用户侧 API 层。
 */
@Configuration
@EnableConfigurationProperties(AdminDataEtlProperties.class)
public class AdminFeatureConfiguration {
}
