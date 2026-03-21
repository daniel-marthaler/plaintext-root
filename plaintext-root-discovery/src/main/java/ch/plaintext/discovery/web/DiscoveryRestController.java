/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.discovery.web;

import ch.plaintext.discovery.dto.LoginTokenRequestMessage;
import ch.plaintext.discovery.dto.UserLoginMessage;
import ch.plaintext.discovery.entity.DiscoveryApp;
import ch.plaintext.discovery.entity.DiscoveryUserSession;
import ch.plaintext.discovery.service.DiscoveryService;
import ch.plaintext.discovery.repository.DiscoveryAppRepository;
import ch.plaintext.discovery.repository.DiscoveryUserSessionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST API for Discovery service
 */
@ConditionalOnProperty(value = "discovery.enabled", havingValue = "true", matchIfMissing = false)
@RestController
@RequestMapping("/api/discovery")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Discovery", description = "Multi-instance discovery service for cross-app navigation, login announcements, and token exchange")
public class DiscoveryRestController {
    
    private final DiscoveryService discoveryService;
    private final DiscoveryAppRepository appRepository;
    private final DiscoveryUserSessionRepository sessionRepository;
    
    /**
     * Announce user login (alternative to MQTT)
     */
    @Operation(summary = "Announce user login",
               description = "Notifies the discovery service about a user login event. "
                           + "This is a REST-based alternative to MQTT-based announcements.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login announced successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or processing error")
    })
    @PostMapping("/announce-login")
    public ResponseEntity<Map<String, String>> announceUserLogin(
            @RequestBody UserLoginMessage loginMessage) {
        try {
            log.info("REST: User login announcement for {}", loginMessage.getUserEmail());
            
            discoveryService.announceUserLogin(
                loginMessage.getUserEmail(),
                loginMessage.getUserId(),
                loginMessage.getUserName()
            );
            
            return ResponseEntity.ok(Map.of("status", "success", "message", "Login announced"));
            
        } catch (Exception e) {
            log.error("Error announcing user login via REST", e);
            return ResponseEntity.badRequest()
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
    
    /**
     * Request login token for cross-app navigation
     */
    @Operation(summary = "Request a cross-app login token",
               description = "Generates a short-lived login token for cross-app navigation. "
                           + "The token is valid for 300 seconds and can be used to authenticate on a target application.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token generated successfully"),
            @ApiResponse(responseCode = "400", description = "User not authorized or invalid request")
    })
    @PostMapping("/request-token")
    public ResponseEntity<Map<String, String>> requestLoginToken(
            @RequestBody LoginTokenRequestMessage request) {
        try {
            log.info("REST: Token request for user {} to app {}", 
                request.getUserEmail(), request.getTargetAppId());
            
            String token = discoveryService.generateLoginToken(request.getUserEmail());
            
            if (token != null) {
                return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "token", token,
                    "loginUrl", "/discovery/login?token=" + token,
                    "validFor", "300"
                ));
            } else {
                return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "User not authorized"));
            }
            
        } catch (Exception e) {
            log.error("Error generating login token via REST", e);
            return ResponseEntity.badRequest()
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
    
    /**
     * Get list of active apps
     */
    @Operation(summary = "List active applications",
               description = "Returns all applications currently registered and active in the discovery network.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of active applications returned"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/apps")
    public ResponseEntity<List<DiscoveryApp>> getActiveApps() {
        try {
            List<DiscoveryApp> activeApps = appRepository.findByActiveTrue();
            return ResponseEntity.ok(activeApps);
        } catch (Exception e) {
            log.error("Error fetching active apps", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get remote apps for a specific user
     */
    @Operation(summary = "Get remote apps for a user",
               description = "Returns the list of remote application sessions where the specified user is currently logged in.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of remote app sessions returned"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/user/{userEmail}/apps")
    public ResponseEntity<List<DiscoveryUserSession>> getRemoteAppsForUser(
            @Parameter(description = "Email address of the user") @PathVariable String userEmail) {
        try {
            List<DiscoveryUserSession> remoteApps = discoveryService.getRemoteAppsForUser(userEmail);
            return ResponseEntity.ok(remoteApps);
        } catch (Exception e) {
            log.error("Error fetching remote apps for user {}", userEmail, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Health check endpoint
     */
    @Operation(summary = "Discovery health check",
               description = "Returns the health status of the discovery service including the number of active apps and sessions from the last hour.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Service is healthy"),
            @ApiResponse(responseCode = "500", description = "Service is unhealthy or an error occurred")
    })
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            int activeApps = appRepository.findByActiveTrue().size();
            long activeSessions = sessionRepository
                .findBySessionActiveTrueAndLastActivityAtAfter(LocalDateTime.now().minusHours(1))
                .size();
            
            return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "activeApps", activeApps,
                "activeSessions", activeSessions,
                "timestamp", LocalDateTime.now()
            ));
        } catch (Exception e) {
            log.error("Error in discovery health check", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}