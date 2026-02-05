package com.ticketblitz.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserDto {

    @NotNull(message = "Id is required")
    private Long id;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 100, message = "Email must be <= 100 characters")
    private String email;

    // Password hash coming from user-service internal endpoint
    @NotBlank(message = "Password hash is required")
    private String password;

    @NotNull(message = "Role is required")
    private Role role;

    private boolean active;
}
