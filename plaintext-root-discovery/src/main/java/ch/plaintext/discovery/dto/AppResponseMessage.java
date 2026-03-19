/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.discovery.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Response message when an app recognizes a user
 * Published to: plaintext/response/{targetAppId}
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AppResponseMessage extends DiscoveryMessage {
    
    private String targetAppId; // App that should receive this message
    private String inResponseToMessageId; // Original login message ID
    
    private String userEmail; // Confirming we know this user
    private boolean userKnown; // true if we have this user
    private String appUrl; // Our URL for cross-app navigation
    private String appDisplayName; // Human readable name (e.g. "BIT Production")
    private String appEnvironment;
    
    public AppResponseMessage() {
        setType(MessageType.APP_RESPONSE);
    }
}