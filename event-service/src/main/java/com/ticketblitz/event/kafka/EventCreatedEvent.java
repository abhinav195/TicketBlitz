package com.ticketblitz.event.kafka;

import com.ticketblitz.event.dto.EventDto;
import org.springframework.context.ApplicationEvent;

public class EventCreatedEvent extends ApplicationEvent {

    private final EventDto event;

    public EventCreatedEvent(Object source, EventDto event) {
        super(source);
        this.event = event;
    }

    public EventDto getEvent() {
        return event;
    }
}
