package com.ticketblitz.booking.event;

import com.ticketblitz.booking.client.EventClient;
import com.ticketblitz.booking.dto.PaymentUpdateEvent;
import com.ticketblitz.booking.entity.Booking;
import com.ticketblitz.booking.entity.BookingStatus;
import com.ticketblitz.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingSagaConsumer {

    private final BookingRepository bookingRepository;
    private final EventClient eventClient;

    @KafkaListener(topics = "payment.updates", groupId = "booking-saga-group")
    @Transactional // Ensures the read-modify-write happens in one DB transaction
    public void handlePaymentUpdate(PaymentUpdateEvent event) {
        log.info("📩 Saga Update Received for Booking ID: {}", event.getBookingId());

        try {
            // 1. Fetch Existing (Using lock is safer but standard findById is okay for MVP)
            Booking booking = bookingRepository.findById(event.getBookingId())
                    .orElseThrow(() -> new RuntimeException("Booking not found: " + event.getBookingId()));

            // 2. Logic to update Status
            if ("SUCCESS".equals(event.getStatus())) {
                booking.setStatus(BookingStatus.CONFIRMED);
                // Note: updated_at will be handled by @UpdateTimestamp (Java) or DB Trigger
                log.info("✅ Booking {} CONFIRMED.", booking.getId());
            } else {
                booking.setStatus(BookingStatus.FAILED);
                log.warn("❌ Booking {} FAILED. Releasing tickets...", booking.getId());

                // Compensating Transaction: Release Tickets
                try {
                    String token = event.getAuthToken(); // Assuming you added this field to DTO
                    if (token != null && !token.startsWith("Bearer ")) {
                        token = "Bearer " + token;
                    }
                    // Ideally pass token here too if Event Service is secured!
                    eventClient.releaseTickets(booking.getEventId(), booking.getTicketCount(), token);
                } catch (Exception ex) {
                    log.error("Failed to release tickets for failed booking {}", booking.getId(), ex);
                }
            }

            // 3. Save (Updates the existing row)
            bookingRepository.save(booking);

        } catch (Exception e) {
            log.error("Error processing Saga update for Booking {}", event.getBookingId(), e);
        }
    }
}
