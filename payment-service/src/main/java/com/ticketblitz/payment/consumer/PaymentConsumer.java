package com.ticketblitz.payment.consumer;

import com.ticketblitz.payment.dto.BookingCreatedEvent;
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

    /**
     * KAFKA LISTENER
     * Topic: payment process
     * Action: Delegates to Service
     */
    @KafkaListener(topics = "payment.process", groupId = "payment-group")
    public void processPayment(BookingCreatedEvent event) {
        log.info("📥 [Kafka] Received Payment Request | BookingID: {}", event.getBookingId());
        paymentService.processPayment(event);
    }
}
