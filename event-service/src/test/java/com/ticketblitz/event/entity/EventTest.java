package com.ticketblitz.event.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EventTest {

    @Test
    @DisplayName("Event: Builder should create instance with all fields")
    void event_Builder() {
        Category category = new Category(1L, "Music", "Live concerts");
        LocalDateTime now = LocalDateTime.now();
        List<String> images = new ArrayList<>(List.of("img1.jpg"));

        Event event = Event.builder()
                .id(1L)
                .title("Concert")
                .description("Great show")
                .date(now)
                .location("Stadium")
                .category(category)
                .price(new BigDecimal("100"))
                .totalTickets(500)
                .availableTickets(500)
                .imageUrls(images)
                .version(0L)
                .build();

        assertThat(event.getId()).isEqualTo(1L);
        assertThat(event.getTitle()).isEqualTo("Concert");
        assertThat(event.getCategory()).isEqualTo(category);
        assertThat(event.getImageUrls()).hasSize(1);
    }

    @Test
    @DisplayName("Event: NoArgsConstructor and setters")
    void event_NoArgsConstructor() {
        Event event = new Event();
        event.setId(2L);
        event.setTitle("Test Event");

        assertThat(event.getId()).isEqualTo(2L);
        assertThat(event.getTitle()).isEqualTo("Test Event");
    }

    @Test
    @DisplayName("Event: AllArgsConstructor")
    void event_AllArgsConstructor() {
        Category category = new Category(1L, "Sports", "Matches");
        LocalDateTime date = LocalDateTime.now();

        Event event = new Event(1L, "Match", "Description", date, "Stadium",
                category, new BigDecimal("50"), 1000, 1000,
                new ArrayList<>(), null, 0L);

        assertThat(event.getId()).isEqualTo(1L);
        assertThat(event.getTotalTickets()).isEqualTo(1000);
    }
}
