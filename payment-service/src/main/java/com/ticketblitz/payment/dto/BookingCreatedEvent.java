package com.ticketblitz.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // FIXED: Ensures forward compatibility
public class BookingCreatedEvent {
    private Long bookingId;
    private Long userId;
    private BigDecimal amount;
    private String email;
    private String authToken;
}
