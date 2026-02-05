package com.ticketblitz.booking.dto;

import com.ticketblitz.booking.entity.BookingStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class BookingDtoTest {

    // ========================================================================
    // BookTicketRequest - COMPREHENSIVE BRANCH COVERAGE
    // ========================================================================

    @Test
    @DisplayName("BookTicketRequest - All Getters and Setters")
    void bookTicketRequest_gettersSetters() {
        BookTicketRequest request = new BookTicketRequest();
        request.setUserId(10L);
        request.setEventId(99L);
        request.setTicketCount(5);

        assertThat(request.getUserId()).isEqualTo(10L);
        assertThat(request.getEventId()).isEqualTo(99L);
        assertThat(request.getTicketCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("BookTicketRequest - Equals: Same Object")
    void bookTicketRequest_equalsSameObject() {
        BookTicketRequest request = new BookTicketRequest();
        request.setUserId(10L);
        assertThat(request.equals(request)).isTrue();
    }

    @Test
    @DisplayName("BookTicketRequest - Equals: Null")
    void bookTicketRequest_equalsNull() {
        BookTicketRequest request = new BookTicketRequest();
        assertThat(request.equals(null)).isFalse();
    }

    @Test
    @DisplayName("BookTicketRequest - Equals: Different Class")
    void bookTicketRequest_equalsDifferentClass() {
        BookTicketRequest request = new BookTicketRequest();
        assertThat(request.equals(new Object())).isFalse();
        assertThat(request.equals("string")).isFalse();
    }

    @Test
    @DisplayName("BookTicketRequest - Equals: Equal Objects")
    void bookTicketRequest_equalsEqualObjects() {
        BookTicketRequest r1 = new BookTicketRequest();
        r1.setUserId(10L);
        r1.setEventId(99L);
        r1.setTicketCount(3);

        BookTicketRequest r2 = new BookTicketRequest();
        r2.setUserId(10L);
        r2.setEventId(99L);
        r2.setTicketCount(3);

        assertThat(r1.equals(r2)).isTrue();
        assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
    }

    @Test
    @DisplayName("BookTicketRequest - Equals: Different UserId")
    void bookTicketRequest_differentUserId() {
        BookTicketRequest r1 = new BookTicketRequest();
        r1.setUserId(10L);

        BookTicketRequest r2 = new BookTicketRequest();
        r2.setUserId(20L);

        assertThat(r1.equals(r2)).isFalse();
    }

    @Test
    @DisplayName("BookTicketRequest - Equals: Different EventId")
    void bookTicketRequest_differentEventId() {
        BookTicketRequest r1 = new BookTicketRequest();
        r1.setEventId(99L);

        BookTicketRequest r2 = new BookTicketRequest();
        r2.setEventId(100L);

        assertThat(r1.equals(r2)).isFalse();
    }

    @Test
    @DisplayName("BookTicketRequest - Equals: Different TicketCount")
    void bookTicketRequest_differentTicketCount() {
        BookTicketRequest r1 = new BookTicketRequest();
        r1.setTicketCount(3);

        BookTicketRequest r2 = new BookTicketRequest();
        r2.setTicketCount(5);

        assertThat(r1.equals(r2)).isFalse();
    }

    @Test
    @DisplayName("BookTicketRequest - CanEqual")
    void bookTicketRequest_canEqual() {
        BookTicketRequest r1 = new BookTicketRequest();
        BookTicketRequest r2 = new BookTicketRequest();

        assertThat(r1.canEqual(r2)).isTrue();
        assertThat(r1.canEqual(new Object())).isFalse();
    }

    @Test
    @DisplayName("BookTicketRequest - ToString")
    void bookTicketRequest_toString() {
        BookTicketRequest request = new BookTicketRequest();
        request.setUserId(10L);
        String result = request.toString();
        assertThat(result).contains("BookTicketRequest");
    }

    // ========================================================================
    // EventDto - COMPREHENSIVE BRANCH COVERAGE
    // ========================================================================

    @Test
    @DisplayName("EventDto - All Fields")
    void eventDto_allFields() {
        EventDto dto = new EventDto();
        LocalDateTime now = LocalDateTime.now();

        dto.setId(99L);
        dto.setName("Concert");
        dto.setPrice(new BigDecimal("250.00"));
        dto.setAvailableSeats(100);
        dto.setEventDate(now);

        assertThat(dto.getId()).isEqualTo(99L);
        assertThat(dto.getName()).isEqualTo("Concert");
        assertThat(dto.getPrice()).isEqualByComparingTo("250.00");
        assertThat(dto.getAvailableSeats()).isEqualTo(100);
        assertThat(dto.getEventDate()).isEqualTo(now);
    }

    @Test
    @DisplayName("EventDto - Equals: All Branches")
    void eventDto_equalsAllBranches() {
        LocalDateTime now = LocalDateTime.now();

        EventDto dto1 = new EventDto();
        dto1.setId(99L);
        dto1.setName("Concert");
        dto1.setPrice(new BigDecimal("250"));
        dto1.setAvailableSeats(100);
        dto1.setEventDate(now);

        // Same object
        assertThat(dto1.equals(dto1)).isTrue();

        // Null
        assertThat(dto1.equals(null)).isFalse();

        // Different class
        assertThat(dto1.equals(new Object())).isFalse();

        // Equal object
        EventDto dto2 = new EventDto();
        dto2.setId(99L);
        dto2.setName("Concert");
        dto2.setPrice(new BigDecimal("250"));
        dto2.setAvailableSeats(100);
        dto2.setEventDate(now);
        assertThat(dto1.equals(dto2)).isTrue();
        assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());

        // Different id
        EventDto dto3 = new EventDto();
        dto3.setId(100L);
        assertThat(dto1.equals(dto3)).isFalse();

        // Different name
        EventDto dto4 = new EventDto();
        dto4.setId(99L);
        dto4.setName("Theater");
        assertThat(dto1.equals(dto4)).isFalse();

        // Different price
        EventDto dto5 = new EventDto();
        dto5.setId(99L);
        dto5.setName("Concert");
        dto5.setPrice(new BigDecimal("300"));
        assertThat(dto1.equals(dto5)).isFalse();

        // Different availableSeats
        EventDto dto6 = new EventDto();
        dto6.setId(99L);
        dto6.setName("Concert");
        dto6.setPrice(new BigDecimal("250"));
        dto6.setAvailableSeats(200);
        assertThat(dto1.equals(dto6)).isFalse();

        // Different eventDate
        EventDto dto7 = new EventDto();
        dto7.setId(99L);
        dto7.setName("Concert");
        dto7.setPrice(new BigDecimal("250"));
        dto7.setAvailableSeats(100);
        dto7.setEventDate(now.plusDays(1));
        assertThat(dto1.equals(dto7)).isFalse();
    }

    @Test
    @DisplayName("EventDto - CanEqual")
    void eventDto_canEqual() {
        EventDto dto = new EventDto();
        assertThat(dto.canEqual(new EventDto())).isTrue();
        assertThat(dto.canEqual(new Object())).isFalse();
    }

    @Test
    @DisplayName("EventDto - ToString")
    void eventDto_toString() {
        EventDto dto = new EventDto();
        dto.setId(99L);
        assertThat(dto.toString()).contains("EventDto");
    }

    // ========================================================================
    // PaymentUpdateEvent - COMPREHENSIVE BRANCH COVERAGE
    // ========================================================================

    @Test
    @DisplayName("PaymentUpdateEvent - Constructors and Getters")
    void paymentUpdateEvent_constructors() {
        PaymentUpdateEvent event1 = new PaymentUpdateEvent(123L, 10L, "SUCCESS", "TXN1", "token");
        assertThat(event1.getBookingId()).isEqualTo(123L);

        PaymentUpdateEvent event2 = new PaymentUpdateEvent();
        event2.setBookingId(456L);
        assertThat(event2.getBookingId()).isEqualTo(456L);
    }

    @Test
    @DisplayName("PaymentUpdateEvent - Equals: All Branches")
    void paymentUpdateEvent_equalsAllBranches() {
        PaymentUpdateEvent e1 = new PaymentUpdateEvent(123L, 10L, "SUCCESS", "TXN1", "token");

        // Same object
        assertThat(e1.equals(e1)).isTrue();

        // Null
        assertThat(e1.equals(null)).isFalse();

        // Different class
        assertThat(e1.equals(new Object())).isFalse();

        // Equal
        PaymentUpdateEvent e2 = new PaymentUpdateEvent(123L, 10L, "SUCCESS", "TXN1", "token");
        assertThat(e1.equals(e2)).isTrue();
        assertThat(e1.hashCode()).isEqualTo(e2.hashCode());

        // Different bookingId
        assertThat(e1.equals(new PaymentUpdateEvent(456L, 10L, "SUCCESS", "TXN1", "token"))).isFalse();

        // Different userId
        assertThat(e1.equals(new PaymentUpdateEvent(123L, 20L, "SUCCESS", "TXN1", "token"))).isFalse();

        // Different status
        assertThat(e1.equals(new PaymentUpdateEvent(123L, 10L, "FAILED", "TXN1", "token"))).isFalse();

        // Different transactionId
        assertThat(e1.equals(new PaymentUpdateEvent(123L, 10L, "SUCCESS", "TXN2", "token"))).isFalse();

        // Different authToken
        assertThat(e1.equals(new PaymentUpdateEvent(123L, 10L, "SUCCESS", "TXN1", "token2"))).isFalse();
    }

    @Test
    @DisplayName("PaymentUpdateEvent - CanEqual")
    void paymentUpdateEvent_canEqual() {
        PaymentUpdateEvent event = new PaymentUpdateEvent();
        assertThat(event.canEqual(new PaymentUpdateEvent())).isTrue();
        assertThat(event.canEqual(new Object())).isFalse();
    }

    @Test
    @DisplayName("PaymentUpdateEvent - ToString")
    void paymentUpdateEvent_toString() {
        PaymentUpdateEvent event = new PaymentUpdateEvent(123L, 10L, "SUCCESS", "TXN1", "token");
        assertThat(event.toString()).contains("PaymentUpdateEvent");
    }

    // ========================================================================
    // BookingResponse - COMPREHENSIVE BRANCH COVERAGE
    // ========================================================================

    @Test
    @DisplayName("BookingResponse - All Fields")
    void bookingResponse_allFields() {
        BookingResponse response = new BookingResponse();
        response.setBookingId(123L);
        response.setEventId(99L);
        response.setUserId(10L);
        response.setTicketCount(3);
        response.setStatus(BookingStatus.CONFIRMED);
        response.setTotalPrice(new BigDecimal("750"));

        assertThat(response.getBookingId()).isEqualTo(123L);
        assertThat(response.getEventId()).isEqualTo(99L);
        assertThat(response.getUserId()).isEqualTo(10L);
        assertThat(response.getTicketCount()).isEqualTo(3);
        assertThat(response.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(response.getTotalPrice()).isEqualByComparingTo("750");
    }

    @Test
    @DisplayName("BookingResponse - Equals: All Branches")
    void bookingResponse_equalsAllBranches() {
        BookingResponse r1 = new BookingResponse();
        r1.setBookingId(123L);
        r1.setStatus(BookingStatus.CONFIRMED);

        // Same, null, different class
        assertThat(r1.equals(r1)).isTrue();
        assertThat(r1.equals(null)).isFalse();
        assertThat(r1.equals(new Object())).isFalse();

        // Equal
        BookingResponse r2 = new BookingResponse();
        r2.setBookingId(123L);
        r2.setStatus(BookingStatus.CONFIRMED);
        assertThat(r1.equals(r2)).isTrue();
        assertThat(r1.hashCode()).isEqualTo(r2.hashCode());

        // Different
        BookingResponse r3 = new BookingResponse();
        r3.setBookingId(456L);
        assertThat(r1.equals(r3)).isFalse();
    }

    @Test
    @DisplayName("BookingResponse - CanEqual and ToString")
    void bookingResponse_canEqualToString() {
        BookingResponse response = new BookingResponse();
        assertThat(response.canEqual(new BookingResponse())).isTrue();
        assertThat(response.canEqual(new Object())).isFalse();
        assertThat(response.toString()).contains("BookingResponse");
    }

    // ========================================================================
    // BookingCreatedEvent - COMPREHENSIVE BRANCH COVERAGE
    // ========================================================================

    @Test
    @DisplayName("BookingCreatedEvent - Constructors")
    void bookingCreatedEvent_constructors() {
        BookingCreatedEvent event1 = new BookingCreatedEvent(123L, 10L, new BigDecimal("500"), "email@test.com", "token");
        assertThat(event1.getBookingId()).isEqualTo(123L);

        BookingCreatedEvent event2 = new BookingCreatedEvent();
        event2.setBookingId(456L);
        assertThat(event2.getBookingId()).isEqualTo(456L);
    }

    @Test
    @DisplayName("BookingCreatedEvent - Equals: All Branches")
    void bookingCreatedEvent_equalsAllBranches() {
        BookingCreatedEvent e1 = new BookingCreatedEvent(123L, 10L, new BigDecimal("500"), "email", "token");

        assertThat(e1.equals(e1)).isTrue();
        assertThat(e1.equals(null)).isFalse();
        assertThat(e1.equals(new Object())).isFalse();

        BookingCreatedEvent e2 = new BookingCreatedEvent(123L, 10L, new BigDecimal("500"), "email", "token");
        assertThat(e1.equals(e2)).isTrue();
        assertThat(e1.hashCode()).isEqualTo(e2.hashCode());

        BookingCreatedEvent e3 = new BookingCreatedEvent(456L, 10L, new BigDecimal("500"), "email", "token");
        assertThat(e1.equals(e3)).isFalse();
    }

    @Test
    @DisplayName("BookingCreatedEvent - CanEqual and ToString")
    void bookingCreatedEvent_canEqualToString() {
        BookingCreatedEvent event = new BookingCreatedEvent();
        assertThat(event.canEqual(new BookingCreatedEvent())).isTrue();
        assertThat(event.toString()).contains("BookingCreatedEvent");
    }
}
