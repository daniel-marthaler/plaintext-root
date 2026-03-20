/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.discovery.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DiscoveryPropertiesTest {

    @Nested
    class Defaults {

        @Test
        void mqttDefaults() {
            DiscoveryProperties props = new DiscoveryProperties();

            assertNotNull(props.getMqtt());
            assertEquals("", props.getMqtt().getBroker());
            assertEquals("plaintext-discovery", props.getMqtt().getClientId());
            assertEquals(10, props.getMqtt().getConnectionTimeoutSeconds());
            assertEquals(60, props.getMqtt().getKeepAliveIntervalSeconds());
        }

        @Test
        void appDefaults() {
            DiscoveryProperties props = new DiscoveryProperties();

            assertNotNull(props.getApp());
            assertNull(props.getApp().getId());
            assertEquals("Plaintext App", props.getApp().getName());
            assertEquals("dev", props.getApp().getEnvironment());
            assertEquals("unknown", props.getApp().getVersion());
            assertNull(props.getApp().getBaseUrl());
        }

        @Test
        void heartbeatDefaults() {
            DiscoveryProperties props = new DiscoveryProperties();

            assertNotNull(props.getHeartbeat());
            assertTrue(props.getHeartbeat().isEnabled());
            assertEquals(120000, props.getHeartbeat().getIntervalMs());
            assertEquals(600000, props.getHeartbeat().getCleanupIntervalMs());
            assertEquals(6, props.getHeartbeat().getSessionTimeoutHours());
        }

        @Test
        void tokenDefaults() {
            DiscoveryProperties props = new DiscoveryProperties();

            assertNotNull(props.getToken());
            assertEquals(300, props.getToken().getValiditySeconds());
            assertTrue(props.getToken().isEncryptionEnabled());
        }
    }

    @Nested
    class MqttConfiguration {

        @Test
        void customMqttSettings() {
            DiscoveryProperties.Mqtt mqtt = new DiscoveryProperties.Mqtt();
            mqtt.setBroker("tcp://broker.example.com:1883");
            mqtt.setClientId("custom-client");
            mqtt.setConnectionTimeoutSeconds(30);
            mqtt.setKeepAliveIntervalSeconds(120);

            assertEquals("tcp://broker.example.com:1883", mqtt.getBroker());
            assertEquals("custom-client", mqtt.getClientId());
            assertEquals(30, mqtt.getConnectionTimeoutSeconds());
            assertEquals(120, mqtt.getKeepAliveIntervalSeconds());
        }
    }

    @Nested
    class AppConfiguration {

        @Test
        void customAppSettings() {
            DiscoveryProperties.App app = new DiscoveryProperties.App();
            app.setId("my-app-id");
            app.setName("My Application");
            app.setEnvironment("prod");
            app.setVersion("2.0.0");
            app.setBaseUrl("https://myapp.example.com");

            assertEquals("my-app-id", app.getId());
            assertEquals("My Application", app.getName());
            assertEquals("prod", app.getEnvironment());
            assertEquals("2.0.0", app.getVersion());
            assertEquals("https://myapp.example.com", app.getBaseUrl());
        }
    }

    @Nested
    class HeartbeatConfiguration {

        @Test
        void customHeartbeatSettings() {
            DiscoveryProperties.Heartbeat heartbeat = new DiscoveryProperties.Heartbeat();
            heartbeat.setEnabled(false);
            heartbeat.setIntervalMs(60000);
            heartbeat.setCleanupIntervalMs(300000);
            heartbeat.setSessionTimeoutHours(12);

            assertFalse(heartbeat.isEnabled());
            assertEquals(60000, heartbeat.getIntervalMs());
            assertEquals(300000, heartbeat.getCleanupIntervalMs());
            assertEquals(12, heartbeat.getSessionTimeoutHours());
        }
    }

    @Nested
    class TokenConfiguration {

        @Test
        void customTokenSettings() {
            DiscoveryProperties.Token token = new DiscoveryProperties.Token();
            token.setValiditySeconds(600);
            token.setEncryptionEnabled(false);

            assertEquals(600, token.getValiditySeconds());
            assertFalse(token.isEncryptionEnabled());
        }
    }

    @Nested
    class NestedObjectsAreReplaceable {

        @Test
        void canReplaceNestedObjects() {
            DiscoveryProperties props = new DiscoveryProperties();

            DiscoveryProperties.Mqtt newMqtt = new DiscoveryProperties.Mqtt();
            newMqtt.setBroker("tcp://new-broker:1883");
            props.setMqtt(newMqtt);

            DiscoveryProperties.App newApp = new DiscoveryProperties.App();
            newApp.setName("New App");
            props.setApp(newApp);

            DiscoveryProperties.Heartbeat newHeartbeat = new DiscoveryProperties.Heartbeat();
            newHeartbeat.setEnabled(false);
            props.setHeartbeat(newHeartbeat);

            DiscoveryProperties.Token newToken = new DiscoveryProperties.Token();
            newToken.setValiditySeconds(60);
            props.setToken(newToken);

            assertEquals("tcp://new-broker:1883", props.getMqtt().getBroker());
            assertEquals("New App", props.getApp().getName());
            assertFalse(props.getHeartbeat().isEnabled());
            assertEquals(60, props.getToken().getValiditySeconds());
        }
    }
}
