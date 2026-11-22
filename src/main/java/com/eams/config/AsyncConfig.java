package com.eams.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables @Async (emails, AI triage run in the background)
 * and @Scheduled (daily warranty / overdue alerts).
 */
@Configuration
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(AppProperties.class)
public class AsyncConfig {
}
