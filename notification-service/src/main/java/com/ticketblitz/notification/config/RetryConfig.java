package com.ticketblitz.notification.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Enable Spring Retry for SMTP fault tolerance.
 * Methods annotated with @Retryable will automatically retry on failure.
 */
@Configuration
@EnableRetry
public class RetryConfig {
    // Spring Boot auto-configuration handles the rest
    public boolean isRetryEnabled() {
        return true;
    }
}
