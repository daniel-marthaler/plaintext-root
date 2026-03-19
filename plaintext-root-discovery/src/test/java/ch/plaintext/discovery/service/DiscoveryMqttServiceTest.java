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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
