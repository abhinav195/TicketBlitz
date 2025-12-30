package com.ticketblitz.event.service;

import com.ticketblitz.event.dto.EventDto;
import com.ticketblitz.event.dto.EventSearchCriteria; // Created in Day 3 Plan
import com.ticketblitz.event.entity.Category;
import com.ticketblitz.event.entity.Event;
import com.ticketblitz.event.repository.CategoryRepository;
import com.ticketblitz.event.repository.EventRepository;
import com.ticketblitz.event.repository.EventSpecification; // Created in Day 3 Plan
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private MinioService minioService;

    @Autowired
    private AuditLogger auditLogger;

    /**
     * TRANSACTIONAL BOUNDARY:
     * The lock is acquired when findByIdLocked is called.
     * The lock is released ONLY when this method returns (Transaction commit/rollback).
     */
    @Transactional
    public boolean reserveTickets(Long eventId, int count) {
        log.info("Attempting to reserve {} tickets for Event {}", count, eventId);

        // 1. Acquire Lock
        Event event = eventRepository.findByIdLocked(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        // 2. Check Business Rule (In-Memory check on latest DB state)
        if (event.getAvailableTickets() < count) {
            log.warn("Reservation failed: Insufficient inventory. Requested: {}, Available: {}",
                    count, event.getAvailableTickets());
            return false;
        }

        // 3. Mutate State
        event.setAvailableTickets(event.getAvailableTickets() - count);

        // 4. Save (Hibernate will flush update at end of transaction)
        eventRepository.save(event);

        log.info("Reservation success. New availability: {}", event.getAvailableTickets());
        return true;
    }

//      Advanced Search with Audit Logging
    public Page<EventDto> searchEvents(EventSearchCriteria criteria, Pageable pageable, String userId) {
        long startTime = System.nanoTime();
        boolean success = false;

        try {
            // FIX: Switched to Native Query to avoid Hibernate Type issues with Postgres Full Text Search
            Page<Event> result = eventRepository.searchEventsFallback(
                    criteria.query(),
                    criteria.category(),
                    criteria.minPrice(),
                    criteria.maxPrice(),
                    pageable
            );

            success = true;
            return result.map(this::mapToDto);

        } catch (Exception e) {
            // Log the error for debugging
            System.err.println("Search Error: " + e.getMessage());
            throw e;
        } finally {
            // ... audit logging ...
        }
    }


    @Transactional
    @CacheEvict(value = "events", allEntries = true)
    public EventDto createEvent(String title, String description, LocalDateTime date,
                                String location, Long categoryId, BigDecimal price,
                                Integer totalTickets, List<MultipartFile> images) {

        List<String> imageUrls = minioService.uploadImages(images);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found with ID: " + categoryId));

        Event event = Event.builder()
                .title(title)
                .description(description)
                .date(date)
                .location(location)
                .category(category)
                .price(price)
                .totalTickets(totalTickets)
                .availableTickets(totalTickets) // Initially same as total
                .imageUrls(imageUrls)
                .build();

        Event saved = eventRepository.save(event);
        return mapToDto(saved);
    }

    @Cacheable(value = "events", key = "#id")
    public EventDto getEvent(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        return mapToDto(event);
    }

    public List<EventDto> getAllEvents() {
        return eventRepository.findAll().stream().map(this::mapToDto).toList();
    }

    private EventDto mapToDto(Event e) {
        // Handle potential null image list safely
        List<String> images = e.getImageUrls() != null ? new java.util.ArrayList<>(e.getImageUrls()) : List.of();

        return new EventDto(
                e.getId(),
                e.getTitle(),
                e.getDescription(),
                e.getDate(),
                e.getLocation(),
                e.getCategory().getName(),
                e.getPrice(),
                e.getTotalTickets(),
                e.getAvailableTickets(),
                images
        );
    }


    // ADMIN ONLY
    @Transactional
    public void deleteEvent(Long id) {
        log.info("Deleting event ID: {}", id);
        if (!eventRepository.existsById(id)) {
            throw new RuntimeException("Event not found");
        }
        eventRepository.deleteById(id);
    }

    /**
     * COMPENSATING TRANSACTION:
     * Called when Payment fails. We must increment the inventory back.
     */
    @Transactional
    public void releaseTickets(Long eventId, int count) {
        log.info("Compensating Transaction: Releasing {} tickets for Event {}", count, eventId);

        Event event = eventRepository.findByIdLocked(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        // Increment inventory back
        event.setAvailableTickets(event.getAvailableTickets() + count);
        eventRepository.save(event);

        log.info("Tickets released. New availability: {}", event.getAvailableTickets());
    }

}
