package com.ticketblitz.auth.service;

import com.ticketblitz.auth.client.UserClient;
import com.ticketblitz.auth.dto.*;
import feign.FeignException;
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

    // REGISTER
    public UserDto register(UserRegistrationRequest request) {
        try {
            return userClient.registerUser(request);
        } catch (FeignException e) {
            throw new RuntimeException(e.contentUTF8());
        }
    }

    // LOGIN
    public AuthResponse login(AuthRequest request) {
        UserDto user;
        try {
            user = userClient.getUserByUsernameInternal(request.getUsername());
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
}
