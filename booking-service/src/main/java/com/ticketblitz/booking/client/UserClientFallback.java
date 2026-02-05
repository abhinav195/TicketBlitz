package com.ticketblitz.booking.client;

import org.springframework.stereotype.Component;

@Component
public class UserClientFallback implements UserClient {

    @Override
    public boolean validateUser(Long userId, String token) {
        // Fallback logic: Assume user is NOT valid if service is down,
        // or return true/false based on your resilience strategy.
        // For strict consistency, we usually return false or throw exception.
        System.out.println("⚠️ User Service is down. Fallback for userId: " + userId);
        return false;
    }
}
