/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.discovery.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DiscoveryAutoConfigurationTest {

    @Nested
    class Initialization {

        @Test
        void initLogsWithValidProperties() {
            DiscoveryProperties props = new DiscoveryProperties();
            props.getApp().setEnvironment("prod");
            props.getMqtt().setBroker("tcp://broker:1883");

            DiscoveryAutoConfiguration config = new DiscoveryAutoConfiguration(props);

            // Should not throw
            config.init();
        }

        @Test
        void initHandlesNullAppProperty() {
            DiscoveryProperties props = new DiscoveryProperties();
            props.setApp(null);

            DiscoveryAutoConfiguration config = new DiscoveryAutoConfiguration(props);

            // Should not throw - handles null gracefully
            config.init();
        }

        @Test
        void initHandlesNullMqttProperty() {
            DiscoveryProperties props = new DiscoveryProperties();
            props.setMqtt(null);

            DiscoveryAutoConfiguration config = new DiscoveryAutoConfiguration(props);

            // Should not throw - handles null gracefully
            config.init();
        }

        @Test
        void initHandlesNullProperties() {
            DiscoveryAutoConfiguration config = new DiscoveryAutoConfiguration(null);

            // Should not throw
            config.init();
        }
    }
}
