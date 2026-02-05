package com.ticketblitz.notification.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DTOTest {

    @Test
    @DisplayName("EmailDispatchEvent: All constructors and methods")
    void emailDispatchEventTest() {
        EmailDispatchEvent event1 = new EmailDispatchEvent();
        event1.setRecipientEmail("test@test.com");
        event1.setSubject("Test");
        event1.setBody("Body");

        assertThat(event1.getRecipientEmail()).isEqualTo("test@test.com");

        EmailDispatchEvent event2 = new EmailDispatchEvent("email@test.com", "Subject", "Body");
        assertThat(event2.getRecipientEmail()).isEqualTo("email@test.com");
    }

    @Test
    @DisplayName("PaymentUpdateEvent: All constructors and methods")
    void paymentUpdateEventTest() {
        PaymentUpdateEvent event1 = new PaymentUpdateEvent();
        event1.setBookingId(1L);
        event1.setStatus("SUCCESS");

        assertThat(event1.getBookingId()).isEqualTo(1L);

        PaymentUpdateEvent event2 = new PaymentUpdateEvent(2L, "FAILED", "TXN456", 200L, "Bearer token");
        assertThat(event2.getStatus()).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("RecommendationRequestEvent: Builder and constructors")
    void recommendationRequestEventTest() {
        RecommendationRequestEvent event1 = RecommendationRequestEvent.builder()
                .userId(1L)
                .eventId(50L)
                .userEmail("user@test.com")
                .username("John")
                .authToken("Bearer token")
                .build();

        assertThat(event1.getUserId()).isEqualTo(1L);

        RecommendationRequestEvent event2 = new RecommendationRequestEvent();
        event2.setUserId(2L);
        assertThat(event2.getUserId()).isEqualTo(2L);

        RecommendationRequestEvent event3 = new RecommendationRequestEvent(3L, 60L, "test@test.com", "Jane", "token");
        assertThat(event3.getEventId()).isEqualTo(60L);
    }
}
