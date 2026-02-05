package com.ticketblitz.notification.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailDispatchEvent {
    private String recipientEmail;
    private String subject;
    private String body;
}
