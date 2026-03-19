/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.discovery.service;

import ch.plaintext.discovery.dto.DiscoveryMessage;
import ch.plaintext.discovery.dto.HeartbeatMessage;
import ch.plaintext.discovery.entity.DiscoveryApp;
import ch.plaintext.discovery.entity.DiscoveryUserSession;
import ch.plaintext.discovery.repository.DiscoveryUserSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscoveryHeartbeatServiceTest {

    @Mock
    private DiscoveryMqttService mqttService;

    @Mock
    private DiscoveryService discoveryService;

    @Mock
    private DiscoveryUserSessionRepository sessionRepository;

    @Mock
    private DiscoveryEncryptionService encryptionService;

    private DiscoveryHeartbeatService heartbeatService;

    @BeforeEach
    void setUp() {
        heartbeatService = new DiscoveryHeartbeatService(
            mqttService, discoveryService, sessionRepository, encryptionService);

        ReflectionTestUtils.setField(heartbeatService, "appId", "test-app");
        ReflectionTestUtils.setField(heartbeatService, "appName", "Test App");
        ReflectionTestUtils.setField(heartbeatService, "environment", "dev");
        ReflectionTestUtils.setField(heartbeatService, "version", "1.0.0");
    }

    @Nested
    class SendHeartbeat {

        @Test
        void publishesHeartbeatWithActiveUsers() {
            DiscoveryApp localApp = new DiscoveryApp();
            localApp.setAppId("test-app");

            DiscoveryUserSession session = new DiscoveryUserSession();
            session.setApp(localApp);
            session.setUserEmail("user@test.com");
            session.setSessionActive(true);
            session.setLastActivityAt(LocalDateTime.now().minusMinutes(5));

            when(sessionRepository.findBySessionActiveTrueAndLastActivityAtAfter(any()))
                .thenReturn(List.of(session));
            when(encryptionService.getPublicKeyString()).thenReturn("pub-key");
            when(discoveryService.getBaseUrl()).thenReturn("http://localhost:8080");

            heartbeatService.sendHeartbeat();

            ArgumentCaptor<DiscoveryMessage> captor = ArgumentCaptor.forClass(DiscoveryMessage.class);
            verify(mqttService).publishMessage(eq("plaintext/heartbeat"), captor.capture());

            HeartbeatMessage heartbeat = (HeartbeatMessage) captor.getValue();
            assertEquals("Test App", heartbeat.getFromAppName());
            assertEquals("http://localhost:8080", heartbeat.getAppUrl());
            assertEquals("1.0.0", heartbeat.getAppVersion());
            assertEquals("dev", heartbeat.getAppEnvironment());
            assertEquals("pub-key", heartbeat.getPublicKey());
            assertEquals(1, heartbeat.getActiveUserCount());
            assertTrue(heartbeat.getActiveUserEmails().contains("user@test.com"));
        }

        @Test
        void filtersSessionsToOnlyLocalApp() {
            DiscoveryApp localApp = new DiscoveryApp();
            localApp.setAppId("test-app");
            DiscoveryApp remoteApp = new DiscoveryApp();
            remoteApp.setAppId("other-app");

            DiscoveryUserSession localSession = new DiscoveryUserSession();
            localSession.setApp(localApp);
            localSession.setUserEmail("local@test.com");

            DiscoveryUserSession remoteSession = new DiscoveryUserSession();
            remoteSession.setApp(remoteApp);
            remoteSession.setUserEmail("remote@test.com");

            when(sessionRepository.findBySessionActiveTrueAndLastActivityAtAfter(any()))
                .thenReturn(List.of(localSession, remoteSession));
            when(encryptionService.getPublicKeyString()).thenReturn("key");
            when(discoveryService.getBaseUrl()).thenReturn("http://localhost:8080");

            heartbeatService.sendHeartbeat();

            ArgumentCaptor<DiscoveryMessage> captor = ArgumentCaptor.forClass(DiscoveryMessage.class);
            verify(mqttService).publishMessage(eq("plaintext/heartbeat"), captor.capture());

            HeartbeatMessage heartbeat = (HeartbeatMessage) captor.getValue();
            assertEquals(1, heartbeat.getActiveUserCount());
            assertEquals("local@test.com", heartbeat.getActiveUserEmails().get(0));
        }

        @Test
        void handlesExceptionGracefully() {
            when(sessionRepository.findBySessionActiveTrueAndLastActivityAtAfter(any()))
                .thenThrow(new RuntimeException("DB error"));

            // The method should handle the exception for getActiveUserEmails internally,
            // then continue with an empty list
            when(encryptionService.getPublicKeyString()).thenReturn("key");
            when(discoveryService.getBaseUrl()).thenReturn("http://localhost:8080");

            // Should not throw
            heartbeatService.sendHeartbeat();
        }

        @Test
        void deduplicatesUserEmails() {
            DiscoveryApp localApp = new DiscoveryApp();
            localApp.setAppId("test-app");

            DiscoveryUserSession session1 = new DiscoveryUserSession();
            session1.setApp(localApp);
            session1.setUserEmail("user@test.com");

            DiscoveryUserSession session2 = new DiscoveryUserSession();
            session2.setApp(localApp);
            session2.setUserEmail("user@test.com"); // Same email

            when(sessionRepository.findBySessionActiveTrueAndLastActivityAtAfter(any()))
                .thenReturn(List.of(session1, session2));
            when(encryptionService.getPublicKeyString()).thenReturn("key");
            when(discoveryService.getBaseUrl()).thenReturn("http://localhost:8080");

            heartbeatService.sendHeartbeat();

            ArgumentCaptor<DiscoveryMessage> captor = ArgumentCaptor.forClass(DiscoveryMessage.class);
            verify(mqttService).publishMessage(eq("plaintext/heartbeat"), captor.capture());

            HeartbeatMessage heartbeat = (HeartbeatMessage) captor.getValue();
            assertEquals(1, heartbeat.getActiveUserCount());
        }
    }

    @Nested
    class Cleanup {

        @Test
        void deactivatesStaleApps() {
            DiscoveryApp staleApp = new DiscoveryApp();
            staleApp.setAppId("stale-app");
            when(discoveryService.findStaleApps(any())).thenReturn(List.of(staleApp));
            when(discoveryService.findAppsNotSeenSince(any())).thenReturn(List.of());
            when(sessionRepository.findExpiredUnusedTokens(any())).thenReturn(List.of());
            when(sessionRepository.findBySessionActiveTrueAndLastActivityAtAfter(any())).thenReturn(List.of());

            heartbeatService.cleanup();

            verify(discoveryService).deactivateApp(staleApp);
        }

        @Test
        void deletesDeadApps() {
            DiscoveryApp deadApp = new DiscoveryApp();
            deadApp.setAppId("dead-app");
            when(discoveryService.findStaleApps(any())).thenReturn(List.of());
            when(discoveryService.findAppsNotSeenSince(any())).thenReturn(List.of(deadApp));
            when(sessionRepository.findExpiredUnusedTokens(any())).thenReturn(List.of());
            when(sessionRepository.findBySessionActiveTrueAndLastActivityAtAfter(any())).thenReturn(List.of());

            heartbeatService.cleanup();

            verify(discoveryService).deleteApp(deadApp);
        }

        @Test
        void cleansUpExpiredTokens() {
            DiscoveryUserSession expiredSession = new DiscoveryUserSession();
            expiredSession.setTokenUsed(false);

            when(discoveryService.findStaleApps(any())).thenReturn(List.of());
            when(discoveryService.findAppsNotSeenSince(any())).thenReturn(List.of());
            when(sessionRepository.findExpiredUnusedTokens(any())).thenReturn(List.of(expiredSession));
            when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(sessionRepository.findBySessionActiveTrueAndLastActivityAtAfter(any())).thenReturn(List.of());

            heartbeatService.cleanup();

            assertTrue(expiredSession.getTokenUsed());
            verify(sessionRepository).save(expiredSession);
        }

        @Test
        void handlesCleanupExceptionGracefully() {
            when(discoveryService.findStaleApps(any())).thenThrow(new RuntimeException("DB error"));

            // Should not throw
            heartbeatService.cleanup();
        }
    }
}
