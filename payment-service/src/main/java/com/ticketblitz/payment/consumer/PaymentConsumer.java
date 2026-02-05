package com.ticketblitz.payment.consumer;

import com.ticketblitz.payment.dto.BookingCreatedEvent;
import com.ticketblitz.payment.repository.PaymentRepository;
import com.ticketblitz.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentConsumer {

    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;

    @KafkaListener(
            topics = "payment.process",
            groupId = "payment-group"
            // REMOVED: properties override. We rely on KafkaConfig's TYPE_MAPPINGS.
    )
    public void processPayment(BookingCreatedEvent event) {
        log.info("📥 [Kafka] Received Payment Request | BookingID: {}", event.getBookingId());

        if (paymentRepository.existsByBookingId(event.getBookingId())) {
            log.warn("⚠️ Duplicate Payment Event ignored for BookingID: {}. Message Acknowledged.", event.getBookingId());
            return;
        }

        try {
            paymentService.processPayment(event);
        } catch (Exception e) {
            log.error("Error processing payment", e);
            throw e;
        }
    }
}
