/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.discovery.web;

import ch.plaintext.boot.plugins.security.PlaintextSecurityHolder;
import ch.plaintext.discovery.config.DiscoveryContextHolder;
import ch.plaintext.discovery.entity.DiscoveryApp;
import ch.plaintext.discovery.entity.DiscoveryUserSession;
import ch.plaintext.discovery.repository.DiscoveryAppRepository;
import ch.plaintext.discovery.repository.DiscoveryUserSessionRepository;
import ch.plaintext.discovery.service.DiscoveryEncryptionService;
import ch.plaintext.discovery.service.DiscoveryService;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Backing bean for discovery integration in topbar (user menu).
 * Note: transient services may be null after session deserialization (blue-green deploy).
 * Use getXxx() accessors which re-resolve from ApplicationContext when needed.
 */
@Component("discoveryTopbarBackingBean")
@Scope("session")
@Data
@Slf4j
@RequiredArgsConstructor
public class DiscoveryTopbarBackingBean implements Serializable {

    private final transient DiscoveryService discoveryService;
    private final transient DiscoveryUserSessionRepository sessionRepository;
    private final transient DiscoveryAppRepository appRepository;
    private final transient DiscoveryEncryptionService encryptionService;

    @Value("${discovery.app.id:${spring.application.name:plaintext}}")
    private String appId;

    private List<RemoteAppItem> remoteApps;
    
    @PostConstruct
    public void init() {
        loadRemoteApps();
    }
    
    public void loadRemoteApps() {
        try {
            String userEmail = PlaintextSecurityHolder.getUser(); // Username/Email
            if (userEmail != null) {
                List<DiscoveryUserSession> sessions = discoveryService.getRemoteAppsForUser(userEmail);
                remoteApps = sessions.stream()
                    .map(this::convertToRemoteAppItem)
                    .toList();
                log.debug("Loaded {} remote apps for user {}", remoteApps.size(), userEmail);
            }
        } catch (Exception e) {
            log.error("Error loading remote apps", e);
        }
    }
    
    private RemoteAppItem convertToRemoteAppItem(DiscoveryUserSession session) {
        RemoteAppItem item = new RemoteAppItem();
        item.setAppId(session.getApp().getAppId());
        item.setAppName(session.getApp().getAppName());
        item.setAppUrl(session.getApp().getAppUrl());
        item.setEnvironment(session.getApp().getEnvironment().toString());
        item.setIcon(getIconForEnvironment(session.getApp().getEnvironment().toString()));
        return item;
    }

    private String getIconForEnvironment(String environment) {
        return switch (environment.toLowerCase()) {
            case "prod" -> "pi pi-server";
            case "dev" -> "pi pi-wrench";
            case "int" -> "pi pi-cog";
            case "test" -> "pi pi-verified";
            default -> "pi pi-globe";
        };
    }

    public boolean hasRemoteApps() {
        return remoteApps != null && !remoteApps.isEmpty();
    }

    /**
     * Generate a discovery login URL for a remote app.
     * Creates an RSA-encrypted token containing the user's email, timestamp, source app ID, and a nonce.
     * Only the target app can decrypt it (with its private key).
     */
    public String getLoginUrl(RemoteAppItem item) {
        try {
            String userEmail = PlaintextSecurityHolder.getUser();
            if (userEmail == null) {
                return item.getAppUrl();
            }

            // Re-resolve services if null (after session deserialization during blue-green deploy)
            DiscoveryAppRepository repo = resolveService(appRepository, DiscoveryAppRepository.class);
            DiscoveryEncryptionService encryption = resolveService(encryptionService, DiscoveryEncryptionService.class);
            if (repo == null || encryption == null) {
                log.warn("Discovery services unavailable (session deserialized?), using plain URL for {}", item.getAppName());
                return item.getAppUrl();
            }

            // Lookup target app's public key
            var targetApp = repo.findByAppId(item.getAppId()).orElse(null);
            if (targetApp == null) {
                log.warn("Discovery login: app {} not found in DB", item.getAppId());
                return item.getAppUrl();
            }
            if (targetApp.getPublicKey() == null) {
                log.warn("Discovery login: no public key for app {} ({})", item.getAppId(), item.getAppName());
                return item.getAppUrl();
            }

            String token = encryption.createDiscoveryToken(userEmail, appId, targetApp.getPublicKey());
            String url = item.getAppUrl() + "/discovery/login?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
            log.info("Discovery login URL generated for user {} -> app {} (token length: {})", userEmail, item.getAppId(), token.length());
            return url;

        } catch (Exception e) {
            log.warn("Error generating login URL for app {}: {}", item.getAppName(), e.getMessage());
            return item.getAppUrl();
        }
    }

    /**
     * Re-resolve a transient service from static ApplicationContext if it's null (after session deserialization).
     */
    private <T> T resolveService(T service, Class<T> clazz) {
        if (service != null) return service;
        return DiscoveryContextHolder.getBean(clazz);
    }

    @Data
    public static class RemoteAppItem implements Serializable {
        private String appId;
        private String appName;
        private String appUrl;
        private String environment;
        private String icon;
    }
}