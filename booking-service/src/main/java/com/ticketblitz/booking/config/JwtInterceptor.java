package com.ticketblitz.booking.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.crypto.SecretKey; // Note: Use SecretKey, not generic Key
import java.nio.charset.StandardCharsets;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Value("${jwt.secret}")
    private String secretKey;

    public JwtInterceptor() {
        System.out.println("DEBUG: JwtInterceptor Created (Constructor)");
    }

    @PostConstruct // Add javax.annotation or jakarta.annotation
    public void init() {
        System.out.println("DEBUG: JwtInterceptor Initialized. Secret is: " + secretKey);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (request.getRequestURI().contains("/inventory")) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Missing or Invalid Authorization Header");
            return false;
        }

        String token = authHeader.substring(7);
        try {
            byte[] keyBytes = io.jsonwebtoken.io.Decoders.BASE64.decode(secretKey);
            SecretKey key = Keys.hmacShaKeyFor(keyBytes);

            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token) // New JJWT syntax
                    .getPayload();

            // Handling the "Subject is not a Number" issue gracefully for testing
            String userIdStr = claims.getSubject();
            Long userId;
            try {
                userId = Long.parseLong(userIdStr);
            } catch (NumberFormatException e) {
                // If subject is "john_doe", default to ID 1 for this demo
                userId = 1L;
            }

            request.setAttribute("authenticatedUserId", userId);
            return true;

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid Token: " + e.getMessage());
            return false;
        }
    }
}
