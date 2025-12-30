package com.ticketblitz.notification.listener;

import com.ticketblitz.notification.client.BookingClient;
import com.ticketblitz.notification.client.UserClient;
import com.ticketblitz.notification.dto.PaymentUpdateEvent;
import com.ticketblitz.notification.event.RecommendationReadyEvent;
import com.ticketblitz.notification.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {

    private final JavaMailSender mailSender;
    private final UserClient userClient;
    private final BookingClient bookingClient;
    private final RecommendationService recommendationService;

    // --- 1. HANDLE PAYMENT (Kafka) ---
    @KafkaListener(topics = "payment.updates", groupId = "notification-group")
    public void handlePaymentUpdate(PaymentUpdateEvent event) {
        log.info("📩 Kafka: Payment Update received for Booking {} | Status: {}", event.getBookingId(), event.getStatus());

        try {
            // Step 1: Fetch User Details (Required for both Success and Failure to get Email)
            UserClient.UserDto user = userClient.getUserById(event.getUserId(), event.getAuthToken());

            if (user == null || user.getEmail() == null) {
                log.warn("❌ User not found or email missing for User ID {}", event.getUserId());
                return;
            }

            // Step 2: Route based on Status
            if ("SUCCESS".equals(event.getStatus())) {
                processSuccessfulPayment(event, user);
            } else if ("FAILED".equals(event.getStatus())) {
                processFailedPayment(event, user);
            }

        } catch (Exception e) {
            log.error("❌ Critical error in NotificationListener", e);
        }
    }

    // --- Logic for Success ---
    private void processSuccessfulPayment(PaymentUpdateEvent event, UserClient.UserDto user) {
        // A. Fetch Booking to get Event ID (for AI)
        Long eventId = null;
        try {
            BookingClient.BookingDto booking = bookingClient.getBookingById(event.getBookingId(), event.getAuthToken());
            if (booking != null) {
                eventId = booking.getEventId();
            }
        } catch (Exception e) {
            log.warn("⚠️ Could not fetch booking details for ID {}. Proceeding with receipt only.", event.getBookingId());
        }

        // B. Send Confirmation Email
        String emailBody = String.format(
                "Hi %s,\n\nYour payment was successful!\nTransaction ID: %s\n\nEnjoy the show!",
                user.getUsername(), event.getTransactionId()
        );
        sendEmail(user.getEmail(), "Booking Confirmed: #" + event.getBookingId(), emailBody);

        // C. Trigger AI Recommendation (if we successfully got the Event ID)
        if (eventId != null) {
            recommendationService.generateAndNotify(user.getUsername(), user.getEmail(), eventId);
        }
    }

    // --- Logic for Failure ---
    private void processFailedPayment(PaymentUpdateEvent event, UserClient.UserDto user) {
        log.info("Processing failure notification for User {}", user.getEmail());

        // Note: We use the helper method 'sendEmail' instead of undefined 'emailService'
        // Note: We use 'user.getEmail()' because the Kafka event might not have the email string
        String emailBody = String.format(
                "Hi %s,\n\nWe were unable to process your payment for Booking #%s.\n" +
                        "Any pending charges will be reversed, and the tickets have been released.",
                user.getUsername(), event.getBookingId()
        );

        sendEmail(user.getEmail(), "Payment Failed - TicketBlitz", emailBody);
    }

    // --- 2. HANDLE RECOMMENDATION (Internal Spring Event) ---
    @EventListener
    public void handleRecommendationReady(RecommendationReadyEvent event) {
        log.info("✨ EventListener: Sending suggestions to {}", event.getEmail());

        String emailBody = "Hi " + event.getUsername() + ",\n\n" +
                "Based on your recent booking, here are 3 other events you might like:\n\n" +
                event.getRecommendations() + "\n\n" +
                "See you there!\nTicketBlitz AI";

        try {
            // Add a small delay if suspecting rate limits, or just rely on retry
            try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
            sendEmail(event.getEmail(), "Top Picks for You! 🎸", emailBody);
        } catch (Exception e) {
            // Log explicitly so we know IT IS the mail sender failing
            log.error("CRITICAL: Failed to send Recommendation Email. Likely SMTP issue.", e);
        }
    }

    // --- Helper ---
    private void sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(text);
            mailSender.send(msg);
            log.info("✅ Email sent to {}", to);
        } catch (Exception e) {
            log.error("❌ Failed to send email to {}", to, e);
        }
    }
}
