package com.ticketblitz.notification.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.retry.annotation.EnableRetry;

import static org.assertj.core.api.Assertions.assertThat;

class RetryConfigTest {

    @Test
    @DisplayName("RetryConfig: Should be instantiable")
    void retryConfigShouldBeInstantiable() {
        RetryConfig config = new RetryConfig();
        assertThat(config).isNotNull();
    }

    @Test
    @DisplayName("RetryConfig: Should indicate retry is enabled")
    void retryConfigShouldIndicateRetryEnabled() {
        RetryConfig config = new RetryConfig();
        assertThat(config.isRetryEnabled()).isTrue();
    }

    @Test
    @DisplayName("RetryConfig: Should have @Configuration annotation")
    void shouldHaveConfigurationAnnotation() {
        assertThat(RetryConfig.class.isAnnotationPresent(
                org.springframework.context.annotation.Configuration.class
        )).isTrue();
    }

    @Test
    @DisplayName("RetryConfig: Should have @EnableRetry annotation")
    void shouldHaveEnableRetryAnnotation() {
        assertThat(RetryConfig.class.isAnnotationPresent(EnableRetry.class)).isTrue();
    }
}
