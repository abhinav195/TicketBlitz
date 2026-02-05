package com.ticketblitz.booking.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // FIXED: Robustness
public class PaymentUpdateEvent {
    private Long bookingId;
    private Long userId;
    private String status; // "SUCCESS" or "FAILED"
    private String transactionId;
    private String authToken;
}
