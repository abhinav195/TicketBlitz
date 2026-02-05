package com.ticketblitz.notification.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationRequestEvent {
    private Long userId;
    private Long eventId;
    private String userEmail;
    private String username;
    private String authToken;
}
