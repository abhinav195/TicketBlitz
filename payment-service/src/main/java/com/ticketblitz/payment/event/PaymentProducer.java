package com.ticketblitz.payment.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // The topic name where Booking Service and Notification Service will listen
    private static final String TOPIC = "payment.updates";

    public void sendPaymentResult(Long bookingId, String status, String txId, Long userId, String authToken) {
        log.info("Preparing to publish Payment Result for Booking ID: {}", bookingId);

        PaymentUpdateEvent event = new PaymentUpdateEvent(bookingId, userId, status, txId, authToken);

        try {
            // Convert Object -> JSON String
            String jsonMessage = objectMapper.writeValueAsString(event);

            // Publish to Kafka
            kafkaTemplate.send(TOPIC, jsonMessage);

            log.info("✅ Kafka Message Sent: [Topic: {}] [Payload: {}]", TOPIC, jsonMessage);
        } catch (JsonProcessingException e) {
            log.error("❌ Failed to serialize PaymentUpdateEvent", e);
        } catch (Exception e) {
            log.error("❌ Failed to send message to Kafka", e);
        }
    }
}
