package com.ticketblitz.user.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ApiErrorTest {

    @Test
    @DisplayName("Builder: Should create ApiError with all fields")
    void builder_AllFields() {
        Instant now = Instant.now();
        Map<String, String> errors = Map.of("field1", "error1");

        ApiError apiError = ApiError.builder()
                .timestamp(now)
                .status(400)
                .error("Bad Request")
                .message("Test message")
                .path("/test/path")
                .fieldErrors(errors)
                .build();

        assertThat(apiError.getTimestamp()).isEqualTo(now);
        assertThat(apiError.getStatus()).isEqualTo(400);
        assertThat(apiError.getError()).isEqualTo("Bad Request");
        assertThat(apiError.getMessage()).isEqualTo("Test message");
        assertThat(apiError.getPath()).isEqualTo("/test/path");
        assertThat(apiError.getFieldErrors()).isEqualTo(errors);
    }

    @Test
    @DisplayName("Setters: Should update all fields")
    void setters_UpdateFields() {
        Instant now = Instant.now();
        Map<String, String> errors = Map.of("field1", "error1");

        ApiError apiError = ApiError.builder().build();
        apiError.setTimestamp(now);
        apiError.setStatus(404);
        apiError.setError("Not Found");
        apiError.setMessage("Resource not found");
        apiError.setPath("/users/999");
        apiError.setFieldErrors(errors);

        assertThat(apiError.getTimestamp()).isEqualTo(now);
        assertThat(apiError.getStatus()).isEqualTo(404);
        assertThat(apiError.getError()).isEqualTo("Not Found");
        assertThat(apiError.getMessage()).isEqualTo("Resource not found");
        assertThat(apiError.getPath()).isEqualTo("/users/999");
        assertThat(apiError.getFieldErrors()).isEqualTo(errors);
    }

    @Test
    @DisplayName("Builder: Should create with partial fields")
    void builder_PartialFields() {
        ApiError apiError = ApiError.builder()
                .status(500)
                .message("Internal error")
                .build();

        assertThat(apiError.getStatus()).isEqualTo(500);
        assertThat(apiError.getMessage()).isEqualTo("Internal error");
        assertThat(apiError.getTimestamp()).isNull();
        assertThat(apiError.getFieldErrors()).isNull();
    }
}
