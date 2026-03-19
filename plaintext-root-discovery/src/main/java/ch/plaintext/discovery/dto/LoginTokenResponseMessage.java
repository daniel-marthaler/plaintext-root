/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.discovery.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Response with encrypted temporary login token
 * Published to: plaintext/login/{requestingAppId}
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LoginTokenResponseMessage extends DiscoveryMessage {
    
    private String targetAppId; // App that requested the token
    private String inResponseToMessageId; // Original token request message ID
    
    private String userEmail;
    private String encryptedToken; // RSA-encrypted temporary login token
    private String loginUrl; // Full URL for auto-login with token
    private long tokenValidForSeconds; // Token TTL (default: 300 seconds = 5 minutes)
    
    public LoginTokenResponseMessage() {
        setType(MessageType.LOGIN_TOKEN_RESPONSE);
    }
}