/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.boot.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test verifying security configuration
 * allows/blocks expected URL patterns against a real PostgreSQL.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("testcontainers")
class SecurityTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("plaintext_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @LocalServerPort
    private int port;

    @Test
    void versionEndpointIsPublic() {
        RestClient client = RestClient.create("http://localhost:" + port);
        String version = client.get().uri("/nosec/version").retrieve().body(String.class);
        assertNotNull(version);
    }

    @Test
    void healthEndpointIsPublic() {
        RestClient client = RestClient.create("http://localhost:" + port);
        String health = client.get().uri("/actuator/health").retrieve().body(String.class);
        assertNotNull(health);
        assertTrue(health.contains("status"));
    }

    @Test
    void applicationStartsSuccessfully() {
        // If we reach this point, the full application context with PostgreSQL
        // started without errors — security config, Flyway, JPA, etc.
        assertTrue(port > 0);
    }
}
