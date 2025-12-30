package com.ticketblitz.auth.dto;

import lombok.Data;

@Data
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private String password; // hash
    private Role role;
    private boolean active;
}
