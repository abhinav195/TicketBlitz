package com.ticketblitz.event.kafka;

import com.ticketblitz.event.dto.EventDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventProducerTest {

    @Mock
    private KafkaTemplate<String, EventDto> kafkaTemplate;

    @InjectMocks
    private EventProducer eventProducer;

    @Test
    @DisplayName("publishEventCreated: Should send message to correct Kafka topic with event ID as key")
    void publishEventCreatedSuccess() {
        // Arrange
        EventDto mockEventDto = new EventDto(
                100L,
                "Coldplay Concert",
                "Music of the Spheres",
                LocalDateTime.now().plusDays(30),
                "Wembley Stadium",
                "Music",
                new BigDecimal("150.00"),
                50000,
                50000,
                List.of("poster1.jpg")
        );

        // Act
        eventProducer.publishEventCreated(mockEventDto);

        // Assert: Verify kafkaTemplate.send was called with correct topic, key, and value
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<EventDto> valueCaptor = ArgumentCaptor.forClass(EventDto.class);

        verify(kafkaTemplate, times(1)).send(topicCaptor.capture(), keyCaptor.capture(), valueCaptor.capture());

        // Verify topic
        assertThat(topicCaptor.getValue()).isEqualTo("ticketblitz.events.created");

        // Verify key = event ID as String
        assertThat(keyCaptor.getValue()).isEqualTo("100");

        // Verify value = EventDto
        assertThat(valueCaptor.getValue().id()).isEqualTo(100L);
        assertThat(valueCaptor.getValue().title()).isEqualTo("Coldplay Concert");
    }

    @Test
    @DisplayName("publishEventCreated: Should handle null event ID (key should be null)")
    void publishEventCreatedNullId() {
        // Arrange: EventDto with null ID
        EventDto mockEventDto = new EventDto(
                null,  // Null ID
                "Test Event",
                "Description",
                LocalDateTime.now().plusDays(10),
                "Location",
                "Category",
                BigDecimal.TEN,
                100,
                100,
                null
        );

        // Act
        eventProducer.publishEventCreated(mockEventDto);

        // Assert: Verify key is null when event ID is null
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate, times(1)).send(eq("ticketblitz.events.created"), keyCaptor.capture(), eq(mockEventDto));

        assertThat(keyCaptor.getValue()).isNull();
    }

    @Test
    @DisplayName("publishEventCreated: Should handle null EventDto gracefully")
    void publishEventCreatedNullEvent() {
        // Act: Pass null EventDto
        eventProducer.publishEventCreated(null);

        // Assert: Should still call send (Kafka will serialize null, or NPE - depends on your serializer config)
        // For defensive programming, you might add null-check in EventProducer.
        // This test documents current behavior.
        verify(kafkaTemplate, times(1)).send(eq("ticketblitz.events.created"), isNull(), isNull());
    }
}
