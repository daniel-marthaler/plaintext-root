/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.email.migration;

import ch.plaintext.email.model.EmailConfig;
import ch.plaintext.email.persistence.EmailConfigRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailConfigMigrationTest {

    @Mock
    private EmailConfigRepository emailConfigRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private EmailConfigMigration emailConfigMigration;

    @BeforeEach
    void setUp() {
        // EntityManager is injected via @PersistenceContext, not constructor,
        // so we need to set it via reflection
        ReflectionTestUtils.setField(emailConfigMigration, "entityManager", entityManager);
    }

    // =========================================================================
    // migrateNullConfigNames_December2024 tests (existing)
    // =========================================================================

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

    // =========================================================================
    // runMigrations tests
    // =========================================================================

    @Test
    void runMigrations_callsBothMigrationsInTransactions() {
        // Make TransactionTemplate actually execute the callbacks
        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            TransactionCallback<?> callback = inv.getArgument(0);
            return callback.doInTransaction(null);
        });

        // Set up mocks for migrateToEmailConfigV2 (old table empty -> skip)
        Query countQuery = mock(Query.class);
        when(entityManager.createNativeQuery("SELECT COUNT(*) FROM email_config")).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(0L);

        // Set up mocks for migrateNullConfigNames (empty list -> no fixes)
        when(emailConfigRepository.findAll()).thenReturn(Collections.emptyList());

        emailConfigMigration.runMigrations();

        // Verify TransactionTemplate was called twice (once per migration)
        verify(transactionTemplate, times(2)).execute(any());
        // Verify the V2 migration was attempted
        verify(entityManager).createNativeQuery("SELECT COUNT(*) FROM email_config");
        // Verify the null config names migration was attempted
        verify(emailConfigRepository).findAll();
    }

    // =========================================================================
    // migrateToEmailConfigV2_December2024 tests (via runMigrations)
    // =========================================================================

    @Test
    void migrateToV2_oldTableEmpty_skipsMigration() {
        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            TransactionCallback<?> callback = inv.getArgument(0);
            return callback.doInTransaction(null);
        });

        // Old table count returns 0
        Query countQuery = mock(Query.class);
        when(entityManager.createNativeQuery("SELECT COUNT(*) FROM email_config")).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(0L);

        // For the second migration (null config names)
        when(emailConfigRepository.findAll()).thenReturn(Collections.emptyList());

        emailConfigMigration.runMigrations();

        // Should NOT check new table or execute INSERT
        verify(emailConfigRepository, never()).count();
    }

    @Test
    void migrateToV2_newTableAlreadyHasData_skipsMigration() {
        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            TransactionCallback<?> callback = inv.getArgument(0);
            return callback.doInTransaction(null);
        });

        // Old table has data
        Query countQuery = mock(Query.class);
        when(entityManager.createNativeQuery("SELECT COUNT(*) FROM email_config")).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(5L);

        // New table already has entries
        when(emailConfigRepository.count()).thenReturn(3L);

        // For the second migration
        when(emailConfigRepository.findAll()).thenReturn(Collections.emptyList());

        emailConfigMigration.runMigrations();

        // Should NOT execute INSERT since new table already has data
        verify(entityManager, never()).createNativeQuery(contains("INSERT INTO email_config_v2"));
    }

    @Test
    void migrateToV2_successfulMigration_executesInsert() {
        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            TransactionCallback<?> callback = inv.getArgument(0);
            return callback.doInTransaction(null);
        });

        // Old table has data
        Query countQuery = mock(Query.class);
        when(entityManager.createNativeQuery("SELECT COUNT(*) FROM email_config")).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(3L);

        // New table is empty
        when(emailConfigRepository.count()).thenReturn(0L);

        // INSERT query
        Query insertQuery = mock(Query.class);
        when(entityManager.createNativeQuery(contains("INSERT INTO email_config_v2"))).thenReturn(insertQuery);
        when(insertQuery.executeUpdate()).thenReturn(3);

        // For the second migration
        when(emailConfigRepository.findAll()).thenReturn(Collections.emptyList());

        emailConfigMigration.runMigrations();

        // Verify the INSERT was executed
        verify(insertQuery).executeUpdate();
    }

    @Test
    void migrateToV2_exceptionWithEmailConfigInMessage_treatedAsNewInstallation() {
        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            TransactionCallback<?> callback = inv.getArgument(0);
            return callback.doInTransaction(null);
        });

        // Throw exception with "email_config" in the message (table doesn't exist)
        when(entityManager.createNativeQuery("SELECT COUNT(*) FROM email_config"))
                .thenThrow(new RuntimeException("Table email_config does not exist"));

        // For the second migration
        when(emailConfigRepository.findAll()).thenReturn(Collections.emptyList());

        // Should not throw - treated as new installation
        assertDoesNotThrow(() -> emailConfigMigration.runMigrations());

        // Should not attempt any further queries for V2 migration
        verify(emailConfigRepository, never()).count();
    }

    @Test
    void migrateToV2_exceptionWithoutEmailConfigInMessage_loggedAsError() {
        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            TransactionCallback<?> callback = inv.getArgument(0);
            return callback.doInTransaction(null);
        });

        // Throw exception without "email_config" in the message
        when(entityManager.createNativeQuery("SELECT COUNT(*) FROM email_config"))
                .thenThrow(new RuntimeException("Connection refused"));

        // For the second migration
        when(emailConfigRepository.findAll()).thenReturn(Collections.emptyList());

        // Should not throw - errors are caught internally
        assertDoesNotThrow(() -> emailConfigMigration.runMigrations());
    }

    @Test
    void migrateToV2_exceptionWithNullMessage_loggedAsError() {
        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            TransactionCallback<?> callback = inv.getArgument(0);
            return callback.doInTransaction(null);
        });

        // Throw exception with null message
        when(entityManager.createNativeQuery("SELECT COUNT(*) FROM email_config"))
                .thenThrow(new RuntimeException((String) null));

        // For the second migration
        when(emailConfigRepository.findAll()).thenReturn(Collections.emptyList());

        // Should not throw - the null message check should not cause NPE
        assertDoesNotThrow(() -> emailConfigMigration.runMigrations());
    }

    /**
     * Helper to create a Mockito argument matcher for strings containing a substring.
     */
    private static String contains(String substring) {
        return argThat(argument -> argument != null && argument.contains(substring));
    }
}
