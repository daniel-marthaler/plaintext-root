/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
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
