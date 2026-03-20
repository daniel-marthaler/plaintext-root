/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.email.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
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
