package com.ticketblitz.user.controller;

import com.ticketblitz.user.dto.UserRegistrationRequest;
import com.ticketblitz.user.dto.UserResponse;
import com.ticketblitz.user.entity.Role;
import com.ticketblitz.user.entity.User;
import com.ticketblitz.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Public Registration
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRegistrationRequest request) {
        return ResponseEntity.ok(userService.registerUser(request));
    }

    // INTERNAL API For Auth Service to get Password
    @GetMapping("/internal/{username}")
    public ResponseEntity<User> getUserByUsernameInternal(@PathVariable String username) {
        return ResponseEntity.ok(userService.getUserEntity(username));
    }

    // ADMIN Get All Users
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deactivate(@PathVariable Long id) {
        userService.deactivateUser(id);
        return ResponseEntity.ok("User deactivated. Please contact support.");
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> updateRole(@PathVariable Long id, @RequestParam Role role) {
        userService.updateUserRole(id, role);
        return ResponseEntity.ok("User role updated to " + role);
    }

    // FIX: Changed path from "/{id}/exists" to "/{id}/validate" to match Client
    @GetMapping("/{id}/validate")
    public ResponseEntity<Boolean> validateUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.existsById(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }
}
