package com.ticketblitz.notification.listener;

import com.ticketblitz.notification.client.BookingClient;
import com.ticketblitz.notification.client.UserClient;
import com.ticketblitz.notification.dto.EmailDispatchEvent;
import com.ticketblitz.notification.dto.PaymentUpdateEvent;
import com.ticketblitz.notification.dto.RecommendationRequestEvent;
import com.ticketblitz.notification.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationListenerTest {

    @Mock
    private EmailService emailService;

    @Mock
    private UserClient userClient;

    @Mock
    private BookingClient bookingClient;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private NotificationListener notificationListener;

    private static final String RECOMMENDATION_TOPIC = "recommendation.request";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationListener, "recommendationRequestTopic", RECOMMENDATION_TOPIC);
    }

    // ========== PAYMENT UPDATE - SUCCESS ==========

    @Test
    @DisplayName("handlePaymentUpdate: Should send confirmation email and route to recommendation on SUCCESS")
    void handlePaymentUpdateSuccess() throws InterruptedException {
        PaymentUpdateEvent event = new PaymentUpdateEvent(1L, "SUCCESS", "TXN123", 100L, "Bearer token123");

        UserClient.UserDto user = new UserClient.UserDto();
        user.setId(100L);
        user.setEmail("user@test.com");
        user.setUsername("John Doe");

        BookingClient.BookingDto booking = new BookingClient.BookingDto();
        booking.setId(1L);
        booking.setEventId(50L);

        when(userClient.getUserById(100L, "Bearer token123")).thenReturn(user);
        when(bookingClient.getBookingById(1L, "Bearer token123")).thenReturn(booking);

        notificationListener.handlePaymentUpdate(event);

        TimeUnit.MILLISECONDS.sleep(500);

        verify(userClient).getUserById(100L, "Bearer token123");
        verify(bookingClient).getBookingById(1L, "Bearer token123");
        verify(emailService, timeout(2000)).sendEmail(eq("user@test.com"), eq("Booking Confirmed!"), contains("TXN123"));

        ArgumentCaptor<RecommendationRequestEvent> captor = ArgumentCaptor.forClass(RecommendationRequestEvent.class);
        verify(kafkaTemplate).send(eq(RECOMMENDATION_TOPIC), captor.capture());

        assertThat(captor.getValue().getUserId()).isEqualTo(100L);
        assertThat(captor.getValue().getEventId()).isEqualTo(50L);
    }

    @Test
    @DisplayName("handlePaymentUpdate: Should send failure email when status is FAILED")
    void handlePaymentUpdateFailed() throws InterruptedException {
        PaymentUpdateEvent event = new PaymentUpdateEvent(2L, "FAILED", "TXN456", 200L, "Bearer token456");

        UserClient.UserDto user = new UserClient.UserDto();
        user.setId(200L);
        user.setEmail("failed@test.com");
        user.setUsername("Jane Smith");

        when(userClient.getUserById(200L, "Bearer token456")).thenReturn(user);

        notificationListener.handlePaymentUpdate(event);

        TimeUnit.MILLISECONDS.sleep(500);

        verify(emailService, timeout(2000)).sendEmail(eq("failed@test.com"), eq("Payment Failed"), contains("payment could not be processed"));
        verify(kafkaTemplate, never()).send(anyString(), any());
    }

    @Test
    @DisplayName("handlePaymentUpdate: Should skip when user not found")
    void handlePaymentUpdateUserNotFound() {
        PaymentUpdateEvent event = new PaymentUpdateEvent(3L, "SUCCESS", "TXN789", 300L, "Bearer token789");
        when(userClient.getUserById(300L, "Bearer token789")).thenReturn(null);

        assertDoesNotThrow(() -> notificationListener.handlePaymentUpdate(event));

        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("handlePaymentUpdate: Should skip when user email is missing")
    void handlePaymentUpdateUserEmailMissing() {
        PaymentUpdateEvent event = new PaymentUpdateEvent(4L, "SUCCESS", "TXN999", 400L, "Bearer token999");

        UserClient.UserDto user = new UserClient.UserDto();
        user.setId(400L);
        user.setEmail(null);

        when(userClient.getUserById(400L, "Bearer token999")).thenReturn(user);

        assertDoesNotThrow(() -> notificationListener.handlePaymentUpdate(event));
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("handlePaymentUpdate: Should handle UserClient exception")
    void handlePaymentUpdateUserClientException() {
        PaymentUpdateEvent event = new PaymentUpdateEvent(5L, "SUCCESS", "TXN111", 500L, "Bearer token111");

        when(userClient.getUserById(500L, "Bearer token111")).thenThrow(new RuntimeException("Service down"));

        assertDoesNotThrow(() -> notificationListener.handlePaymentUpdate(event));
    }

    @Test
    @DisplayName("handlePaymentUpdate: Should handle BookingClient exception")
    void handlePaymentUpdateBookingClientException() throws InterruptedException {
        PaymentUpdateEvent event = new PaymentUpdateEvent(6L, "SUCCESS", "TXN222", 600L, "Bearer token222");

        UserClient.UserDto user = new UserClient.UserDto();
        user.setId(600L);
        user.setEmail("booking-error@test.com");
        user.setUsername("Test");

        when(userClient.getUserById(600L, "Bearer token222")).thenReturn(user);
        when(bookingClient.getBookingById(6L, "Bearer token222")).thenThrow(new RuntimeException("Booking service down"));

        notificationListener.handlePaymentUpdate(event);

        TimeUnit.MILLISECONDS.sleep(500);

        verify(emailService, timeout(2000)).sendEmail(anyString(), anyString(), anyString());
        verify(kafkaTemplate, never()).send(anyString(), any());
    }

    @Test
    @DisplayName("handlePaymentUpdate: Should handle null booking")
    void handlePaymentUpdateNullBooking() throws InterruptedException {
        PaymentUpdateEvent event = new PaymentUpdateEvent(7L, "SUCCESS", "TXN333", 700L, "Bearer token333");

        UserClient.UserDto user = new UserClient.UserDto();
        user.setId(700L);
        user.setEmail("null-booking@test.com");
        user.setUsername("Test");

        when(userClient.getUserById(700L, "Bearer token333")).thenReturn(user);
        when(bookingClient.getBookingById(7L, "Bearer token333")).thenReturn(null);

        notificationListener.handlePaymentUpdate(event);

        TimeUnit.MILLISECONDS.sleep(500);

        verify(kafkaTemplate, never()).send(anyString(), any());
    }

    @Test
    @DisplayName("handlePaymentUpdate: Should handle booking with null eventId")
    void handlePaymentUpdateNullEventId() throws InterruptedException {
        PaymentUpdateEvent event = new PaymentUpdateEvent(8L, "SUCCESS", "TXN444", 800L, "Bearer token444");

        UserClient.UserDto user = new UserClient.UserDto();
        user.setId(800L);
        user.setEmail("no-event@test.com");
        user.setUsername("Test");

        BookingClient.BookingDto booking = new BookingClient.BookingDto();
        booking.setEventId(null);

        when(userClient.getUserById(800L, "Bearer token444")).thenReturn(user);
        when(bookingClient.getBookingById(8L, "Bearer token444")).thenReturn(booking);

        notificationListener.handlePaymentUpdate(event);

        TimeUnit.MILLISECONDS.sleep(500);

        verify(kafkaTemplate, never()).send(anyString(), any());
    }

    // ========== EMAIL DISPATCH ==========

    @Test
    @DisplayName("handleEmailDispatch: Should schedule email from AI lane")
    void handleEmailDispatchSuccess() throws InterruptedException {
        EmailDispatchEvent event = new EmailDispatchEvent("ai@test.com", "AI Recommendations", "Check out these events!");

        notificationListener.handleEmailDispatch(event);

        TimeUnit.MILLISECONDS.sleep(500);

        verify(emailService, timeout(2000)).sendEmail(eq("ai@test.com"), eq("AI Recommendations"), anyString());
    }

    @Test
    @DisplayName("NotificationListener: Constructor coverage")
    void constructorCoverage() {
        NotificationListener listener = new NotificationListener(emailService, userClient, bookingClient, kafkaTemplate);
        assertThat(listener).isNotNull();
    }
}
