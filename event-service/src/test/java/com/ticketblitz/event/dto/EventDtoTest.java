package com.ticketblitz.event.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EventDtoTest {

    @Test
    @DisplayName("EventDto: Record should create instance with all fields")
    void eventDto_AllFields() {
        LocalDateTime now = LocalDateTime.now();
        EventDto dto = new EventDto(1L, "Title", "Desc", now, "Location",
                "Music", new BigDecimal("100"), 500, 500, List.of("url1"));

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.title()).isEqualTo("Title");
        assertThat(dto.category()).isEqualTo("Music");
    }

    @Test
    @DisplayName("EventDto: Equals - same object")
    void eventDto_EqualsSameObject() {
        EventDto dto = new EventDto(1L, "Title", "Desc", LocalDateTime.now(), "Loc",
                "Cat", BigDecimal.TEN, 10, 10, null);
        assertThat(dto.equals(dto)).isTrue();
    }

    @Test
    @DisplayName("EventDto: Equals - null")
    void eventDto_EqualsNull() {
        EventDto dto = new EventDto(1L, "Title", "Desc", LocalDateTime.now(), "Loc",
                "Cat", BigDecimal.TEN, 10, 10, null);
        assertThat(dto.equals(null)).isFalse();
    }

    @Test
    @DisplayName("EventDto: Equals - different class")
    void eventDto_EqualsDifferentClass() {
        EventDto dto = new EventDto(1L, "Title", "Desc", LocalDateTime.now(), "Loc",
                "Cat", BigDecimal.TEN, 10, 10, null);
        assertThat(dto.equals(new Object())).isFalse();
    }

    @Test
    @DisplayName("EventDto: Equals - equal objects")
    void eventDto_EqualsEqualObjects() {
        LocalDateTime now = LocalDateTime.now();
        EventDto dto1 = new EventDto(1L, "Title", "Desc", now, "Loc", "Cat",
                BigDecimal.TEN, 10, 10, List.of("url"));
        EventDto dto2 = new EventDto(1L, "Title", "Desc", now, "Loc", "Cat",
                BigDecimal.TEN, 10, 10, List.of("url"));

        assertThat(dto1.equals(dto2)).isTrue();
        assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());
    }

    @Test
    @DisplayName("EventDto: Equals - different id")
    void eventDto_DifferentId() {
        LocalDateTime now = LocalDateTime.now();
        EventDto dto1 = new EventDto(1L, "Title", "Desc", now, "Loc", "Cat",
                BigDecimal.TEN, 10, 10, null);
        EventDto dto2 = new EventDto(2L, "Title", "Desc", now, "Loc", "Cat",
                BigDecimal.TEN, 10, 10, null);
        assertThat(dto1.equals(dto2)).isFalse();
    }

    @Test
    @DisplayName("EventDto: ToString contains class name")
    void eventDto_ToString() {
        EventDto dto = new EventDto(1L, "Title", "Desc", LocalDateTime.now(), "Loc",
                "Cat", BigDecimal.TEN, 10, 10, null);
        assertThat(dto.toString()).contains("EventDto");
    }
}
