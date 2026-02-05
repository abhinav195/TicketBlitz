package com.ticketblitz.booking.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true) // FIXED: Robustness against schema changes
public class EventDto {
    private Long id;

    @JsonProperty("title") // FIXED: Maps JSON 'title' from EventService to 'name'
    private String name;

    private BigDecimal price;

    @JsonProperty("availableTickets") // FIXED: Maps JSON 'availableTickets' to 'availableSeats'
    private Integer availableSeats;

    @JsonProperty("date") // FIXED: Maps JSON 'date' to 'eventDate'
    private LocalDateTime eventDate;
}
