/*
 * Copyright (C) eMad, 2026.
 */
package ch.plaintext.apitoken;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

/**
 * JWT Token Service with RSA signing (RS256).
 * Tokens expire after 90 days.
 * Logs warning when token expires within 7 days.
 *
 * @author info@emad.ch
 * @since 2026
 */
@Service
@Slf4j
public class JwtTokenService {

    public static final int MIN_VALIDITY_DAYS = 7;
    public static final int MAX_VALIDITY_DAYS = 365;
    public static final int DEFAULT_VALIDITY_DAYS = 90;
    private static final Duration EXPIRY_WARNING_THRESHOLD = Duration.ofDays(7);
    private static final String CLAIM_MANDAT = "mandat";
    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_TOKEN_NAME = "tokenName";

    private PrivateKey privateKey;
    private PublicKey publicKey;

    @PostConstruct
    public void init() {
        try {
            this.privateKey = loadPrivateKey();
            this.publicKey = loadPublicKey();
            log.info("JWT RSA keys loaded successfully");
        } catch (Exception e) {
            log.error("Failed to load JWT RSA keys: {}", e.getMessage(), e);
            throw new IllegalStateException("Cannot initialize JWT service without RSA keys", e);
        }
    }

    /**
     * Generate a new JWT token for a user.
     *
     * @param userId       User ID
     * @param mandat       Mandat identifier
     * @param email        User's email address
     * @param tokenName    User-defined name for this token
     * @param validityDays Token validity in days (7-90)
     * @return Signed JWT token string
     */
    public String generateToken(Long userId, String mandat, String email, String tokenName, int validityDays) {
        // Enforce bounds
        if (validityDays < MIN_VALIDITY_DAYS) validityDays = MIN_VALIDITY_DAYS;
        if (validityDays > MAX_VALIDITY_DAYS) validityDays = MAX_VALIDITY_DAYS;

        Instant now = Instant.now();
        Instant expiry = now.plus(Duration.ofDays(validityDays));

        var builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_MANDAT, mandat)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry));

        if (email != null && !email.isBlank()) {
            builder.claim(CLAIM_EMAIL, email);
        }
        if (tokenName != null && !tokenName.isBlank()) {
            builder.claim(CLAIM_TOKEN_NAME, tokenName);
        }

        String token = builder.signWith(privateKey, Jwts.SIG.RS256).compact();

        log.info("Generated JWT token for userId={}, mandat={}, tokenName={}, validityDays={}, expires={}",
                userId, mandat, tokenName, validityDays, expiry);

        return token;
    }

    /**
     * Generate a new JWT token with default validity (90 days).
     */
    public String generateToken(Long userId, String mandat, String email, String tokenName) {
        return generateToken(userId, mandat, email, tokenName, DEFAULT_VALIDITY_DAYS);
    }

    /**
     * Generate a new JWT token for a user (without email/tokenName).
     */
    public String generateToken(Long userId, String mandat) {
        return generateToken(userId, mandat, null, null, DEFAULT_VALIDITY_DAYS);
    }

    /**
     * Validate a JWT token and extract claims.
     *
     * @param token JWT token string
     * @return Validation result with claims, or empty if invalid
     */
    public Optional<JwtValidationResult> validateToken(String token) {
        if (token == null || token.isBlank()) {
            log.debug("Token validation failed: token is null or empty");
            return Optional.empty();
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Long userId = claims.get(CLAIM_USER_ID, Long.class);
            String mandat = claims.get(CLAIM_MANDAT, String.class);
            String email = claims.get(CLAIM_EMAIL, String.class);
            String tokenName = claims.get(CLAIM_TOKEN_NAME, String.class);
            Instant expiry = claims.getExpiration().toInstant();

            // Check if token expires soon (within 7 days)
            Duration timeUntilExpiry = Duration.between(Instant.now(), expiry);
            if (timeUntilExpiry.compareTo(EXPIRY_WARNING_THRESHOLD) <= 0) {
                log.warn("JWT token for userId={}, mandat={} expires in {} days - renewal recommended",
                        userId, mandat, timeUntilExpiry.toDays());
            }

            log.debug("JWT token validated successfully for userId={}, mandat={}, email={}", userId, mandat, email);
            return Optional.of(new JwtValidationResult(userId, mandat, email, tokenName, expiry));

        } catch (ExpiredJwtException e) {
            Claims claims = e.getClaims();
            Long userId = claims != null ? claims.get(CLAIM_USER_ID, Long.class) : null;
            String mandat = claims != null ? claims.get(CLAIM_MANDAT, String.class) : null;
            Instant expiredAt = claims != null ? claims.getExpiration().toInstant() : null;

            log.warn("JWT token expired for userId={}, mandat={}, expiredAt={}",
                    userId, mandat, expiredAt);
            return Optional.empty();

        } catch (JwtException e) {
            log.warn("JWT token validation failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Check if a token is expired.
     *
     * @param token JWT token string
     * @return true if expired or invalid
     */
    public boolean isExpired(String token) {
        return validateToken(token).isEmpty();
    }

    /**
     * Get remaining validity duration.
     *
     * @param token JWT token string
     * @return Duration until expiry, or empty if invalid/expired
     */
    public Optional<Duration> getRemainingValidity(String token) {
        return validateToken(token)
                .map(result -> Duration.between(Instant.now(), result.expiresAt()));
    }

    private PrivateKey loadPrivateKey() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        String pem = loadResourceAsString("/keys/private.pem");
        String base64 = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] decoded = Base64.getDecoder().decode(base64);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    private PublicKey loadPublicKey() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        String pem = loadResourceAsString("/keys/public.pem");
        String base64 = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] decoded = Base64.getDecoder().decode(base64);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

    private String loadResourceAsString(String resourcePath) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Result of JWT validation containing extracted claims.
     */
    public record JwtValidationResult(Long userId, String mandat, String email, String tokenName, Instant expiresAt) {}
}
