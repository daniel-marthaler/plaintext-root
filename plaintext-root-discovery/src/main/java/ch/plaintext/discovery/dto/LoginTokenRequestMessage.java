/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.discovery.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Request for a temporary login token for cross-app authentication
 * Published to: plaintext/login/{targetAppId}
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LoginTokenRequestMessage extends DiscoveryMessage {
    
    private String targetAppId; // App that should generate the token
    private String userEmail; // User requesting access
    private String returnUrl; // URL to redirect to after login (optional)
    
    public LoginTokenRequestMessage() {
        setType(MessageType.LOGIN_TOKEN_REQUEST);
    }
}