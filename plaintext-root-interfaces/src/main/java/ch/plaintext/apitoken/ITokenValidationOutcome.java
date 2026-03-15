package ch.plaintext.apitoken;

import org.springframework.http.ResponseEntity;

/**
 * Outcome of a Bearer token validation.
 * Either contains an error response (token missing/invalid/expired/revoked)
 * or a successful validation result.
 */
public interface ITokenValidationOutcome {

    boolean hasError();

    ResponseEntity<?> getErrorResponse();

    IApiTokenService.ApiTokenValidationResult getValidation();
}
