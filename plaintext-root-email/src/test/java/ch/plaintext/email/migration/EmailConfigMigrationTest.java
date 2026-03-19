/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.email.migration;

import ch.plaintext.email.model.EmailConfig;
import ch.plaintext.email.persistence.EmailConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailConfigMigrationTest {

    @Mock
    private EmailConfigRepository emailConfigRepository;

    @InjectMocks
    private EmailConfigMigration emailConfigMigration;

    @Test
    void migrateNullConfigNamesFixesNullName() {
        EmailConfig config = new EmailConfig();
        config.setId(42L);
        config.setMandat("test");
        config.setConfigName(null);

        when(emailConfigRepository.findAll()).thenReturn(List.of(config));
        when(emailConfigRepository.save(any(EmailConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        emailConfigMigration.migrateNullConfigNames_December2024();

        verify(emailConfigRepository).save(config);
        assertEquals("config_42", config.getConfigName());
    }

    @Test
    void migrateNullConfigNamesFixesEmptyName() {
        EmailConfig config = new EmailConfig();
        config.setId(10L);
        config.setMandat("test");
        config.setConfigName("");

        when(emailConfigRepository.findAll()).thenReturn(List.of(config));
        when(emailConfigRepository.save(any(EmailConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        emailConfigMigration.migrateNullConfigNames_December2024();

        verify(emailConfigRepository).save(config);
        assertEquals("config_10", config.getConfigName());
    }

    @Test
    void migrateNullConfigNamesFixesBlankName() {
        EmailConfig config = new EmailConfig();
        config.setId(5L);
        config.setMandat("test");
        config.setConfigName("   ");

        when(emailConfigRepository.findAll()).thenReturn(List.of(config));
        when(emailConfigRepository.save(any(EmailConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        emailConfigMigration.migrateNullConfigNames_December2024();

        verify(emailConfigRepository).save(config);
        assertEquals("config_5", config.getConfigName());
    }

    @Test
    void migrateNullConfigNamesSkipsValidConfigs() {
        EmailConfig config = new EmailConfig();
        config.setId(1L);
        config.setMandat("test");
        config.setConfigName("valid-name");

        when(emailConfigRepository.findAll()).thenReturn(List.of(config));

        emailConfigMigration.migrateNullConfigNames_December2024();

        verify(emailConfigRepository, never()).save(any());
        assertEquals("valid-name", config.getConfigName());
    }

    @Test
    void migrateNullConfigNamesHandlesEmptyList() {
        when(emailConfigRepository.findAll()).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> emailConfigMigration.migrateNullConfigNames_December2024());

        verify(emailConfigRepository, never()).save(any());
    }

    @Test
    void migrateNullConfigNamesHandlesMultipleConfigs() {
        EmailConfig validConfig = new EmailConfig();
        validConfig.setId(1L);
        validConfig.setMandat("test");
        validConfig.setConfigName("valid");

        EmailConfig nullConfig = new EmailConfig();
        nullConfig.setId(2L);
        nullConfig.setMandat("test");
        nullConfig.setConfigName(null);

        EmailConfig emptyConfig = new EmailConfig();
        emptyConfig.setId(3L);
        emptyConfig.setMandat("test");
        emptyConfig.setConfigName("");

        when(emailConfigRepository.findAll())
                .thenReturn(List.of(validConfig, nullConfig, emptyConfig));
        when(emailConfigRepository.save(any(EmailConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        emailConfigMigration.migrateNullConfigNames_December2024();

        // Only the null and empty configs should be saved
        verify(emailConfigRepository, times(2)).save(any(EmailConfig.class));
        assertEquals("valid", validConfig.getConfigName());
        assertEquals("config_2", nullConfig.getConfigName());
        assertEquals("config_3", emptyConfig.getConfigName());
    }

    @Test
    void migrateNullConfigNamesHandlesException() {
        when(emailConfigRepository.findAll())
                .thenThrow(new RuntimeException("DB error"));

        // Should not throw - errors are caught internally
        assertDoesNotThrow(() -> emailConfigMigration.migrateNullConfigNames_December2024());
    }
}
