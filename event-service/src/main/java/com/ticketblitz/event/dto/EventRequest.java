package com.ticketblitz.event.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EventRequest {
    private String title;
    private String description;
    private LocalDateTime date;
    private String location;
    private Double price;
    private Integer totalTickets;
    private String category;
}
