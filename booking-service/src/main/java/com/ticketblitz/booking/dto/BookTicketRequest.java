package com.ticketblitz.booking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookTicketRequest {
    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Event ID is required")
    private Long eventId;

    @Min(value = 1, message = "At least 1 ticket must be booked")
    private int ticketCount;
}
