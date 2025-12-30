package com.ticketblitz.booking.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentUpdateEvent {
    private Long bookingId;
    private Long userId;
    private String status;
    private String transactionId;
}
