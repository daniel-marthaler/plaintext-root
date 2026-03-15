package ch.plaintext.apitoken;

import java.time.Instant;

/**
 * Interface for standardized API error responses (RFC 7807).
 * Implementations are serialized to JSON in REST responses.
 */
public interface IApiErrorResponse {

    int getStatus();

    String getError();

    String getMessage();

    Instant getTimestamp();

    Instant getExpiredAt();

    String getPath();
}
