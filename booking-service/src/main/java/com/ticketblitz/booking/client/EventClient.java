package com.ticketblitz.booking.client;

import com.ticketblitz.booking.dto.EventDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "event-service", url = "${event-service.url}")
public interface EventClient {
    @GetMapping("/events/{id}")
    ResponseEntity<EventDto> getEventById(@PathVariable("id") Long id);

    @PostMapping("/events/internal/{id}/reserve")
    ResponseEntity<Boolean> reserveTickets(
            @PathVariable("id") Long id,
            @RequestParam("count") int count
    );

    /**
     * SAGA COMPENSATING TRANSACTION
     * If payment fails, we call this to add tickets back to the event.
     */
    @PutMapping("/events/{eventId}/release")
    void releaseTickets(@PathVariable("eventId") Long eventId,
                        @RequestParam("count") int count,
                        @RequestHeader("Authorization") String token);
}
