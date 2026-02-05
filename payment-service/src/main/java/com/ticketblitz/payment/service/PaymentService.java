package com.ticketblitz.payment.service;

import com.ticketblitz.payment.dto.BookingCreatedEvent;
import com.ticketblitz.payment.entity.Payment;
import com.ticketblitz.payment.event.PaymentProducer;
import com.ticketblitz.payment.exception.PaymentGatewayException;
import com.ticketblitz.payment.repository.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.model.Charge;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentProducer paymentProducer;

    @Value("${stripe.api-key}")
    private String stripeApiKey;

    // FIXED: Inject currency from application.yml (defaults to 'inr' if missing)
    @Value("${stripe.currency:inr}")
    private String currency;

    @CircuitBreaker(name = "stripeService", fallbackMethod = "stripeFallback")
    public void processPayment(BookingCreatedEvent event) {
        Stripe.apiKey = stripeApiKey;

        try {
            Map<String, Object> chargeParams = new HashMap<>();
            // Stripe expects amount in the smallest currency unit (e.g., cents for USD, paise for INR)
            chargeParams.put("amount", event.getAmount().multiply(BigDecimal.valueOf(100)).intValue());

            // FIXED: Use the configured currency instead of hardcoded "usd"
            chargeParams.put("currency", currency);

            chargeParams.put("source", "tok_visa"); // Test token
            chargeParams.put("description", "Booking ID: " + event.getBookingId());

            // External Call to Stripe
            Charge charge = Charge.create(chargeParams);

            log.info("Payment Successful. Stripe ID: {}", charge.getId());
            savePayment(event, "SUCCESS", charge.getId());

            // Send Success Event to Kafka
            paymentProducer.sendPaymentResult(
                    event.getBookingId(),
                    "SUCCESS",
                    charge.getId(),
                    event.getUserId(),
                    event.getAuthToken()
            );

        } catch (Exception e) {
            log.error("Stripe Charge Failed", e);
            throw new RuntimeException("Payment Gateway Failure");
        }
    }

    // FALLBACK METHOD
    public void stripeFallback(BookingCreatedEvent event, Throwable t) {
        log.error("🛑 CIRCUIT OPEN or Stripe Down. Executing Fallback for Booking: {}", event.getBookingId());
        savePayment(event, "FAILED", "FALLBACK_ERROR");

        // Send Failed Event so Booking Service knows to cancel
        paymentProducer.sendPaymentResult(
                event.getBookingId(),
                "FAILED",
                "FALLBACK_ERROR",
                event.getUserId(),
                event.getAuthToken()
        );

        // Throwing specific exception to trigger Saga Compensation
        throw new PaymentGatewayException("Payment Service Unavailable. Please try again later.");
    }

    private void savePayment(BookingCreatedEvent event, String status, String txId) {
        Payment payment = new Payment();
        payment.setBookingId(event.getBookingId());
        payment.setUserId(event.getUserId());
        payment.setAmount(event.getAmount());
        payment.setStatus(status);
        payment.setStripePaymentId(txId);
        paymentRepository.save(payment);
    }
}
