/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.discovery.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Auto-configuration for Discovery service
 */
@Configuration
@EnableConfigurationProperties(DiscoveryProperties.class)
@ComponentScan(basePackages = "ch.plaintext.discovery")
@EnableScheduling
@ConditionalOnProperty(value = "discovery.enabled", havingValue = "true", matchIfMissing = false)
@Slf4j
@RequiredArgsConstructor
public class DiscoveryAutoConfiguration {
    
    private final DiscoveryProperties properties;
    
    @PostConstruct
    public void init() {
        log.info("🌐 Discovery Multi-Instance Navigation enabled");
        log.info("   Environment: {}", getEnvironmentName());
        log.info("   MQTT Broker: {}", getMqttBroker());
    }
    
    private String getEnvironmentName() {
        return properties != null && properties.getApp() != null ? 
            properties.getApp().getEnvironment() : "unknown";
    }
    
    private String getMqttBroker() {
        return properties != null && properties.getMqtt() != null ? 
            properties.getMqtt().getBroker() : "default";
    }
}