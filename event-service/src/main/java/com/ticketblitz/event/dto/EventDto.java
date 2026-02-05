package com.ticketblitz.event.dto;

import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record EventDto(
        Long id,

        @NotBlank(message = "Title is required")
        @Size(min = 3, max = 255, message = "Title must be between 3 and 255 characters")
        String title,

        @NotBlank(message = "Description is required")
        @Size(max = 2000, message = "Description cannot exceed 2000 characters")
        String description,

        @NotNull(message = "Event date is required")
        @Future(message = "Event date must be in the future")
        LocalDateTime date,

        @NotBlank(message = "Location is required")
        String location,

        @NotBlank(message = "Category is required")
        String category,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
        BigDecimal price,

        @NotNull(message = "Total tickets is required")
        @Min(value = 1, message = "Total tickets must be at least 1")
        Integer totalTickets,

        Integer availableTickets, // No validation needed (system-controlled)

        List<String> imageUrls // Optional
) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
}
