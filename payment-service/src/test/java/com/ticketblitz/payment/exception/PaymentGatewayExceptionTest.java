package com.ticketblitz.payment.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentGatewayExceptionTest {

    @Test
    @DisplayName("PaymentGatewayException: Should create with message")
    void createWithMessage() {
        String errorMessage = "Payment Service Unavailable";

        PaymentGatewayException exception = new PaymentGatewayException(errorMessage);

        assertThat(exception.getMessage()).isEqualTo(errorMessage);
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("PaymentGatewayException: Should be throwable")
    void shouldBeThrowable() {
        assertThatThrownBy(() -> {
            throw new PaymentGatewayException("Circuit Breaker Open");
        })
                .isInstanceOf(PaymentGatewayException.class)
                .hasMessage("Circuit Breaker Open");
    }

    @Test
    @DisplayName("PaymentGatewayException: Should handle empty message")
    void emptyMessage() {
        PaymentGatewayException exception = new PaymentGatewayException("");
        assertThat(exception.getMessage()).isEmpty();
    }

    @Test
    @DisplayName("PaymentGatewayException: Should handle null message")
    void nullMessage() {
        PaymentGatewayException exception = new PaymentGatewayException(null);
        assertThat(exception.getMessage()).isNull();
    }
}
