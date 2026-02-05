package com.ticketblitz.event.kafka;

import com.ticketblitz.event.dto.EventDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventEventListenerTest {

    @Mock
    private EventProducer eventProducer;

    @InjectMocks
    private EventEventListener eventEventListener;

    @Test
    @DisplayName("onEventCreated: Should call EventProducer.publishEventCreated with correct EventDto")
    void onEventCreatedShouldPublishToKafka() {
        // Arrange: Create a mock EventDto
        EventDto mockEventDto = new EventDto(
                1L,
                "Coldplay Concert",
                "Music of the Spheres World Tour",
                LocalDateTime.now().plusDays(30),
                "Wembley Stadium",
                "Music",
                new BigDecimal("150.00"),
                50000,
                50000,
                List.of("poster1.jpg", "poster2.jpg")
        );

        // Create the internal Spring event
        EventCreatedEvent event = new EventCreatedEvent(this, mockEventDto);

        // Act: Call the listener method (simulating @TransactionalEventListener(AFTER_COMMIT))
        eventEventListener.onEventCreated(event);

        // Assert: Verify EventProducer.publishEventCreated was called exactly once with the correct DTO
        verify(eventProducer, times(1)).publishEventCreated(mockEventDto);
    }

    @Test
    @DisplayName("onEventCreated: Should handle event with null images gracefully")
    void onEventCreatedWithNullImages() {
        // Arrange
        EventDto mockEventDto = new EventDto(
                2L,
                "Test Event",
                "Description",
                LocalDateTime.now().plusDays(10),
                "London",
                "Sports",
                BigDecimal.TEN,
                100,
                100,
                null  // Null images
        );

        EventCreatedEvent event = new EventCreatedEvent(this, mockEventDto);

        // Act
        eventEventListener.onEventCreated(event);

        // Assert: Should still call producer
        verify(eventProducer, times(1)).publishEventCreated(mockEventDto);
    }
}
