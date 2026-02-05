package com.ticketblitz.payment.consumer;

import com.ticketblitz.payment.dto.BookingCreatedEvent;
import com.ticketblitz.payment.repository.PaymentRepository;
import com.ticketblitz.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class PaymentConsumerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentConsumer paymentConsumer;

    @Test
    void processPayment_duplicateEvent_idempotencyCheckPasses_stripeNotCalled_messageAcknowledged() {
        BookingCreatedEvent event = new BookingCreatedEvent();
        event.setBookingId(101L);
        event.setUserId(10L);
        event.setAmount(new BigDecimal("250.00"));
        event.setEmail("duplicate@example.com");
        event.setAuthToken("Bearer token123");

        // CASE A: Duplicate - Payment already exists
        when(paymentRepository.existsByBookingId(101L)).thenReturn(true);

        // Execute
        paymentConsumer.processPayment(event);

        // ASSERTIONS
        verify(paymentRepository).existsByBookingId(101L);
        verify(paymentService, never()).processPayment(any());
        verifyNoMoreInteractions(paymentService);
    }

    @Test
    void processPayment_newEvent_idempotencyCheckFails_stripeCalled_paymentProcessed() {
        BookingCreatedEvent event = new BookingCreatedEvent();
        event.setBookingId(102L);
        event.setUserId(11L);
        event.setAmount(new BigDecimal("100.00"));
        event.setEmail("new@example.com");
        event.setAuthToken("Bearer token456");

        // CASE B: New - Payment does NOT exist
        when(paymentRepository.existsByBookingId(102L)).thenReturn(false);
        doNothing().when(paymentService).processPayment(event);

        // Execute
        paymentConsumer.processPayment(event);

        // ASSERTIONS
        verify(paymentRepository).existsByBookingId(102L);
        verify(paymentService).processPayment(event);
        verifyNoMoreInteractions(paymentService);
    }

    @Test
    void processPayment_newEvent_serviceThrowsException_exceptionBubblesUp_messageNotAcknowledged() {
        BookingCreatedEvent event = new BookingCreatedEvent();
        event.setBookingId(103L);
        event.setUserId(12L);
        event.setAmount(new BigDecimal("500.00"));
        event.setEmail("error@example.com");
        event.setAuthToken("Bearer token789");

        when(paymentRepository.existsByBookingId(103L)).thenReturn(false);
        doThrow(new RuntimeException("Stripe Gateway Down")).when(paymentService).processPayment(event);

        // Execute & Assert
        assertThatThrownBy(() -> paymentConsumer.processPayment(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Stripe Gateway Down");

        verify(paymentRepository).existsByBookingId(103L);
        verify(paymentService).processPayment(event);
    }

    @Test
    void processPayment_multipleEventsForSameBooking_onlyFirstProcessed_subsequentIgnored() {
        BookingCreatedEvent event1 = new BookingCreatedEvent();
        event1.setBookingId(104L);
        event1.setUserId(13L);
        event1.setAmount(new BigDecimal("200.00"));
        event1.setEmail("first@example.com");
        event1.setAuthToken("Bearer tokenFirst");

        BookingCreatedEvent event2 = new BookingCreatedEvent();
        event2.setBookingId(104L); // Same booking ID
        event2.setUserId(13L);
        event2.setAmount(new BigDecimal("200.00"));
        event2.setEmail("second@example.com");
        event2.setAuthToken("Bearer tokenSecond");

        // First event: No duplicate
        when(paymentRepository.existsByBookingId(104L))
                .thenReturn(false)  // First call
                .thenReturn(true);  // Second call (after processing first)

        doNothing().when(paymentService).processPayment(event1);

        // Process first event
        paymentConsumer.processPayment(event1);
        verify(paymentService, times(1)).processPayment(event1);

        // Process second (duplicate) event
        paymentConsumer.processPayment(event2);
        verify(paymentService, times(1)).processPayment(any()); // Still only 1 call

        verify(paymentRepository, times(2)).existsByBookingId(104L);
    }

    @Test
    void processPayment_validEvent_zeroAmount_processedNormally() {
        BookingCreatedEvent event = new BookingCreatedEvent();
        event.setBookingId(105L);
        event.setUserId(14L);
        event.setAmount(BigDecimal.ZERO);
        event.setEmail("zero@example.com");
        event.setAuthToken("Bearer tokenZero");

        when(paymentRepository.existsByBookingId(105L)).thenReturn(false);
        doNothing().when(paymentService).processPayment(event);

        paymentConsumer.processPayment(event);

        verify(paymentRepository).existsByBookingId(105L);
        verify(paymentService).processPayment(event);
    }

    @Test
    void processPayment_nullBookingId_idempotencyCheckStillCalled_serviceInvoked() {
        BookingCreatedEvent event = new BookingCreatedEvent();
        event.setBookingId(null); // Edge case
        event.setUserId(15L);
        event.setAmount(new BigDecimal("300.00"));
        event.setEmail("null@example.com");
        event.setAuthToken("Bearer tokenNull");

        when(paymentRepository.existsByBookingId(null)).thenReturn(false);
        doNothing().when(paymentService).processPayment(event);

        paymentConsumer.processPayment(event);

        verify(paymentRepository).existsByBookingId(null);
        verify(paymentService).processPayment(event);
    }
}
