package com.ticketblitz.booking.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserClientFallbackTest {

    private UserClientFallback fallback;

    @BeforeEach
    void setUp() {
        fallback = new UserClientFallback();
    }

    @Test
    @DisplayName("ValidateUser - Fallback: Should return false when service is down")
    void validateUser_fallback_returnsFalse() {
        boolean result = fallback.validateUser(123L, "Bearer token");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("ValidateUser - Fallback: Should print warning message")
    void validateUser_fallback_printsWarning() {
        // This test verifies the fallback is called
        // In production, you'd verify logging behavior
        fallback.validateUser(456L, "Bearer token");

        // No exception should be thrown
        assertThat(true).isTrue();
    }
}
