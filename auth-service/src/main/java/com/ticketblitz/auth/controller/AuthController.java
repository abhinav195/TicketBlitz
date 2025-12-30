package com.ticketblitz.auth.controller;

import com.ticketblitz.auth.dto.AuthRequest;
import com.ticketblitz.auth.dto.AuthResponse;
import com.ticketblitz.auth.dto.UserDto;
import com.ticketblitz.auth.dto.UserRegistrationRequest; // You need this DTO
import com.ticketblitz.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@RequestBody UserRegistrationRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest authRequest) {
        // Call the updated service method
        return ResponseEntity.ok(authService.login(authRequest));
    }

    @GetMapping("/validate")
    public ResponseEntity<String> validateToken(@RequestParam("token") String token) {
        authService.validateToken(token);
        return ResponseEntity.ok("Token is valid");
    }
}
