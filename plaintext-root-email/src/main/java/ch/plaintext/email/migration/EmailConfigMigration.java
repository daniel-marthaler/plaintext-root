package ch.plaintext.email.migration;

import ch.plaintext.email.model.EmailConfig;
import ch.plaintext.email.persistence.EmailConfigRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * Migration component that fixes email configurations with NULL or empty config_name values.
 * This runs automatically on application startup.
 *
 * @author info@plaintext.ch
 * @since 2024
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EmailConfigMigration {

    private final EmailConfigRepository emailConfigRepository;
    private final TransactionTemplate transactionTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    @PostConstruct
    public void runMigrations() {
        // Run all migrations in order within transactions
        transactionTemplate.execute(status -> {
            migrateToEmailConfigV2_December2024();
            return null;
        });

        transactionTemplate.execute(status -> {
            migrateNullConfigNames_December2024();
            return null;
        });
    }

    /**
     * Migrates data from old email_config table to new email_config_v2 table.
     * This migration was added in December 2024 to fix constraint issues with the old table.
     *
     * TODO: Remove this migration method after 2025-03-01 (3 months after deployment)
     *       By then, all production instances should have been migrated.
     *
     * @deprecated This is a temporary migration that should be removed after 2025-03-01.
     *             Check production logs to ensure all data has been migrated to email_config_v2,
     *             then delete this method and the old email_config table.
     */
    @Deprecated(since = "1.94.0", forRemoval = true)
    private void migrateToEmailConfigV2_December2024() {
        log.info("Running EmailConfig migration - migrating data from email_config to email_config_v2");

        try {
            // Check if old table exists and has data
            Long oldTableCount = ((Number) entityManager.createNativeQuery(
                    "SELECT COUNT(*) FROM email_config")
                    .getSingleResult()).longValue();

            if (oldTableCount == 0) {
                log.info("Old email_config table is empty - no migration needed");
                return;
            }

            // Check if new table already has data (migration already ran)
            Long newTableCount = (Long) emailConfigRepository.count();
            if (newTableCount > 0) {
                log.info("email_config_v2 table already has {} entries - skipping migration", newTableCount);
                return;
            }

            // Copy all valid configurations from old table to new table
            // Using native SQL to avoid JPA mapping issues
            int migratedCount = entityManager.createNativeQuery(
                    "INSERT INTO email_config_v2 " +
                    "(mandat, config_name, smtp_host, smtp_port, smtp_username, smtp_password, " +
                    "smtp_use_tls, smtp_use_ssl, smtp_from_address, smtp_from_name, smtp_enabled, " +
                    "imap_enabled, imap_host, imap_port, imap_username, imap_password, " +
                    "imap_use_ssl, imap_folder, imap_mark_as_read, imap_delete_after_fetch, " +
                    "imap_poll_interval, created_at, updated_at) " +
                    "SELECT mandat, " +
                    "CASE WHEN config_name IS NULL OR TRIM(config_name) = '' " +
                    "     THEN CONCAT('config_', CAST(id AS VARCHAR)) " +
                    "     ELSE config_name END, " +
                    "smtp_host, smtp_port, smtp_username, smtp_password, " +
                    "smtp_use_tls, smtp_use_ssl, smtp_from_address, smtp_from_name, smtp_enabled, " +
                    "imap_enabled, imap_host, imap_port, imap_username, imap_password, " +
                    "imap_use_ssl, imap_folder, imap_mark_as_read, imap_delete_after_fetch, " +
                    "imap_poll_interval, created_at, updated_at " +
                    "FROM email_config"
            ).executeUpdate();

            log.info("EmailConfig migration completed - Migrated {} configuration(s) from email_config to email_config_v2", migratedCount);

        } catch (Exception e) {
            // Table might not exist in new installations
            if (e.getMessage() != null && e.getMessage().contains("email_config")) {
                log.info("Old email_config table does not exist - this is a new installation, no migration needed");
            } else {
                log.error("Error during EmailConfig table migration", e);
                // Don't throw exception to prevent application startup failure
            }
        }
    }

    /**
     * Fixes email configurations with NULL or empty config_name values.
     * This migration was added in December 2024 to fix data quality issues.
     *
     * TODO: Remove this migration method after 2025-03-01 (3 months after deployment)
     *       By then, all production instances should have been migrated.
     *
     * @deprecated This is a temporary migration that should be removed after 2025-03-01.
     *             Check production logs to ensure all NULL config_name values have been fixed,
     *             then delete this method and its test.
     */
    @Deprecated(since = "1.94.0", forRemoval = true)
    void migrateNullConfigNames_December2024() {
        log.info("Running EmailConfig migration - checking for NULL or empty config_name values");

        try {
            // Get all email configurations
            List<EmailConfig> allConfigs = emailConfigRepository.findAll();

            int fixedCount = 0;
            for (EmailConfig config : allConfigs) {
                if (config.getConfigName() == null || config.getConfigName().trim().isEmpty()) {
                    String oldName = config.getConfigName();
                    String newName = "config_" + config.getId();

                    log.warn("Found email config with NULL/empty name - ID: {}, Mandat: {}, Old Name: '{}'. Setting to: '{}'",
                            config.getId(), config.getMandat(), oldName, newName);

                    config.setConfigName(newName);
                    emailConfigRepository.save(config);
                    fixedCount++;
                }
            }

            if (fixedCount > 0) {
                log.info("EmailConfig migration completed - Fixed {} configuration(s) with NULL/empty config_name", fixedCount);
            } else {
                log.info("EmailConfig migration completed - No configurations needed fixing");
            }

        } catch (Exception e) {
            log.error("Error during EmailConfig migration", e);
            // Don't throw exception to prevent application startup failure
            // The validation in EmailService will catch issues when users try to save
        }
    }
}
