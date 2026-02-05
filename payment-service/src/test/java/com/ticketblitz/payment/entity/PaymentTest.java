package com.ticketblitz.payment.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentTest {

    @Test
    @DisplayName("Payment: Should create entity and set all fields")
    void createPaymentEntity() {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setBookingId(101L);
        payment.setUserId(10L);
        payment.setAmount(new BigDecimal("250.00"));
        payment.setStatus("SUCCESS");
        payment.setStripePaymentId("ch_test_12345");

        assertThat(payment.getId()).isEqualTo(1L);
        assertThat(payment.getBookingId()).isEqualTo(101L);
        assertThat(payment.getUserId()).isEqualTo(10L);
        assertThat(payment.getAmount()).isEqualByComparingTo("250.00");
        assertThat(payment.getStatus()).isEqualTo("SUCCESS");
        assertThat(payment.getStripePaymentId()).isEqualTo("ch_test_12345");
    }

    @Test
    @DisplayName("Payment: @PrePersist should set createdAt")
    void prePersistShouldSetCreatedAt() {
        Payment payment = new Payment();

        LocalDateTime before = LocalDateTime.now();
        payment.onCreate();
        LocalDateTime after = LocalDateTime.now();

        assertThat(payment.getCreatedAt()).isNotNull();
        assertThat(payment.getCreatedAt()).isAfterOrEqualTo(before);
        assertThat(payment.getCreatedAt()).isBeforeOrEqualTo(after);
    }

    @Test
    @DisplayName("Payment: Should handle FAILED status")
    void failedPaymentStatus() {
        Payment payment = new Payment();
        payment.setStatus("FAILED");
        payment.setStripePaymentId("FALLBACK_ERROR");

        assertThat(payment.getStatus()).isEqualTo("FAILED");
        assertThat(payment.getStripePaymentId()).isEqualTo("FALLBACK_ERROR");
    }

    @Test
    @DisplayName("Payment: Should handle zero amount")
    void zeroAmount() {
        Payment payment = new Payment();
        payment.setAmount(BigDecimal.ZERO);

        assertThat(payment.getAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Payment: Should handle large amounts")
    void largeAmount() {
        Payment payment = new Payment();
        payment.setAmount(new BigDecimal("999999.99"));

        assertThat(payment.getAmount()).isEqualByComparingTo("999999.99");
    }
}
