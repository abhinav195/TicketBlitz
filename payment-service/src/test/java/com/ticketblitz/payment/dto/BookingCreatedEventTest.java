package com.ticketblitz.payment.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BookingCreatedEventTest {

    @Test
    @DisplayName("BookingCreatedEvent: Should create with no-arg constructor")
    void noArgConstructor() {
        BookingCreatedEvent event = new BookingCreatedEvent();
        assertThat(event).isNotNull();
    }

    @Test
    @DisplayName("BookingCreatedEvent: Should create with all-args constructor")
    void allArgsConstructor() {
        BookingCreatedEvent event = new BookingCreatedEvent(
                101L,
                10L,
                new BigDecimal("250.00"),
                "user@example.com",
                "Bearer token123"
        );

        assertThat(event.getBookingId()).isEqualTo(101L);
        assertThat(event.getUserId()).isEqualTo(10L);
        assertThat(event.getAmount()).isEqualByComparingTo("250.00");
        assertThat(event.getEmail()).isEqualTo("user@example.com");
        assertThat(event.getAuthToken()).isEqualTo("Bearer token123");
    }

    @Test
    @DisplayName("BookingCreatedEvent: Should set and get all fields")
    void gettersAndSetters() {
        BookingCreatedEvent event = new BookingCreatedEvent();

        event.setBookingId(102L);
        event.setUserId(11L);
        event.setAmount(new BigDecimal("100.50"));
        event.setEmail("test@example.com");
        event.setAuthToken("Bearer tokenABC");

        assertThat(event.getBookingId()).isEqualTo(102L);
        assertThat(event.getUserId()).isEqualTo(11L);
        assertThat(event.getAmount()).isEqualByComparingTo("100.50");
        assertThat(event.getEmail()).isEqualTo("test@example.com");
        assertThat(event.getAuthToken()).isEqualTo("Bearer tokenABC");
    }

    @Test
    @DisplayName("BookingCreatedEvent: Should handle null values")
    void nullValues() {
        BookingCreatedEvent event = new BookingCreatedEvent(null, null, null, null, null);

        assertThat(event.getBookingId()).isNull();
        assertThat(event.getUserId()).isNull();
        assertThat(event.getAmount()).isNull();
        assertThat(event.getEmail()).isNull();
        assertThat(event.getAuthToken()).isNull();
    }

    @Test
    @DisplayName("BookingCreatedEvent: Should handle BigDecimal zero")
    void zeroAmount() {
        BookingCreatedEvent event = new BookingCreatedEvent();
        event.setAmount(BigDecimal.ZERO);

        assertThat(event.getAmount()).isEqualByComparingTo("0.00");
    }

    // ==================== EQUALS METHOD TESTS ====================

    @Test
    @DisplayName("Equals: Should return true for same instance")
    void equalsSameInstance() {
        BookingCreatedEvent event = new BookingCreatedEvent(1L, 2L, new BigDecimal("100"), "test@test.com", "token");
        assertThat(event).isEqualTo(event);
    }

    @Test
    @DisplayName("Equals: Should return true for equal objects")
    void equalsEqualObjects() {
        BookingCreatedEvent event1 = new BookingCreatedEvent(1L, 2L, new BigDecimal("100"), "test@test.com", "token");
        BookingCreatedEvent event2 = new BookingCreatedEvent(1L, 2L, new BigDecimal("100"), "test@test.com", "token");
        assertThat(event1).isEqualTo(event2);
    }

    @Test
    @DisplayName("Equals: Should return false for null")
    void equalsNull() {
        BookingCreatedEvent event = new BookingCreatedEvent(1L, 2L, new BigDecimal("100"), "test@test.com", "token");
        assertThat(event).isNotEqualTo(null);
    }

    @Test
    @DisplayName("Equals: Should return false for different class")
    void equalsDifferentClass() {
        BookingCreatedEvent event = new BookingCreatedEvent(1L, 2L, new BigDecimal("100"), "test@test.com", "token");
        assertThat(event).isNotEqualTo("Not a BookingCreatedEvent");
    }

    @Test
    @DisplayName("Equals: Should return false for different bookingId")
    void equalsDifferentBookingId() {
        BookingCreatedEvent event1 = new BookingCreatedEvent(1L, 2L, new BigDecimal("100"), "test@test.com", "token");
        BookingCreatedEvent event2 = new BookingCreatedEvent(999L, 2L, new BigDecimal("100"), "test@test.com", "token");
        assertThat(event1).isNotEqualTo(event2);
    }

    @Test
    @DisplayName("Equals: Should return false for different userId")
    void equalsDifferentUserId() {
        BookingCreatedEvent event1 = new BookingCreatedEvent(1L, 2L, new BigDecimal("100"), "test@test.com", "token");
        BookingCreatedEvent event2 = new BookingCreatedEvent(1L, 999L, new BigDecimal("100"), "test@test.com", "token");
        assertThat(event1).isNotEqualTo(event2);
    }

    @Test
    @DisplayName("Equals: Should return false for different amount")
    void equalsDifferentAmount() {
        BookingCreatedEvent event1 = new BookingCreatedEvent(1L, 2L, new BigDecimal("100"), "test@test.com", "token");
        BookingCreatedEvent event2 = new BookingCreatedEvent(1L, 2L, new BigDecimal("200"), "test@test.com", "token");
        assertThat(event1).isNotEqualTo(event2);
    }

    @Test
    @DisplayName("Equals: Should return false for different email")
    void equalsDifferentEmail() {
        BookingCreatedEvent event1 = new BookingCreatedEvent(1L, 2L, new BigDecimal("100"), "test@test.com", "token");
        BookingCreatedEvent event2 = new BookingCreatedEvent(1L, 2L, new BigDecimal("100"), "other@test.com", "token");
        assertThat(event1).isNotEqualTo(event2);
    }

    @Test
    @DisplayName("Equals: Should return false for different authToken")
    void equalsDifferentAuthToken() {
        BookingCreatedEvent event1 = new BookingCreatedEvent(1L, 2L, new BigDecimal("100"), "test@test.com", "token1");
        BookingCreatedEvent event2 = new BookingCreatedEvent(1L, 2L, new BigDecimal("100"), "test@test.com", "token2");
        assertThat(event1).isNotEqualTo(event2);
    }

    @Test
    @DisplayName("Equals: Should handle null bookingId in both objects")
    void equalsNullBookingIdBoth() {
        BookingCreatedEvent event1 = new BookingCreatedEvent(null, 2L, new BigDecimal("100"), "test@test.com", "token");
        BookingCreatedEvent event2 = new BookingCreatedEvent(null, 2L, new BigDecimal("100"), "test@test.com", "token");
        assertThat(event1).isEqualTo(event2);
    }

    @Test
    @DisplayName("Equals: Should handle null userId in both objects")
    void equalsNullUserIdBoth() {
        BookingCreatedEvent event1 = new BookingCreatedEvent(1L, null, new BigDecimal("100"), "test@test.com", "token");
        BookingCreatedEvent event2 = new BookingCreatedEvent(1L, null, new BigDecimal("100"), "test@test.com", "token");
        assertThat(event1).isEqualTo(event2);
    }

    @Test
    @DisplayName("Equals: Should handle null amount in both objects")
    void equalsNullAmountBoth() {
        BookingCreatedEvent event1 = new BookingCreatedEvent(1L, 2L, null, "test@test.com", "token");
        BookingCreatedEvent event2 = new BookingCreatedEvent(1L, 2L, null, "test@test.com", "token");
        assertThat(event1).isEqualTo(event2);
    }

    @Test
    @DisplayName("Equals: Should handle null email in both objects")
    void equalsNullEmailBoth() {
        BookingCreatedEvent event1 = new BookingCreatedEvent(1L, 2L, new BigDecimal("100"), null, "token");
        BookingCreatedEvent event2 = new BookingCreatedEvent(1L, 2L, new BigDecimal("100"), null, "token");
        assertThat(event1).isEqualTo(event2);
    }

    @Test
    @DisplayName("Equals: Should handle null authToken in both objects")
    void equalsNullAuthTokenBoth() {
        BookingCreatedEvent event1 = new BookingCreatedEvent(1L, 2L, new BigDecimal("100"), "test@test.com", null);
        BookingCreatedEvent event2 = new BookingCreatedEvent(1L, 2L, new BigDecimal("100"), "test@test.com", null);
        assertThat(event1).isEqualTo(event2);
    }

    @Test
    @DisplayName("Equals: Should return false when one bookingId is null")
    void equalsOneNullBookingId() {
        BookingCreatedEvent event1 = new BookingCreatedEvent(1L, 2L, new BigDecimal("100"), "test@test.com", "token");
        BookingCreatedEvent event2 = new BookingCreatedEvent(null, 2L, new BigDecimal("100"), "test@test.com", "token");
        assertThat(event1).isNotEqualTo(event2);
    }

    @Test
    @DisplayName("Equals: Should return false when one userId is null")
    void equalsOneNullUserId() {
        BookingCreatedEvent event1 = new BookingCreatedEvent(1L, 2L, new BigDecimal("100"), "test@test.com", "token");
        BookingCreatedEvent event2 = new BookingCreatedEvent(1L, null, new BigDecimal("100"), "test@test.com", "token");
        assertThat(event1).isNotEqualTo(event2);
    }

    @Test
    @DisplayName("Equals: Should return false when one amount is null")
    void equalsOneNullAmount() {
        BookingCreatedEvent event1 = new BookingCreatedEvent(1L, 2L, new BigDecimal("100"), "test@test.com", "token");
        BookingCreatedEvent event2 = new BookingCreatedEvent(1L, 2L, null, "test@test.com", "token");
        assertThat(event1).isNotEqualTo(event2);
    }

    @Test
    @DisplayName("Equals: Should return false when one email is null")
    void equalsOneNullEmail() {
        BookingCreatedEvent event1 = new BookingCreatedEvent(1L, 2L, new BigDecimal("100"), "test@test.com", "token");
        BookingCreatedEvent event2 = new BookingCreatedEvent(1L, 2L, new BigDecimal("100"), null, "token");
        assertThat(event1).isNotEqualTo(event2);
    }

    @Test
    @DisplayName("Equals: Should return false when one authToken is null")
    void equalsOneNullAuthToken() {
        BookingCreatedEvent event1 = new BookingCreatedEvent(1L, 2L, new BigDecimal("100"), "test@test.com", "token");
        BookingCreatedEvent event2 = new BookingCreatedEvent(1L, 2L, new BigDecimal("100"), "test@test.com", null);
        assertThat(event1).isNotEqualTo(event2);
    }

    // ==================== HASHCODE METHOD TESTS ====================

    @Test
    @DisplayName("HashCode: Should return same hashCode for equal objects")
    void hashCodeEqualObjects() {
        BookingCreatedEvent event1 = new BookingCreatedEvent(1L, 2L, new BigDecimal("100"), "test@test.com", "token");
        BookingCreatedEvent event2 = new BookingCreatedEvent(1L, 2L, new BigDecimal("100"), "test@test.com", "token");
        assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
    }

    @Test
    @DisplayName("HashCode: Should be consistent across multiple calls")
    void hashCodeConsistency() {
        BookingCreatedEvent event = new BookingCreatedEvent(1L, 2L, new BigDecimal("100"), "test@test.com", "token");
        int hash1 = event.hashCode();
        int hash2 = event.hashCode();
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("HashCode: Should handle all null fields")
    void hashCodeAllNullFields() {
        BookingCreatedEvent event1 = new BookingCreatedEvent(null, null, null, null, null);
        BookingCreatedEvent event2 = new BookingCreatedEvent(null, null, null, null, null);
        assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
    }

    @Test
    @DisplayName("HashCode: Should handle partial null fields")
    void hashCodePartialNullFields() {
        BookingCreatedEvent event = new BookingCreatedEvent(1L, null, new BigDecimal("100"), null, "token");
        assertThat(event.hashCode()).isNotZero();
    }

    // ==================== TOSTRING METHOD TESTS ====================

    @Test
    @DisplayName("ToString: Should contain all field values")
    void toStringContainsAllFields() {
        BookingCreatedEvent event = new BookingCreatedEvent(
                101L,
                202L,
                new BigDecimal("99.99"),
                "user@example.com",
                "Bearer abc123"
        );

        String toString = event.toString();
        assertThat(toString).contains("101");
        assertThat(toString).contains("202");
        assertThat(toString).contains("99.99");
        assertThat(toString).contains("user@example.com");
        assertThat(toString).contains("Bearer abc123");
    }

    @Test
    @DisplayName("ToString: Should handle null fields gracefully")
    void toStringWithNullFields() {
        BookingCreatedEvent event = new BookingCreatedEvent(null, null, null, null, null);
        String toString = event.toString();
        assertThat(toString).isNotNull();
        assertThat(toString).contains("null");
    }

    @Test
    @DisplayName("ToString: Should not be null or empty")
    void toStringNotNullOrEmpty() {
        BookingCreatedEvent event = new BookingCreatedEvent();
        assertThat(event.toString()).isNotNull();
        assertThat(event.toString()).isNotEmpty();
    }
}
