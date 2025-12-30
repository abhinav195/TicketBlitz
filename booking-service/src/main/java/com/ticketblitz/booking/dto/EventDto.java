package com.ticketblitz.booking.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class EventDto {
    private Long id;
    private String name;
    private BigDecimal price; // Crucial field
    private Integer availableSeats;
    private LocalDateTime eventDate;
}
