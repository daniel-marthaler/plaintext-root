/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.discovery.web;

import ch.plaintext.discovery.entity.DiscoveryApp;
import ch.plaintext.discovery.entity.DiscoveryUserSession;
import ch.plaintext.discovery.repository.DiscoveryAppRepository;
import ch.plaintext.discovery.repository.DiscoveryUserSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscoveryStatsBackingBeanTest {

    @Mock
    private DiscoveryAppRepository appRepository;

    @Mock
    private DiscoveryUserSessionRepository sessionRepository;

    private DiscoveryStatsBackingBean backingBean;

    @BeforeEach
    void setUp() {
        backingBean = new DiscoveryStatsBackingBean(appRepository, sessionRepository);
    }

    @Nested
    class LoadStatistics {

        @Test
        void loadsAppsAndSessions() {
            DiscoveryApp app1 = new DiscoveryApp();
            app1.setAppId("app1");
            app1.setActive(true);

            DiscoveryApp app2 = new DiscoveryApp();
            app2.setAppId("app2");
            app2.setActive(false);

            DiscoveryUserSession session1 = new DiscoveryUserSession();
            session1.setUserEmail("user@test.com");
            session1.setUserName("Test User");
            session1.setSessionActive(true);
            session1.setLastActivityAt(LocalDateTime.now().minusMinutes(5));
            session1.setApp(app1);

            when(appRepository.findAll()).thenReturn(List.of(app1, app2));
            when(sessionRepository.findBySessionActiveTrueAndLastActivityAtAfter(any()))
                .thenReturn(List.of(session1));

            backingBean.loadStatistics();

            assertEquals(2, backingBean.getTotalApps());
            assertEquals(1, backingBean.getTotalActiveApps());
            assertEquals(1, backingBean.getTotalSessions());
            assertEquals(1, backingBean.getTotalActiveSessions());
            assertEquals(1, backingBean.getUniqueActiveUsers());
        }

        @Test
        void groupsSessionsByUser() {
            DiscoveryApp app1 = new DiscoveryApp();
            app1.setAppId("app1");

            DiscoveryApp app2 = new DiscoveryApp();
            app2.setAppId("app2");

            DiscoveryUserSession session1 = new DiscoveryUserSession();
            session1.setUserEmail("user@test.com");
            session1.setUserName("Test User");
            session1.setSessionActive(true);
            session1.setLastActivityAt(LocalDateTime.now().minusMinutes(5));
            session1.setApp(app1);

            DiscoveryUserSession session2 = new DiscoveryUserSession();
            session2.setUserEmail("user@test.com");
            session2.setSessionActive(true);
            session2.setLastActivityAt(LocalDateTime.now().minusMinutes(10));
            session2.setApp(app2);

            when(appRepository.findAll()).thenReturn(List.of(app1, app2));
            when(sessionRepository.findBySessionActiveTrueAndLastActivityAtAfter(any()))
                .thenReturn(List.of(session1, session2));

            backingBean.loadStatistics();

            // Both sessions are for the same user
            assertEquals(1, backingBean.getUniqueActiveUsers());
            assertEquals(2, backingBean.getUserRows().get(0).getSessions().size());
        }

        @Test
        void handlesEmptyData() {
            when(appRepository.findAll()).thenReturn(List.of());
            when(sessionRepository.findBySessionActiveTrueAndLastActivityAtAfter(any()))
                .thenReturn(List.of());

            backingBean.loadStatistics();

            assertEquals(0, backingBean.getTotalApps());
            assertEquals(0, backingBean.getTotalActiveApps());
            assertEquals(0, backingBean.getTotalSessions());
            assertEquals(0, backingBean.getUniqueActiveUsers());
            assertTrue(backingBean.getUserRows().isEmpty());
        }

        @Test
        void handlesExceptionGracefully() {
            when(appRepository.findAll()).thenThrow(new RuntimeException("DB error"));

            // Should not throw
            backingBean.loadStatistics();
        }
    }

    @Nested
    class EnvironmentCss {

        @Test
        void prodGetsGreenBackground() {
            String css = backingBean.getAppEnvironmentCss(DiscoveryApp.AppEnvironment.PROD);
            assertTrue(css.contains("#28a745"));
        }

        @Test
        void devGetsCyanBackground() {
            String css = backingBean.getAppEnvironmentCss(DiscoveryApp.AppEnvironment.DEV);
            assertTrue(css.contains("#17a2b8"));
        }

        @Test
        void intGetsYellowBackground() {
            String css = backingBean.getAppEnvironmentCss(DiscoveryApp.AppEnvironment.INT);
            assertTrue(css.contains("#ffc107"));
        }

        @Test
        void testGetsGrayBackground() {
            String css = backingBean.getAppEnvironmentCss(DiscoveryApp.AppEnvironment.TEST);
            assertTrue(css.contains("#6c757d"));
        }
    }

    @Nested
    class FormatLastSeen {

        @Test
        void returnsNeverForNull() {
            assertEquals("Never", backingBean.formatLastSeen(null));
        }

        @Test
        void returnsMinutesAgoForRecentTime() {
            LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);
            String result = backingBean.formatLastSeen(tenMinutesAgo);
            assertTrue(result.endsWith("min ago"));
        }

        @Test
        void returnsHoursAgoForOlderTime() {
            LocalDateTime threeHoursAgo = LocalDateTime.now().minusHours(3);
            String result = backingBean.formatLastSeen(threeHoursAgo);
            assertTrue(result.endsWith("hours ago"));
        }

        @Test
        void returnsDaysAgoForVeryOldTime() {
            LocalDateTime twoDaysAgo = LocalDateTime.now().minusDays(2);
            String result = backingBean.formatLastSeen(twoDaysAgo);
            assertTrue(result.endsWith("days ago"));
        }
    }

    @Nested
    class UserRow {

        @Test
        void getDisplayNameReturnsUserNameWhenSet() {
            DiscoveryStatsBackingBean.UserRow row = new DiscoveryStatsBackingBean.UserRow();
            row.setUserName("John Doe");
            row.setUserEmail("john@test.com");

            assertEquals("John Doe", row.getDisplayName());
        }

        @Test
        void getDisplayNameReturnsEmailWhenNameIsNull() {
            DiscoveryStatsBackingBean.UserRow row = new DiscoveryStatsBackingBean.UserRow();
            row.setUserName(null);
            row.setUserEmail("john@test.com");

            assertEquals("john@test.com", row.getDisplayName());
        }

        @Test
        void getDisplayNameReturnsEmailWhenNameIsBlank() {
            DiscoveryStatsBackingBean.UserRow row = new DiscoveryStatsBackingBean.UserRow();
            row.setUserName("   ");
            row.setUserEmail("john@test.com");

            assertEquals("john@test.com", row.getDisplayName());
        }
    }
}
