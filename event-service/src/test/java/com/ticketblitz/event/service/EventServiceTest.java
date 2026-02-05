package com.ticketblitz.event.service;

import com.ticketblitz.event.dto.EventDto;
import com.ticketblitz.event.dto.EventSearchCriteria;
import com.ticketblitz.event.entity.Category;
import com.ticketblitz.event.entity.Event;
import com.ticketblitz.event.kafka.EventCreatedEvent;
import com.ticketblitz.event.repository.CategoryRepository;
import com.ticketblitz.event.repository.EventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private MinioService minioService;

    @Mock
    private AuditLogger auditLogger;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private EventService eventService;

    // ========== 1. PESSIMISTIC LOCKING & INVENTORY MANAGEMENT ==========

    @Test
    @DisplayName("reserveTickets: Should decrement inventory when enough tickets are available")
    void reserveTicketsSuccess() {
        Long eventId = 1L;
        int requestCount = 5;
        Event mockEvent = Event.builder()
                .id(eventId)
                .availableTickets(100)
                .totalTickets(100)
                .build();

        when(eventRepository.findByIdLocked(eventId)).thenReturn(Optional.of(mockEvent));

        boolean result = eventService.reserveTickets(eventId, requestCount);

        assertThat(result).isTrue();
        assertThat(mockEvent.getAvailableTickets()).isEqualTo(95);
        verify(eventRepository).save(mockEvent);
    }

    @Test
    @DisplayName("reserveTickets: Should return false when Sold Out (Insufficient Inventory)")
    void reserveTicketsSoldOut() {
        Long eventId = 1L;
        Event mockEvent = Event.builder()
                .id(eventId)
                .availableTickets(2)
                .totalTickets(100)
                .build();

        when(eventRepository.findByIdLocked(eventId)).thenReturn(Optional.of(mockEvent));

        boolean result = eventService.reserveTickets(eventId, 5);

        assertThat(result).isFalse();
        assertThat(mockEvent.getAvailableTickets()).isEqualTo(2);
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("reserveTickets: Should throw Exception if Event not found")
    void reserveTicketsEventNotFound() {
        when(eventRepository.findByIdLocked(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.reserveTickets(99L, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Event not found");
    }

    @Test
    @DisplayName("reserveTickets: Should successfully reserve exact available tickets")
    void reserveTicketsExactMatch() {
        Event mockEvent = Event.builder()
                .id(1L)
                .availableTickets(10)
                .totalTickets(100)
                .build();

        when(eventRepository.findByIdLocked(1L)).thenReturn(Optional.of(mockEvent));

        boolean result = eventService.reserveTickets(1L, 10);

        assertTrue(result);
        assertThat(mockEvent.getAvailableTickets()).isEqualTo(0);
        verify(eventRepository).save(mockEvent);
    }

    // ========== 2. COMPENSATING TRANSACTION (Release Tickets) ==========

    @Test
    @DisplayName("releaseTickets: Should increment inventory (Compensating Transaction)")
    void releaseTicketsSuccess() {
        Long eventId = 1L;
        Event mockEvent = Event.builder()
                .id(eventId)
                .availableTickets(90)
                .totalTickets(100)
                .build();

        when(eventRepository.findByIdLocked(eventId)).thenReturn(Optional.of(mockEvent));

        eventService.releaseTickets(eventId, 5);

        assertThat(mockEvent.getAvailableTickets()).isEqualTo(95);
        verify(eventRepository).save(mockEvent);
    }

    @Test
    @DisplayName("releaseTickets: Should throw exception when event not found")
    void releaseTicketsEventNotFound() {
        when(eventRepository.findByIdLocked(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.releaseTickets(99L, 5))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Event not found");
    }

    @Test
    @DisplayName("releaseTickets: Should allow exceeding totalTickets (no validation)")
    void releaseTicketsExceedingTotal() {
        Event mockEvent = Event.builder()
                .id(1L)
                .availableTickets(95)
                .totalTickets(100)
                .build();

        when(eventRepository.findByIdLocked(1L)).thenReturn(Optional.of(mockEvent));

        eventService.releaseTickets(1L, 10);

        // Current implementation allows exceeding totalTickets
        assertThat(mockEvent.getAvailableTickets()).isEqualTo(105);
        verify(eventRepository).save(mockEvent);
    }

    // ========== 3. CACHING LOGIC ==========

    @Test
    @DisplayName("getEvent: Should verify Repository call (Cache Miss)")
    void getEventSuccess() {
        Long eventId = 1L;
        Category cat = Category.builder()
                .id(1L)
                .name("Music")
                .build();

        Event mockEvent = Event.builder()
                .id(eventId)
                .title("Test Event")
                .description("Test Description")
                .date(LocalDateTime.now().plusDays(10))
                .location("NYC")
                .category(cat)
                .price(BigDecimal.TEN)
                .totalTickets(100)
                .availableTickets(100)
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(mockEvent));

        EventDto result = eventService.getEvent(eventId);

        assertThat(result.id()).isEqualTo(eventId);
        assertThat(result.title()).isEqualTo("Test Event");
        assertThat(result.category()).isEqualTo("Music");
        verify(eventRepository).findById(eventId);
    }

    @Test
    @DisplayName("getEvent: Should throw exception when event not found")
    void getEventNotFound() {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getEvent(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Event not found");
    }

    // ========== 4. CREATE EVENT ==========

    @Test
    @DisplayName("createEvent: Happy Path with Coldplay Concert + Image Upload + Event Published")
    void createEventHappyPathColdplay() {
        String title = "Coldplay Concert";
        String description = "Music of the Spheres World Tour";
        LocalDateTime date = LocalDateTime.now().plusDays(30);
        String location = "Wembley Stadium";
        Long categoryId = 1L;
        BigDecimal price = new BigDecimal("150.00");
        Integer totalTickets = 50000;

        MultipartFile file1 = mock(MultipartFile.class);
        MultipartFile file2 = mock(MultipartFile.class);
        List<MultipartFile> images = List.of(file1, file2);

        Category mockCategory = Category.builder()
                .id(1L)
                .name("Music")
                .description("Live Music Events")
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(mockCategory));
        when(minioService.uploadImages(images)).thenReturn(
                List.of("MOTS-TOUR_1080x1350_F-1.jpg", "MusicoftheSpheresWorldTourPoster.png")
        );

        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event e = invocation.getArgument(0);
            e.setId(100L);
            return e;
        });

        EventDto result = eventService.createEvent(
                title, description, date, location, categoryId, price, totalTickets, images
        );

        assertThat(result.title()).isEqualTo("Coldplay Concert");
        assertThat(result.description()).isEqualTo("Music of the Spheres World Tour");
        assertThat(result.category()).isEqualTo("Music");
        assertThat(result.price()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(result.totalTickets()).isEqualTo(50000);
        assertThat(result.availableTickets()).isEqualTo(50000);
        assertThat(result.imageUrls()).hasSize(2);
        assertThat(result.imageUrls()).contains("MOTS-TOUR_1080x1350_F-1.jpg");

        verify(minioService).uploadImages(images);
        verify(eventRepository).save(any(Event.class));
        verify(applicationEventPublisher, times(1)).publishEvent(any(EventCreatedEvent.class));
    }

    @Test
    @DisplayName("createEvent: Should throw exception when category not found")
    void createEventCategoryNotFound() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.createEvent(
                "Title", "Description", LocalDateTime.now().plusDays(1),
                "Location", 99L, BigDecimal.TEN, 10, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Category not found");

        verify(eventRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("createEvent: Should handle empty image list gracefully")
    void createEventNoImages() {
        String title = "No Image Event";
        List<MultipartFile> emptyImages = Collections.emptyList();

        Category mockCategory = Category.builder()
                .id(1L)
                .name("Music")
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event e = invocation.getArgument(0);
            e.setId(1L);
            return e;
        });

        EventDto result = eventService.createEvent(
                title, "Description", LocalDateTime.now().plusDays(1),
                "Location", 1L, BigDecimal.TEN, 10, emptyImages
        );

        assertThat(result.imageUrls()).isEmpty();
        verify(minioService, never()).uploadImages(any());
        verify(applicationEventPublisher, times(1)).publishEvent(any(EventCreatedEvent.class));
    }

    @Test
    @DisplayName("createEvent: Should handle null image list")
    void createEventNullImagesList() {
        Category mockCategory = Category.builder()
                .id(1L)
                .name("Music")
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event e = invocation.getArgument(0);
            e.setId(1L);
            return e;
        });

        EventDto result = eventService.createEvent(
                "Title", "Description", LocalDateTime.now().plusDays(1),
                "Location", 1L, BigDecimal.TEN, 10, null
        );

        assertThat(result.imageUrls()).isEmpty();
        verify(minioService, never()).uploadImages(any());
        verify(applicationEventPublisher, times(1)).publishEvent(any(EventCreatedEvent.class));
    }

    @Test
    @DisplayName("createEvent: Should call MinIO service when images provided")
    void createEventCallsMinioService() {
        Category mockCategory = Category.builder()
                .id(1L)
                .name("Music")
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory));

        MultipartFile file1 = mock(MultipartFile.class);
        MultipartFile file2 = mock(MultipartFile.class);
        List<MultipartFile> images = List.of(file1, file2);

        when(minioService.uploadImages(images)).thenReturn(
                List.of("uploaded1.jpg", "uploaded2.jpg")
        );

        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event e = invocation.getArgument(0);
            e.setId(99L);
            return e;
        });

        EventDto result = eventService.createEvent(
                "Test", "Description", LocalDateTime.now().plusDays(1),
                "Location", 1L, new BigDecimal("50.00"), 100, images
        );

        verify(minioService, times(1)).uploadImages(images);
        assertThat(result.imageUrls()).hasSize(2);
        assertThat(result.imageUrls()).containsExactly("uploaded1.jpg", "uploaded2.jpg");
        verify(applicationEventPublisher, times(1)).publishEvent(any(EventCreatedEvent.class));
    }

    // ========== 5. RESILIENCE: CIRCUIT BREAKER FALLBACK ==========

    @Test
    @DisplayName("Resilience: Verify Fallback Logic returns Default Image (Reflection Test)")
    void testFallbackImageUploadLogic() throws Exception {
        Method fallbackMethod = EventService.class.getDeclaredMethod(
                "fallbackImageUpload", List.class, Throwable.class
        );
        fallbackMethod.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) fallbackMethod.invoke(
                eventService, List.of(), new RuntimeException("MinIO Down")
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo("default-event.png");
    }

    // ========== 6. SEARCH & AUDIT LOGGING ==========

    @Test
    @DisplayName("searchEvents: Should trigger Audit Log on success")
    void searchEventsSuccess() {
        EventSearchCriteria criteria = new EventSearchCriteria(
                "Rock", null, null, null
        );
        Pageable pageable = Pageable.unpaged();
        String userId = "user-123";

        Page<Event> emptyPage = new PageImpl<>(List.of());
        when(eventRepository.searchEventsFallback(any(), any(), any(), any(), any()))
                .thenReturn(emptyPage);

        eventService.searchEvents(criteria, pageable, userId);

        verify(auditLogger).log(
                eq(userId),
                eq("SEARCH_EVENTS"),
                anyString(),
                anyLong(),
                eq(true)
        );
    }

    @Test
    @DisplayName("searchEvents: Should log error when repository fails")
    void searchEventsFailure() {
        EventSearchCriteria criteria = new EventSearchCriteria(
                "Rock", null, null, null
        );
        Pageable pageable = Pageable.unpaged();
        String userId = "user-123";

        when(eventRepository.searchEventsFallback(any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("DB Connection Failed"));

        assertThatThrownBy(() -> eventService.searchEvents(criteria, pageable, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB Connection Failed");

        verify(auditLogger).log(
                eq(userId),
                eq("SEARCH_EVENTS"),
                anyString(),
                anyLong(),
                eq(false)
        );
    }

    // ========== 7. DELETE EVENT ==========

    @Test
    @DisplayName("deleteEvent: Should successfully delete existing event")
    void deleteEventSuccess() {
        Long eventId = 1L;

        when(eventRepository.existsById(eventId)).thenReturn(true);
        doNothing().when(eventRepository).deleteById(eventId);

        eventService.deleteEvent(eventId);

        verify(eventRepository).existsById(eventId);
        verify(eventRepository).deleteById(eventId);
    }

    @Test
    @DisplayName("deleteEvent: Should throw exception if event does not exist")
    void deleteEventNotFound() {
        Long eventId = 99L;

        when(eventRepository.existsById(eventId)).thenReturn(false);

        assertThatThrownBy(() -> eventService.deleteEvent(eventId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Event not found");

        verify(eventRepository, never()).deleteById(any());
    }

    // ========== 8. GET ALL EVENTS ==========

    @Test
    @DisplayName("getAllEvents: Should return all events")
    void getAllEventsSuccess() {
        Category cat = Category.builder()
                .id(1L)
                .name("Music")
                .build();

        Event event1 = Event.builder()
                .id(1L)
                .title("Event 1")
                .description("Description 1")
                .date(LocalDateTime.now().plusDays(10))
                .location("Location 1")
                .category(cat)
                .price(BigDecimal.TEN)
                .totalTickets(100)
                .availableTickets(100)
                .build();

        Event event2 = Event.builder()
                .id(2L)
                .title("Event 2")
                .description("Description 2")
                .date(LocalDateTime.now().plusDays(20))
                .location("Location 2")
                .category(cat)
                .price(new BigDecimal("50"))
                .totalTickets(200)
                .availableTickets(200)
                .build();

        when(eventRepository.findAll()).thenReturn(List.of(event1, event2));

        List<EventDto> result = eventService.getAllEvents();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).title()).isEqualTo("Event 1");
        assertThat(result.get(1).title()).isEqualTo("Event 2");
        verify(eventRepository).findAll();
    }

    @Test
    @DisplayName("getAllEvents: Should return empty list when no events exist")
    void getAllEventsEmpty() {
        when(eventRepository.findAll()).thenReturn(Collections.emptyList());

        List<EventDto> result = eventService.getAllEvents();

        assertThat(result).isEmpty();
        verify(eventRepository).findAll();
    }

    // ========== 9. EDGE CASES: NULL/EMPTY IMAGE HANDLING ==========

    @Test
    @DisplayName("getEvent: Should handle null image list in Entity")
    void getEventNullImages() {
        Long eventId = 1L;
        Category cat = Category.builder()
                .id(1L)
                .name("Music")
                .build();

        Event mockEvent = Event.builder()
                .id(eventId)
                .title("Null Images Event")
                .description("Description")
                .date(LocalDateTime.now().plusDays(10))
                .location("NYC")
                .category(cat)
                .price(BigDecimal.TEN)
                .totalTickets(100)
                .availableTickets(100)
                .imageUrls(null)
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(mockEvent));

        EventDto result = eventService.getEvent(eventId);

        assertThat(result.imageUrls()).isEmpty();
    }

    @Test
    @DisplayName("getEvent: Should correctly copy populated image list")
    void getEventWithPopulatedImages() {
        Long eventId = 10L;
        Category cat = Category.builder()
                .id(2L)
                .name("Technology")
                .build();

        Event mockEvent = Event.builder()
                .id(eventId)
                .title("Google I/O 2026")
                .description("Annual Developer Conference")
                .date(LocalDateTime.now().plusMonths(6))
                .location("Shoreline Amphitheatre")
                .category(cat)
                .price(BigDecimal.ZERO)
                .totalTickets(5000)
                .availableTickets(5000)
                .imageUrls(new ArrayList<>(List.of("io-logo.png", "keynote.jpg")))
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(mockEvent));

        EventDto result = eventService.getEvent(eventId);

        assertThat(result.imageUrls()).isNotNull();
        assertThat(result.imageUrls()).hasSize(2);
        assertThat(result.imageUrls()).containsExactly("io-logo.png", "keynote.jpg");
    }

    @Test
    @DisplayName("mapToDto: Should handle entity with multiple images")
    void mapToDtoWithMultipleImages() {
        Category cat = Category.builder()
                .id(1L)
                .name("Music")
                .build();

        Event event = Event.builder()
                .id(1L)
                .title("Test Event")
                .description("Test Description")
                .date(LocalDateTime.now().plusDays(10))
                .location("Test Location")
                .category(cat)
                .price(BigDecimal.TEN)
                .totalTickets(100)
                .availableTickets(100)
                .imageUrls(List.of("img1.jpg", "img2.jpg", "img3.jpg"))
                .build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        EventDto result = eventService.getEvent(1L);

        assertThat(result.imageUrls()).hasSize(3);
        assertThat(result.imageUrls()).containsExactly("img1.jpg", "img2.jpg", "img3.jpg");
    }

    // ========== 10. ADDITIONAL COVERAGE TESTS ==========

    @Test
    @DisplayName("createEvent: Should set availableTickets equal to totalTickets initially")
    void createEventInitialInventory() {
        Category mockCategory = Category.builder()
                .id(1L)
                .name("Sports")
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(mockCategory));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event e = invocation.getArgument(0);
            e.setId(1L);
            return e;
        });

        EventDto result = eventService.createEvent(
                "Football Match", "Premier League", LocalDateTime.now().plusDays(7),
                "Stadium", 1L, new BigDecimal("75.00"), 30000, null
        );

        assertThat(result.totalTickets()).isEqualTo(30000);
        assertThat(result.availableTickets()).isEqualTo(30000);
    }

    @Test
    @DisplayName("reserveTickets: Should handle concurrent reservation scenario")
    void reserveTicketsConcurrentScenario() {
        Event mockEvent = Event.builder()
                .id(1L)
                .availableTickets(10)
                .totalTickets(100)
                .build();

        when(eventRepository.findByIdLocked(1L)).thenReturn(Optional.of(mockEvent));

        // First reservation
        boolean firstResult = eventService.reserveTickets(1L, 5);

        // Simulate concurrent access - update the mock
        mockEvent.setAvailableTickets(5);

        // Second reservation
        boolean secondResult = eventService.reserveTickets(1L, 5);

        assertTrue(firstResult);
        assertTrue(secondResult);
        assertThat(mockEvent.getAvailableTickets()).isEqualTo(0);
    }

    @Test
    @DisplayName("getAllEvents: Should map all fields correctly")
    void getAllEventsCorrectMapping() {
        Category cat = Category.builder()
                .id(1L)
                .name("Conference")
                .build();

        LocalDateTime futureDate = LocalDateTime.now().plusMonths(2);

        Event event = Event.builder()
                .id(42L)
                .title("AWS re:Invent 2026")
                .description("Cloud Computing Conference")
                .date(futureDate)
                .location("Las Vegas")
                .category(cat)
                .price(new BigDecimal("1999.00"))
                .totalTickets(10000)
                .availableTickets(8500)
                .imageUrls(List.of("reinvent-logo.png"))
                .build();

        when(eventRepository.findAll()).thenReturn(List.of(event));

        List<EventDto> results = eventService.getAllEvents();

        assertThat(results).hasSize(1);
        EventDto dto = results.get(0);
        assertThat(dto.id()).isEqualTo(42L);
        assertThat(dto.title()).isEqualTo("AWS re:Invent 2026");
        assertThat(dto.description()).isEqualTo("Cloud Computing Conference");
        assertThat(dto.location()).isEqualTo("Las Vegas");
        assertThat(dto.category()).isEqualTo("Conference");
        assertThat(dto.price()).isEqualByComparingTo(new BigDecimal("1999.00"));
        assertThat(dto.totalTickets()).isEqualTo(10000);
        assertThat(dto.availableTickets()).isEqualTo(8500);
        assertThat(dto.imageUrls()).containsExactly("reinvent-logo.png");
    }
}
