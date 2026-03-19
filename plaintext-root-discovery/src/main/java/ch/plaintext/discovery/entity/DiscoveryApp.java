/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.discovery.entity;

import ch.plaintext.framework.SuperModel;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Represents a discovered remote application instance
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "discovery_app")
public class DiscoveryApp extends SuperModel {

    @Column(name = "app_id", length = 100, nullable = false)
    private String appId; // Unique identifier for this app instance
    
    @Column(name = "app_name", length = 200, nullable = false) 
    private String appName; // Human readable name (e.g. "Trimstein Prod", "BIT Dev")
    
    @Column(name = "app_url", length = 500, nullable = false)
    private String appUrl; // Base URL for this app
    
    @Enumerated(EnumType.STRING)
    @Column(name = "environment", length = 20, nullable = false)
    private AppEnvironment environment; // prod, dev, int, test
    
    @Column(name = "public_key", columnDefinition = "TEXT")
    private String publicKey; // RSA public key for encryption
    
    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt; // Last heartbeat/activity
    
    @Column(name = "version", length = 50)
    private String version; // App version
    
    @Column(name = "active", nullable = false)
    private Boolean active = true; // Still responding to heartbeats
    
    public enum AppEnvironment {
        PROD, DEV, INT, TEST
    }
}