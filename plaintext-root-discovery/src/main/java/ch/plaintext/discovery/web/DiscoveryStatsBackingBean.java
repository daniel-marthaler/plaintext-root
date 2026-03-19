/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.discovery.web;

import ch.plaintext.discovery.entity.DiscoveryApp;
import ch.plaintext.discovery.entity.DiscoveryUserSession;
import ch.plaintext.discovery.repository.DiscoveryAppRepository;
import ch.plaintext.discovery.repository.DiscoveryUserSessionRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Backing bean for Discovery Statistics page (ROOT only)
 */
@Component("discoveryStatsBackingBean")
@Scope("view")
@Data
@Slf4j
@RequiredArgsConstructor
public class DiscoveryStatsBackingBean {

    private final DiscoveryAppRepository appRepository;
    private final DiscoveryUserSessionRepository sessionRepository;

    private List<DiscoveryApp> allApps;
    private List<UserRow> userRows;

    private int totalApps;
    private int totalActiveApps;
    private int totalSessions;
    private int totalActiveSessions;
    private int uniqueActiveUsers;

    @PostConstruct
    public void init() {
        loadStatistics();
    }

    public void loadStatistics() {
        try {
            allApps = appRepository.findAll();

            List<DiscoveryUserSession> activeSessions = sessionRepository.findBySessionActiveTrueAndLastActivityAtAfter(
                LocalDateTime.now().minusHours(24));

            // Group sessions by user email
            Map<String, List<DiscoveryUserSession>> byUser = activeSessions.stream()
                .collect(Collectors.groupingBy(DiscoveryUserSession::getUserEmail));

            userRows = byUser.entrySet().stream()
                .map(e -> {
                    UserRow row = new UserRow();
                    row.setUserEmail(e.getKey());
                    // Get userName from any session that has it
                    row.setUserName(e.getValue().stream()
                        .map(DiscoveryUserSession::getUserName)
                        .filter(n -> n != null && !n.isBlank())
                        .findFirst()
                        .orElse(null));
                    row.setSessions(e.getValue());
                    row.setLastActivity(e.getValue().stream()
                        .map(DiscoveryUserSession::getLastActivityAt)
                        .max(LocalDateTime::compareTo)
                        .orElse(null));
                    row.setActive(e.getValue().stream().anyMatch(s -> Boolean.TRUE.equals(s.getSessionActive())));
                    return row;
                })
                .sorted(Comparator.comparing(UserRow::getLastActivity, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

            // Calculate statistics
            totalApps = allApps.size();
            totalActiveApps = (int) allApps.stream().filter(a -> Boolean.TRUE.equals(a.getActive())).count();
            totalSessions = activeSessions.size();
            totalActiveSessions = (int) activeSessions.stream().filter(s -> Boolean.TRUE.equals(s.getSessionActive())).count();
            uniqueActiveUsers = userRows.size();

            log.info("Discovery statistics loaded: {} apps ({} active), {} sessions, {} unique users",
                totalApps, totalActiveApps, totalSessions, uniqueActiveUsers);

        } catch (Exception e) {
            log.error("Error loading discovery statistics", e);
        }
    }

    public String getAppEnvironmentCss(DiscoveryApp.AppEnvironment environment) {
        return switch (environment) {
            case PROD -> "background-color: #28a745; color: white;";
            case DEV -> "background-color: #17a2b8; color: white;";
            case INT -> "background-color: #ffc107; color: black;";
            case TEST -> "background-color: #6c757d; color: white;";
        };
    }

    public String formatLastSeen(LocalDateTime lastSeen) {
        if (lastSeen == null) return "Never";

        LocalDateTime now = LocalDateTime.now();
        long hoursAgo = java.time.Duration.between(lastSeen, now).toHours();

        if (hoursAgo < 1) {
            long minutesAgo = java.time.Duration.between(lastSeen, now).toMinutes();
            return minutesAgo + " min ago";
        } else if (hoursAgo < 24) {
            return hoursAgo + " hours ago";
        } else {
            long daysAgo = java.time.Duration.between(lastSeen, now).toDays();
            return daysAgo + " days ago";
        }
    }

    /**
     * One row per unique user, with all their sessions/apps
     */
    @Data
    public static class UserRow {
        private String userEmail;
        private String userName;
        private List<DiscoveryUserSession> sessions;
        private LocalDateTime lastActivity;
        private boolean active;

        public String getDisplayName() {
            return userName != null && !userName.isBlank() ? userName : userEmail;
        }
    }
}
