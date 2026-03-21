/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.discovery.service;

import ch.plaintext.discovery.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Handles incoming discovery MQTT messages
 */
@ConditionalOnProperty(value = "discovery.enabled", havingValue = "true", matchIfMissing = false)
@Service
@Slf4j
@RequiredArgsConstructor
public class DiscoveryMessageHandler {
    
    private final DiscoveryService discoveryService;
    
    public void handleMessage(String topic, DiscoveryMessage message) {
        log.debug("Processing message type {} from app {}", message.getType(), message.getFromAppId());
        
        switch (message.getType()) {
            case USER_LOGIN -> handleUserLogin((UserLoginMessage) message);
            case APP_RESPONSE -> handleAppResponse((AppResponseMessage) message);
            case LOGIN_TOKEN_REQUEST -> handleLoginTokenRequest((LoginTokenRequestMessage) message);
            case LOGIN_TOKEN_RESPONSE -> handleLoginTokenResponse((LoginTokenResponseMessage) message);
            case HEARTBEAT -> handleHeartbeat((HeartbeatMessage) message);
            default -> log.warn("Unknown message type: {}", message.getType());
        }
    }
    
    private void handleUserLogin(UserLoginMessage message) {
        try {
            log.info("User login detected: {} from app {}", message.getUserEmail(), message.getFromAppId());
            
            // Register the remote app
            discoveryService.registerOrUpdateApp(
                message.getFromAppId(),
                message.getFromAppName(),
                message.getAppUrl(),
                message.getAppEnvironment(),
                message.getPublicKey()
            );
            
            // Check if we know this user
            boolean userKnown = discoveryService.isUserKnownLocally(message.getUserEmail());
            
            if (userKnown) {
                // Send response that we know this user
                discoveryService.sendAppResponse(message);
            }
            
        } catch (Exception e) {
            log.error("Error handling user login message", e);
        }
    }
    
    private void handleAppResponse(AppResponseMessage message) {
        try {
            log.info("App response from {}: user {} known={}", 
                message.getFromAppId(), message.getUserEmail(), message.isUserKnown());
            
            if (message.isUserKnown()) {
                // Register this app as knowing the user
                discoveryService.registerUserInRemoteApp(message);
            }
            
        } catch (Exception e) {
            log.error("Error handling app response message", e);
        }
    }
    
    private void handleLoginTokenRequest(LoginTokenRequestMessage message) {
        try {
            log.info("Login token request from {} for user {}", 
                message.getFromAppId(), message.getUserEmail());
            
            // Generate temporary login token
            String loginToken = discoveryService.generateLoginToken(message.getUserEmail());
            
            if (loginToken != null) {
                discoveryService.sendLoginTokenResponse(message, loginToken);
            }
            
        } catch (Exception e) {
            log.error("Error handling login token request", e);
        }
    }
    
    private void handleLoginTokenResponse(LoginTokenResponseMessage message) {
        try {
            log.info("Login token response from {} for user {}", 
                message.getFromAppId(), message.getUserEmail());
            
            // Store the login URL for the user
            discoveryService.storeRemoteLoginUrl(message);
            
        } catch (Exception e) {
            log.error("Error handling login token response", e);
        }
    }
    
    private void handleHeartbeat(HeartbeatMessage message) {
        try {
            log.debug("Heartbeat from app {}: {} active users", 
                message.getFromAppId(), message.getActiveUserCount());
            
            // Update app status
            discoveryService.updateAppHeartbeat(
                message.getFromAppId(),
                message.getFromAppName(),
                message.getAppUrl(),
                message.getAppEnvironment(),
                message.getActiveUserCount(),
                message.getPublicKey(),
                message.getActiveUserEmails()
            );
            
        } catch (Exception e) {
            log.error("Error handling heartbeat message", e);
        }
    }
}