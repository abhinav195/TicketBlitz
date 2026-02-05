package com.ticketblitz.event.controller;

import com.ticketblitz.event.dto.EventDto;
import com.ticketblitz.event.dto.EventResponse;
import com.ticketblitz.event.service.EventService;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Validated
public class EventController {

    private final EventService eventService;

    // ADMIN ONLY - CREATE EVENT
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventResponse> createEvent(
            @RequestParam("title") @NotBlank String title,
            @RequestParam("description") @NotBlank String description,
            @RequestParam("date") @NotNull @Future LocalDateTime date,
            @RequestParam("location") @NotBlank String location,
            @RequestParam("categoryId") @NotNull @Min(1) Long categoryId,
            @RequestParam("price") @NotNull @DecimalMin("0.01") BigDecimal price,
            @RequestParam("totalTickets") @NotNull @Min(1) Integer totalTickets,
            @RequestParam(value = "images", required = false) List<MultipartFile> images
    ) {
        EventDto dto = eventService.createEvent(title, description, date, location, categoryId, price, totalTickets, images);
        return ResponseEntity.ok(mapDtoToResponse(dto));
    }

    // ADMIN ONLY - DELETE EVENT
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    // PUBLIC - GET ALL EVENTS
    @GetMapping
    public ResponseEntity<List<EventDto>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    // PUBLIC - GET EVENT BY ID
    @GetMapping("/{id}")
    public ResponseEntity<EventDto> getEventById(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEvent(id));
    }

    // INTERNAL ENDPOINT - RESERVE TICKETS
    @PostMapping("/internal/{id}/reserve")
    public ResponseEntity<Boolean> reserveTickets(
            @PathVariable Long id,
            @RequestParam @Min(1) int count
    ) {
        boolean success = eventService.reserveTickets(id, count);
        if (success) {
            return ResponseEntity.ok(true);
        } else {
            return ResponseEntity.badRequest().body(false);
        }
    }
    /**
     * PUBLIC ENDPOINT - Get Latest Events
     * Used by Recommendation Service as Tier 3 fallback
     * No authentication required (internal service communication)
     */
    @GetMapping("/latest")
    public ResponseEntity<List<EventDto>> getLatestEvents(
            @RequestParam(defaultValue = "5") @Min(1) @Max(10) int limit
    ) {
        List<EventDto> events = eventService.getLatestEvents(limit);
        return ResponseEntity.ok(events);
    }


    // COMPENSATING TRANSACTION - RELEASE TICKETS
    @PutMapping("/{id}/release")
    public ResponseEntity<Boolean> releaseTickets(
            @PathVariable Long id,
            @RequestParam @Min(1) int count
    ) {
        eventService.releaseTickets(id, count);
        return ResponseEntity.ok(true);
    }

    private EventResponse mapDtoToResponse(EventDto dto) {
        EventResponse response = new EventResponse();
        response.setId(dto.id());
        response.setTitle(dto.title());
        response.setDescription(dto.description());
        response.setDate(dto.date());
        response.setLocation(dto.location());
        response.setCategory(dto.category());
        response.setPrice(dto.price());
        response.setTotalTickets(dto.totalTickets());
        response.setAvailableTickets(dto.availableTickets());
        response.setImageUrls(dto.imageUrls());
        return response;
    }
}
