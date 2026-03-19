/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.discovery.service;

import ch.plaintext.discovery.dto.DiscoveryMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.CompletableFuture;

/**
 * MQTT service for discovery message communication between app instances
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DiscoveryMqttService {

    @Value("${discovery.mqtt.broker:tcp://192.168.1.224:1883}")
    private String brokerUrl;
    
    @Value("${discovery.mqtt.clientId:plaintext-discovery}")
    private String baseClientId;
    
    @Value("${discovery.app.id:${spring.application.name:plaintext}}")
    private String appId;
    
    private final ObjectMapper objectMapper;
    @Lazy
    private final DiscoveryMessageHandler messageHandler;
    
    private MqttClient mqttClient;
    // Topic listeners can be added later if needed
    
    @PostConstruct
    public void init() {
        CompletableFuture.runAsync(() -> {
            try {
                connectToMqtt();
                subscribeToTopics();
                log.info("Discovery MQTT service initialized for app: {}", appId);
            } catch (Exception e) {
                log.error("Failed to initialize Discovery MQTT service: {}", e.getMessage());
            }
        });
    }
    
    private void connectToMqtt() throws MqttException {
        String clientId = baseClientId + "-" + appId + "-" + System.currentTimeMillis();
        mqttClient = new MqttClient(brokerUrl, clientId);
        
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(60);
        options.setAutomaticReconnect(true);
        
        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                log.warn("MQTT connection lost", cause);
            }
            
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                handleIncomingMessage(topic, message);
            }
            
            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // Not used for subscriptions
            }
        });
        
        mqttClient.connect(options);
        log.info("Connected to MQTT broker: {} with client ID: {}", brokerUrl, clientId);
    }
    
    private void subscribeToTopics() throws MqttException {
        // Subscribe to discovery announcements
        mqttClient.subscribe("plaintext/discovery", 1);
        
        // Subscribe to responses for this app
        mqttClient.subscribe("plaintext/response/" + appId, 1);
        
        // Subscribe to login token requests for this app
        mqttClient.subscribe("plaintext/login/" + appId, 1);
        
        // Subscribe to heartbeat monitoring
        mqttClient.subscribe("plaintext/heartbeat", 1);
        
        log.info("Subscribed to discovery topics for app: {}", appId);
    }
    
    private void handleIncomingMessage(String topic, MqttMessage mqttMessage) {
        try {
            String payload = new String(mqttMessage.getPayload());
            log.debug("Received MQTT message on topic {}: {}", topic, payload);
            
            DiscoveryMessage message = objectMapper.readValue(payload, DiscoveryMessage.class);
            
            // Don't process our own messages
            if (appId.equals(message.getFromAppId())) {
                return;
            }
            
            messageHandler.handleMessage(topic, message);
            
        } catch (Exception e) {
            log.error("Error processing MQTT message on topic {}: {}", topic, e.getMessage(), e);
        }
    }
    
    /**
     * Publish a discovery message to the specified topic
     */
    public void publishMessage(String topic, DiscoveryMessage message) {
        try {
            if (mqttClient == null || !mqttClient.isConnected()) {
                log.warn("MQTT client not connected, cannot publish message to topic: {}", topic);
                return;
            }
            
            message.setFromAppId(appId);
            String payload = objectMapper.writeValueAsString(message);
            
            MqttMessage mqttMessage = new MqttMessage(payload.getBytes());
            mqttMessage.setQos(1);
            mqttMessage.setRetained(false);
            
            mqttClient.publish(topic, mqttMessage);
            log.debug("Published message to topic {}: {}", topic, payload);
            
        } catch (Exception e) {
            log.error("Error publishing MQTT message to topic {}: {}", topic, e.getMessage(), e);
        }
    }
    
    /**
     * Publish an encrypted message. Per-field encryption (e.g. token) is handled in DiscoveryService.
     * This method publishes the message as-is (fields already encrypted where needed).
     */
    public void publishEncryptedMessage(String topic, DiscoveryMessage message, String recipientPublicKey) {
        try {
            publishMessage(topic, message);
        } catch (Exception e) {
            log.error("Error publishing encrypted MQTT message: {}", e.getMessage(), e);
        }
    }
    
    @PreDestroy
    public void cleanup() {
        if (mqttClient != null) {
            try {
                mqttClient.disconnect();
                mqttClient.close();
                log.info("MQTT client disconnected");
            } catch (MqttException e) {
                log.warn("Error disconnecting MQTT client", e);
            }
        }
    }
}