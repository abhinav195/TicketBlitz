package com.ticketblitz.notification.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class RecommendationReadyEvent extends ApplicationEvent {
    private final String email;
    private final String username;
    private final String recommendations;

    public RecommendationReadyEvent(Object source, String email, String username, String recommendations) {
        super(source);
        this.email = email;
        this.username = username;
        this.recommendations = recommendations;
    }
}
