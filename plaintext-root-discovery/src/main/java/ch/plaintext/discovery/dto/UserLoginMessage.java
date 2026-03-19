/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.discovery.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Message sent when a user logs into an app
 * Published to: plaintext/discovery
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserLoginMessage extends DiscoveryMessage {
    
    private String userEmail;
    private Long userId;
    private String userName;
    private String appUrl;
    private String appEnvironment; // prod, dev, int, test
    private String publicKey; // RSA public key for encrypted responses
    
    public UserLoginMessage() {
        setType(MessageType.USER_LOGIN);
    }
}