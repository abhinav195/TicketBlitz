package com.ticketblitz.booking.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "user-service", url = "${user-service.url}", fallback = UserClientFallback.class)
public interface UserClient {

    // FIX: Added token parameter to support Async calls
    @GetMapping("/users/{id}/validate")
    @CircuitBreaker(name = "userService")
    boolean validateUser(@PathVariable("id") Long userId, @RequestHeader("Authorization") String token);
}
