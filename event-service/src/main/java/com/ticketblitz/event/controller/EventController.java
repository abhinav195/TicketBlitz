package com.ticketblitz.event.controller;

import com.ticketblitz.event.dto.EventDto;
import com.ticketblitz.event.dto.EventResponse;
import com.ticketblitz.event.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    // ADMIN ONLY
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventResponse> createEvent(
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("date") LocalDateTime date,
            @RequestParam("location") String location,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam("price") BigDecimal price,
            @RequestParam("totalTickets") Integer totalTickets,
            @RequestParam(value = "images", required = false) List<MultipartFile> images
    ) {
        EventDto dto = eventService.createEvent(title, description, date, location, categoryId, price, totalTickets, images);
        return ResponseEntity.ok(mapDtoToResponse(dto));
    }

    private EventResponse mapDtoToResponse(EventDto dto) {
        EventResponse response = new EventResponse();
        response.setId(dto.id());
        response.setTitle(dto.title());
        response.setDate(dto.date());
        response.setLocation(dto.location());
        response.setCategory(dto.category());
        response.setPrice(dto.price());
        response.setTotalTickets(dto.totalTickets());
        response.setAvailableTickets(dto.availableTickets());
        response.setImageUrls(dto.imageUrls());
        return response;
    }

    // ADMIN ONLY
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    // PUBLIC
    @GetMapping
    public ResponseEntity<List<EventDto>> getAllEvents() {
        // Warning: Generic wildcard <?> replaced with <List<EventDto>> for clarity
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    // PUBLIC - ADDED THIS METHOD
    @GetMapping("/{id}")
    public ResponseEntity<EventDto> getEventById(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEvent(id));
    }

    /**
     * INTERNAL ENDPOINT
     */
    @PostMapping("/internal/{id}/reserve")
    public ResponseEntity<Boolean> reserveTickets(
            @PathVariable Long id,
            @RequestParam int count
    ) {
        boolean success = eventService.reserveTickets(id, count);
        if (success) {
            return ResponseEntity.ok(true);
        } else {
            return ResponseEntity.badRequest().body(false);
        }
    }

    @PutMapping("/{id}/release")
    public ResponseEntity<Boolean> releaseTickets(@PathVariable Long id, @RequestParam int count) {
        eventService.releaseTickets(id, count);
        return ResponseEntity.ok(true);
    }

}
