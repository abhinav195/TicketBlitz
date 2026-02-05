package com.ticketblitz.event.service;

import com.ticketblitz.event.dto.EventDto;
import com.ticketblitz.event.dto.EventSearchCriteria;
import com.ticketblitz.event.entity.Category;
import com.ticketblitz.event.entity.Event;
import com.ticketblitz.event.kafka.EventCreatedEvent;
import com.ticketblitz.event.repository.CategoryRepository;
import com.ticketblitz.event.repository.EventRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final MinioService minioService;
    private final AuditLogger auditLogger;

    // NEW: publish internal Spring event; Kafka send occurs AFTER_COMMIT via @TransactionalEventListener
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * TRANSACTIONAL BOUNDARY
     * The lock is acquired when findByIdLocked() is called.
     * The lock is released ONLY when this method returns (Transaction commit/rollback).
     */
    @Transactional
    @CacheEvict(value = "events", key = "#eventId") // Invalidate cache on reservation
    public boolean reserveTickets(Long eventId, int count) {
        log.info("Attempting to reserve {} tickets for Event {}", count, eventId);

        // 1. Acquire Lock
        Event event = eventRepository.findByIdLocked(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        // 2. Check Business Rule (In-Memory check on latest DB state)
        if (event.getAvailableTickets() < count) {
            log.warn("Reservation failed: Insufficient inventory. Requested: {}, Available: {}", count, event.getAvailableTickets());
            return false;
        }

        // 3. Mutate State
        event.setAvailableTickets(event.getAvailableTickets() - count);

        // 4. Save (Hibernate will flush update at end of transaction)
        eventRepository.save(event);

        log.info("Reservation success. New availability: {}", event.getAvailableTickets());
        return true;
    }

    /**
     * CREATE EVENT with Circuit Breaker on MinIO
     */
    @Transactional
    @CacheEvict(value = "events", allEntries = true) // Clear all cached events
    public EventDto createEvent(String title, String description, LocalDateTime date, String location,
                                Long categoryId, BigDecimal price, Integer totalTickets, List<MultipartFile> images) {
        // Upload images with circuit breaker protection
        List<String> imageUrls = uploadImagesWithResilience(images);

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

        // NEW: internal event (Kafka publish will happen AFTER_COMMIT in listener)
        EventDto dto = mapToDto(saved);
        applicationEventPublisher.publishEvent(new EventCreatedEvent(this, dto));

        return dto;
    }

    /**
     * GET EVENT BY ID with Caching
     */
    @Cacheable(value = "events", key = "#id")
    public EventDto getEvent(Long id) {
        log.info("Fetching Event {} from database (Cache Miss)", id);
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        return mapToDto(event);
    }

    /**
     * GET ALL EVENTS
     */
    public List<EventDto> getAllEvents() {
        return eventRepository.findAll().stream().map(this::mapToDto).toList();
    }

    /**
     * DELETE EVENT
     */
    @Transactional
    @CacheEvict(value = "events", key = "#id") // Remove specific event from cache
    public void deleteEvent(Long id) {
        log.info("Deleting event ID: {}", id);
        if (!eventRepository.existsById(id)) {
            throw new RuntimeException("Event not found");
        }
        eventRepository.deleteById(id);
    }
    /**
     * Get Latest Events (Tier 3 Fallback Support for Recommendation Service)
     * Returns the N most recently created upcoming events
     *
     * @param limit Number of events to return
     * @return List of latest event DTOs
     */
    public List<EventDto> getLatestEvents(int limit) {
        log.info("Fetching {} latest events for fallback recommendation", limit);

        Pageable pageable = PageRequest.of(0, limit);
        List<Event> events = eventRepository.findLatestEvents(pageable);

        log.info("Retrieved {} latest events", events.size());
        return events.stream()
                .map(this::mapToDto)
                .toList();
    }

    /**
     * COMPENSATING TRANSACTION
     * Called when Payment fails. We must increment the inventory back.
     */
    @Transactional
    @CacheEvict(value = "events", key = "#eventId") // Invalidate cache on release
    public void releaseTickets(Long eventId, int count) {
        log.info("Compensating Transaction: Releasing {} tickets for Event {}", count, eventId);

        Event event = eventRepository.findByIdLocked(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        // Increment inventory back
        event.setAvailableTickets(event.getAvailableTickets() + count);
        eventRepository.save(event);

        log.info("Tickets released. New availability: {}", event.getAvailableTickets());
    }

    /**
     * ADVANCED SEARCH with Audit Logging
     */
    public Page<EventDto> searchEvents(EventSearchCriteria criteria, Pageable pageable, String userId) {
        long startTime = System.nanoTime();
        boolean success = false;
        try {
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
            log.error("Search Error: {}", e.getMessage());
            throw e;
        } finally {
            auditLogger.log(userId, "SEARCH_EVENTS", criteria.toString(), System.nanoTime() - startTime, success);
        }
    }

    // ========== RESILIENCE4J CIRCUIT BREAKER ==========

    /**
     * Wraps MinIO upload with Circuit Breaker
     */
    @CircuitBreaker(name = "minio", fallbackMethod = "fallbackImageUpload")
    private List<String> uploadImagesWithResilience(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }
        log.info("Uploading {} images to MinIO", images.size());
        return minioService.uploadImages(images);
    }

    /**
     * FALLBACK: If MinIO is down, use placeholder image
     */
    private List<String> fallbackImageUpload(List<MultipartFile> images, Throwable throwable) {
        log.error("MinIO is unavailable. Using placeholder image. Error: {}", throwable.getMessage());
        return List.of("default-event.png"); // Placeholder image
    }

    // ========== HELPER METHODS ==========

    private EventDto mapToDto(Event e) {
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
}
