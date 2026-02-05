package com.ticketblitz.event.kafka;

import com.ticketblitz.event.dto.EventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventProducer {

    public static final String TOPIC_EVENTS_CREATED = "ticketblitz.events.created";

    private final KafkaTemplate<String, EventDto> kafkaTemplate;

    public void publishEventCreated(EventDto event) {
        // Key = String, Value = JSON(EventDto)
        String key = (event != null && event.id() != null) ? event.id().toString() : null;
        kafkaTemplate.send(TOPIC_EVENTS_CREATED, key, event);
        log.info("Published event-created to Kafka. topic={}, key={}", TOPIC_EVENTS_CREATED, key);
    }
}
