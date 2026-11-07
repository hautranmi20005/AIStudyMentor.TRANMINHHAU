package com.aistudymentor.config;

import com.aistudymentor.auth.entity.AccountStatus;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret:defaultSecretKeyWithAtLeast256BitsLengthForHmacSha256Security123456789}")
    private String jwtSecret;

    @Value("${app.jwt.access-token-expiration-ms:900000}")
    private long accessTokenExpirationMs;

    @Value("${app.jwt.refresh-token-expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    @Value("${app.jwt.mfa-session-expiration-ms:300000}")
    private long mfaSessionExpirationMs;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UUID userId, String email, AccountStatus accountStatus) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("userId", userId.toString())
                .claim("accountStatus", accountStatus.name())
                .claim("tokenType", "ACCESS")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(UUID userId, String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("userId", userId.toString())
                .claim("tokenType", "REFRESH")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public String generateMfaSessionToken(UUID userId, String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + mfaSessionExpirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("userId", userId.toString())
                .claim("tokenType", "MFA_SESSION")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token has expired: {}", e.getMessage());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
        }
        return false;
    }

    public String getEmailFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public UUID getUserIdFromToken(String token) {
        String userIdStr = parseClaims(token).get("userId", String.class);
        return UUID.fromString(userIdStr);
    }

    public String getTokenType(String token) {
        return parseClaims(token).get("tokenType", String.class);
    }

    public AccountStatus getAccountStatusFromToken(String token) {
        String status = parseClaims(token).get("accountStatus", String.class);
        return status != null ? AccountStatus.valueOf(status) : null;
    }
}
