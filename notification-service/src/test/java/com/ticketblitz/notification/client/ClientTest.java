package com.ticketblitz.notification.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClientTest {

    @Test
    @DisplayName("BookingDto: All getters and setters")
    void bookingDtoTest() {
        BookingClient.BookingDto dto = new BookingClient.BookingDto();
        dto.setId(1L);
        dto.setUserId(100L);
        dto.setEventId(50L);
        dto.setStatus("CONFIRMED");

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getUserId()).isEqualTo(100L);
        assertThat(dto.getEventId()).isEqualTo(50L);
        assertThat(dto.getStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    @DisplayName("UserDto: All getters and setters")
    void userDtoTest() {
        UserClient.UserDto dto = new UserClient.UserDto();
        dto.setId(1L);
        dto.setEmail("test@test.com");
        dto.setUsername("testuser");

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getEmail()).isEqualTo("test@test.com");
        assertThat(dto.getUsername()).isEqualTo("testuser");
    }
}
