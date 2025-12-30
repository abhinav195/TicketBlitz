package com.ticketblitz.auth.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders; // [code_file:1] IMPORT THIS
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey getSigningKey() {
        // [code_file:1] FIX: Decode Base64 before generating HMAC key
        byte[] keyBytes = Decoders.BASE64.decode(secret);

        // DEBUGGER: PRINT HEX
        StringBuilder hex = new StringBuilder();
        for (byte b : keyBytes) hex.append(String.format("%02X", b));
        System.out.println("AUTH KEY HEX: " + hex.toString());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String username, String role) {
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }
}
