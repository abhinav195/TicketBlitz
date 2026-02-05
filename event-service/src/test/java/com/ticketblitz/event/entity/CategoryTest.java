package com.ticketblitz.event.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryTest {

    @Test
    @DisplayName("Category: Builder should create instance")
    void category_Builder() {
        Category category = Category.builder()
                .id(1L)
                .name("Music")
                .description("Live concerts")
                .build();

        assertThat(category.getId()).isEqualTo(1L);
        assertThat(category.getName()).isEqualTo("Music");
    }

    @Test
    @DisplayName("Category: NoArgsConstructor and setters")
    void category_NoArgsConstructor() {
        Category category = new Category();
        category.setId(2L);
        category.setName("Sports");

        assertThat(category.getId()).isEqualTo(2L);
        assertThat(category.getName()).isEqualTo("Sports");
    }

    @Test
    @DisplayName("Category: AllArgsConstructor")
    void category_AllArgsConstructor() {
        Category category = new Category(1L, "Technology", "Tech events");

        assertThat(category.getId()).isEqualTo(1L);
        assertThat(category.getDescription()).isEqualTo("Tech events");
    }
}
