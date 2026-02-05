package com.ticketblitz.booking.client;

import com.ticketblitz.booking.dto.EventDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "event-service", url = "${event-service.url}", fallback = EventClientFallback.class)
public interface EventClient {

    @GetMapping("/events/{id}")
    @CircuitBreaker(name = "eventService")
    ResponseEntity<EventDto> getEventById(@PathVariable("id") Long eventId);

    @PostMapping("/events/internal/{id}/reserve")
    @CircuitBreaker(name = "eventService")
    ResponseEntity<Boolean> reserveTickets(@PathVariable("id") Long eventId,
                                           @RequestParam("count") int count,
                                           @RequestHeader("Authorization") String token);

    @PostMapping("/events/{id}/release")
    @CircuitBreaker(name = "eventService")
    ResponseEntity<Boolean> releaseTickets(@PathVariable("id") Long eventId,
                                           @RequestParam("count") int count,
                                           @RequestHeader("Authorization") String token);
}
