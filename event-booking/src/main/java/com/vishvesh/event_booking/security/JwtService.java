package com.vishvesh.event_booking.security;

import com.vishvesh.event_booking.dto.authdto.JwtDto;
import com.vishvesh.event_booking.utils.enums.Role;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@Slf4j
public class JwtService {

    private final long EXPIRY_TIME;
    private final SecretKey SECRET_KEY;

    public JwtService(@Value("${JWT_SECRET}") String secret, @Value("${JWT_EXPIRY}") long expiry) {
        this.EXPIRY_TIME = expiry;
        this.SECRET_KEY = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(JwtDto data) {
//        Map<String, Object> claims = new HashMap<>();
//        Date now = new Date();
//        Date expiry = new Date(now.getTime() + EXPIRY_TIME*60*60*1000);
//
//        return Jwts.builder()
//                .subject(data.getUserId().toString())
//                .claim("email", data.getEmail())
//                .claim("role", data.getRole().name())
//                .claim("verified", data.getIsVerified())
//                .issuedAt(now)
//                .expiration(expiry)
//                .signWith(SECRET_KEY)
//                .compact();
        Map<String, Object> claims = new HashMap<>();
        Date now = new Date();
        Date expiry = new Date(now.getTime() + EXPIRY_TIME * 60 * 60 * 1000);
        String roleName = data.getRole() != null ? data.getRole().name() : Role.USER.name();

        return Jwts.builder()
                .subject(data.getUserId().toString())
                .claim("email", data.getEmail())
                .claim("role", roleName) // 2. Use the safe variable here!
                .claim("verified", data.getIsVerified())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(SECRET_KEY)
                .compact();
    }

    public Optional<JwtDto> parseToken(String token) {
//        try {
//            Claims claims = Jwts.parser()           // 0.12.x: parserBuilder() is gone — use parser()
//                    .verifyWith(SECRET_KEY)
//                    .build()
//                    .parseSignedClaims(token)
//                    .getPayload();
//
//            JwtDto data = JwtDto.builder()
//                    .userId(UUID.fromString(claims.getSubject()))
//                    .email(claims.get("email", String.class))
//                    .role(Role.valueOf(claims.get("role", String.class)))
//                    .isVerified(Boolean.TRUE.equals(claims.get("isVerified", Boolean.class)))
//                    .build();
//
//            return Optional.of(data);
//
//        } catch (JwtException | IllegalArgumentException ex) {
//            log.warn("Invalid JWT: {}", ex.getMessage());
//            return Optional.empty();
//        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(SECRET_KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // 1. Safely extract the role string from the token
            String roleString = claims.get("role", String.class);

            JwtDto data = JwtDto.builder()
                    .userId(UUID.fromString(claims.getSubject()))
                    .email(claims.get("email", String.class))
                    // 2. Safe fallback: If the token is old and has no role, default to USER
                    .role(roleString != null ? Role.valueOf(roleString) : Role.USER)
                    .isVerified(Boolean.TRUE.equals(claims.get("verified", Boolean.class)))
                    .build();

            return Optional.of(data);

        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Invalid JWT: {}", ex.getMessage());
            return Optional.empty();
        }
    }
}
