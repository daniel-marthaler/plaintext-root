/*
 * Copyright (C) plaintext.ch, 2024.
 */
package ch.plaintext.menuesteuerung.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;

/**
 * JPA configuration for the menuesteuerung module.
 * Ensures that entities are properly scanned.
 * Repository scanning is handled by Spring Boot's auto-configuration.
 *
 * @author plaintext.ch
 * @since 1.42.0
 */
@Configuration
@EntityScan(basePackages = "ch.plaintext.menuesteuerung.model")
public class MenuesteuerungJpaConfig {
}
