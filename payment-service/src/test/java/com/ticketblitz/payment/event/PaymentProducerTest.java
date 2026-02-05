package com.ticketblitz.payment.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentProducerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PaymentProducer paymentProducer;

    @Test
    @DisplayName("sendPaymentResult: Should successfully send payment update to Kafka")
    void sendPaymentResultSuccess() throws JsonProcessingException {
        // Arrange
        Long bookingId = 101L;
        String status = "SUCCESS";
        String txId = "ch_test_12345";
        Long userId = 10L;
        String authToken = "Bearer token123";

        PaymentUpdateEvent expectedEvent = new PaymentUpdateEvent(bookingId, userId, status, txId, authToken);
        String expectedJson = "{\"bookingId\":101,\"userId\":10,\"status\":\"SUCCESS\"}";

        when(objectMapper.writeValueAsString(any(PaymentUpdateEvent.class))).thenReturn(expectedJson);

        // Act
        paymentProducer.sendPaymentResult(bookingId, status, txId, userId, authToken);

        // Assert
        ArgumentCaptor<PaymentUpdateEvent> eventCaptor = ArgumentCaptor.forClass(PaymentUpdateEvent.class);
        verify(objectMapper).writeValueAsString(eventCaptor.capture());

        PaymentUpdateEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getBookingId()).isEqualTo(bookingId);
        assertThat(capturedEvent.getStatus()).isEqualTo(status);
        assertThat(capturedEvent.getTransactionId()).isEqualTo(txId);
        assertThat(capturedEvent.getUserId()).isEqualTo(userId);
        assertThat(capturedEvent.getAuthToken()).isEqualTo(authToken);

        verify(kafkaTemplate).send(eq("payment.updates"), eq(expectedJson));
    }

    @Test
    @DisplayName("sendPaymentResult: Should handle JsonProcessingException gracefully")
    void sendPaymentResultJsonProcessingException() throws JsonProcessingException {
        // Arrange
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("Serialization error") {});

        // Act
        paymentProducer.sendPaymentResult(102L, "FAILED", "txn_error", 11L, "Bearer token456");

        // Assert - Should not throw, just log error
        verify(objectMapper).writeValueAsString(any());
        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }

    @Test
    @DisplayName("sendPaymentResult: Should handle Kafka send failure gracefully")
    void sendPaymentResultKafkaFailure() throws JsonProcessingException {
        // Arrange
        String expectedJson = "{\"bookingId\":103}";
        when(objectMapper.writeValueAsString(any())).thenReturn(expectedJson);
        doThrow(new RuntimeException("Kafka connection failed")).when(kafkaTemplate).send(anyString(), anyString());

        // Act - Should not throw, just log error
        paymentProducer.sendPaymentResult(103L, "SUCCESS", "ch_test", 12L, "Bearer token789");

        // Assert
        verify(kafkaTemplate).send(eq("payment.updates"), eq(expectedJson));
    }

    @Test
    @DisplayName("sendPaymentResult: Should send FAILED status correctly")
    void sendPaymentResultFailedStatus() throws JsonProcessingException {
        // Arrange
        String expectedJson = "{\"status\":\"FAILED\"}";
        when(objectMapper.writeValueAsString(any())).thenReturn(expectedJson);

        // Act
        paymentProducer.sendPaymentResult(104L, "FAILED", "FALLBACK_ERROR", 13L, "Bearer tokenFB");

        // Assert
        ArgumentCaptor<PaymentUpdateEvent> captor = ArgumentCaptor.forClass(PaymentUpdateEvent.class);
        verify(objectMapper).writeValueAsString(captor.capture());

        assertThat(captor.getValue().getStatus()).isEqualTo("FAILED");
        assertThat(captor.getValue().getTransactionId()).isEqualTo("FALLBACK_ERROR");
    }

    @Test
    @DisplayName("PaymentProducer: Should be instantiable via constructor")
    void paymentProducerConstructor() {
        PaymentProducer producer = new PaymentProducer(kafkaTemplate, objectMapper);
        assertThat(producer).isNotNull();
    }
}
