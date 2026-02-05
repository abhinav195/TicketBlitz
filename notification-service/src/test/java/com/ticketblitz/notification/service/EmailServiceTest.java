package com.ticketblitz.notification.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    @DisplayName("sendEmail: Should successfully send email")
    void sendEmailSuccess() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> emailService.sendEmail("test@test.com", "Subject", "Body"));

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("sendEmail: Should throw MailException to trigger retry")
    void sendEmailThrowsMailException() {
        doThrow(new MailSendException("SMTP unavailable")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> emailService.sendEmail("fail@test.com", "Subject", "Body"))
                .isInstanceOf(MailException.class);

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("recoverFromEmailFailure: Should handle recovery gracefully")
    void recoverFromEmailFailure() {
        MailException exception = new MailSendException("All retries exhausted");

        assertDoesNotThrow(() -> emailService.recoverFromEmailFailure(exception, "test@test.com", "Subject", "Body"));
    }

    @Test
    @DisplayName("EmailService: Constructor coverage")
    void constructorCoverage() {
        EmailService service = new EmailService(mailSender);
        assertDoesNotThrow(() -> service.sendEmail("test@test.com", "Subject", "Body"));
    }
}
