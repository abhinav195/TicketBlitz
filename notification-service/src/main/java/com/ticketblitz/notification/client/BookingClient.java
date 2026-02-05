package com.ticketblitz.notification.client;

import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

// Uses property 'booking-service.url' from application.yml
@FeignClient(name = "booking-service", url = "${booking-service.url}")
public interface BookingClient {

    @GetMapping("/bookings/{id}")
    BookingDto getBookingById(@PathVariable("id") Long id, @RequestHeader("Authorization") String authToken);

    @Data
    class BookingDto {
        private Long id;
        private Long userId;
        private Long eventId;
        private String status;
    }
}
