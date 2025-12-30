package com.ticketblitz.notification.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;

@FeignClient(name = "event-service", url = "${event-service.url}")
public interface EventClient {

    @GetMapping("/events")
    List<EventDto> getAllEvents();

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    class EventDto {
        private Long id;
        private String title;
        private String category;
        private String date;
        private String description;
    }
}
