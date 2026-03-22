/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.boot.web;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component("versionBean")
@Data
@Scope("application")
@ConditionalOnWebApplication
@Slf4j
public class VersionBean {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Value("${plaintext.root.version:unknown}")
    private String rootVersion;

    private String version;
    private String buildTimestamp;

    @PostConstruct
    public void init() {
        // Determine which version file to use based on profile
        String versionFile = isProdProfile() ? "versionRelease.txt" : "version.txt";

        // Try to read version from file in project root
        try {
            Path versionPath = Paths.get(versionFile);
            if (Files.exists(versionPath)) {
                version = Files.readString(versionPath).trim();
                log.info("Version loaded from {}: {}", versionFile, version);
            } else {
                log.warn("Version file not found: {}, using default", versionFile);
                version = "dev-SNAPSHOT";
            }
        } catch (IOException e) {
            log.error("Error reading version from {}: {}", versionFile, e.getMessage());
            version = "dev-SNAPSHOT";
        }

        // Try to read build timestamp from file
        try {
            Path buildTimestampPath = Paths.get("buildTimestamp.txt");
            if (Files.exists(buildTimestampPath)) {
                buildTimestamp = Files.readString(buildTimestampPath).trim();
                log.info("Build timestamp loaded: {}", buildTimestamp);
            } else {
                // Use current timestamp as fallback
                buildTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yy HH:mm"));
                log.warn("Build timestamp file not found, using current time: {}", buildTimestamp);
            }
        } catch (IOException e) {
            log.error("Error reading build timestamp: {}", e.getMessage());
            buildTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yy HH:mm"));
        }
    }

    private boolean isProdProfile() {
        return activeProfile != null && activeProfile.contains("prod");
    }

    public String getVersion() {
        return version;
    }

    public String getBuildTimestamp() {
        return buildTimestamp;
    }

    public String getFullVersion() {
        if (buildTimestamp != null && !buildTimestamp.isEmpty()) {
            return version + " (Build: " + buildTimestamp + ")";
        }
        return version;
    }
}
