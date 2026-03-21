/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.discovery.service;

import ch.plaintext.boot.plugins.security.PlaintextSecurityHolder;
import ch.plaintext.discovery.config.DiscoveryProperties;
import ch.plaintext.discovery.dto.*;
import ch.plaintext.discovery.entity.DiscoveryApp;
import ch.plaintext.discovery.entity.DiscoveryUserSession;
import ch.plaintext.discovery.repository.DiscoveryAppRepository;
import ch.plaintext.discovery.repository.DiscoveryUserSessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core discovery service for multi-instance navigation
 */
@ConditionalOnProperty(value = "discovery.enabled", havingValue = "true", matchIfMissing = false)
@Service
@Slf4j
public class DiscoveryService {

    @Value("${discovery.app.id:${spring.application.name:plaintext}}")
    private String appId;

    @Value("${discovery.app.name:Plaintext App}")
    private String appName;

    @Value("${discovery.app.environment:dev}")
    private String environment;

    @Value("${discovery.app.url:http://localhost:${server.port:8080}}")
    private String appUrl;

    private final DiscoveryAppRepository appRepository;
    private final DiscoveryUserSessionRepository sessionRepository;
    private final DiscoveryMqttService mqttService;
    private final DiscoveryEncryptionService encryptionService;
    private final DiscoveryProperties properties;
    private final UserDetailsService userDetailsService;

    private final Map<String, LoginUrlEntry> remoteLoginUrls = new ConcurrentHashMap<>();

    public DiscoveryService(DiscoveryAppRepository appRepository,
                           DiscoveryUserSessionRepository sessionRepository,
                           @Lazy DiscoveryMqttService mqttService,
                           DiscoveryEncryptionService encryptionService,
                           DiscoveryProperties properties,
                           @Lazy UserDetailsService userDetailsService) {
        this.appRepository = appRepository;
        this.sessionRepository = sessionRepository;
        this.mqttService = mqttService;
        this.encryptionService = encryptionService;
        this.properties = properties;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Announce that a user has logged in
     */
    public void announceUserLogin(String userEmail, Long userId, String userName) {
        try {
            UserLoginMessage message = new UserLoginMessage();
            message.setFromAppName(appName);
            message.setUserEmail(userEmail);
            message.setUserId(userId);
            message.setUserName(userName);
            message.setAppUrl(getBaseUrl());
            message.setAppEnvironment(environment);
            message.setPublicKey(encryptionService.getPublicKeyString());

            mqttService.publishMessage("plaintext/discovery", message);

            recordLocalUserSession(userEmail, userId, userName);

            log.info("Announced user login: {} ({})", userEmail, userName);

        } catch (Exception e) {
            log.error("Error announcing user login", e);
        }
    }

    @Transactional
    public void recordLocalUserSession(String userEmail, Long userId, String userName) {
        DiscoveryApp thisApp = findOrCreateApp(appId, appName, getBaseUrl(), environment,
            encryptionService.getPublicKeyString());

        Optional<DiscoveryUserSession> existingSession =
            sessionRepository.findByAppAndUserEmailAndSessionActiveTrue(thisApp, userEmail);

        DiscoveryUserSession session = existingSession.orElse(new DiscoveryUserSession());
        session.setApp(thisApp);
        session.setUserEmail(userEmail);
        session.setUserId(userId);
        session.setUserName(userName);
        session.setLoggedInAt(existingSession.isEmpty() ? LocalDateTime.now() : session.getLoggedInAt());
        session.setLastActivityAt(LocalDateTime.now());
        session.setSessionActive(true);
        try {
            session.setMandat(PlaintextSecurityHolder.getMandat());
        } catch (Exception e) {
            // No security context available (e.g. MQTT handler thread)
        }

        sessionRepository.save(session);
    }

    @Transactional
    public void registerOrUpdateApp(String remoteAppId, String remoteAppName, String appUrl,
                                  String environment, String publicKey) {
        DiscoveryApp app = findOrCreateApp(remoteAppId, remoteAppName, appUrl, environment, publicKey);
        app.setLastSeenAt(LocalDateTime.now());
        app.setActive(true);
        appRepository.save(app);

        log.debug("Registered/updated app: {} ({})", remoteAppId, remoteAppName);
    }

    private DiscoveryApp findOrCreateApp(String appId, String appName, String appUrl,
                                       String environment, String publicKey) {
        return appRepository.findByAppId(appId)
            .map(app -> {
                app.setAppName(appName);
                app.setAppUrl(appUrl);
                app.setEnvironment(DiscoveryApp.AppEnvironment.valueOf(environment.toUpperCase()));
                app.setPublicKey(publicKey);
                app.setLastSeenAt(LocalDateTime.now());
                app.setActive(true);
                return app;
            })
            .orElseGet(() -> {
                DiscoveryApp newApp = new DiscoveryApp();
                newApp.setAppId(appId);
                newApp.setAppName(appName);
                newApp.setAppUrl(appUrl);
                newApp.setEnvironment(DiscoveryApp.AppEnvironment.valueOf(environment.toUpperCase()));
                newApp.setPublicKey(publicKey);
                newApp.setLastSeenAt(LocalDateTime.now());
                newApp.setActive(true);
                try {
                    newApp.setMandat(PlaintextSecurityHolder.getMandat());
                } catch (Exception e) {
                    // No security context (MQTT handler thread)
                }
                return appRepository.save(newApp);
            });
    }

    /**
     * Check if a user is known in our local system (username = email)
     */
    public boolean isUserKnownLocally(String userEmail) {
        try {
            userDetailsService.loadUserByUsername(userEmail);
            return true;
        } catch (UsernameNotFoundException e) {
            return false;
        } catch (Exception e) {
            log.warn("Error checking if user {} is known locally", userEmail, e);
            return false;
        }
    }

    /**
     * Send response that we know a user
     */
    public void sendAppResponse(UserLoginMessage originalMessage) {
        AppResponseMessage response = new AppResponseMessage();
        response.setFromAppName(appName);
        response.setTargetAppId(originalMessage.getFromAppId());
        response.setInResponseToMessageId(originalMessage.getMessageId());
        response.setUserEmail(originalMessage.getUserEmail());
        response.setUserKnown(true);
        response.setAppUrl(getBaseUrl());
        response.setAppDisplayName(appName);
        response.setAppEnvironment(environment);

        mqttService.publishMessage("plaintext/response/" + originalMessage.getFromAppId(), response);

        log.info("Sent app response to {} for user {}", originalMessage.getFromAppId(),
            originalMessage.getUserEmail());
    }

    @Transactional
    public void registerUserInRemoteApp(AppResponseMessage message) {
        DiscoveryApp remoteApp = findOrCreateApp(message.getFromAppId(), message.getFromAppName(),
            message.getAppUrl(), message.getAppEnvironment(), null);

        Optional<DiscoveryUserSession> existingSession =
            sessionRepository.findByAppAndUserEmailAndSessionActiveTrue(remoteApp, message.getUserEmail());

        DiscoveryUserSession session = existingSession.orElse(new DiscoveryUserSession());
        session.setApp(remoteApp);
        session.setUserEmail(message.getUserEmail());
        session.setLoggedInAt(existingSession.isEmpty() ? LocalDateTime.now() : session.getLoggedInAt());
        session.setLastActivityAt(LocalDateTime.now());
        session.setSessionActive(true);
        // Don't call getMandat() - this runs on MQTT handler thread without SecurityContext

        sessionRepository.save(session);

        log.info("Registered user {} in remote app {}", message.getUserEmail(), message.getFromAppId());
    }

    public List<DiscoveryUserSession> getRemoteAppsForUser(String userEmail) {
        List<DiscoveryUserSession> sessions = sessionRepository.findByUserEmailAndSessionActiveTrue(userEmail);
        return sessions.stream()
            .filter(session -> !appId.equals(session.getApp().getAppId()))
            .toList();
    }

    /**
     * Generate a temporary login token and persist it
     */
    @Transactional
    public String generateLoginToken(String userEmail) {
        if (!isUserKnownLocally(userEmail)) {
            log.warn("Cannot generate token for unknown user: {}", userEmail);
            return null;
        }

        String token = UUID.randomUUID().toString();
        int validitySeconds = properties.getToken().getValiditySeconds();

        Optional<DiscoveryApp> localApp = appRepository.findByAppId(appId);
        if (localApp.isEmpty()) {
            DiscoveryApp app = findOrCreateApp(appId, appName, getBaseUrl(), environment,
                encryptionService.getPublicKeyString());
            appRepository.save(app);
            localApp = Optional.of(app);
        }

        Optional<DiscoveryUserSession> sessionOpt =
            sessionRepository.findByAppAndUserEmailAndSessionActiveTrue(localApp.get(), userEmail);

        DiscoveryUserSession session = sessionOpt.orElse(new DiscoveryUserSession());
        session.setApp(localApp.get());
        session.setUserEmail(userEmail);
        session.setLoggedInAt(sessionOpt.isEmpty() ? LocalDateTime.now() : session.getLoggedInAt());
        session.setLastActivityAt(LocalDateTime.now());
        session.setSessionActive(true);
        session.setLoginToken(token);
        session.setTokenExpiresAt(LocalDateTime.now().plusSeconds(validitySeconds));
        session.setTokenUsed(false);

        sessionRepository.save(session);

        log.info("Generated login token for user {} (valid for {}s)", userEmail, validitySeconds);
        return token;
    }

    /**
     * Send login token response with PKI encryption
     */
    public void sendLoginTokenResponse(LoginTokenRequestMessage request, String token) {
        LoginTokenResponseMessage response = new LoginTokenResponseMessage();
        response.setFromAppName(appName);
        response.setTargetAppId(request.getFromAppId());
        response.setInResponseToMessageId(request.getMessageId());
        response.setUserEmail(request.getUserEmail());
        response.setLoginUrl(getBaseUrl() + "/discovery/login?token=" + token);
        response.setTokenValidForSeconds(properties.getToken().getValiditySeconds());

        // Encrypt token with requesting app's public key
        DiscoveryApp requestingApp = appRepository.findByAppId(request.getFromAppId()).orElse(null);
        if (requestingApp != null && requestingApp.getPublicKey() != null && properties.getToken().isEncryptionEnabled()) {
            try {
                response.setEncryptedToken(encryptionService.encrypt(token, requestingApp.getPublicKey()));
            } catch (Exception e) {
                log.warn("Encryption failed for app {}, sending token unencrypted", request.getFromAppId());
                response.setEncryptedToken(token);
            }
        } else {
            response.setEncryptedToken(token);
        }

        mqttService.publishMessage("plaintext/login/" + request.getFromAppId(), response);

        log.info("Sent login token response to {} for user {}", request.getFromAppId(), request.getUserEmail());
    }

    /**
     * Store remote login URL for UI consumption (consume-once)
     */
    public void storeRemoteLoginUrl(LoginTokenResponseMessage message) {
        LocalDateTime expiry = LocalDateTime.now().plusSeconds(message.getTokenValidForSeconds());
        remoteLoginUrls.put(message.getUserEmail(), new LoginUrlEntry(message.getLoginUrl(), expiry));
        log.info("Stored login URL for user {}: {}", message.getUserEmail(), message.getLoginUrl());
    }

    /**
     * Get stored remote login URL (consume-once, returns null if expired or absent)
     */
    public String getRemoteLoginUrl(String userEmail) {
        LoginUrlEntry entry = remoteLoginUrls.remove(userEmail);
        if (entry != null && entry.expiresAt.isAfter(LocalDateTime.now())) {
            return entry.loginUrl;
        }
        return null;
    }

    @Transactional
    public void updateAppHeartbeat(String appId, String appName, String appUrl, String environment,
                                 int activeUsers, String publicKey, List<String> activeUserEmails) {
        registerOrUpdateApp(appId, appName, appUrl, environment, publicKey);

        // Sync user sessions from heartbeat
        if (activeUserEmails != null && !activeUserEmails.isEmpty()) {
            DiscoveryApp remoteApp = appRepository.findByAppId(appId).orElse(null);
            if (remoteApp != null) {
                for (String email : activeUserEmails) {
                    Optional<DiscoveryUserSession> existing =
                        sessionRepository.findByAppAndUserEmailAndSessionActiveTrue(remoteApp, email);
                    DiscoveryUserSession session = existing.orElse(new DiscoveryUserSession());
                    session.setApp(remoteApp);
                    session.setUserEmail(email);
                    session.setLoggedInAt(existing.isEmpty() ? LocalDateTime.now() : session.getLoggedInAt());
                    session.setLastActivityAt(LocalDateTime.now());
                    session.setSessionActive(true);
                    sessionRepository.save(session);
                }
                // Deactivate sessions for users no longer active on that app
                var allActive = sessionRepository.findByAppAndSessionActiveTrue(remoteApp);
                for (var session : allActive) {
                    if (!activeUserEmails.contains(session.getUserEmail())) {
                        session.setSessionActive(false);
                        sessionRepository.save(session);
                    }
                }
            }
        }
    }

    public List<DiscoveryApp> findStaleApps(LocalDateTime before) {
        return appRepository.findStaleApps(before);
    }

    public List<DiscoveryApp> findAppsNotSeenSince(LocalDateTime before) {
        return appRepository.findAppsNotSeenSince(before);
    }

    @Transactional
    public void deactivateApp(DiscoveryApp app) {
        app.setActive(false);
        appRepository.save(app);
        // Also deactivate all sessions for this app
        var sessions = sessionRepository.findByAppAndSessionActiveTrue(app);
        for (var session : sessions) {
            session.setSessionActive(false);
            sessionRepository.save(session);
        }
    }

    @Transactional
    public void deleteApp(DiscoveryApp app) {
        // Delete all sessions for this app first
        var sessions = sessionRepository.findByApp(app);
        sessionRepository.deleteAll(sessions);
        appRepository.delete(app);
        log.info("Deleted stale app {} ({})", app.getAppName(), app.getAppId());
    }

    public void requestCrossAppLogin(LoginTokenRequestMessage request) {
        try {
            mqttService.publishMessage("plaintext/login/" + request.getTargetAppId(), request);
            log.info("Sent login token request to app: {}", request.getTargetAppId());
        } catch (Exception e) {
            log.error("Error sending login token request", e);
        }
    }

    /**
     * Update app URL from the actual browser request (only if not explicitly configured via env var)
     */
    public void updateAppUrlFromRequest(String requestBaseUrl) {
        if (requestBaseUrl != null && !requestBaseUrl.isBlank() && appUrl.contains("localhost")) {
            log.info("Updating app URL from request: {} -> {}", appUrl, requestBaseUrl);
            this.appUrl = requestBaseUrl;
        }
    }

    public String getBaseUrl() {
        return appUrl;
    }

    private record LoginUrlEntry(String loginUrl, LocalDateTime expiresAt) {}
}
