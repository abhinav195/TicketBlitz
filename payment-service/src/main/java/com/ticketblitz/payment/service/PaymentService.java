package com.ticketblitz.payment.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.ticketblitz.payment.dto.BookingCreatedEvent;
import com.ticketblitz.payment.entity.Payment;
import com.ticketblitz.payment.event.PaymentProducer;
import com.ticketblitz.payment.repository.PaymentRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentProducer paymentProducer;

    @Value("${stripe.api-key}")
    private String stripeApiKey;

    @Value("${stripe.currency}")
    private String currency; // "inr"

    @PostConstruct
    public void init() {
        if (stripeApiKey == null || stripeApiKey.startsWith("sk_test_YOUR_KEY")) {
            log.warn("⚠️ STRIPE API KEY NOT SET! Please check application.yml");
        } else {
            Stripe.apiKey = stripeApiKey;
            log.info("✅ Stripe Initialized with Currency: {}", currency.toUpperCase());
        }
    }

    public void processPayment(BookingCreatedEvent event) {
        log.info("Processing payment for Booking ID: {} | Amount: ₹{}", event.getBookingId(), event.getAmount());

        String status = "FAILED";
        String transactionId = null;

        try {
            // 1. Validation (Example: Logic limit for testing, can be removed for prod)
            if (event.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                log.error("❌ Invalid Amount: ₹{}. Aborting payment.", event.getAmount());
                return; // Don't even reply if data is garbage
            }

            // 2. Conversion: INR -> Paise
            long amountInPaise = event.getAmount().multiply(BigDecimal.valueOf(100)).longValue();

            // 3. Build Stripe Request
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInPaise)
                    .setCurrency(currency)
                    .setDescription("TicketBlitz Booking: " + event.getBookingId())
                    .setPaymentMethod("pm_card_visa") // Test Card (Always valid)
                    .setConfirm(true) // Charge immediately
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                                    .build()
                    )
                    .build();

            // 4. Execute Charge
            log.info("🚀 Calling Stripe API...");
            PaymentIntent paymentIntent = PaymentIntent.create(params);

            if ("succeeded".equals(paymentIntent.getStatus())) {
                status = "SUCCESS";
                transactionId = paymentIntent.getId();
                log.info("✅ STRIPE SUCCESS | ID: {}", transactionId);
            } else {
                status = "FAILED";
                transactionId = paymentIntent.getId();
                log.warn("⚠️ STRIPE FAILED/PENDING | Status: {}", paymentIntent.getStatus());
            }

        } catch (StripeException e) {
            log.error("❌ STRIPE EXCEPTION | Code: {} | Message: {}", e.getCode(), e.getMessage());
            status = "FAILED";
            transactionId = "STRIPE_ERROR_" + e.getCode();
        } catch (Exception e) {
            log.error("❌ UNEXPECTED ERROR during payment processing", e);
            status = "FAILED";
            transactionId = "INTERNAL_ERROR";
        }

        // 5. Save to DB
        savePaymentRecord(event, status, transactionId);

        // 6. Send Saga Reply
        sendSagaReply(event, status, transactionId);
    }

    private void savePaymentRecord(BookingCreatedEvent event, String status, String transactionId) {
        try {
            Payment payment = new Payment();
            payment.setBookingId(event.getBookingId());
            payment.setUserId(event.getUserId());
            payment.setAmount(event.getAmount());
            payment.setStatus(status);
            payment.setStripePaymentId(transactionId);
            paymentRepository.save(payment);
        } catch (Exception e) {
            log.error("Failed to save Payment record to DB", e);
        }
    }

    private void sendSagaReply(BookingCreatedEvent event, String status, String transactionId) {
        paymentProducer.sendPaymentResult(
                event.getBookingId(),
                status,
                transactionId,
                event.getUserId(),
                event.getAuthToken()
        );
    }
}
