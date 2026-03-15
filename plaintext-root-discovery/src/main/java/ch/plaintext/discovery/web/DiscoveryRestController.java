package ch.plaintext.discovery.web;

import ch.plaintext.discovery.dto.LoginTokenRequestMessage;
import ch.plaintext.discovery.dto.UserLoginMessage;
import ch.plaintext.discovery.entity.DiscoveryApp;
import ch.plaintext.discovery.entity.DiscoveryUserSession;
import ch.plaintext.discovery.service.DiscoveryService;
import ch.plaintext.discovery.repository.DiscoveryAppRepository;
import ch.plaintext.discovery.repository.DiscoveryUserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST API for Discovery service
 */
@RestController
@RequestMapping("/api/discovery")
@Slf4j
@RequiredArgsConstructor
public class DiscoveryRestController {
    
    private final DiscoveryService discoveryService;
    private final DiscoveryAppRepository appRepository;
    private final DiscoveryUserSessionRepository sessionRepository;
    
    /**
     * Announce user login (alternative to MQTT)
     */
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
    @GetMapping("/user/{userEmail}/apps")
    public ResponseEntity<List<DiscoveryUserSession>> getRemoteAppsForUser(
            @PathVariable String userEmail) {
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