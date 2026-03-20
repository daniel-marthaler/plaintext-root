/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.discovery.web;

import ch.plaintext.discovery.dto.LoginTokenRequestMessage;
import ch.plaintext.discovery.dto.UserLoginMessage;
import ch.plaintext.discovery.entity.DiscoveryApp;
import ch.plaintext.discovery.entity.DiscoveryUserSession;
import ch.plaintext.discovery.repository.DiscoveryAppRepository;
import ch.plaintext.discovery.repository.DiscoveryUserSessionRepository;
import ch.plaintext.discovery.service.DiscoveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscoveryRestControllerTest {

    @Mock
    private DiscoveryService discoveryService;

    @Mock
    private DiscoveryAppRepository appRepository;

    @Mock
    private DiscoveryUserSessionRepository sessionRepository;

    private DiscoveryRestController controller;

    @BeforeEach
    void setUp() {
        controller = new DiscoveryRestController(discoveryService, appRepository, sessionRepository);
    }

    @Nested
    class AnnounceLogin {

        @Test
        void returnsSuccessOnValidLogin() {
            UserLoginMessage message = new UserLoginMessage();
            message.setUserEmail("user@test.com");
            message.setUserId(42L);
            message.setUserName("Test User");

            ResponseEntity<Map<String, String>> response = controller.announceUserLogin(message);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("success", response.getBody().get("status"));
            verify(discoveryService).announceUserLogin("user@test.com", 42L, "Test User");
        }

        @Test
        void returnsBadRequestOnException() {
            UserLoginMessage message = new UserLoginMessage();
            message.setUserEmail("user@test.com");
            message.setUserId(1L);
            message.setUserName("User");

            doThrow(new RuntimeException("DB error"))
                .when(discoveryService).announceUserLogin(any(), any(), any());

            ResponseEntity<Map<String, String>> response = controller.announceUserLogin(message);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("error", response.getBody().get("status"));
            assertTrue(response.getBody().get("message").contains("DB error"));
        }
    }

    @Nested
    class RequestToken {

        @Test
        void returnsTokenOnSuccess() {
            LoginTokenRequestMessage request = new LoginTokenRequestMessage();
            request.setUserEmail("user@test.com");
            request.setTargetAppId("target-app");

            when(discoveryService.generateLoginToken("user@test.com")).thenReturn("generated-token");

            ResponseEntity<Map<String, String>> response = controller.requestLoginToken(request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("success", response.getBody().get("status"));
            assertEquals("generated-token", response.getBody().get("token"));
            assertTrue(response.getBody().get("loginUrl").contains("generated-token"));
            assertEquals("300", response.getBody().get("validFor"));
        }

        @Test
        void returnsBadRequestWhenTokenNull() {
            LoginTokenRequestMessage request = new LoginTokenRequestMessage();
            request.setUserEmail("unknown@test.com");
            request.setTargetAppId("target-app");

            when(discoveryService.generateLoginToken("unknown@test.com")).thenReturn(null);

            ResponseEntity<Map<String, String>> response = controller.requestLoginToken(request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("error", response.getBody().get("status"));
            assertEquals("User not authorized", response.getBody().get("message"));
        }

        @Test
        void returnsBadRequestOnException() {
            LoginTokenRequestMessage request = new LoginTokenRequestMessage();
            request.setUserEmail("user@test.com");
            request.setTargetAppId("target-app");

            when(discoveryService.generateLoginToken(any()))
                .thenThrow(new RuntimeException("Token error"));

            ResponseEntity<Map<String, String>> response = controller.requestLoginToken(request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("error", response.getBody().get("status"));
        }
    }

    @Nested
    class GetActiveApps {

        @Test
        void returnsActiveApps() {
            DiscoveryApp app = new DiscoveryApp();
            app.setAppId("app1");
            app.setActive(true);

            when(appRepository.findByActiveTrue()).thenReturn(List.of(app));

            ResponseEntity<List<DiscoveryApp>> response = controller.getActiveApps();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(1, response.getBody().size());
            assertEquals("app1", response.getBody().get(0).getAppId());
        }

        @Test
        void returnsEmptyListWhenNoApps() {
            when(appRepository.findByActiveTrue()).thenReturn(List.of());

            ResponseEntity<List<DiscoveryApp>> response = controller.getActiveApps();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().isEmpty());
        }

        @Test
        void returnsInternalServerErrorOnException() {
            when(appRepository.findByActiveTrue()).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<List<DiscoveryApp>> response = controller.getActiveApps();

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }
    }

    @Nested
    class GetRemoteAppsForUser {

        @Test
        void returnsRemoteAppsForUser() {
            DiscoveryApp remoteApp = new DiscoveryApp();
            remoteApp.setAppId("remote-app");

            DiscoveryUserSession session = new DiscoveryUserSession();
            session.setApp(remoteApp);
            session.setUserEmail("user@test.com");

            when(discoveryService.getRemoteAppsForUser("user@test.com"))
                .thenReturn(List.of(session));

            ResponseEntity<List<DiscoveryUserSession>> response =
                controller.getRemoteAppsForUser("user@test.com");

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(1, response.getBody().size());
        }

        @Test
        void returnsInternalServerErrorOnException() {
            when(discoveryService.getRemoteAppsForUser(any()))
                .thenThrow(new RuntimeException("DB error"));

            ResponseEntity<List<DiscoveryUserSession>> response =
                controller.getRemoteAppsForUser("user@test.com");

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }
    }

    @Nested
    class HealthCheck {

        @Test
        void returnsHealthyStatus() {
            when(appRepository.findByActiveTrue()).thenReturn(List.of(new DiscoveryApp()));
            when(sessionRepository.findBySessionActiveTrueAndLastActivityAtAfter(any()))
                .thenReturn(List.of(new DiscoveryUserSession(), new DiscoveryUserSession()));

            ResponseEntity<Map<String, Object>> response = controller.healthCheck();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("healthy", response.getBody().get("status"));
            assertEquals(1, response.getBody().get("activeApps"));
            assertEquals(2L, response.getBody().get("activeSessions"));
            assertNotNull(response.getBody().get("timestamp"));
        }

        @Test
        void returnsErrorOnException() {
            when(appRepository.findByActiveTrue()).thenThrow(new RuntimeException("DB down"));

            ResponseEntity<Map<String, Object>> response = controller.healthCheck();

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("error", response.getBody().get("status"));
        }
    }
}
