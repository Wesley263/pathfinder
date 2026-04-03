package com.flightpathfinder.admin.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Enables configuration required by the admin feature area.
 *
 * <p>The admin feature has its own ETL-oriented properties because data import and maintenance concerns
 * belong to the management surface rather than the user-facing API layer.
 */
@Configuration
@EnableConfigurationProperties(AdminDataEtlProperties.class)
public class AdminFeatureConfiguration {
}
