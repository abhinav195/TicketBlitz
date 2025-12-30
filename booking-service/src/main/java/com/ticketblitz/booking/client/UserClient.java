package com.ticketblitz.booking.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "${user-service.url}")
public interface UserClient {

    // Calls the NEW endpoint defined above: GET /users/{id}/exists
    @GetMapping("/users/{id}/exists")
    boolean validateUser(@PathVariable("id") Long id);
}
