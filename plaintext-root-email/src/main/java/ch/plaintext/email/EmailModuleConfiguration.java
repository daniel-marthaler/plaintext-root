package ch.plaintext.email;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "ch.plaintext.email")
public class EmailModuleConfiguration {
    // This configuration ensures that Spring Boot scans this module
    // Entities are auto-scanned by Spring Boot from the 'ch' package and sub-packages
}
