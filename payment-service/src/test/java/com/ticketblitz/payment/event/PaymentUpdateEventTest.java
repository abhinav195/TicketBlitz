package com.ticketblitz.payment.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentUpdateEventTest {

    @Test
    @DisplayName("PaymentUpdateEvent: Should create with no-arg constructor")
    void noArgConstructor() {
        PaymentUpdateEvent event = new PaymentUpdateEvent();
        assertThat(event).isNotNull();
    }

    @Test
    @DisplayName("PaymentUpdateEvent: Should create with all-args constructor")
    void allArgsConstructor() {
        PaymentUpdateEvent event = new PaymentUpdateEvent(
                101L,
                10L,
                "SUCCESS",
                "ch_test_12345",
                "Bearer token123"
        );

        assertThat(event.getBookingId()).isEqualTo(101L);
        assertThat(event.getUserId()).isEqualTo(10L);
        assertThat(event.getStatus()).isEqualTo("SUCCESS");
        assertThat(event.getTransactionId()).isEqualTo("ch_test_12345");
        assertThat(event.getAuthToken()).isEqualTo("Bearer token123");
    }

    @Test
    @DisplayName("PaymentUpdateEvent: Should set and get all fields")
    void gettersAndSetters() {
        PaymentUpdateEvent event = new PaymentUpdateEvent();

        event.setBookingId(102L);
        event.setUserId(11L);
        event.setStatus("FAILED");
        event.setTransactionId("FALLBACK_ERROR");
        event.setAuthToken("Bearer tokenFB");

        assertThat(event.getBookingId()).isEqualTo(102L);
        assertThat(event.getUserId()).isEqualTo(11L);
        assertThat(event.getStatus()).isEqualTo("FAILED");
        assertThat(event.getTransactionId()).isEqualTo("FALLBACK_ERROR");
        assertThat(event.getAuthToken()).isEqualTo("Bearer tokenFB");
    }

    @Test
    @DisplayName("PaymentUpdateEvent: Should handle null values")
    void nullValues() {
        PaymentUpdateEvent event = new PaymentUpdateEvent(null, null, null, null, null);

        assertThat(event.getBookingId()).isNull();
        assertThat(event.getUserId()).isNull();
        assertThat(event.getStatus()).isNull();
        assertThat(event.getTransactionId()).isNull();
        assertThat(event.getAuthToken()).isNull();
    }
}
