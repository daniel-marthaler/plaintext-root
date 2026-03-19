/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.email;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "ch.plaintext.email")
public class EmailModuleConfiguration {
    // This configuration ensures that Spring Boot scans this module
    // Entities are auto-scanned by Spring Boot from the 'ch' package and sub-packages
}
