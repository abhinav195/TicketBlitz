package com.ticketblitz.booking.service;

import com.ticketblitz.booking.client.EventClient;
import com.ticketblitz.booking.client.UserClient;
import com.ticketblitz.booking.dto.BookTicketRequest;
import com.ticketblitz.booking.dto.BookingCreatedEvent;
import com.ticketblitz.booking.dto.BookingResponse;
import com.ticketblitz.booking.dto.EventDto;
import com.ticketblitz.booking.entity.Booking;
import com.ticketblitz.booking.entity.BookingStatus;
import com.ticketblitz.booking.mapper.BookingMapper;
import com.ticketblitz.booking.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private EventClient eventClient;

    @Mock
    private UserClient userClient;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private BookingMapper bookingMapper;

    @InjectMocks
    private BookingService bookingService;

    private BookTicketRequest standardRequest;
    private EventDto standardEventDto;
    private Booking standardBooking;
    private BookingResponse standardResponse;
    private static final String AUTH_TOKEN = "Bearer test-token";

    @BeforeEach
    void setUp() {
        standardRequest = new BookTicketRequest();
        standardRequest.setUserId(10L);
        standardRequest.setEventId(99L);
        standardRequest.setTicketCount(3);

        standardEventDto = new EventDto();
        standardEventDto.setId(99L);
        standardEventDto.setPrice(new BigDecimal("250.00"));

        standardBooking = new Booking();
        standardBooking.setId(123L);
        standardBooking.setUserId(10L);
        standardBooking.setEventId(99L);
        standardBooking.setTicketCount(3);
        standardBooking.setTotalPrice(new BigDecimal("750.00"));
        standardBooking.setStatus(BookingStatus.PENDING);

        standardResponse = new BookingResponse();
        standardResponse.setBookingId(123L);
        standardResponse.setUserId(10L);
        standardResponse.setEventId(99L);
        standardResponse.setTicketCount(3);
        standardResponse.setStatus(BookingStatus.PENDING);
        standardResponse.setTotalPrice(new BigDecimal("750.00"));
    }

    // ========================================================================
    // HAPPY PATH TESTS
    // ========================================================================

    @Test
    @DisplayName("BookTicket - Happy Path: Parallel async calls, booking saved, Kafka published")
    void bookTicket_asyncOrchestration_happyPath_parallelFetches_bookingSavedPending_totalCalculated_kafkaPublished() {
        CountDownLatch userStarted = new CountDownLatch(1);
        CountDownLatch eventStarted = new CountDownLatch(1);

        when(userClient.validateUser(10L, AUTH_TOKEN)).thenAnswer(inv -> {
            userStarted.countDown();
            assertThat(eventStarted.await(800, TimeUnit.MILLISECONDS))
                    .as("Event call should have started in parallel")
                    .isTrue();
            return true;
        });

        when(eventClient.getEventById(99L)).thenAnswer(inv -> {
            eventStarted.countDown();
            assertThat(userStarted.await(800, TimeUnit.MILLISECONDS))
                    .as("User call should have started in parallel")
                    .isTrue();
            return ResponseEntity.ok(standardEventDto);
        });

        when(eventClient.reserveTickets(99L, 3, AUTH_TOKEN)).thenReturn(ResponseEntity.ok(true));

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        when(bookingRepository.save(bookingCaptor.capture())).thenAnswer(inv -> {
            Booking b = inv.getArgument(0, Booking.class);
            b.setId(123L);
            return b;
        });

        when(bookingMapper.toResponse(any(Booking.class))).thenReturn(standardResponse);

        BookingResponse response = bookingService.bookTicket(standardRequest, AUTH_TOKEN);

        assertThat(response.getBookingId()).isEqualTo(123L);
        assertThat(response.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(response.getTotalPrice()).isEqualByComparingTo("750.00");

        Booking saved = bookingCaptor.getValue();
        assertThat(saved.getUserId()).isEqualTo(10L);
        assertThat(saved.getEventId()).isEqualTo(99L);
        assertThat(saved.getTicketCount()).isEqualTo(3);
        assertThat(saved.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(saved.getTotalPrice()).isEqualByComparingTo("750.00");

        verify(userClient).validateUser(10L, AUTH_TOKEN);
        verify(eventClient).getEventById(99L);
        verify(eventClient).reserveTickets(99L, 3, AUTH_TOKEN);

        ArgumentCaptor<Object> kafkaPayloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("payment.process"), kafkaPayloadCaptor.capture());

        Object payload = kafkaPayloadCaptor.getValue();
        assertThat(payload).isInstanceOf(BookingCreatedEvent.class);

        BookingCreatedEvent producedEvent = (BookingCreatedEvent) payload;
        assertThat(producedEvent.getBookingId()).isEqualTo(123L);
        assertThat(producedEvent.getUserId()).isEqualTo(10L);
        assertThat(producedEvent.getAmount()).isEqualByComparingTo("750.00");
        assertThat(producedEvent.getAuthToken()).isEqualTo(AUTH_TOKEN);
    }

    // ========================================================================
    // USER VALIDATION FAILURE TESTS
    // ========================================================================

    @Test
    @DisplayName("BookTicket - User Not Found: Should throw IllegalArgumentException")
    void bookTicket_userDoesNotExist_throwsIllegalArgumentException_bookingNotSaved_kafkaNotPublished() {
        when(userClient.validateUser(10L, AUTH_TOKEN)).thenReturn(false);
        when(eventClient.getEventById(99L)).thenReturn(ResponseEntity.ok(standardEventDto));

        assertThatThrownBy(() -> bookingService.bookTicket(standardRequest, AUTH_TOKEN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User ID not found");

        verify(bookingRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(anyString(), any());
        verify(eventClient, never()).reserveTickets(anyLong(), anyInt(), anyString());
    }

    @Test
    @DisplayName("BookTicket - UserClient Throws Exception: Should throw RuntimeException")
    void bookTicket_asyncFutureThrows_exceptionStopsFlow_bookingNotSaved_kafkaNotPublished() {
        when(userClient.validateUser(10L, AUTH_TOKEN)).thenThrow(new RuntimeException("user-service down"));
        when(eventClient.getEventById(99L)).thenReturn(ResponseEntity.ok(standardEventDto));

        assertThatThrownBy(() -> bookingService.bookTicket(standardRequest, AUTH_TOKEN))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to fetch User or Event details");

        verify(bookingRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(anyString(), any());
        verify(eventClient, never()).reserveTickets(anyLong(), anyInt(), anyString());
    }

    // ========================================================================
    // EVENT FETCH FAILURE TESTS (FIXED)
    // ========================================================================

    @Test
    @DisplayName("BookTicket - Event Not Found (Non-2xx Status): Should throw RuntimeException wrapping IllegalArgumentException")
    void bookTicket_eventNotFound_non2xxStatus_throwsException() {
        when(userClient.validateUser(10L, AUTH_TOKEN)).thenReturn(true);
        when(eventClient.getEventById(99L)).thenReturn(ResponseEntity.status(HttpStatus.NOT_FOUND).build());

        // FIXED: Expect RuntimeException wrapper, not direct IllegalArgumentException
        assertThatThrownBy(() -> bookingService.bookTicket(standardRequest, AUTH_TOKEN))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to fetch User or Event details")
                .hasMessageContaining("Event not found");

        verify(bookingRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(anyString(), any());
        verify(eventClient, never()).reserveTickets(anyLong(), anyInt(), anyString());
    }

    @Test
    @DisplayName("BookTicket - Event Not Found (Null Body): Should throw RuntimeException wrapping IllegalArgumentException")
    void bookTicket_eventNotFound_nullBody_throwsException() {
        when(userClient.validateUser(10L, AUTH_TOKEN)).thenReturn(true);
        when(eventClient.getEventById(99L)).thenReturn(ResponseEntity.ok(null));

        // FIXED: Expect RuntimeException wrapper
        assertThatThrownBy(() -> bookingService.bookTicket(standardRequest, AUTH_TOKEN))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to fetch User or Event details")
                .hasMessageContaining("Event not found");

        verify(bookingRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(anyString(), any());
    }

    @Test
    @DisplayName("BookTicket - EventClient Throws Exception: Should throw RuntimeException")
    void bookTicket_eventClientThrowsException_asyncFailure() {
        when(userClient.validateUser(10L, AUTH_TOKEN)).thenReturn(true);
        when(eventClient.getEventById(99L)).thenThrow(new RuntimeException("event-service down"));

        assertThatThrownBy(() -> bookingService.bookTicket(standardRequest, AUTH_TOKEN))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to fetch User or Event details");

        verify(bookingRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(anyString(), any());
    }

    // ========================================================================
    // TICKET RESERVATION FAILURE TESTS
    // ========================================================================

    @Test
    @DisplayName("BookTicket - Tickets Sold Out (Boolean.FALSE): Should throw IllegalStateException")
    void bookTicket_inventorySoldOut_throwsIllegalStateException_bookingNotSaved_kafkaNotPublished() {
        when(userClient.validateUser(10L, AUTH_TOKEN)).thenReturn(true);
        when(eventClient.getEventById(99L)).thenReturn(ResponseEntity.ok(standardEventDto));
        when(eventClient.reserveTickets(99L, 3, AUTH_TOKEN)).thenReturn(ResponseEntity.ok(false));

        assertThatThrownBy(() -> bookingService.bookTicket(standardRequest, AUTH_TOKEN))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sold out");

        verify(bookingRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(anyString(), any());
    }

    @Test
    @DisplayName("BookTicket - Reserve Tickets Returns Null Body: Should throw IllegalStateException")
    void bookTicket_reserveTicketsReturnsNullBody_throwsException() {
        when(userClient.validateUser(10L, AUTH_TOKEN)).thenReturn(true);
        when(eventClient.getEventById(99L)).thenReturn(ResponseEntity.ok(standardEventDto));
        when(eventClient.reserveTickets(99L, 3, AUTH_TOKEN)).thenReturn(ResponseEntity.ok(null));

        assertThatThrownBy(() -> bookingService.bookTicket(standardRequest, AUTH_TOKEN))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sold out");

        verify(bookingRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(anyString(), any());
    }

    // ========================================================================
    // PRICE CALCULATION TESTS
    // ========================================================================

    @Test
    @DisplayName("BookTicket - Single Ticket: Price calculated correctly")
    void bookTicket_singleTicket_priceCalculatedCorrectly() {
        BookTicketRequest singleTicketRequest = new BookTicketRequest();
        singleTicketRequest.setUserId(10L);
        singleTicketRequest.setEventId(99L);
        singleTicketRequest.setTicketCount(1);

        EventDto eventDto = new EventDto();
        eventDto.setId(99L);
        eventDto.setPrice(new BigDecimal("100.00"));

        when(userClient.validateUser(10L, AUTH_TOKEN)).thenReturn(true);
        when(eventClient.getEventById(99L)).thenReturn(ResponseEntity.ok(eventDto));
        when(eventClient.reserveTickets(99L, 1, AUTH_TOKEN)).thenReturn(ResponseEntity.ok(true));

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        when(bookingRepository.save(bookingCaptor.capture())).thenAnswer(inv -> {
            Booking b = inv.getArgument(0, Booking.class);
            b.setId(200L);
            return b;
        });

        BookingResponse response = new BookingResponse();
        response.setBookingId(200L);
        response.setTotalPrice(new BigDecimal("100.00"));
        when(bookingMapper.toResponse(any(Booking.class))).thenReturn(response);

        bookingService.bookTicket(singleTicketRequest, AUTH_TOKEN);

        Booking saved = bookingCaptor.getValue();
        assertThat(saved.getTotalPrice()).isEqualByComparingTo("100.00");
        assertThat(saved.getTicketCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("BookTicket - Large Quantity: Price calculated correctly")
    void bookTicket_largeQuantity_priceCalculatedCorrectly() {
        BookTicketRequest largeRequest = new BookTicketRequest();
        largeRequest.setUserId(10L);
        largeRequest.setEventId(99L);
        largeRequest.setTicketCount(10);

        EventDto eventDto = new EventDto();
        eventDto.setId(99L);
        eventDto.setPrice(new BigDecimal("50.50"));

        when(userClient.validateUser(10L, AUTH_TOKEN)).thenReturn(true);
        when(eventClient.getEventById(99L)).thenReturn(ResponseEntity.ok(eventDto));
        when(eventClient.reserveTickets(99L, 10, AUTH_TOKEN)).thenReturn(ResponseEntity.ok(true));

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        when(bookingRepository.save(bookingCaptor.capture())).thenAnswer(inv -> {
            Booking b = inv.getArgument(0, Booking.class);
            b.setId(300L);
            return b;
        });

        BookingResponse response = new BookingResponse();
        response.setBookingId(300L);
        when(bookingMapper.toResponse(any(Booking.class))).thenReturn(response);

        bookingService.bookTicket(largeRequest, AUTH_TOKEN);

        Booking saved = bookingCaptor.getValue();
        assertThat(saved.getTotalPrice()).isEqualByComparingTo("505.00");
        assertThat(saved.getTicketCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("BookTicket - Decimal Price: Total calculated with precision")
    void bookTicket_decimalPrice_totalCalculatedWithPrecision() {
        BookTicketRequest request = new BookTicketRequest();
        request.setUserId(10L);
        request.setEventId(99L);
        request.setTicketCount(3);

        EventDto eventDto = new EventDto();
        eventDto.setId(99L);
        eventDto.setPrice(new BigDecimal("99.99"));

        when(userClient.validateUser(10L, AUTH_TOKEN)).thenReturn(true);
        when(eventClient.getEventById(99L)).thenReturn(ResponseEntity.ok(eventDto));
        when(eventClient.reserveTickets(99L, 3, AUTH_TOKEN)).thenReturn(ResponseEntity.ok(true));

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        when(bookingRepository.save(bookingCaptor.capture())).thenAnswer(inv -> {
            Booking b = inv.getArgument(0, Booking.class);
            b.setId(400L);
            return b;
        });

        when(bookingMapper.toResponse(any(Booking.class))).thenReturn(standardResponse);

        bookingService.bookTicket(request, AUTH_TOKEN);

        Booking saved = bookingCaptor.getValue();
        assertThat(saved.getTotalPrice()).isEqualByComparingTo("299.97");
    }

    // ========================================================================
    // GET BOOKING BY ID TESTS
    // ========================================================================

    @Test
    @DisplayName("GetBookingById - Found: Should return mapped response")
    void getBookingById_found_returnsMappedResponse() {
        Booking booking = new Booking();
        booking.setId(7L);

        BookingResponse resp = new BookingResponse();
        resp.setBookingId(7L);

        when(bookingRepository.findById(7L)).thenReturn(Optional.of(booking));
        when(bookingMapper.toResponse(booking)).thenReturn(resp);

        BookingResponse result = bookingService.getBookingById(7L);

        assertThat(result.getBookingId()).isEqualTo(7L);
        verify(bookingRepository).findById(7L);
        verify(bookingMapper).toResponse(booking);
    }

    @Test
    @DisplayName("GetBookingById - Not Found: Should throw IllegalArgumentException")
    void getBookingById_notFound_throwsIllegalArgumentException() {
        when(bookingRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.getBookingById(7L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Booking not found");

        verify(bookingMapper, never()).toResponse(any());
    }

    // ========================================================================
    // AUTH TOKEN PROPAGATION TESTS
    // ========================================================================

    @Test
    @DisplayName("BookTicket - Auth Token Propagation: Token passed to all client calls")
    void bookTicket_authTokenPropagation_tokenPassedToAllClients() {
        String customToken = "Bearer custom-auth-token";

        when(userClient.validateUser(10L, customToken)).thenReturn(true);
        when(eventClient.getEventById(99L)).thenReturn(ResponseEntity.ok(standardEventDto));
        when(eventClient.reserveTickets(99L, 3, customToken)).thenReturn(ResponseEntity.ok(true));

        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0, Booking.class);
            b.setId(123L);
            return b;
        });

        when(bookingMapper.toResponse(any(Booking.class))).thenReturn(standardResponse);

        bookingService.bookTicket(standardRequest, customToken);

        verify(userClient).validateUser(10L, customToken);
        verify(eventClient).reserveTickets(99L, 3, customToken);

        ArgumentCaptor<Object> kafkaCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("payment.process"), kafkaCaptor.capture());

        BookingCreatedEvent event = (BookingCreatedEvent) kafkaCaptor.getValue();
        assertThat(event.getAuthToken()).isEqualTo(customToken);
    }

    // ========================================================================
    // EDGE CASE TESTS
    // ========================================================================

    @Test
    @DisplayName("BookTicket - Zero Price Event: Total is zero")
    void bookTicket_zeroPriceEvent_totalIsZero() {
        EventDto freeEvent = new EventDto();
        freeEvent.setId(99L);
        freeEvent.setPrice(BigDecimal.ZERO);

        when(userClient.validateUser(10L, AUTH_TOKEN)).thenReturn(true);
        when(eventClient.getEventById(99L)).thenReturn(ResponseEntity.ok(freeEvent));
        when(eventClient.reserveTickets(99L, 3, AUTH_TOKEN)).thenReturn(ResponseEntity.ok(true));

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        when(bookingRepository.save(bookingCaptor.capture())).thenAnswer(inv -> {
            Booking b = inv.getArgument(0, Booking.class);
            b.setId(500L);
            return b;
        });

        when(bookingMapper.toResponse(any(Booking.class))).thenReturn(standardResponse);

        bookingService.bookTicket(standardRequest, AUTH_TOKEN);

        Booking saved = bookingCaptor.getValue();
        assertThat(saved.getTotalPrice()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("BookTicket - Booking Status: Should be PENDING initially")
    void bookTicket_bookingStatus_shouldBePending() {
        when(userClient.validateUser(10L, AUTH_TOKEN)).thenReturn(true);
        when(eventClient.getEventById(99L)).thenReturn(ResponseEntity.ok(standardEventDto));
        when(eventClient.reserveTickets(99L, 3, AUTH_TOKEN)).thenReturn(ResponseEntity.ok(true));

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        when(bookingRepository.save(bookingCaptor.capture())).thenAnswer(inv -> {
            Booking b = inv.getArgument(0, Booking.class);
            b.setId(600L);
            return b;
        });

        when(bookingMapper.toResponse(any(Booking.class))).thenReturn(standardResponse);

        bookingService.bookTicket(standardRequest, AUTH_TOKEN);

        Booking saved = bookingCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(BookingStatus.PENDING);
    }

    @Test
    @DisplayName("BookTicket - CompletableFuture.get() throws ExecutionException: Should handle properly")
    void bookTicket_executionException_thrownDuringGet() {
        // This edge case covers the InterruptedException/ExecutionException catch block
        when(userClient.validateUser(10L, AUTH_TOKEN))
                .thenAnswer(inv -> {
                    Thread.sleep(100); // Simulate delay
                    return true;
                });

        when(eventClient.getEventById(99L))
                .thenAnswer(inv -> {
                    throw new RuntimeException("Simulated execution error");
                });

        assertThatThrownBy(() -> bookingService.bookTicket(standardRequest, AUTH_TOKEN))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to fetch User or Event details");

        verify(bookingRepository, never()).save(any());
    }
    @Test
    @DisplayName("BookTicket - ExecutionException: Should handle and wrap exception")
    void bookTicket_executionException_wrappedInRuntimeException() throws Exception {
        // Create a CompletableFuture that will throw ExecutionException when get() is called
        CompletableFuture<Boolean> failingUserFuture = new CompletableFuture<>();
        failingUserFuture.completeExceptionally(new RuntimeException("Simulated execution error"));

        // Mock userClient to return the failing future
        when(userClient.validateUser(10L, AUTH_TOKEN))
                .thenAnswer(inv -> {
                    throw new RuntimeException("Simulated execution error");
                });

        when(eventClient.getEventById(99L))
                .thenReturn(ResponseEntity.ok(standardEventDto));

        // This will trigger the ExecutionException catch block
        assertThatThrownBy(() -> bookingService.bookTicket(standardRequest, AUTH_TOKEN))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to fetch User or Event details");

        verify(bookingRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(anyString(), any());
    }


}
