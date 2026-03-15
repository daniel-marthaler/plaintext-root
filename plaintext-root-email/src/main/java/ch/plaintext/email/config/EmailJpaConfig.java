/*
 * Copyright (C) plaintext.ch, 2024.
 */
package ch.plaintext.email.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;

/**
 * JPA configuration for the email module.
 * Ensures that entities are properly scanned.
 * Repository scanning is handled by Spring Boot's auto-configuration.
 *
 * @author plaintext.ch
 * @since 1.59.0
 */
@Configuration
@EntityScan(basePackages = "ch.plaintext.email.model")
public class EmailJpaConfig {
}
