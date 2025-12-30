package com.ticketblitz.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookingCreatedEvent {
    private Long bookingId;
    private Long userId;
    private BigDecimal amount;
    private String email;
    private String authToken;
}
