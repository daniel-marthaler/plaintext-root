/*
 * Copyright (C) eMad, 2026.
 */
package ch.plaintext.apitoken;

import ch.plaintext.apitoken.IApiTokenService.ApiTokenValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * Centralized Bearer token validation for all REST API controllers.
 * Extracts JWT from Authorization header, validates signature, expiry, and revocation.
 *
 * @author info@emad.ch
 * @since 2026
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApiTokenValidatorServiceImpl implements ApiTokenValidatorService {

    private final IApiTokenService apiTokenService;
    private final JwtTokenService jwtTokenService;

    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public ITokenValidationOutcome validateRequest(String authorizationHeader, String requestPath) {
        String token = extractBearerToken(authorizationHeader);

        if (token == null || token.isEmpty()) {
            log.warn("API request without token to {}", requestPath);
            return errorOutcome(ApiErrorResponse.tokenMissing(requestPath));
        }

        // Full validation: JWT signature (PKI) + hash lookup (revocation check)
        Optional<ApiTokenValidationResult> result = apiTokenService.validateToken(token);

        if (result.isPresent()) {
            return new TokenValidationOutcome(null, result.get());
        }

        // Determine specific error type for better API error messages
        if (isTokenExpired(token)) {
            Instant expiredAt = getTokenExpiry(token);
            log.warn("API request with expired token to {} - expired at {}", requestPath, expiredAt);
            return errorOutcome(ApiErrorResponse.tokenExpired(expiredAt, requestPath));
        }

        // Check if JWT signature is valid (to distinguish invalid vs revoked)
        Optional<JwtTokenService.JwtValidationResult> jwtResult = jwtTokenService.validateToken(token);
        if (jwtResult.isPresent()) {
            // JWT is valid but not found in DB → revoked
            log.warn("API request with revoked token to {} for userId={}", requestPath, jwtResult.get().userId());
            return errorOutcome(ApiErrorResponse.tokenRevoked(requestPath));
        }

        log.warn("API request with invalid token to {}", requestPath);
        return errorOutcome(ApiErrorResponse.tokenInvalid(requestPath));
    }

    private String extractBearerToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private ITokenValidationOutcome errorOutcome(ApiErrorResponse error) {
        return new TokenValidationOutcome(
                ResponseEntity.status(HttpStatus.valueOf(error.status())).body(error),
                null
        );
    }

    private boolean isTokenExpired(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return false;
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            return payload.contains("\"exp\"");
        } catch (Exception ignored) {
        }
        return false;
    }

    private Instant getTokenExpiry(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return null;
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            int expIdx = payload.indexOf("\"exp\"");
            if (expIdx >= 0) {
                int colonIdx = payload.indexOf(":", expIdx);
                int endIdx = payload.indexOf(",", colonIdx);
                if (endIdx < 0) endIdx = payload.indexOf("}", colonIdx);
                if (colonIdx >= 0 && endIdx >= 0) {
                    String expStr = payload.substring(colonIdx + 1, endIdx).trim();
                    long expSeconds = Long.parseLong(expStr);
                    return Instant.ofEpochSecond(expSeconds);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private record TokenValidationOutcome(
            ResponseEntity<?> errorResponse,
            ApiTokenValidationResult validation
    ) implements ITokenValidationOutcome {

        @Override
        public boolean hasError() {
            return errorResponse != null;
        }

        @Override
        public ResponseEntity<?> getErrorResponse() {
            return errorResponse;
        }

        @Override
        public ApiTokenValidationResult getValidation() {
            return validation;
        }
    }
}
