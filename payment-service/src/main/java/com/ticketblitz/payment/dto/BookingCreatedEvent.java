package com.ticketblitz.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookingCreatedEvent {
    private Long bookingId; // Changed from UUID to Long to match Booking Service
    private Long userId;
    private BigDecimal amount;
    private String email;
    private String authToken;
}
