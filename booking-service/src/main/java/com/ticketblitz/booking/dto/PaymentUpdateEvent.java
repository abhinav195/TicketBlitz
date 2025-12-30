package com.ticketblitz.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentUpdateEvent {
    private Long bookingId;
    private String status; // "SUCCESS" or "FAILED"
    private String transactionId; // or stripePaymentId
    private Long userId;
    private String authToken;
    private BigDecimal amount;
    private String userEmail;
}
