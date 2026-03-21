/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.discovery.web;

import ch.plaintext.discovery.repository.DiscoveryAppRepository;
import ch.plaintext.discovery.repository.DiscoveryUserSessionRepository;
import ch.plaintext.discovery.service.DiscoveryEncryptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Public status endpoint for discovery debugging (no auth required, under /nosec/).
 */
@ConditionalOnProperty(value = "discovery.enabled", havingValue = "true", matchIfMissing = false)
@RestController
@RequestMapping("/nosec/discovery")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Discovery Status", description = "Public discovery status and diagnostics endpoint (no authentication required)")
public class DiscoveryStatusController {

    private final DiscoveryAppRepository appRepository;
    private final DiscoveryUserSessionRepository sessionRepository;
    private final DiscoveryEncryptionService encryptionService;

    @Value("${discovery.app.id:${spring.application.name:plaintext}}")
    private String appId;

    @Value("${discovery.app.name:Plaintext App}")
    private String appName;

    @Operation(summary = "Get discovery status",
               description = "Returns a diagnostic overview of the discovery service including this app's identity, "
                           + "known peer applications, active session count, and an encryption self-test result. "
                           + "This endpoint is publicly accessible without authentication.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status information returned successfully")
    })
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("appId", appId);
        result.put("appName", appName);
        result.put("publicKeyHash", hashPublicKey(encryptionService.getPublicKeyString()));
        result.put("timestamp", LocalDateTime.now());

        var apps = appRepository.findByActiveTrue();
        result.put("knownApps", apps.stream().map(app -> {
            Map<String, Object> appInfo = new LinkedHashMap<>();
            appInfo.put("appId", app.getAppId());
            appInfo.put("appName", app.getAppName());
            appInfo.put("appUrl", app.getAppUrl());
            appInfo.put("environment", app.getEnvironment());
            appInfo.put("publicKeyHash", hashPublicKey(app.getPublicKey()));
            appInfo.put("lastSeenAt", app.getLastSeenAt());
            return appInfo;
        }).toList());

        var sessions = sessionRepository.findBySessionActiveTrueAndLastActivityAtAfter(
            LocalDateTime.now().minusHours(1));
        result.put("activeSessions", sessions.size());

        // Self-test: encrypt/decrypt roundtrip
        try {
            String testPayload = "test|" + System.currentTimeMillis() + "|" + appId + "|nonce";
            String encrypted = encryptionService.createDiscoveryToken("test@test.com", appId,
                encryptionService.getPublicKeyString());
            var decrypted = encryptionService.decryptDiscoveryToken(encrypted);
            result.put("selfTestEncryption", decrypted != null ? "OK" : "FAILED");
        } catch (Exception e) {
            result.put("selfTestEncryption", "ERROR: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    private String hashPublicKey(String publicKey) {
        if (publicKey == null) return "null";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(publicKey.getBytes());
            return Base64.getEncoder().encodeToString(hash).substring(0, 12);
        } catch (Exception e) {
            return "error";
        }
    }
}
