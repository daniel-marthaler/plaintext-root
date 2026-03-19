/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.discovery.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Periodic heartbeat to announce app is still alive
 * Published to: plaintext/heartbeat
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class HeartbeatMessage extends DiscoveryMessage {

    private String appUrl;
    private String appVersion;
    private String appEnvironment;
    private int activeUserCount;
    private List<String> activeUserEmails;
    private String publicKey; // RSA public key for this app

    public HeartbeatMessage() {
        setType(MessageType.HEARTBEAT);
    }
}