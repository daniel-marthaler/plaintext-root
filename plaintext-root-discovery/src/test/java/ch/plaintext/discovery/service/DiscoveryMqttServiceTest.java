/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.discovery.service;

import ch.plaintext.discovery.dto.DiscoveryMessage;
import ch.plaintext.discovery.dto.HeartbeatMessage;
import ch.plaintext.discovery.dto.UserLoginMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscoveryMqttServiceTest {

    @Mock
    private MqttClient mqttClient;

    @Mock
    private DiscoveryMessageHandler messageHandler;

    private ObjectMapper objectMapper;
    private DiscoveryMqttService mqttService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mqttService = new DiscoveryMqttService(objectMapper, messageHandler);
        ReflectionTestUtils.setField(mqttService, "appId", "test-app");
        ReflectionTestUtils.setField(mqttService, "mqttClient", mqttClient);
    }

    @Nested
    class Init {

        @Test
        void standaloneWhenBrokerUrlIsNull() {
            ReflectionTestUtils.setField(mqttService, "brokerUrl", null);
            ReflectionTestUtils.setField(mqttService, "mqttClient", null);

            // Should return early without attempting MQTT connection
            mqttService.init();

            // mqttClient should remain null (no connection attempted)
            assertNull(ReflectionTestUtils.getField(mqttService, "mqttClient"));
        }

        @Test
        void standaloneWhenBrokerUrlIsBlank() {
            ReflectionTestUtils.setField(mqttService, "brokerUrl", "");
            ReflectionTestUtils.setField(mqttService, "mqttClient", null);

            mqttService.init();

            assertNull(ReflectionTestUtils.getField(mqttService, "mqttClient"));
        }

        @Test
        void standaloneWhenBrokerUrlIsWhitespace() {
            ReflectionTestUtils.setField(mqttService, "brokerUrl", "   ");
            ReflectionTestUtils.setField(mqttService, "mqttClient", null);

            mqttService.init();

            assertNull(ReflectionTestUtils.getField(mqttService, "mqttClient"));
        }
    }

    @Nested
    class HandleIncomingMessage {

        @Test
        void processesMessageSuccessfully() throws Exception {
            UserLoginMessage loginMessage = new UserLoginMessage();
            loginMessage.setFromAppId("other-app");
            loginMessage.setUserEmail("user@test.com");
            loginMessage.setAppUrl("http://other:8080");

            String payload = objectMapper.writeValueAsString(loginMessage);
            MqttMessage mqttMessage = new MqttMessage(payload.getBytes());

            invokeHandleIncomingMessage("plaintext/discovery", mqttMessage);

            verify(messageHandler).handleMessage(eq("plaintext/discovery"), any(UserLoginMessage.class));
        }

        @Test
        void ignoresOwnMessages() throws Exception {
            UserLoginMessage loginMessage = new UserLoginMessage();
            loginMessage.setFromAppId("test-app"); // same as our appId
            loginMessage.setUserEmail("user@test.com");

            String payload = objectMapper.writeValueAsString(loginMessage);
            MqttMessage mqttMessage = new MqttMessage(payload.getBytes());

            invokeHandleIncomingMessage("plaintext/discovery", mqttMessage);

            verifyNoInteractions(messageHandler);
        }

        @Test
        void handlesParseExceptionGracefully() throws Exception {
            MqttMessage mqttMessage = new MqttMessage("invalid json {{{".getBytes());

            // Should not throw
            invokeHandleIncomingMessage("plaintext/discovery", mqttMessage);

            verifyNoInteractions(messageHandler);
        }

        @Test
        void handlesHeartbeatMessage() throws Exception {
            HeartbeatMessage heartbeat = new HeartbeatMessage();
            heartbeat.setFromAppId("remote-app");
            heartbeat.setAppUrl("http://remote:8080");
            heartbeat.setAppVersion("2.0.0");
            heartbeat.setAppEnvironment("prod");

            String payload = objectMapper.writeValueAsString(heartbeat);
            MqttMessage mqttMessage = new MqttMessage(payload.getBytes());

            invokeHandleIncomingMessage("plaintext/heartbeat", mqttMessage);

            verify(messageHandler).handleMessage(eq("plaintext/heartbeat"), any(HeartbeatMessage.class));
        }

        @Test
        void handlesEmptyPayloadGracefully() throws Exception {
            MqttMessage mqttMessage = new MqttMessage("".getBytes());

            // Should not throw
            invokeHandleIncomingMessage("plaintext/discovery", mqttMessage);

            verifyNoInteractions(messageHandler);
        }

        private void invokeHandleIncomingMessage(String topic, MqttMessage message) throws Exception {
            Method method = DiscoveryMqttService.class
                .getDeclaredMethod("handleIncomingMessage", String.class, MqttMessage.class);
            method.setAccessible(true);
            method.invoke(mqttService, topic, message);
        }
    }

    @Nested
    class SubscribeToTopics {

        @Test
        void subscribesToAllExpectedTopics() throws Exception {
            Method method = DiscoveryMqttService.class
                .getDeclaredMethod("subscribeToTopics");
            method.setAccessible(true);
            method.invoke(mqttService);

            verify(mqttClient).subscribe("plaintext/discovery", 1);
            verify(mqttClient).subscribe("plaintext/response/test-app", 1);
            verify(mqttClient).subscribe("plaintext/login/test-app", 1);
            verify(mqttClient).subscribe("plaintext/heartbeat", 1);
            verify(mqttClient, times(4)).subscribe(anyString(), eq(1));
        }

        @Test
        void usesAppIdInResponseTopic() throws Exception {
            ReflectionTestUtils.setField(mqttService, "appId", "custom-app-id");

            Method method = DiscoveryMqttService.class
                .getDeclaredMethod("subscribeToTopics");
            method.setAccessible(true);
            method.invoke(mqttService);

            verify(mqttClient).subscribe("plaintext/response/custom-app-id", 1);
            verify(mqttClient).subscribe("plaintext/login/custom-app-id", 1);
        }
    }

    @Nested
    class PublishMessage {

        @Test
        void publishesMessageWithCorrectTopicAndPayload() throws Exception {
            when(mqttClient.isConnected()).thenReturn(true);

            HeartbeatMessage heartbeat = new HeartbeatMessage();
            heartbeat.setFromAppName("Test App");
            heartbeat.setAppUrl("http://localhost:8080");
            heartbeat.setAppVersion("1.0.0");
            heartbeat.setAppEnvironment("dev");

            mqttService.publishMessage("plaintext/heartbeat", heartbeat);

            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<MqttMessage> messageCaptor = ArgumentCaptor.forClass(MqttMessage.class);
            verify(mqttClient).publish(topicCaptor.capture(), messageCaptor.capture());

            assertEquals("plaintext/heartbeat", topicCaptor.getValue());

            MqttMessage published = messageCaptor.getValue();
            assertEquals(1, published.getQos());
            assertFalse(published.isRetained());

            // Verify the payload is valid JSON and can be deserialized
            String payload = new String(published.getPayload());
            DiscoveryMessage deserialized = objectMapper.readValue(payload, DiscoveryMessage.class);
            assertInstanceOf(HeartbeatMessage.class, deserialized);
        }

        @Test
        void setsAppIdOnMessage() throws Exception {
            when(mqttClient.isConnected()).thenReturn(true);

            UserLoginMessage message = new UserLoginMessage();
            message.setUserEmail("user@test.com");

            mqttService.publishMessage("plaintext/discovery", message);

            ArgumentCaptor<MqttMessage> captor = ArgumentCaptor.forClass(MqttMessage.class);
            verify(mqttClient).publish(anyString(), captor.capture());

            String payload = new String(captor.getValue().getPayload());
            assertTrue(payload.contains("\"fromAppId\":\"test-app\""));
        }

        @Test
        void doesNotPublishWhenClientNotConnected() throws Exception {
            when(mqttClient.isConnected()).thenReturn(false);

            mqttService.publishMessage("plaintext/heartbeat", new HeartbeatMessage());

            verify(mqttClient, never()).publish(anyString(), any(MqttMessage.class));
        }

        @Test
        void doesNotPublishWhenClientIsNull() throws Exception {
            ReflectionTestUtils.setField(mqttService, "mqttClient", null);

            // Should not throw
            mqttService.publishMessage("plaintext/heartbeat", new HeartbeatMessage());
        }

        @Test
        void handlesPublishExceptionGracefully() throws Exception {
            when(mqttClient.isConnected()).thenReturn(true);
            doThrow(new org.eclipse.paho.client.mqttv3.MqttException(0))
                .when(mqttClient).publish(anyString(), any(MqttMessage.class));

            // Should not throw
            mqttService.publishMessage("plaintext/heartbeat", new HeartbeatMessage());
        }
    }

    @Nested
    class PublishEncryptedMessage {

        @Test
        void delegatesToPublishMessage() throws Exception {
            when(mqttClient.isConnected()).thenReturn(true);

            HeartbeatMessage message = new HeartbeatMessage();
            mqttService.publishEncryptedMessage("plaintext/heartbeat", message, "recipient-key");

            verify(mqttClient).publish(eq("plaintext/heartbeat"), any(MqttMessage.class));
        }
    }

    @Nested
    class Cleanup {

        @Test
        void disconnectsAndClosesClient() throws Exception {
            mqttService.cleanup();

            verify(mqttClient).disconnect();
            verify(mqttClient).close();
        }

        @Test
        void handlesCleanupExceptionGracefully() throws Exception {
            doThrow(new org.eclipse.paho.client.mqttv3.MqttException(0))
                .when(mqttClient).disconnect();

            // Should not throw
            mqttService.cleanup();
        }

        @Test
        void handlesNullClientGracefully() {
            ReflectionTestUtils.setField(mqttService, "mqttClient", null);

            // Should not throw
            mqttService.cleanup();
        }
    }
}
