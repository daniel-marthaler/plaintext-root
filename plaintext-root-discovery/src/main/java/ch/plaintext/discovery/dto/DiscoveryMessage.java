/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.discovery.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Base class for all discovery MQTT messages
 */
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = UserLoginMessage.class, name = "USER_LOGIN"),
    @JsonSubTypes.Type(value = AppResponseMessage.class, name = "APP_RESPONSE"),
    @JsonSubTypes.Type(value = LoginTokenRequestMessage.class, name = "LOGIN_TOKEN_REQUEST"),
    @JsonSubTypes.Type(value = LoginTokenResponseMessage.class, name = "LOGIN_TOKEN_RESPONSE"),
    @JsonSubTypes.Type(value = HeartbeatMessage.class, name = "HEARTBEAT")
})
public abstract class DiscoveryMessage {
    
    public enum MessageType {
        USER_LOGIN, APP_RESPONSE, LOGIN_TOKEN_REQUEST, LOGIN_TOKEN_RESPONSE, HEARTBEAT
    }
    
    private MessageType type;
    private String fromAppId;
    private String fromAppName;
    private LocalDateTime timestamp = LocalDateTime.now();
    private String messageId = java.util.UUID.randomUUID().toString();
}