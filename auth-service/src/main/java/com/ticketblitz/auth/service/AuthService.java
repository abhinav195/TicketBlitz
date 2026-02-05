package com.ticketblitz.auth.service;

import com.ticketblitz.auth.client.UserClient;
import com.ticketblitz.auth.dto.*;
import com.ticketblitz.auth.exception.ServiceUnavailableException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserClient userClient;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // REGISTER (no flow change; adds resilience around the Feign call)
    public UserDto register(UserRegistrationRequest request) {
        return registerRemote(request);
    }

    // LOGIN (no flow change: Get user -> verify password -> create token)
    public AuthResponse login(AuthRequest request) {
        UserDto user;
        try {
            user = getUserRemote(request.getUsername());
        } catch (FeignException.NotFound e) {
            throw new RuntimeException("Invalid credentials");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtService.generateToken(user.getUsername(), user.getRole());
        return new AuthResponse(token, user.getUsername(), user.getId());
    }

    public void validateToken(String token) {
        jwtService.validateToken(token);
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "fallbackRegisterUser")
    @Retry(name = "userService")
    protected UserDto registerRemote(UserRegistrationRequest request) {
        try {
            return userClient.registerUser(request);
        } catch (FeignException e) {
            // preserve existing behavior: bubble up user-service error content
            throw new RuntimeException(e.contentUTF8());
        }
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "fallbackGetUser")
    @Retry(name = "userService")
    protected UserDto getUserRemote(String username) {
        try {
            return userClient.getUserByUsernameInternal(username);
        } catch (FeignException.NotFound e) {
            throw e; // handled as invalid credentials by caller
        } catch (FeignException e) {
            // preserve existing behavior as much as possible
            throw new RuntimeException(e.contentUTF8());
        }
    }

    // Fallbacks must match signature (+ Throwable at end). They throw specific exception as required.
    protected UserDto fallbackGetUser(String username, Throwable ex) {
        throw new ServiceUnavailableException("User Service is unavailable. Please try again later.", ex);
    }

    protected UserDto fallbackRegisterUser(UserRegistrationRequest request, Throwable ex) {
        throw new ServiceUnavailableException("User Service is unavailable. Please try again later.", ex);
    }
}
