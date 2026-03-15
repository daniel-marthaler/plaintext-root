/*
 * Copyright (C) plaintext.ch, 2024.
 */
package ch.plaintext.menuesteuerung.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Module configuration for menuesteuerung.
 * Ensures that Spring Boot scans this module and dependencies.
 *
 * @author plaintext.ch
 * @since 1.42.0
 */
@Configuration
@ComponentScan(basePackages = {"ch.plaintext.menuesteuerung", "ch.plaintext.boot.menu"})
public class MenuesteuerungModuleConfiguration {
    // This configuration ensures that Spring Boot scans this module
    // and the menu module to make MenuRegistry available
}
