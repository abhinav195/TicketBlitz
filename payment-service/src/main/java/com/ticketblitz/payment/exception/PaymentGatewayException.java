package com.ticketblitz.payment.exception; // Adjust package if needed

public class PaymentGatewayException extends RuntimeException {
    public PaymentGatewayException(String message) {
        super(message);
    }
}
