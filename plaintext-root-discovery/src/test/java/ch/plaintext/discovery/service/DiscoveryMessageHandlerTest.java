/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.discovery.service;

import ch.plaintext.discovery.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscoveryMessageHandlerTest {

    @Mock
    private DiscoveryService discoveryService;

    @InjectMocks
    private DiscoveryMessageHandler messageHandler;

    @Nested
    class HandleUserLogin {

        @Test
        void registersRemoteAppAndSendsResponseWhenUserKnown() {
            UserLoginMessage message = new UserLoginMessage();
            message.setFromAppId("remote-app");
            message.setFromAppName("Remote App");
            message.setUserEmail("user@example.com");
            message.setAppUrl("http://remote:8080");
            message.setAppEnvironment("dev");
            message.setPublicKey("some-public-key");

            when(discoveryService.isUserKnownLocally("user@example.com")).thenReturn(true);

            messageHandler.handleMessage("plaintext/discovery", message);

            verify(discoveryService).registerOrUpdateApp(
                "remote-app", "Remote App", "http://remote:8080", "dev", "some-public-key");
            verify(discoveryService).isUserKnownLocally("user@example.com");
            verify(discoveryService).sendAppResponse(message);
        }

        @Test
        void registersRemoteAppButDoesNotRespondWhenUserUnknown() {
            UserLoginMessage message = new UserLoginMessage();
            message.setFromAppId("remote-app");
            message.setFromAppName("Remote App");
            message.setUserEmail("unknown@example.com");
            message.setAppUrl("http://remote:8080");
            message.setAppEnvironment("dev");
            message.setPublicKey("some-public-key");

            when(discoveryService.isUserKnownLocally("unknown@example.com")).thenReturn(false);

            messageHandler.handleMessage("plaintext/discovery", message);

            verify(discoveryService).registerOrUpdateApp(
                "remote-app", "Remote App", "http://remote:8080", "dev", "some-public-key");
            verify(discoveryService).isUserKnownLocally("unknown@example.com");
            verify(discoveryService, never()).sendAppResponse(any());
        }

        @Test
        void handlesExceptionGracefully() {
            UserLoginMessage message = new UserLoginMessage();
            message.setFromAppId("remote-app");
            message.setFromAppName("Remote App");
            message.setUserEmail("user@example.com");
            message.setAppUrl("http://remote:8080");
            message.setAppEnvironment("dev");
            message.setPublicKey("key");

            when(discoveryService.isUserKnownLocally(anyString()))
                .thenThrow(new RuntimeException("DB error"));

            // Should not throw
            messageHandler.handleMessage("plaintext/discovery", message);
        }
    }

    @Nested
    class HandleAppResponse {

        @Test
        void registersUserInRemoteAppWhenUserKnown() {
            AppResponseMessage message = new AppResponseMessage();
            message.setFromAppId("remote-app");
            message.setFromAppName("Remote App");
            message.setUserEmail("user@example.com");
            message.setUserKnown(true);
            message.setAppUrl("http://remote:8080");
            message.setAppEnvironment("dev");

            messageHandler.handleMessage("plaintext/response/my-app", message);

            verify(discoveryService).registerUserInRemoteApp(message);
        }

        @Test
        void doesNotRegisterUserWhenUserNotKnown() {
            AppResponseMessage message = new AppResponseMessage();
            message.setFromAppId("remote-app");
            message.setFromAppName("Remote App");
            message.setUserEmail("user@example.com");
            message.setUserKnown(false);

            messageHandler.handleMessage("plaintext/response/my-app", message);

            verify(discoveryService, never()).registerUserInRemoteApp(any());
        }

        @Test
        void handlesExceptionGracefully() {
            AppResponseMessage message = new AppResponseMessage();
            message.setFromAppId("remote-app");
            message.setFromAppName("Remote App");
            message.setUserEmail("user@example.com");
            message.setUserKnown(true);

            doThrow(new RuntimeException("DB error")).when(discoveryService).registerUserInRemoteApp(any());

            // Should not throw
            messageHandler.handleMessage("plaintext/response/my-app", message);
        }
    }

    @Nested
    class HandleLoginTokenRequest {

        @Test
        void generatesTokenAndSendsResponse() {
            LoginTokenRequestMessage message = new LoginTokenRequestMessage();
            message.setFromAppId("requesting-app");
            message.setFromAppName("Requesting App");
            message.setUserEmail("user@example.com");
            message.setTargetAppId("my-app");

            when(discoveryService.generateLoginToken("user@example.com")).thenReturn("token-uuid");

            messageHandler.handleMessage("plaintext/login/my-app", message);

            verify(discoveryService).generateLoginToken("user@example.com");
            verify(discoveryService).sendLoginTokenResponse(message, "token-uuid");
        }

        @Test
        void doesNotSendResponseWhenTokenGenerationFails() {
            LoginTokenRequestMessage message = new LoginTokenRequestMessage();
            message.setFromAppId("requesting-app");
            message.setFromAppName("Requesting App");
            message.setUserEmail("unknown@example.com");
            message.setTargetAppId("my-app");

            when(discoveryService.generateLoginToken("unknown@example.com")).thenReturn(null);

            messageHandler.handleMessage("plaintext/login/my-app", message);

            verify(discoveryService).generateLoginToken("unknown@example.com");
            verify(discoveryService, never()).sendLoginTokenResponse(any(), any());
        }

        @Test
        void handlesExceptionGracefully() {
            LoginTokenRequestMessage message = new LoginTokenRequestMessage();
            message.setFromAppId("requesting-app");
            message.setFromAppName("Requesting App");
            message.setUserEmail("user@example.com");
            message.setTargetAppId("my-app");

            when(discoveryService.generateLoginToken(anyString()))
                .thenThrow(new RuntimeException("DB error"));

            // Should not throw
            messageHandler.handleMessage("plaintext/login/my-app", message);
        }
    }

    @Nested
    class HandleLoginTokenResponse {

        @Test
        void storesRemoteLoginUrl() {
            LoginTokenResponseMessage message = new LoginTokenResponseMessage();
            message.setFromAppId("target-app");
            message.setFromAppName("Target App");
            message.setUserEmail("user@example.com");
            message.setLoginUrl("http://target:8080/discovery/login?token=abc");
            message.setTokenValidForSeconds(300);

            messageHandler.handleMessage("plaintext/login/my-app", message);

            verify(discoveryService).storeRemoteLoginUrl(message);
        }

        @Test
        void handlesExceptionGracefully() {
            LoginTokenResponseMessage message = new LoginTokenResponseMessage();
            message.setFromAppId("target-app");
            message.setFromAppName("Target App");
            message.setUserEmail("user@example.com");

            doThrow(new RuntimeException("Error")).when(discoveryService).storeRemoteLoginUrl(any());

            // Should not throw
            messageHandler.handleMessage("plaintext/login/my-app", message);
        }
    }

    @Nested
    class HandleHeartbeat {

        @Test
        void updatesAppHeartbeat() {
            HeartbeatMessage message = new HeartbeatMessage();
            message.setFromAppId("remote-app");
            message.setFromAppName("Remote App");
            message.setAppUrl("http://remote:8080");
            message.setAppVersion("1.0.0");
            message.setAppEnvironment("prod");
            message.setActiveUserCount(3);
            message.setActiveUserEmails(List.of("a@test.com", "b@test.com", "c@test.com"));
            message.setPublicKey("pub-key");

            messageHandler.handleMessage("plaintext/heartbeat", message);

            verify(discoveryService).updateAppHeartbeat(
                "remote-app", "Remote App", "http://remote:8080", "prod",
                3, "pub-key", List.of("a@test.com", "b@test.com", "c@test.com"));
        }

        @Test
        void handlesHeartbeatWithNullEmails() {
            HeartbeatMessage message = new HeartbeatMessage();
            message.setFromAppId("remote-app");
            message.setFromAppName("Remote App");
            message.setAppUrl("http://remote:8080");
            message.setAppEnvironment("dev");
            message.setActiveUserCount(0);
            message.setActiveUserEmails(null);
            message.setPublicKey("pub-key");

            messageHandler.handleMessage("plaintext/heartbeat", message);

            verify(discoveryService).updateAppHeartbeat(
                "remote-app", "Remote App", "http://remote:8080", "dev",
                0, "pub-key", null);
        }

        @Test
        void handlesExceptionGracefully() {
            HeartbeatMessage message = new HeartbeatMessage();
            message.setFromAppId("remote-app");
            message.setFromAppName("Remote App");
            message.setAppUrl("http://remote:8080");
            message.setAppEnvironment("dev");
            message.setActiveUserCount(0);
            message.setPublicKey("key");

            doThrow(new RuntimeException("DB error"))
                .when(discoveryService).updateAppHeartbeat(any(), any(), any(), any(), anyInt(), any(), any());

            // Should not throw
            messageHandler.handleMessage("plaintext/heartbeat", message);
        }
    }
}
