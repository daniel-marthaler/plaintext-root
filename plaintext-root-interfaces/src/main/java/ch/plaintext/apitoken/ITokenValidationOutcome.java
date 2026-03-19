/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
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
