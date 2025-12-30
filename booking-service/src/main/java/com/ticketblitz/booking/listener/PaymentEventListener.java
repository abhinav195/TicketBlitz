package com.ticketblitz.booking.listener;

import com.ticketblitz.booking.client.EventClient;
import com.ticketblitz.booking.entity.Booking;
import com.ticketblitz.booking.entity.BookingStatus;
import com.ticketblitz.booking.repository.BookingRepository;
// You might need to create/import this DTO in booking service if it doesn't exist
import com.ticketblitz.booking.dto.PaymentUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final BookingRepository bookingRepository;
    private final EventClient eventClient;

    // FIX: Changed parameter from String to PaymentUpdateEvent
    @KafkaListener(topics = "payment.updates", groupId = "booking-group")
    @Transactional
    public void handlePaymentUpdate(PaymentUpdateEvent event) {
        log.info("Received Payment Update for Booking: {}", event.getBookingId());

        if ("FAILED".equals(event.getStatus())) {
            handlePaymentFailure(event.getBookingId(), event.getAuthToken());
        } else {
            // Optional: Handle SUCCESS (e.g., mark booking as CONFIRMED)
            handlePaymentSuccess(event.getBookingId());
        }
    }

    private void handlePaymentSuccess(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
            log.info("✅ Booking {} CONFIRMED.", bookingId);
        }
    }

    public void handlePaymentFailure(Long bookingId, String token) {
        log.warn("Payment FAILED for Booking {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return;
        }

        // 1. Update Status to FAILED/CANCELLED
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // 2. COMPENSATE: Release Tickets
        try {
            eventClient.releaseTickets(booking.getEventId(), booking.getTicketCount(), token);
            log.info("Compensation Successful: Tickets released for Booking {}", bookingId);
        } catch (Exception e) {
            log.error("CRITICAL: Failed to release tickets for Booking {}.", bookingId, e);
        }
    }
}
