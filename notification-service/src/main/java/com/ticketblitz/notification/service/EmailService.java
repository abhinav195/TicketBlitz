package com.ticketblitz.notification.service;

import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * Resilient Email Service with:
 * - Automatic Retry (3 attempts with exponential backoff)
 * - Graceful Degradation (@Recover method)
 * - Distributed Tracing (@Observed)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * Send email with automatic retry on SMTP failures.
     *
     * Retry Configuration:
     * - Max Attempts: 3
     * - Backoff: 1s, 2s, 4s (exponential)
     * - Recoverable Exceptions: MailException (SMTP failures)
     *
     * If all retries fail, the @Recover method is invoked.
     */
    @Retryable(
            retryFor = {MailException.class},  // Retry on SMTP errors
            maxAttempts = 3,
            backoff = @Backoff(
                    delay = 1000,      // Initial delay: 1 second
                    multiplier = 2.0   // Exponential: 1s, 2s, 4s
            )
    )
    @Observed(name = "email.send", contextualName = "send-email")
    public void sendEmail(String to, String subject, String body) {
        log.info("📧 [ATTEMPT] Sending email to: {}", to);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);

            log.info("✅ [SUCCESS] Email sent to: {}", to);
        } catch (MailException e) {
            log.error("❌ [SMTP ERROR] Failed to send email to: {}. Error: {}", to, e.getMessage());
            throw e;  // Rethrow to trigger retry
        }
    }

    /**
     * Recover method: Invoked when all retry attempts fail.
     *
     * This prevents the Kafka consumer from crashing and allows it to continue processing.
     * The failed email is logged as a DEAD_LETTER for manual intervention.
     *
     * @param e The exception that caused all retries to fail
     * @param to Recipient email
     * @param subject Email subject
     * @param body Email body
     */
    @Recover
    public void recoverFromEmailFailure(MailException e, String to, String subject, String body) {
        log.error("🛑 [DEAD_LETTER] All retry attempts exhausted for email to: {}. Subject: '{}'. Error: {}",
                to, subject, e.getMessage());

        // ========== DEAD LETTER HANDLING ==========
        // In production, you would:
        // 1. Send to a Dead Letter Queue (DLQ)
        // 2. Store in a database for manual retry
        // 3. Send alert to ops team

        log.warn("📝 [ACTION REQUIRED] Manual intervention needed for failed email.");

        // Example: Send to Dead Letter Queue
        // deadLetterQueue.send(new FailedEmail(to, subject, body, e.getMessage()));
    }
}
