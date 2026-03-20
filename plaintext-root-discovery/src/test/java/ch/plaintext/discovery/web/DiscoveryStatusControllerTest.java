/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.discovery.web;

import ch.plaintext.discovery.entity.DiscoveryApp;
import ch.plaintext.discovery.entity.DiscoveryUserSession;
import ch.plaintext.discovery.repository.DiscoveryAppRepository;
import ch.plaintext.discovery.repository.DiscoveryUserSessionRepository;
import ch.plaintext.discovery.service.DiscoveryEncryptionService;
import ch.plaintext.discovery.service.DiscoveryEncryptionService.DiscoveryToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscoveryStatusControllerTest {

    @Mock
    private DiscoveryAppRepository appRepository;

    @Mock
    private DiscoveryUserSessionRepository sessionRepository;

    @Mock
    private DiscoveryEncryptionService encryptionService;

    private DiscoveryStatusController controller;

    @BeforeEach
    void setUp() {
        controller = new DiscoveryStatusController(appRepository, sessionRepository, encryptionService);
        ReflectionTestUtils.setField(controller, "appId", "test-app");
        ReflectionTestUtils.setField(controller, "appName", "Test App");
    }

    @Nested
    class Status {

        @Test
        void returnsStatusWithAllFields() {
            when(encryptionService.getPublicKeyString()).thenReturn("base64-public-key");
            when(appRepository.findByActiveTrue()).thenReturn(List.of());
            when(sessionRepository.findBySessionActiveTrueAndLastActivityAtAfter(any()))
                .thenReturn(List.of());
            when(encryptionService.createDiscoveryToken(anyString(), anyString(), anyString()))
                .thenReturn("encrypted-token");
            when(encryptionService.decryptDiscoveryToken("encrypted-token"))
                .thenReturn(new DiscoveryToken("test@test.com", System.currentTimeMillis(), "test-app", "nonce"));

            ResponseEntity<Map<String, Object>> response = controller.status();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            Map<String, Object> body = response.getBody();
            assertNotNull(body);
            assertEquals("test-app", body.get("appId"));
            assertEquals("Test App", body.get("appName"));
            assertNotNull(body.get("publicKeyHash"));
            assertNotNull(body.get("timestamp"));
            assertNotNull(body.get("knownApps"));
            assertEquals(0, body.get("activeSessions"));
            assertEquals("OK", body.get("selfTestEncryption"));
        }

        @Test
        void includesKnownAppsInfo() {
            DiscoveryApp app = new DiscoveryApp();
            app.setAppId("remote-app");
            app.setAppName("Remote App");
            app.setAppUrl("http://remote:8080");
            app.setEnvironment(DiscoveryApp.AppEnvironment.PROD);
            app.setPublicKey("remote-key");
            app.setLastSeenAt(LocalDateTime.now());

            when(encryptionService.getPublicKeyString()).thenReturn("key");
            when(appRepository.findByActiveTrue()).thenReturn(List.of(app));
            when(sessionRepository.findBySessionActiveTrueAndLastActivityAtAfter(any()))
                .thenReturn(List.of());
            when(encryptionService.createDiscoveryToken(anyString(), anyString(), anyString()))
                .thenReturn("token");
            when(encryptionService.decryptDiscoveryToken("token"))
                .thenReturn(new DiscoveryToken("test@test.com", System.currentTimeMillis(), "test-app", "n"));

            ResponseEntity<Map<String, Object>> response = controller.status();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> knownApps = (List<Map<String, Object>>) response.getBody().get("knownApps");
            assertEquals(1, knownApps.size());
            assertEquals("remote-app", knownApps.get(0).get("appId"));
            assertEquals("Remote App", knownApps.get(0).get("appName"));
            assertEquals("http://remote:8080", knownApps.get(0).get("appUrl"));
        }

        @Test
        void handlesEncryptionSelfTestFailure() {
            when(encryptionService.getPublicKeyString()).thenReturn("key");
            when(appRepository.findByActiveTrue()).thenReturn(List.of());
            when(sessionRepository.findBySessionActiveTrueAndLastActivityAtAfter(any()))
                .thenReturn(List.of());
            when(encryptionService.createDiscoveryToken(anyString(), anyString(), anyString()))
                .thenReturn("token");
            when(encryptionService.decryptDiscoveryToken("token")).thenReturn(null);

            ResponseEntity<Map<String, Object>> response = controller.status();

            assertEquals("FAILED", response.getBody().get("selfTestEncryption"));
        }

        @Test
        void handlesEncryptionSelfTestException() {
            when(encryptionService.getPublicKeyString()).thenReturn("key");
            when(appRepository.findByActiveTrue()).thenReturn(List.of());
            when(sessionRepository.findBySessionActiveTrueAndLastActivityAtAfter(any()))
                .thenReturn(List.of());
            when(encryptionService.createDiscoveryToken(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Crypto error"));

            ResponseEntity<Map<String, Object>> response = controller.status();

            String selfTest = (String) response.getBody().get("selfTestEncryption");
            assertTrue(selfTest.startsWith("ERROR:"));
        }

        @Test
        void hashesNullPublicKeyAsNull() {
            // App with null public key
            DiscoveryApp app = new DiscoveryApp();
            app.setAppId("no-key-app");
            app.setAppName("No Key App");
            app.setAppUrl("http://nokey:8080");
            app.setEnvironment(DiscoveryApp.AppEnvironment.DEV);
            app.setPublicKey(null);
            app.setLastSeenAt(LocalDateTime.now());

            when(encryptionService.getPublicKeyString()).thenReturn("key");
            when(appRepository.findByActiveTrue()).thenReturn(List.of(app));
            when(sessionRepository.findBySessionActiveTrueAndLastActivityAtAfter(any()))
                .thenReturn(List.of());
            when(encryptionService.createDiscoveryToken(anyString(), anyString(), anyString()))
                .thenReturn("token");
            when(encryptionService.decryptDiscoveryToken("token"))
                .thenReturn(new DiscoveryToken("t@t.com", System.currentTimeMillis(), "test-app", "n"));

            ResponseEntity<Map<String, Object>> response = controller.status();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> knownApps = (List<Map<String, Object>>) response.getBody().get("knownApps");
            assertEquals("null", knownApps.get(0).get("publicKeyHash"));
        }

        @Test
        void countsActiveSessions() {
            when(encryptionService.getPublicKeyString()).thenReturn("key");
            when(appRepository.findByActiveTrue()).thenReturn(List.of());
            when(sessionRepository.findBySessionActiveTrueAndLastActivityAtAfter(any()))
                .thenReturn(List.of(new DiscoveryUserSession(), new DiscoveryUserSession(), new DiscoveryUserSession()));
            when(encryptionService.createDiscoveryToken(anyString(), anyString(), anyString()))
                .thenReturn("token");
            when(encryptionService.decryptDiscoveryToken("token"))
                .thenReturn(new DiscoveryToken("t@t.com", System.currentTimeMillis(), "test-app", "n"));

            ResponseEntity<Map<String, Object>> response = controller.status();

            assertEquals(3, response.getBody().get("activeSessions"));
        }
    }
}
