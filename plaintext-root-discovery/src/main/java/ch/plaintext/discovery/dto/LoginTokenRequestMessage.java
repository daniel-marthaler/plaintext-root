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