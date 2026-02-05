package com.ticketblitz.payment.service;

import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.ticketblitz.payment.dto.BookingCreatedEvent;
import com.ticketblitz.payment.entity.Payment;
import com.ticketblitz.payment.event.PaymentProducer;
import com.ticketblitz.payment.exception.PaymentGatewayException;
import com.ticketblitz.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentProducer paymentProducer;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        // Inject the test Stripe API key and currency
        ReflectionTestUtils.setField(paymentService, "stripeApiKey", "sk_test_fake_key_for_testing");
        ReflectionTestUtils.setField(paymentService, "currency", "inr");
    }

    @Test
    @DisplayName("processPayment: Should process payment successfully with valid amount")
    void processPaymentShouldSucceedWithValidAmount() {
        // Arrange
        BookingCreatedEvent event = createBookingEvent(101L, 10L, "250.00", "user@example.com", "Bearer token123");

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        when(paymentRepository.save(paymentCaptor.capture())).thenAnswer(inv -> {
            Payment p = inv.getArgument(0, Payment.class);
            p.setId(1L);
            return p;
        });

        // Mock Stripe Charge creation
        try (MockedStatic<Charge> chargeMock = mockStatic(Charge.class)) {
            Charge mockCharge = mock(Charge.class);
            when(mockCharge.getId()).thenReturn("ch_test_12345");
            chargeMock.when(() -> Charge.create(anyMap())).thenReturn(mockCharge);

            // Act
            paymentService.processPayment(event);

            // Assert
            Payment savedPayment = paymentCaptor.getValue();
            assertThat(savedPayment.getBookingId()).isEqualTo(101L);
            assertThat(savedPayment.getUserId()).isEqualTo(10L);
            assertThat(savedPayment.getAmount()).isEqualByComparingTo("250.00");
            assertThat(savedPayment.getStatus()).isEqualTo("SUCCESS");
            assertThat(savedPayment.getStripePaymentId()).isEqualTo("ch_test_12345");

            verify(paymentRepository).save(any(Payment.class));
            verify(paymentProducer).sendPaymentResult(
                    eq(101L),
                    eq("SUCCESS"),
                    eq("ch_test_12345"),
                    eq(10L),
                    eq("Bearer token123")
            );
        }
    }

    @Test
    @DisplayName("processPayment: Should throw RuntimeException when Stripe fails")
    void processPaymentShouldThrowExceptionWhenStripeFails() {
        // Arrange
        BookingCreatedEvent event = createBookingEvent(102L, 11L, "100.00", "fail@example.com", "Bearer token456");

        try (MockedStatic<Charge> chargeMock = mockStatic(Charge.class)) {
            chargeMock.when(() -> Charge.create(anyMap()))
                    .thenThrow(new RuntimeException("Stripe API Error"));

            // Act & Assert
            assertThatThrownBy(() -> paymentService.processPayment(event))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Payment Gateway Failure");

            verify(paymentRepository, never()).save(any());
            verify(paymentProducer, never()).sendPaymentResult(anyLong(), anyString(), anyString(), anyLong(), anyString());
        }
    }

    @Test
    @DisplayName("processPayment: Should handle negative amount")
    void processPaymentShouldHandleNegativeAmount() {
        // Arrange
        BookingCreatedEvent event = createBookingEvent(103L, 12L, "-50.00", "invalid@example.com", "Bearer token789");

        try (MockedStatic<Charge> chargeMock = mockStatic(Charge.class)) {
            chargeMock.when(() -> Charge.create(anyMap()))
                    .thenThrow(new RuntimeException("Invalid amount"));

            // Act & Assert
            assertThatThrownBy(() -> paymentService.processPayment(event))
                    .isInstanceOf(RuntimeException.class);

            verify(paymentRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("processPayment: Should handle zero amount")
    void processPaymentShouldHandleZeroAmount() {
        // Arrange
        BookingCreatedEvent event = createBookingEvent(104L, 13L, "0.00", "zero@example.com", "Bearer token000");

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        when(paymentRepository.save(paymentCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        try (MockedStatic<Charge> chargeMock = mockStatic(Charge.class)) {
            Charge mockCharge = mock(Charge.class);
            when(mockCharge.getId()).thenReturn("ch_zero_123");
            chargeMock.when(() -> Charge.create(anyMap())).thenReturn(mockCharge);

            // Act
            paymentService.processPayment(event);

            // Assert
            Payment saved = paymentCaptor.getValue();
            assertThat(saved.getAmount()).isEqualByComparingTo("0.00");
            assertThat(saved.getStatus()).isEqualTo("SUCCESS");

            verify(paymentProducer).sendPaymentResult(
                    eq(104L),
                    eq("SUCCESS"),
                    eq("ch_zero_123"),
                    eq(13L),
                    eq("Bearer token000")
            );
        }
    }

    @Test
    @DisplayName("processPayment: Should handle large amounts")
    void processPaymentShouldHandleLargeAmounts() {
        // Arrange
        BookingCreatedEvent event = createBookingEvent(106L, 15L, "999999.99", "large@example.com", "Bearer tokenLarge");

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        when(paymentRepository.save(paymentCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        try (MockedStatic<Charge> chargeMock = mockStatic(Charge.class)) {
            Charge mockCharge = mock(Charge.class);
            when(mockCharge.getId()).thenReturn("ch_large_999");
            chargeMock.when(() -> Charge.create(anyMap())).thenReturn(mockCharge);

            // Act
            paymentService.processPayment(event);

            // Assert
            Payment saved = paymentCaptor.getValue();
            assertThat(saved.getAmount()).isEqualByComparingTo("999999.99");
            assertThat(saved.getStatus()).isEqualTo("SUCCESS");
        }
    }

    @Test
    @DisplayName("processPayment: Should use configured currency")
    void processPaymentShouldUseConfiguredCurrency() {
        // Arrange
        BookingCreatedEvent event = createBookingEvent(107L, 16L, "100.00", "currency@example.com", "Bearer tokenCurrency");

        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        try (MockedStatic<Charge> chargeMock = mockStatic(Charge.class)) {
            Charge mockCharge = mock(Charge.class);
            when(mockCharge.getId()).thenReturn("ch_currency");

            ArgumentCaptor<java.util.Map> mapCaptor = ArgumentCaptor.forClass(java.util.Map.class);
            chargeMock.when(() -> Charge.create(mapCaptor.capture())).thenReturn(mockCharge);

            // Act
            paymentService.processPayment(event);

            // Assert
            java.util.Map<String, Object> chargeParams = mapCaptor.getValue();
            assertThat(chargeParams.get("currency")).isEqualTo("inr");
            assertThat(chargeParams.get("amount")).isEqualTo(10000); // 100.00 * 100
            assertThat(chargeParams.get("source")).isEqualTo("tok_visa");
            assertThat(chargeParams.get("description")).asString().contains("Booking ID: 107");
        }
    }

    @Test
    @DisplayName("stripeFallback: Should save failed payment and throw PaymentGatewayException")
    void stripeFallbackShouldSaveFailedPaymentAndThrowException() {
        // Arrange
        BookingCreatedEvent event = createBookingEvent(105L, 14L, "500.00", "fallback@example.com", "Bearer tokenFB");

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        when(paymentRepository.save(paymentCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        Throwable simulatedError = new RuntimeException("Circuit Open");

        // Act & Assert
        assertThatThrownBy(() -> paymentService.stripeFallback(event, simulatedError))
                .isInstanceOf(PaymentGatewayException.class)
                .hasMessageContaining("Payment Service Unavailable");

        // Assert payment saved with FAILED status
        Payment saved = paymentCaptor.getValue();
        assertThat(saved.getBookingId()).isEqualTo(105L);
        assertThat(saved.getUserId()).isEqualTo(14L);
        assertThat(saved.getAmount()).isEqualByComparingTo("500.00");
        assertThat(saved.getStatus()).isEqualTo("FAILED");
        assertThat(saved.getStripePaymentId()).isEqualTo("FALLBACK_ERROR");

        verify(paymentRepository).save(any(Payment.class));
        verify(paymentProducer).sendPaymentResult(
                eq(105L),
                eq("FAILED"),
                eq("FALLBACK_ERROR"),
                eq(14L),
                eq("Bearer tokenFB")
        );
    }

    @Test
    @DisplayName("stripeFallback: Should handle different error types")
    void stripeFallbackShouldHandleDifferentErrorTypes() {
        // Arrange
        BookingCreatedEvent event = createBookingEvent(108L, 17L, "300.00", "error@example.com", "Bearer tokenErr");
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        // Test with StripeException
        StripeException stripeException = new StripeException("Stripe down", "req_123", "code", 500) {};

        // Act & Assert
        assertThatThrownBy(() -> paymentService.stripeFallback(event, stripeException))
                .isInstanceOf(PaymentGatewayException.class);

        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("stripeFallback: Should work with null throwable")
    void stripeFallbackShouldWorkWithNullThrowable() {
        // Arrange
        BookingCreatedEvent event = createBookingEvent(109L, 18L, "200.00", "null@example.com", "Bearer tokenNull");
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act & Assert
        assertThatThrownBy(() -> paymentService.stripeFallback(event, null))
                .isInstanceOf(PaymentGatewayException.class);

        verify(paymentRepository).save(any(Payment.class));
        verify(paymentProducer).sendPaymentResult(anyLong(), eq("FAILED"), eq("FALLBACK_ERROR"), anyLong(), anyString());
    }

    @Test
    @DisplayName("PaymentService: Should have @Service annotation")
    void paymentServiceShouldHaveServiceAnnotation() {
        assertThat(PaymentService.class.isAnnotationPresent(
                org.springframework.stereotype.Service.class
        )).isTrue();
    }

    @Test
    @DisplayName("PaymentService: Should have @CircuitBreaker annotation on processPayment")
    void processPaymentShouldHaveCircuitBreakerAnnotation() throws NoSuchMethodException {
        var method = PaymentService.class.getMethod("processPayment", BookingCreatedEvent.class);
        assertThat(method.isAnnotationPresent(
                io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker.class
        )).isTrue();
    }

    @Test
    @DisplayName("PaymentService: CircuitBreaker should have correct configuration")
    void circuitBreakerShouldHaveCorrectConfiguration() throws NoSuchMethodException {
        var method = PaymentService.class.getMethod("processPayment", BookingCreatedEvent.class);
        var circuitBreaker = method.getAnnotation(
                io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker.class
        );

        assertThat(circuitBreaker.name()).isEqualTo("stripeService");
        assertThat(circuitBreaker.fallbackMethod()).isEqualTo("stripeFallback");
    }

    @Test
    @DisplayName("PaymentService: Should be instantiable via constructor")
    void paymentServiceShouldBeInstantiable() {
        PaymentService service = new PaymentService(paymentRepository, paymentProducer);
        assertThat(service).isNotNull();
    }

    // Helper method
    private BookingCreatedEvent createBookingEvent(Long bookingId, Long userId, String amount, String email, String authToken) {
        BookingCreatedEvent event = new BookingCreatedEvent();
        event.setBookingId(bookingId);
        event.setUserId(userId);
        event.setAmount(new BigDecimal(amount));
        event.setEmail(email);
        event.setAuthToken(authToken);
        return event;
    }
}
