// In BOTH com.ticketblitz.auth.dto AND com.ticketblitz.user.dto
package com.ticketblitz.auth.dto; // (Change package for User Service)

import lombok.Data;

@Data
public class UserRegistrationRequest {
    private String username;
    private String email;
    private String password;
}
