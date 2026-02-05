package com.ticketblitz.booking.client;

import com.ticketblitz.booking.dto.EventDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class EventClientFallbackTest {

    private EventClientFallback fallback;

    @BeforeEach
    void setUp() {
        fallback = new EventClientFallback();
    }

    @Test
    @DisplayName("GetEventById - Fallback: Should return 503 Service Unavailable")
    void getEventById_fallback_returns503() {
        ResponseEntity<EventDto> response = fallback.getEventById(99L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNull();
    }

    @Test
    @DisplayName("ReserveTickets - Fallback: Should return 503 with false body")
    void reserveTickets_fallback_returns503WithFalse() {
        ResponseEntity<Boolean> response = fallback.reserveTickets(99L, 5, "Bearer token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isFalse();
    }

    @Test
    @DisplayName("ReleaseTickets - Fallback: Should return 503 with false body")
    void releaseTickets_fallback_returns503WithFalse() {
        ResponseEntity<Boolean> response = fallback.releaseTickets(99L, 3, "Bearer token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isFalse();
    }
}
