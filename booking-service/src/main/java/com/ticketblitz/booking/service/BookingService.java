package com.ticketblitz.booking.service;

import com.ticketblitz.booking.client.EventClient;
import com.ticketblitz.booking.client.UserClient;
import com.ticketblitz.booking.dto.BookTicketRequest;
import com.ticketblitz.booking.dto.BookingCreatedEvent;
import com.ticketblitz.booking.dto.BookingResponse;
import com.ticketblitz.booking.dto.EventDto; // Make sure you have this DTO
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

        // 1. Validate User
        try {
            boolean userExists = userClient.validateUser(request.getUserId());
            if (!userExists) {
                throw new IllegalArgumentException("User ID not found");
            }
        } catch (Exception e) {
            log.error("User validation failed", e);
            throw new IllegalArgumentException("Invalid User Service response");
        }

        // 2. Fetch Event Details (SECURE WAY: Get price from DB, not Request)
        ResponseEntity<EventDto> eventResponse = eventClient.getEventById(request.getEventId());

        if (!eventResponse.getStatusCode().is2xxSuccessful() || eventResponse.getBody() == null) {
            throw new IllegalArgumentException("Event not found with ID: " + request.getEventId());
        }

        EventDto eventDto = eventResponse.getBody();
        BigDecimal pricePerTicket = eventDto.getPrice();

        // 3. Reserve Tickets
        ResponseEntity<Boolean> reservation = eventClient.reserveTickets(request.getEventId(), request.getTicketCount());

        if (!Boolean.TRUE.equals(reservation.getBody())) {
            throw new IllegalStateException("Tickets sold out or unavailable.");
        }

        // 4. Calculate Total & Save Booking
        Booking booking = new Booking();
        booking.setUserId(request.getUserId());
        booking.setEventId(request.getEventId());
        booking.setTicketCount(request.getTicketCount());

        // Calculation: Real Price from DB * Ticket Count
        BigDecimal total = pricePerTicket.multiply(BigDecimal.valueOf(request.getTicketCount()));

        booking.setTotalPrice(total);
        booking.setStatus(BookingStatus.PENDING);

        Booking savedBooking = bookingRepository.save(booking);

        // 5. Async: Send to Payment Service
        BookingCreatedEvent event = new BookingCreatedEvent(
                savedBooking.getId(),
                savedBooking.getUserId(),
                savedBooking.getTotalPrice(),
                "user@example.com",
                authToken
        );

        kafkaTemplate.send(PAYMENT_TOPIC, event);
        log.info("Booking {} created. Amount: {}. Event sent to Kafka.", savedBooking.getId(), savedBooking.getTotalPrice());

        return bookingMapper.toResponse(savedBooking);
    }

    public BookingResponse getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        return bookingMapper.toResponse(booking);
    }
}
