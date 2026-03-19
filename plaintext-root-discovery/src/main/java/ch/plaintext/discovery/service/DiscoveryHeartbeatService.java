/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.discovery.service;

import ch.plaintext.discovery.dto.HeartbeatMessage;
import ch.plaintext.discovery.repository.DiscoveryUserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service that sends periodic heartbeat messages to announce this app's presence
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(value = "discovery.heartbeat.enabled", havingValue = "true", matchIfMissing = true)
public class DiscoveryHeartbeatService {
    
    @Value("${discovery.app.id:${spring.application.name:plaintext}}")
    private String appId;
    
    @Value("${discovery.app.name:Plaintext App}")
    private String appName;
    
    @Value("${discovery.app.environment:dev}")
    private String environment;
    
    @Value("${discovery.app.version:unknown}")
    private String version;
    
    @Lazy
    private final DiscoveryMqttService mqttService;
    @Lazy
    private final DiscoveryService discoveryService;
    private final DiscoveryUserSessionRepository sessionRepository;
    private final DiscoveryEncryptionService encryptionService;
    
    /**
     * Send heartbeat every 2 minutes
     */
    @Scheduled(fixedRate = 120000) // 2 minutes
    @Transactional(readOnly = true)
    public void sendHeartbeat() {
        try {
            List<String> activeEmails = getActiveUserEmails();

            HeartbeatMessage heartbeat = new HeartbeatMessage();
            heartbeat.setFromAppName(appName);
            heartbeat.setAppUrl(getBaseUrl());
            heartbeat.setAppVersion(version);
            heartbeat.setAppEnvironment(environment);
            heartbeat.setActiveUserCount(activeEmails.size());
            heartbeat.setActiveUserEmails(activeEmails);
            heartbeat.setPublicKey(encryptionService.getPublicKeyString());
            
            mqttService.publishMessage("plaintext/heartbeat", heartbeat);
            
            log.debug("Sent heartbeat: {} active users, environment: {}", activeEmails.size(), environment);
            
        } catch (Exception e) {
            log.error("Error sending heartbeat", e);
        }
    }
    
    /**
     * Clean up stale apps and expired tokens every 10 minutes
     */
    @Scheduled(fixedRate = 600000) // 10 minutes
    public void cleanup() {
        try {
            cleanupStaleApps();
            cleanupExpiredTokens();
            cleanupStaleUserSessions();
            log.debug("Discovery cleanup completed");
        } catch (Exception e) {
            log.error("Error during discovery cleanup", e);
        }
    }

    private List<String> getActiveUserEmails() {
        try {
            LocalDateTime since = LocalDateTime.now().minusMinutes(30);
            return sessionRepository.findBySessionActiveTrueAndLastActivityAtAfter(since)
                .stream()
                .filter(s -> s.getApp() != null && appId.equals(s.getApp().getAppId()))
                .map(s -> s.getUserEmail())
                .distinct()
                .toList();
        } catch (Exception e) {
            log.warn("Error getting active user emails", e);
            return List.of();
        }
    }

    private void cleanupStaleApps() {
        try {
            // Mark apps as inactive if no heartbeat for 5 minutes
            LocalDateTime deactivateCutoff = LocalDateTime.now().minusMinutes(5);
            var staleApps = discoveryService.findStaleApps(deactivateCutoff);
            for (var app : staleApps) {
                discoveryService.deactivateApp(app);
            }
            if (!staleApps.isEmpty()) {
                log.info("Deactivated {} stale apps (no heartbeat for 5+ min)", staleApps.size());
            }

            // Delete apps completely if no heartbeat for 3 hours
            LocalDateTime deleteCutoff = LocalDateTime.now().minusHours(3);
            var deadApps = discoveryService.findAppsNotSeenSince(deleteCutoff);
            for (var app : deadApps) {
                discoveryService.deleteApp(app);
            }
            if (!deadApps.isEmpty()) {
                log.info("Deleted {} dead apps (no heartbeat for 3+ hours)", deadApps.size());
            }
        } catch (Exception e) {
            log.error("Error cleaning up stale apps", e);
        }
    }
    
    private void cleanupExpiredTokens() {
        try {
            var expiredTokens = sessionRepository.findExpiredUnusedTokens(LocalDateTime.now());
            for (var session : expiredTokens) {
                session.setTokenUsed(true); // Mark as expired
                sessionRepository.save(session);
            }
            if (!expiredTokens.isEmpty()) {
                log.info("Cleaned up {} expired discovery tokens", expiredTokens.size());
            }
        } catch (Exception e) {
            log.error("Error cleaning up expired tokens", e);
        }
    }
    
    private void cleanupStaleUserSessions() {
        try {
            // Mark sessions as inactive if no activity for 6 hours
            LocalDateTime cutoff = LocalDateTime.now().minusHours(6);
            var staleSessions = sessionRepository.findBySessionActiveTrueAndLastActivityAtAfter(cutoff);
            
            int cleanedUp = 0;
            for (var session : staleSessions) {
                if (session.getLastActivityAt().isBefore(cutoff)) {
                    session.setSessionActive(false);
                    sessionRepository.save(session);
                    cleanedUp++;
                }
            }
            
            if (cleanedUp > 0) {
                log.info("Marked {} stale user sessions as inactive", cleanedUp);
            }
        } catch (Exception e) {
            log.error("Error cleaning up stale sessions", e);
        }
    }
    
    private String getBaseUrl() {
        return discoveryService.getBaseUrl();
    }
}