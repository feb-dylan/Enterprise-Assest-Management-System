package com.eams.security;

import com.eams.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_USER_ID = "uid";
    public static final String CLAIM_TOKEN_VERSION = "tv";

    private final SecretKey secretKey;
    private final long expiration;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration
    ) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET must be set and at least 32 characters long");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim(CLAIM_ROLE, user.getRole().getName())
                .claim(CLAIM_USER_ID, user.getId())
                .claim(CLAIM_TOKEN_VERSION, user.getTokenVersion())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expiration)))
                .signWith(secretKey)
                .compact();
    }

    public long getExpirationMillis() {
        return expiration;
    }

    /** Returns the claims if the signature and expiry are valid, otherwise empty. */
    public Optional<Claims> parse(String token) {
        try {
            return Optional.of(Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public String extractUsername(String token) {
        return parse(token).map(Claims::getSubject).orElse(null);
    }

    public boolean isTokenValid(String token) {
        return parse(token).isPresent();
    }
}
