package ch.plaintext.discovery.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Discovery service
 */
@ConfigurationProperties(prefix = "discovery")
@Data
public class DiscoveryProperties {
    
    /**
     * MQTT broker configuration
     */
    private Mqtt mqtt = new Mqtt();
    
    /**
     * App identification
     */
    private App app = new App();
    
    /**
     * Heartbeat configuration
     */
    private Heartbeat heartbeat = new Heartbeat();
    
    /**
     * Token configuration
     */
    private Token token = new Token();
    
    @Data
    public static class Mqtt {
        /**
         * MQTT broker URL (default: tcp://192.168.1.224:1883)
         */
        private String broker = "tcp://192.168.1.224:1883";
        
        /**
         * Client ID prefix (default: plaintext-discovery)
         */
        private String clientId = "plaintext-discovery";
        
        /**
         * Connection timeout in seconds
         */
        private int connectionTimeoutSeconds = 10;
        
        /**
         * Keep-alive interval in seconds  
         */
        private int keepAliveIntervalSeconds = 60;
    }
    
    @Data
    public static class App {
        /**
         * Unique app ID (default: spring.application.name)
         */
        private String id;
        
        /**
         * Human-readable app name
         */
        private String name = "Plaintext App";
        
        /**
         * Environment: prod, dev, int, test
         */
        private String environment = "dev";
        
        /**
         * App version (for display in discovery stats)
         */
        private String version = "unknown";
        
        /**
         * Base URL for this app instance
         */
        private String baseUrl;
    }
    
    @Data
    public static class Heartbeat {
        /**
         * Enable/disable heartbeat sending
         */
        private boolean enabled = true;
        
        /**
         * Heartbeat interval in milliseconds (default: 2 minutes)
         */
        private long intervalMs = 120000;
        
        /**
         * Cleanup interval in milliseconds (default: 10 minutes)
         */
        private long cleanupIntervalMs = 600000;
        
        /**
         * Session timeout in hours (mark as inactive after this)
         */
        private int sessionTimeoutHours = 6;
    }
    
    @Data
    public static class Token {
        /**
         * Cross-app login token validity in seconds (default: 5 minutes)
         */
        private int validitySeconds = 300;
        
        /**
         * Enable PKI encryption for tokens
         */
        private boolean encryptionEnabled = true;
    }
}