/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.discovery.service;

import ch.plaintext.discovery.config.DiscoveryProperties;
import ch.plaintext.discovery.dto.*;
import ch.plaintext.discovery.entity.DiscoveryApp;
import ch.plaintext.discovery.entity.DiscoveryUserSession;
import ch.plaintext.discovery.repository.DiscoveryAppRepository;
import ch.plaintext.discovery.repository.DiscoveryUserSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscoveryServiceTest {

    @Mock
    private DiscoveryAppRepository appRepository;

    @Mock
    private DiscoveryUserSessionRepository sessionRepository;

    @Mock
    private DiscoveryMqttService mqttService;

    @Mock
    private DiscoveryEncryptionService encryptionService;

    @Mock
    private UserDetailsService userDetailsService;

    private DiscoveryProperties properties;

    private DiscoveryService discoveryService;

    @BeforeEach
    void setUp() {
        properties = new DiscoveryProperties();
        properties.setToken(new DiscoveryProperties.Token());

        discoveryService = new DiscoveryService(
            appRepository, sessionRepository, mqttService,
            encryptionService, properties, userDetailsService
        );

        ReflectionTestUtils.setField(discoveryService, "appId", "test-app");
        ReflectionTestUtils.setField(discoveryService, "appName", "Test App");
        ReflectionTestUtils.setField(discoveryService, "environment", "dev");
        ReflectionTestUtils.setField(discoveryService, "appUrl", "http://localhost:8080");
    }

    @Nested
    class RegisterOrUpdateApp {

        @Test
        void createsNewAppWhenNotFound() {
            when(appRepository.findByAppId("remote-app")).thenReturn(Optional.empty());
            when(appRepository.save(any(DiscoveryApp.class))).thenAnswer(inv -> inv.getArgument(0));

            discoveryService.registerOrUpdateApp("remote-app", "Remote App",
                "http://remote:8080", "dev", "public-key");

            ArgumentCaptor<DiscoveryApp> captor = ArgumentCaptor.forClass(DiscoveryApp.class);
            verify(appRepository).save(captor.capture());

            DiscoveryApp saved = captor.getValue();
            assertEquals("remote-app", saved.getAppId());
            assertEquals("Remote App", saved.getAppName());
            assertEquals("http://remote:8080", saved.getAppUrl());
            assertEquals(DiscoveryApp.AppEnvironment.DEV, saved.getEnvironment());
            assertEquals("public-key", saved.getPublicKey());
            assertTrue(saved.getActive());
            assertNotNull(saved.getLastSeenAt());
        }

        @Test
        void updatesExistingApp() {
            DiscoveryApp existingApp = new DiscoveryApp();
            existingApp.setAppId("remote-app");
            existingApp.setAppName("Old Name");
            when(appRepository.findByAppId("remote-app")).thenReturn(Optional.of(existingApp));
            when(appRepository.save(any(DiscoveryApp.class))).thenAnswer(inv -> inv.getArgument(0));

            discoveryService.registerOrUpdateApp("remote-app", "New Name",
                "http://remote:9090", "prod", "new-key");

            ArgumentCaptor<DiscoveryApp> captor = ArgumentCaptor.forClass(DiscoveryApp.class);
            verify(appRepository).save(captor.capture());

            DiscoveryApp saved = captor.getValue();
            assertEquals("New Name", saved.getAppName());
            assertEquals("http://remote:9090", saved.getAppUrl());
            assertEquals(DiscoveryApp.AppEnvironment.PROD, saved.getEnvironment());
            assertEquals("new-key", saved.getPublicKey());
            assertTrue(saved.getActive());
        }
    }

    @Nested
    class IsUserKnownLocally {

        @Test
        void returnsTrueWhenUserFound() {
            when(userDetailsService.loadUserByUsername("user@test.com"))
                .thenReturn(mock(UserDetails.class));

            assertTrue(discoveryService.isUserKnownLocally("user@test.com"));
        }

        @Test
        void returnsFalseWhenUserNotFound() {
            when(userDetailsService.loadUserByUsername("unknown@test.com"))
                .thenThrow(new UsernameNotFoundException("Not found"));

            assertFalse(discoveryService.isUserKnownLocally("unknown@test.com"));
        }

        @Test
        void returnsFalseOnUnexpectedException() {
            when(userDetailsService.loadUserByUsername("user@test.com"))
                .thenThrow(new RuntimeException("DB error"));

            assertFalse(discoveryService.isUserKnownLocally("user@test.com"));
        }
    }

    @Nested
    class SendAppResponse {

        @Test
        void publishesResponseToCorrectTopic() {
            UserLoginMessage original = new UserLoginMessage();
            original.setFromAppId("requesting-app");
            original.setUserEmail("user@test.com");

            discoveryService.sendAppResponse(original);

            ArgumentCaptor<DiscoveryMessage> captor = ArgumentCaptor.forClass(DiscoveryMessage.class);
            verify(mqttService).publishMessage(
                eq("plaintext/response/requesting-app"), captor.capture());

            AppResponseMessage response = (AppResponseMessage) captor.getValue();
            assertEquals("requesting-app", response.getTargetAppId());
            assertEquals("user@test.com", response.getUserEmail());
            assertTrue(response.isUserKnown());
            assertEquals("Test App", response.getAppDisplayName());
            assertEquals("http://localhost:8080", response.getAppUrl());
            assertEquals("dev", response.getAppEnvironment());
        }
    }

    @Nested
    class RegisterUserInRemoteApp {

        @Test
        void createsNewSessionForNewUser() {
            AppResponseMessage message = new AppResponseMessage();
            message.setFromAppId("remote-app");
            message.setFromAppName("Remote App");
            message.setUserEmail("user@test.com");
            message.setAppUrl("http://remote:8080");
            message.setAppEnvironment("dev");

            DiscoveryApp remoteApp = new DiscoveryApp();
            remoteApp.setAppId("remote-app");
            when(appRepository.findByAppId("remote-app")).thenReturn(Optional.of(remoteApp));
            when(sessionRepository.findByAppAndUserEmailAndSessionActiveTrue(remoteApp, "user@test.com"))
                .thenReturn(Optional.empty());
            when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            discoveryService.registerUserInRemoteApp(message);

            ArgumentCaptor<DiscoveryUserSession> captor = ArgumentCaptor.forClass(DiscoveryUserSession.class);
            verify(sessionRepository).save(captor.capture());

            DiscoveryUserSession saved = captor.getValue();
            assertEquals("user@test.com", saved.getUserEmail());
            assertEquals(remoteApp, saved.getApp());
            assertTrue(saved.getSessionActive());
            assertNotNull(saved.getLoggedInAt());
            assertNotNull(saved.getLastActivityAt());
        }

        @Test
        void updatesExistingSession() {
            AppResponseMessage message = new AppResponseMessage();
            message.setFromAppId("remote-app");
            message.setFromAppName("Remote App");
            message.setUserEmail("user@test.com");
            message.setAppUrl("http://remote:8080");
            message.setAppEnvironment("dev");

            DiscoveryApp remoteApp = new DiscoveryApp();
            remoteApp.setAppId("remote-app");
            DiscoveryUserSession existing = new DiscoveryUserSession();
            existing.setApp(remoteApp);
            existing.setUserEmail("user@test.com");
            existing.setLoggedInAt(LocalDateTime.now().minusHours(1));

            when(appRepository.findByAppId("remote-app")).thenReturn(Optional.of(remoteApp));
            when(sessionRepository.findByAppAndUserEmailAndSessionActiveTrue(remoteApp, "user@test.com"))
                .thenReturn(Optional.of(existing));
            when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            discoveryService.registerUserInRemoteApp(message);

            ArgumentCaptor<DiscoveryUserSession> captor = ArgumentCaptor.forClass(DiscoveryUserSession.class);
            verify(sessionRepository).save(captor.capture());

            DiscoveryUserSession saved = captor.getValue();
            // loggedInAt should be preserved from existing session
            assertTrue(saved.getLoggedInAt().isBefore(LocalDateTime.now().minusMinutes(30)));
        }
    }

    @Nested
    class GetRemoteAppsForUser {

        @Test
        void filtersOutLocalApp() {
            DiscoveryApp localApp = new DiscoveryApp();
            localApp.setAppId("test-app");
            DiscoveryApp remoteApp = new DiscoveryApp();
            remoteApp.setAppId("other-app");

            DiscoveryUserSession localSession = new DiscoveryUserSession();
            localSession.setApp(localApp);
            localSession.setUserEmail("user@test.com");

            DiscoveryUserSession remoteSession = new DiscoveryUserSession();
            remoteSession.setApp(remoteApp);
            remoteSession.setUserEmail("user@test.com");

            when(sessionRepository.findByUserEmailAndSessionActiveTrue("user@test.com"))
                .thenReturn(List.of(localSession, remoteSession));

            List<DiscoveryUserSession> result = discoveryService.getRemoteAppsForUser("user@test.com");

            assertEquals(1, result.size());
            assertEquals("other-app", result.get(0).getApp().getAppId());
        }

        @Test
        void returnsEmptyListWhenNoSessions() {
            when(sessionRepository.findByUserEmailAndSessionActiveTrue("user@test.com"))
                .thenReturn(List.of());

            List<DiscoveryUserSession> result = discoveryService.getRemoteAppsForUser("user@test.com");

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class GenerateLoginToken {

        @Test
        void generatesTokenForKnownUser() {
            when(userDetailsService.loadUserByUsername("user@test.com"))
                .thenReturn(mock(UserDetails.class));

            DiscoveryApp localApp = new DiscoveryApp();
            localApp.setAppId("test-app");
            when(appRepository.findByAppId("test-app")).thenReturn(Optional.of(localApp));
            when(sessionRepository.findByAppAndUserEmailAndSessionActiveTrue(localApp, "user@test.com"))
                .thenReturn(Optional.empty());
            when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            String token = discoveryService.generateLoginToken("user@test.com");

            assertNotNull(token);
            assertFalse(token.isBlank());

            ArgumentCaptor<DiscoveryUserSession> captor = ArgumentCaptor.forClass(DiscoveryUserSession.class);
            verify(sessionRepository).save(captor.capture());

            DiscoveryUserSession saved = captor.getValue();
            assertEquals(token, saved.getLoginToken());
            assertNotNull(saved.getTokenExpiresAt());
            assertFalse(saved.getTokenUsed());
        }

        @Test
        void returnsNullForUnknownUser() {
            when(userDetailsService.loadUserByUsername("unknown@test.com"))
                .thenThrow(new UsernameNotFoundException("Not found"));

            String token = discoveryService.generateLoginToken("unknown@test.com");

            assertNull(token);
            verify(sessionRepository, never()).save(any());
        }

        @Test
        void tokenExpiresWithConfiguredValidity() {
            properties.getToken().setValiditySeconds(600);

            when(userDetailsService.loadUserByUsername("user@test.com"))
                .thenReturn(mock(UserDetails.class));

            DiscoveryApp localApp = new DiscoveryApp();
            localApp.setAppId("test-app");
            when(appRepository.findByAppId("test-app")).thenReturn(Optional.of(localApp));
            when(sessionRepository.findByAppAndUserEmailAndSessionActiveTrue(localApp, "user@test.com"))
                .thenReturn(Optional.empty());
            when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            discoveryService.generateLoginToken("user@test.com");

            ArgumentCaptor<DiscoveryUserSession> captor = ArgumentCaptor.forClass(DiscoveryUserSession.class);
            verify(sessionRepository).save(captor.capture());

            DiscoveryUserSession saved = captor.getValue();
            // Token should expire approximately 600 seconds from now
            assertTrue(saved.getTokenExpiresAt().isAfter(LocalDateTime.now().plusSeconds(590)));
            assertTrue(saved.getTokenExpiresAt().isBefore(LocalDateTime.now().plusSeconds(610)));
        }
    }

    @Nested
    class SendLoginTokenResponse {

        @Test
        void sendsResponseWithEncryptedToken() {
            properties.getToken().setEncryptionEnabled(true);
            properties.getToken().setValiditySeconds(300);

            LoginTokenRequestMessage request = new LoginTokenRequestMessage();
            request.setFromAppId("requesting-app");
            request.setUserEmail("user@test.com");
            request.setMessageId("msg-123");

            DiscoveryApp requestingApp = new DiscoveryApp();
            requestingApp.setAppId("requesting-app");
            requestingApp.setPublicKey("app-public-key");
            when(appRepository.findByAppId("requesting-app")).thenReturn(Optional.of(requestingApp));
            when(encryptionService.encrypt("my-token", "app-public-key")).thenReturn("encrypted-token");

            discoveryService.sendLoginTokenResponse(request, "my-token");

            ArgumentCaptor<DiscoveryMessage> captor = ArgumentCaptor.forClass(DiscoveryMessage.class);
            verify(mqttService).publishMessage(eq("plaintext/login/requesting-app"), captor.capture());

            LoginTokenResponseMessage response = (LoginTokenResponseMessage) captor.getValue();
            assertEquals("requesting-app", response.getTargetAppId());
            assertEquals("user@test.com", response.getUserEmail());
            assertEquals("encrypted-token", response.getEncryptedToken());
            assertEquals(300, response.getTokenValidForSeconds());
            assertTrue(response.getLoginUrl().contains("my-token"));
        }

        @Test
        void sendsUnencryptedTokenWhenEncryptionDisabled() {
            properties.getToken().setEncryptionEnabled(false);

            LoginTokenRequestMessage request = new LoginTokenRequestMessage();
            request.setFromAppId("requesting-app");
            request.setUserEmail("user@test.com");

            discoveryService.sendLoginTokenResponse(request, "my-token");

            ArgumentCaptor<DiscoveryMessage> captor = ArgumentCaptor.forClass(DiscoveryMessage.class);
            verify(mqttService).publishMessage(eq("plaintext/login/requesting-app"), captor.capture());

            LoginTokenResponseMessage response = (LoginTokenResponseMessage) captor.getValue();
            assertEquals("my-token", response.getEncryptedToken());
            verify(encryptionService, never()).encrypt(any(), any());
        }

        @Test
        void sendsUnencryptedTokenWhenEncryptionFails() {
            properties.getToken().setEncryptionEnabled(true);

            LoginTokenRequestMessage request = new LoginTokenRequestMessage();
            request.setFromAppId("requesting-app");
            request.setUserEmail("user@test.com");

            DiscoveryApp requestingApp = new DiscoveryApp();
            requestingApp.setPublicKey("bad-key");
            when(appRepository.findByAppId("requesting-app")).thenReturn(Optional.of(requestingApp));
            when(encryptionService.encrypt("my-token", "bad-key"))
                .thenThrow(new RuntimeException("Encryption error"));

            discoveryService.sendLoginTokenResponse(request, "my-token");

            ArgumentCaptor<DiscoveryMessage> captor = ArgumentCaptor.forClass(DiscoveryMessage.class);
            verify(mqttService).publishMessage(eq("plaintext/login/requesting-app"), captor.capture());

            LoginTokenResponseMessage response = (LoginTokenResponseMessage) captor.getValue();
            assertEquals("my-token", response.getEncryptedToken());
        }

        @Test
        void sendsUnencryptedTokenWhenAppHasNoPublicKey() {
            properties.getToken().setEncryptionEnabled(true);

            LoginTokenRequestMessage request = new LoginTokenRequestMessage();
            request.setFromAppId("requesting-app");
            request.setUserEmail("user@test.com");

            DiscoveryApp requestingApp = new DiscoveryApp();
            requestingApp.setPublicKey(null);
            when(appRepository.findByAppId("requesting-app")).thenReturn(Optional.of(requestingApp));

            discoveryService.sendLoginTokenResponse(request, "my-token");

            ArgumentCaptor<DiscoveryMessage> captor = ArgumentCaptor.forClass(DiscoveryMessage.class);
            verify(mqttService).publishMessage(eq("plaintext/login/requesting-app"), captor.capture());

            LoginTokenResponseMessage response = (LoginTokenResponseMessage) captor.getValue();
            assertEquals("my-token", response.getEncryptedToken());
            verify(encryptionService, never()).encrypt(any(), any());
        }
    }

    @Nested
    class RemoteLoginUrlManagement {

        @Test
        void storeAndRetrieveLoginUrl() {
            LoginTokenResponseMessage message = new LoginTokenResponseMessage();
            message.setUserEmail("user@test.com");
            message.setLoginUrl("http://remote/discovery/login?token=abc");
            message.setTokenValidForSeconds(300);

            discoveryService.storeRemoteLoginUrl(message);

            String url = discoveryService.getRemoteLoginUrl("user@test.com");
            assertEquals("http://remote/discovery/login?token=abc", url);
        }

        @Test
        void consumeOnceReturnsNullOnSecondCall() {
            LoginTokenResponseMessage message = new LoginTokenResponseMessage();
            message.setUserEmail("user@test.com");
            message.setLoginUrl("http://remote/discovery/login?token=abc");
            message.setTokenValidForSeconds(300);

            discoveryService.storeRemoteLoginUrl(message);

            // First call returns URL
            assertNotNull(discoveryService.getRemoteLoginUrl("user@test.com"));
            // Second call returns null (consume-once)
            assertNull(discoveryService.getRemoteLoginUrl("user@test.com"));
        }

        @Test
        void returnsNullForUnknownUser() {
            String url = discoveryService.getRemoteLoginUrl("unknown@test.com");
            assertNull(url);
        }

        @Test
        void returnsNullForExpiredUrl() {
            LoginTokenResponseMessage message = new LoginTokenResponseMessage();
            message.setUserEmail("user@test.com");
            message.setLoginUrl("http://remote/discovery/login?token=abc");
            message.setTokenValidForSeconds(0); // Immediate expiry

            discoveryService.storeRemoteLoginUrl(message);

            // The URL has already expired (0 seconds validity)
            String url = discoveryService.getRemoteLoginUrl("user@test.com");
            assertNull(url);
        }
    }

    @Nested
    class UpdateAppHeartbeat {

        @Test
        void registersAppAndSyncsUserSessions() {
            DiscoveryApp remoteApp = new DiscoveryApp();
            remoteApp.setAppId("remote-app");
            when(appRepository.findByAppId("remote-app")).thenReturn(Optional.of(remoteApp));
            when(appRepository.save(any(DiscoveryApp.class))).thenAnswer(inv -> inv.getArgument(0));
            when(sessionRepository.findByAppAndUserEmailAndSessionActiveTrue(eq(remoteApp), anyString()))
                .thenReturn(Optional.empty());
            when(sessionRepository.findByAppAndSessionActiveTrue(remoteApp)).thenReturn(List.of());
            when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            discoveryService.updateAppHeartbeat("remote-app", "Remote App",
                "http://remote:8080", "dev", 2, "pub-key",
                List.of("user1@test.com", "user2@test.com"));

            // Should save 2 user sessions
            verify(sessionRepository, times(2)).save(any(DiscoveryUserSession.class));
        }

        @Test
        void deactivatesSessionsForUsersNoLongerActive() {
            DiscoveryApp remoteApp = new DiscoveryApp();
            remoteApp.setAppId("remote-app");

            DiscoveryUserSession staleSession = new DiscoveryUserSession();
            staleSession.setApp(remoteApp);
            staleSession.setUserEmail("stale@test.com");
            staleSession.setSessionActive(true);

            when(appRepository.findByAppId("remote-app")).thenReturn(Optional.of(remoteApp));
            when(appRepository.save(any(DiscoveryApp.class))).thenAnswer(inv -> inv.getArgument(0));
            when(sessionRepository.findByAppAndUserEmailAndSessionActiveTrue(eq(remoteApp), eq("active@test.com")))
                .thenReturn(Optional.empty());
            when(sessionRepository.findByAppAndSessionActiveTrue(remoteApp))
                .thenReturn(List.of(staleSession));
            when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            discoveryService.updateAppHeartbeat("remote-app", "Remote App",
                "http://remote:8080", "dev", 1, "pub-key",
                List.of("active@test.com"));

            // Stale session should be deactivated
            assertFalse(staleSession.getSessionActive());
        }

        @Test
        void handlesNullEmailList() {
            DiscoveryApp remoteApp = new DiscoveryApp();
            remoteApp.setAppId("remote-app");
            when(appRepository.findByAppId("remote-app")).thenReturn(Optional.of(remoteApp));
            when(appRepository.save(any(DiscoveryApp.class))).thenAnswer(inv -> inv.getArgument(0));

            // Should not throw with null email list
            discoveryService.updateAppHeartbeat("remote-app", "Remote App",
                "http://remote:8080", "dev", 0, "pub-key", null);

            // No session operations when email list is null
            verify(sessionRepository, never()).findByAppAndUserEmailAndSessionActiveTrue(any(), anyString());
        }

        @Test
        void handlesEmptyEmailList() {
            DiscoveryApp remoteApp = new DiscoveryApp();
            remoteApp.setAppId("remote-app");
            when(appRepository.findByAppId("remote-app")).thenReturn(Optional.of(remoteApp));
            when(appRepository.save(any(DiscoveryApp.class))).thenAnswer(inv -> inv.getArgument(0));

            discoveryService.updateAppHeartbeat("remote-app", "Remote App",
                "http://remote:8080", "dev", 0, "pub-key", List.of());

            verify(sessionRepository, never()).findByAppAndUserEmailAndSessionActiveTrue(any(), anyString());
        }
    }

    @Nested
    class DeactivateApp {

        @Test
        void deactivatesAppAndAllSessions() {
            DiscoveryApp app = new DiscoveryApp();
            app.setAppId("stale-app");
            app.setActive(true);

            DiscoveryUserSession session1 = new DiscoveryUserSession();
            session1.setSessionActive(true);
            DiscoveryUserSession session2 = new DiscoveryUserSession();
            session2.setSessionActive(true);

            when(sessionRepository.findByAppAndSessionActiveTrue(app))
                .thenReturn(List.of(session1, session2));
            when(appRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            discoveryService.deactivateApp(app);

            assertFalse(app.getActive());
            assertFalse(session1.getSessionActive());
            assertFalse(session2.getSessionActive());
            verify(appRepository).save(app);
            verify(sessionRepository, times(2)).save(any());
        }
    }

    @Nested
    class DeleteApp {

        @Test
        void deletesAppAndAllSessions() {
            DiscoveryApp app = new DiscoveryApp();
            app.setAppId("dead-app");

            DiscoveryUserSession session = new DiscoveryUserSession();
            when(sessionRepository.findByApp(app)).thenReturn(List.of(session));

            discoveryService.deleteApp(app);

            verify(sessionRepository).deleteAll(List.of(session));
            verify(appRepository).delete(app);
        }
    }

    @Nested
    class AnnounceUserLogin {

        @Test
        void publishesUserLoginMessage() {
            when(encryptionService.getPublicKeyString()).thenReturn("pub-key");
            DiscoveryApp localApp = new DiscoveryApp();
            localApp.setAppId("test-app");
            when(appRepository.findByAppId("test-app")).thenReturn(Optional.of(localApp));
            when(sessionRepository.findByAppAndUserEmailAndSessionActiveTrue(localApp, "user@test.com"))
                .thenReturn(Optional.empty());
            when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            discoveryService.announceUserLogin("user@test.com", 42L, "Test User");

            ArgumentCaptor<DiscoveryMessage> captor = ArgumentCaptor.forClass(DiscoveryMessage.class);
            verify(mqttService).publishMessage(eq("plaintext/discovery"), captor.capture());

            UserLoginMessage published = (UserLoginMessage) captor.getValue();
            assertEquals("user@test.com", published.getUserEmail());
            assertEquals(42L, published.getUserId());
            assertEquals("Test User", published.getUserName());
            assertEquals("http://localhost:8080", published.getAppUrl());
            assertEquals("dev", published.getAppEnvironment());
            assertEquals("pub-key", published.getPublicKey());
        }

        @Test
        void handlesExceptionGracefully() {
            when(encryptionService.getPublicKeyString()).thenThrow(new RuntimeException("Key error"));

            // Should not throw
            discoveryService.announceUserLogin("user@test.com", 42L, "Test User");
        }
    }

    @Nested
    class RequestCrossAppLogin {

        @Test
        void publishesLoginTokenRequest() {
            LoginTokenRequestMessage request = new LoginTokenRequestMessage();
            request.setTargetAppId("target-app");
            request.setUserEmail("user@test.com");

            discoveryService.requestCrossAppLogin(request);

            verify(mqttService).publishMessage(eq("plaintext/login/target-app"), eq(request));
        }

        @Test
        void handlesExceptionGracefully() {
            LoginTokenRequestMessage request = new LoginTokenRequestMessage();
            request.setTargetAppId("target-app");

            doThrow(new RuntimeException("MQTT error"))
                .when(mqttService).publishMessage(anyString(), any());

            // Should not throw
            discoveryService.requestCrossAppLogin(request);
        }
    }

    @Nested
    class UpdateAppUrlFromRequest {

        @Test
        void updatesUrlWhenCurrentIsLocalhost() {
            discoveryService.updateAppUrlFromRequest("https://myapp.example.com");

            assertEquals("https://myapp.example.com", discoveryService.getBaseUrl());
        }

        @Test
        void doesNotUpdateWhenCurrentIsNotLocalhost() {
            ReflectionTestUtils.setField(discoveryService, "appUrl", "https://prod.example.com");

            discoveryService.updateAppUrlFromRequest("https://other.example.com");

            assertEquals("https://prod.example.com", discoveryService.getBaseUrl());
        }

        @Test
        void doesNotUpdateWithNullUrl() {
            discoveryService.updateAppUrlFromRequest(null);

            assertEquals("http://localhost:8080", discoveryService.getBaseUrl());
        }

        @Test
        void doesNotUpdateWithBlankUrl() {
            discoveryService.updateAppUrlFromRequest("   ");

            assertEquals("http://localhost:8080", discoveryService.getBaseUrl());
        }
    }

    @Nested
    class FindApps {

        @Test
        void findStaleAppsDelegatesToRepository() {
            LocalDateTime before = LocalDateTime.now().minusMinutes(5);
            discoveryService.findStaleApps(before);
            verify(appRepository).findStaleApps(before);
        }

        @Test
        void findAppsNotSeenSinceDelegatesToRepository() {
            LocalDateTime before = LocalDateTime.now().minusHours(3);
            discoveryService.findAppsNotSeenSince(before);
            verify(appRepository).findAppsNotSeenSince(before);
        }
    }
}
