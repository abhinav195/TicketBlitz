package com.ticketblitz.booking.service;

import com.ticketblitz.booking.client.EventClient;
import com.ticketblitz.booking.client.UserClient;
import com.ticketblitz.booking.dto.BookTicketRequest;
import com.ticketblitz.booking.dto.BookingCreatedEvent;
import com.ticketblitz.booking.dto.BookingResponse;
import com.ticketblitz.booking.dto.EventDto;
import com.ticketblitz.booking.entity.Booking;
import com.ticketblitz.booking.entity.BookingStatus;
import com.ticketblitz.booking.repository.BookingRepository;
import com.ticketblitz.booking.mapper.BookingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final EventClient eventClient;
    private final UserClient userClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final BookingMapper bookingMapper;

    private static final String PAYMENT_TOPIC = "payment.process";

    @Transactional
    public BookingResponse bookTicket(BookTicketRequest request, String authToken) {
        log.info("Starting booking for User {} Event {}", request.getUserId(), request.getEventId());

        // 1. ASYNC ORCHESTRATION: Fetch User and Event in PARALLEL
        CompletableFuture<Boolean> userFuture = CompletableFuture.supplyAsync(() -> {
            // FIX: Explicitly passing authToken to maintain context in new thread
            return userClient.validateUser(request.getUserId(), authToken);
        });

        CompletableFuture<EventDto> eventFuture = CompletableFuture.supplyAsync(() -> {
            ResponseEntity<EventDto> response = eventClient.getEventById(request.getEventId());
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IllegalArgumentException("Event not found");
            }
            return response.getBody();
        });

        // Wait for both to complete
        try {
            CompletableFuture.allOf(userFuture, eventFuture).join();
        } catch (Exception e) {
            log.error("Async Fetch Failed", e);
            throw new RuntimeException("Failed to fetch User or Event details: " + e.getMessage());
        }

        // 2. Extract Results
        boolean userExists;
        EventDto eventDto;
        try {
            userExists = userFuture.get();
            eventDto = eventFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error retrieving async results", e);
        }

        if (!userExists) {
            throw new IllegalArgumentException("User ID not found");
        }

        // 3. Reserve Tickets (Synchronous - Critical Write Operation)
        // FIX: Passing authToken to synchronous call as well to be safe with security
        ResponseEntity<Boolean> reservation = eventClient.reserveTickets(request.getEventId(), request.getTicketCount(), authToken);
        if (!Boolean.TRUE.equals(reservation.getBody())) {
            throw new IllegalStateException("Tickets sold out or unavailable.");
        }

        // 4. Calculate Total & Save Booking
        Booking booking = new Booking();
        booking.setUserId(request.getUserId());
        booking.setEventId(request.getEventId());
        booking.setTicketCount(request.getTicketCount());

        BigDecimal pricePerTicket = eventDto.getPrice();
        BigDecimal total = pricePerTicket.multiply(BigDecimal.valueOf(request.getTicketCount()));
        booking.setTotalPrice(total);
        booking.setStatus(BookingStatus.PENDING);

        Booking savedBooking = bookingRepository.save(booking);

        // 5. Send to Payment Service
        BookingCreatedEvent event = new BookingCreatedEvent(
                savedBooking.getId(),
                savedBooking.getUserId(),
                savedBooking.getTotalPrice(),
                "user@example.com",
                authToken
        );

        kafkaTemplate.send(PAYMENT_TOPIC, event);
        log.info("Booking {} created. Async checks completed. Sent to Kafka.", savedBooking.getId());

        return bookingMapper.toResponse(savedBooking);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with ID: " + id));
        return bookingMapper.toResponse(booking);
    }
}
