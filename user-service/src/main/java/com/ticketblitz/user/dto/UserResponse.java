package com.ticketblitz.user.dto;
import com.ticketblitz.user.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private Role role;
    private boolean isActive;
}
