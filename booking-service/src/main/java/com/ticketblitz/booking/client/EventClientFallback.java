package com.ticketblitz.booking.client;

import com.ticketblitz.booking.dto.EventDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class EventClientFallback implements EventClient {

    @Override
    public ResponseEntity<EventDto> getEventById(Long eventId) {
        log.error("⚠️ Event Service Unreachable. Returning 503 Service Unavailable.");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    // FIX: Updated signature to include token
    @Override
    public ResponseEntity<Boolean> reserveTickets(Long eventId, int count, String token) {
        log.error("⚠️ Cannot reserve tickets. Event Service Unreachable.");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(false);
    }

    @Override
    public ResponseEntity<Boolean> releaseTickets(Long eventId, int count, String token) {
        log.error("⚠️ Cannot release tickets. Event Service Unreachable.");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(false);
    }
}
