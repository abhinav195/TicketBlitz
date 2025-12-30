package com.ticketblitz.notification.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentUpdateEvent {
    private Long bookingId;
    private String status;
    private String transactionId;
    private Long userId;
    private String authToken;
}
