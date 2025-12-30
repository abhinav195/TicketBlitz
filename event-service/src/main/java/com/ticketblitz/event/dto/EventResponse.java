package com.ticketblitz.event.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class EventResponse {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime date;
    private String location;
    private BigDecimal price;
    private Integer availableTickets;
    private Integer totalTickets;
    private String category;
    private List<String> imageUrls;
}
