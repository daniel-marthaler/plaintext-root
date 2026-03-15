package ch.plaintext.boot.plugins.security.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;

/**
 * JPA configuration for all entities.
 * Scans all packages for JPA entities including ch.plaintext, ch.emad, and ch packages.
 */
@Configuration
@EntityScan(basePackages = {"ch.plaintext", "ch.emad", "ch"})
public class SecurityJpaConfig {
}
