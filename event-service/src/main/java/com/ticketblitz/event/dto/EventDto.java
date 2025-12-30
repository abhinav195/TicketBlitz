package com.ticketblitz.event.dto;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// We use this for both Creating (metadata only) and Reading
public record EventDto(
        Long id,
        String title,
        String description,
        LocalDateTime date,
        String location,
        String category,
        BigDecimal price,
        Integer totalTickets,
        Integer availableTickets,
        List<String> imageUrls
) implements Serializable {}
