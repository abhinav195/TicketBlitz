package com.ticketblitz.booking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookTicketRequest {
    @NotNull(message = "Event ID is required")
    private Long eventId;

    @NotNull(message = "User ID is required")
    private Long userId;

    @Min(value = 1, message = "Must book at least 1 ticket")
    private int ticketCount;

    // In a real app, price might come from the event service to avoid manipulation,
    // but for this demo we'll accept it or calculate it.
    private double unitPrice;
}
