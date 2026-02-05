package com.ticketblitz.notification.listener;

import com.ticketblitz.notification.client.BookingClient;
import com.ticketblitz.notification.client.UserClient;
import com.ticketblitz.notification.dto.EmailDispatchEvent;
import com.ticketblitz.notification.dto.PaymentUpdateEvent;
import com.ticketblitz.notification.dto.RecommendationRequestEvent;
import com.ticketblitz.notification.service.EmailService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Notification Gateway with:
 * - Two processing lanes (Receipt & AI Delivery)
 * - Rate limiting (Bucket4j)
 * - Distributed tracing (automatic via Micrometer + Kafka headers)
 * - SMTP fault tolerance (via EmailService)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {

    private final EmailService emailService;  // NEW: Use resilient email service
    private final UserClient userClient;
    private final BookingClient bookingClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.recommendation-request}")
    private String recommendationRequestTopic;

    // Rate Limiter: 2 emails per second
    private final Bucket rateLimiter = Bucket4j.builder()
            .addLimit(Bandwidth.classic(2, Refill.greedy(2, Duration.ofSeconds(1))))
            .build();

    // Executor for async email sending
    private final ScheduledExecutorService emailExecutor = Executors.newScheduledThreadPool(5);

    /**
     * LANE 1: RECEIPT LANE (Payment Success -> Email -> Route to Python)
     *
     * Trace propagation: Kafka automatically extracts trace context from message headers
     * and continues the distributed trace started in payment-service.
     */
    @KafkaListener(
            topics = "${kafka.topics.payment-updates}",
            groupId = "notification-gateway-group",
            properties = {"spring.json.value.default.type=com.ticketblitz.notification.dto.PaymentUpdateEvent"}
    )
    @Observed(name = "kafka.payment-update", contextualName = "handle-payment-update")
    public void handlePaymentUpdate(PaymentUpdateEvent event) {
        log.info("📩 [RECEIPT LANE] Payment event received: Booking={}, Status={}",
                event.getBookingId(), event.getStatus());

        try {
            UserClient.UserDto user = userClient.getUserById(event.getUserId(), event.getAuthToken());
            if (user == null || user.getEmail() == null) {
                log.warn("⚠️ User not found or email missing for User ID: {}", event.getUserId());
                return;
            }

            if ("SUCCESS".equals(event.getStatus())) {
                handleSuccessfulPayment(event, user);
            } else if ("FAILED".equals(event.getStatus())) {
                handleFailedPayment(user);
            }
        } catch (Exception e) {
            log.error("❌ Error processing payment update for Booking: {}", event.getBookingId(), e);
        }
    }

    private void handleSuccessfulPayment(PaymentUpdateEvent event, UserClient.UserDto user) {
        // ACTION A: Send Booking Confirmation Email (Rate Limited)
        scheduleEmail(
                user.getEmail(),
                "Booking Confirmed!",
                String.format("Hi %s,\n\nYour payment was successful! Thank you for choosing TicketBlitz.\n" +
                                "Transaction ID: %s\n\nGet ready for an amazing experience!\n\n" +
                                "Best regards,\nTicketBlitz Team",
                        user.getUsername(), event.getTransactionId())
        );

        // ACTION B: Route to Python Recommendation Service
        try {
            BookingClient.BookingDto booking = bookingClient.getBookingById(
                    event.getBookingId(),
                    event.getAuthToken()
            );

            if (booking != null && booking.getEventId() != null) {
                routeToRecommendationService(user, booking.getEventId(), event.getAuthToken());
            } else {
                log.warn("⚠️ Booking or EventId missing for Booking: {}", event.getBookingId());
            }
        } catch (Exception e) {
            log.error("❌ Failed to route to recommendation service for User: {}", user.getId(), e);
        }
    }

    @Observed(name = "route.recommendation", contextualName = "route-to-recommendation-service")
    private void routeToRecommendationService(UserClient.UserDto user, Long eventId, String authToken) {
        RecommendationRequestEvent requestEvent = RecommendationRequestEvent.builder()
                .userId(user.getId())
                .eventId(eventId)
                .userEmail(user.getEmail())
                .username(user.getUsername())
                .authToken(authToken)
                .build();

        // Kafka automatically propagates trace context in message headers
        kafkaTemplate.send(recommendationRequestTopic, requestEvent);
        log.info("📤 [EVENT ROUTED] Recommendation request -> Python Service (User={}, Event={})",
                user.getId(), eventId);
    }

    private void handleFailedPayment(UserClient.UserDto user) {
        scheduleEmail(
                user.getEmail(),
                "Payment Failed",
                String.format("Hi %s,\n\nWe're sorry, but your payment could not be processed. " +
                        "Please check your payment details and try again.\n\n" +
                        "Best regards,\nTicketBlitz Team", user.getUsername())
        );
    }

    /**
     * LANE 2: AI DELIVERY LANE (Python Email Gateway)
     *
     * This receives emails generated by the Python Recommendation Service
     * and continues the distributed trace.
     */
    @KafkaListener(
            topics = "${kafka.topics.email-dispatch}",
            groupId = "notification-gateway-group",
            properties = {"spring.json.value.default.type=com.ticketblitz.notification.dto.EmailDispatchEvent"}
    )
    @Observed(name = "kafka.email-dispatch", contextualName = "handle-email-dispatch")
    public void handleEmailDispatch(EmailDispatchEvent event) {
        log.info("📧 [AI DELIVERY LANE] Email dispatch received for: {}", event.getRecipientEmail());
        scheduleEmail(event.getRecipientEmail(), event.getSubject(), event.getBody());
    }

    /**
     * Schedule Email with Rate Limiting
     * Uses resilient EmailService with automatic retry
     */
    private void scheduleEmail(String to, String subject, String body) {
        emailExecutor.submit(() -> {
            try {
                if (rateLimiter.tryConsume(1)) {
                    // NEW: Use EmailService with retry capability
                    emailService.sendEmail(to, subject, body);
                } else {
                    // Rate limit hit - reschedule
                    log.warn("⏳ Rate limit hit. Rescheduling email to {}", to);
                    emailExecutor.schedule(
                            () -> scheduleEmail(to, subject, body),
                            10,
                            TimeUnit.SECONDS
                    );
                }
            } catch (Exception e) {
                log.error("❌ Error in email scheduler", e);
            }
        });
    }
}
