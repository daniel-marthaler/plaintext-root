/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.discovery.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DiscoveryMessageSerializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Nested
    class UserLoginMessageTests {

        @Test
        void serializationRoundTrip() throws Exception {
            UserLoginMessage message = new UserLoginMessage();
            message.setFromAppId("app1");
            message.setFromAppName("App One");
            message.setUserEmail("user@example.com");
            message.setUserId(42L);
            message.setUserName("Test User");
            message.setAppUrl("http://localhost:8080");
            message.setAppEnvironment("dev");
            message.setPublicKey("pub-key-123");

            String json = objectMapper.writeValueAsString(message);
            DiscoveryMessage deserialized = objectMapper.readValue(json, DiscoveryMessage.class);

            assertInstanceOf(UserLoginMessage.class, deserialized);
            UserLoginMessage result = (UserLoginMessage) deserialized;
            assertEquals("app1", result.getFromAppId());
            assertEquals("App One", result.getFromAppName());
            assertEquals("user@example.com", result.getUserEmail());
            assertEquals(42L, result.getUserId());
            assertEquals("Test User", result.getUserName());
            assertEquals("http://localhost:8080", result.getAppUrl());
            assertEquals("dev", result.getAppEnvironment());
            assertEquals("pub-key-123", result.getPublicKey());
        }

        @Test
        void hasCorrectMessageType() {
            UserLoginMessage message = new UserLoginMessage();
            assertEquals(DiscoveryMessage.MessageType.USER_LOGIN, message.getType());
        }

        @Test
        void hasMessageIdAndTimestamp() {
            UserLoginMessage message = new UserLoginMessage();
            assertNotNull(message.getMessageId());
            assertNotNull(message.getTimestamp());
        }
    }

    @Nested
    class AppResponseMessageTests {

        @Test
        void serializationRoundTrip() throws Exception {
            AppResponseMessage message = new AppResponseMessage();
            message.setFromAppId("app2");
            message.setFromAppName("App Two");
            message.setTargetAppId("app1");
            message.setInResponseToMessageId("msg-123");
            message.setUserEmail("user@example.com");
            message.setUserKnown(true);
            message.setAppUrl("http://remote:8080");
            message.setAppDisplayName("Remote App");
            message.setAppEnvironment("prod");

            String json = objectMapper.writeValueAsString(message);
            DiscoveryMessage deserialized = objectMapper.readValue(json, DiscoveryMessage.class);

            assertInstanceOf(AppResponseMessage.class, deserialized);
            AppResponseMessage result = (AppResponseMessage) deserialized;
            assertEquals("app1", result.getTargetAppId());
            assertEquals("msg-123", result.getInResponseToMessageId());
            assertEquals("user@example.com", result.getUserEmail());
            assertTrue(result.isUserKnown());
            assertEquals("http://remote:8080", result.getAppUrl());
            assertEquals("Remote App", result.getAppDisplayName());
            assertEquals("prod", result.getAppEnvironment());
        }

        @Test
        void hasCorrectMessageType() {
            AppResponseMessage message = new AppResponseMessage();
            assertEquals(DiscoveryMessage.MessageType.APP_RESPONSE, message.getType());
        }
    }

    @Nested
    class LoginTokenRequestMessageTests {

        @Test
        void serializationRoundTrip() throws Exception {
            LoginTokenRequestMessage message = new LoginTokenRequestMessage();
            message.setFromAppId("app1");
            message.setFromAppName("App One");
            message.setTargetAppId("app2");
            message.setUserEmail("user@example.com");
            message.setReturnUrl("http://app1/return");

            String json = objectMapper.writeValueAsString(message);
            DiscoveryMessage deserialized = objectMapper.readValue(json, DiscoveryMessage.class);

            assertInstanceOf(LoginTokenRequestMessage.class, deserialized);
            LoginTokenRequestMessage result = (LoginTokenRequestMessage) deserialized;
            assertEquals("app2", result.getTargetAppId());
            assertEquals("user@example.com", result.getUserEmail());
            assertEquals("http://app1/return", result.getReturnUrl());
        }

        @Test
        void hasCorrectMessageType() {
            LoginTokenRequestMessage message = new LoginTokenRequestMessage();
            assertEquals(DiscoveryMessage.MessageType.LOGIN_TOKEN_REQUEST, message.getType());
        }
    }

    @Nested
    class LoginTokenResponseMessageTests {

        @Test
        void serializationRoundTrip() throws Exception {
            LoginTokenResponseMessage message = new LoginTokenResponseMessage();
            message.setFromAppId("app2");
            message.setFromAppName("App Two");
            message.setTargetAppId("app1");
            message.setInResponseToMessageId("msg-456");
            message.setUserEmail("user@example.com");
            message.setEncryptedToken("encrypted-token-data");
            message.setLoginUrl("http://app2/discovery/login?token=abc");
            message.setTokenValidForSeconds(300);

            String json = objectMapper.writeValueAsString(message);
            DiscoveryMessage deserialized = objectMapper.readValue(json, DiscoveryMessage.class);

            assertInstanceOf(LoginTokenResponseMessage.class, deserialized);
            LoginTokenResponseMessage result = (LoginTokenResponseMessage) deserialized;
            assertEquals("app1", result.getTargetAppId());
            assertEquals("msg-456", result.getInResponseToMessageId());
            assertEquals("user@example.com", result.getUserEmail());
            assertEquals("encrypted-token-data", result.getEncryptedToken());
            assertEquals("http://app2/discovery/login?token=abc", result.getLoginUrl());
            assertEquals(300, result.getTokenValidForSeconds());
        }

        @Test
        void hasCorrectMessageType() {
            LoginTokenResponseMessage message = new LoginTokenResponseMessage();
            assertEquals(DiscoveryMessage.MessageType.LOGIN_TOKEN_RESPONSE, message.getType());
        }
    }

    @Nested
    class HeartbeatMessageTests {

        @Test
        void serializationRoundTrip() throws Exception {
            HeartbeatMessage message = new HeartbeatMessage();
            message.setFromAppId("app1");
            message.setFromAppName("App One");
            message.setAppUrl("http://localhost:8080");
            message.setAppVersion("1.5.0");
            message.setAppEnvironment("prod");
            message.setActiveUserCount(5);
            message.setActiveUserEmails(List.of("a@test.com", "b@test.com"));
            message.setPublicKey("pub-key");

            String json = objectMapper.writeValueAsString(message);
            DiscoveryMessage deserialized = objectMapper.readValue(json, DiscoveryMessage.class);

            assertInstanceOf(HeartbeatMessage.class, deserialized);
            HeartbeatMessage result = (HeartbeatMessage) deserialized;
            assertEquals("http://localhost:8080", result.getAppUrl());
            assertEquals("1.5.0", result.getAppVersion());
            assertEquals("prod", result.getAppEnvironment());
            assertEquals(5, result.getActiveUserCount());
            assertEquals(List.of("a@test.com", "b@test.com"), result.getActiveUserEmails());
            assertEquals("pub-key", result.getPublicKey());
        }

        @Test
        void hasCorrectMessageType() {
            HeartbeatMessage message = new HeartbeatMessage();
            assertEquals(DiscoveryMessage.MessageType.HEARTBEAT, message.getType());
        }

        @Test
        void handlesNullActiveUserEmails() throws Exception {
            HeartbeatMessage message = new HeartbeatMessage();
            message.setActiveUserEmails(null);
            message.setActiveUserCount(0);

            String json = objectMapper.writeValueAsString(message);
            DiscoveryMessage deserialized = objectMapper.readValue(json, DiscoveryMessage.class);

            HeartbeatMessage result = (HeartbeatMessage) deserialized;
            assertNull(result.getActiveUserEmails());
        }
    }

    @Nested
    class PolymorphicDeserialization {

        @Test
        void deserializesCorrectSubtypeFromTypeField() throws Exception {
            String json = """
                {"type":"USER_LOGIN","fromAppId":"app1","fromAppName":"App",
                 "userEmail":"u@t.com","userId":1,"userName":"U","appUrl":"http://x",
                 "appEnvironment":"dev","publicKey":"k"}
                """;

            DiscoveryMessage result = objectMapper.readValue(json, DiscoveryMessage.class);
            assertInstanceOf(UserLoginMessage.class, result);
        }

        @Test
        void deserializesHeartbeatFromTypeField() throws Exception {
            String json = """
                {"type":"HEARTBEAT","fromAppId":"app1","fromAppName":"App",
                 "appUrl":"http://x","appVersion":"1.0","appEnvironment":"dev",
                 "activeUserCount":0,"publicKey":"k"}
                """;

            DiscoveryMessage result = objectMapper.readValue(json, DiscoveryMessage.class);
            assertInstanceOf(HeartbeatMessage.class, result);
        }

        @Test
        void deserializesAppResponseFromTypeField() throws Exception {
            String json = """
                {"type":"APP_RESPONSE","fromAppId":"app1","fromAppName":"App",
                 "targetAppId":"app2","userEmail":"u@t.com","userKnown":true,
                 "appUrl":"http://x","appEnvironment":"dev"}
                """;

            DiscoveryMessage result = objectMapper.readValue(json, DiscoveryMessage.class);
            assertInstanceOf(AppResponseMessage.class, result);
        }

        @Test
        void deserializesLoginTokenRequestFromTypeField() throws Exception {
            String json = """
                {"type":"LOGIN_TOKEN_REQUEST","fromAppId":"app1","fromAppName":"App",
                 "targetAppId":"app2","userEmail":"u@t.com"}
                """;

            DiscoveryMessage result = objectMapper.readValue(json, DiscoveryMessage.class);
            assertInstanceOf(LoginTokenRequestMessage.class, result);
        }

        @Test
        void deserializesLoginTokenResponseFromTypeField() throws Exception {
            String json = """
                {"type":"LOGIN_TOKEN_RESPONSE","fromAppId":"app1","fromAppName":"App",
                 "targetAppId":"app2","userEmail":"u@t.com","encryptedToken":"tok",
                 "loginUrl":"http://x/login","tokenValidForSeconds":300}
                """;

            DiscoveryMessage result = objectMapper.readValue(json, DiscoveryMessage.class);
            assertInstanceOf(LoginTokenResponseMessage.class, result);
        }
    }

    @Nested
    class MessageIdGeneration {

        @Test
        void eachMessageHasUniqueId() {
            UserLoginMessage msg1 = new UserLoginMessage();
            UserLoginMessage msg2 = new UserLoginMessage();

            assertNotEquals(msg1.getMessageId(), msg2.getMessageId());
        }

        @Test
        void messageIdIsUuidFormat() {
            UserLoginMessage msg = new UserLoginMessage();
            // UUID format: 8-4-4-4-12 hex characters
            assertTrue(msg.getMessageId().matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
        }
    }
}
