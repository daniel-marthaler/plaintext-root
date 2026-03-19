/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.email.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EmailConfigTest {

    @Test
    void defaultValues() {
        EmailConfig config = new EmailConfig();

        assertEquals(587, config.getSmtpPort());
        assertTrue(config.isSmtpUseTls());
        assertFalse(config.isSmtpUseSsl());
        assertFalse(config.isSmtpEnabled());
        assertFalse(config.isImapEnabled());
        assertEquals(993, config.getImapPort());
        assertTrue(config.isImapUseSsl());
        assertEquals("INBOX", config.getImapFolder());
        assertTrue(config.isImapMarkAsRead());
        assertFalse(config.isImapDeleteAfterFetch());
        assertEquals(5, config.getImapPollInterval());
    }

    @Test
    void prePersistSetsTimestamps() {
        EmailConfig config = new EmailConfig();
        assertNull(config.getCreatedAt());
        assertNull(config.getUpdatedAt());

        config.prePersist();

        assertNotNull(config.getCreatedAt());
        assertNotNull(config.getUpdatedAt());
    }

    @Test
    void prePersistDoesNotOverwriteExistingCreatedAt() {
        EmailConfig config = new EmailConfig();
        LocalDateTime fixed = LocalDateTime.of(2024, 6, 15, 10, 30);
        config.setCreatedAt(fixed);

        config.prePersist();

        assertEquals(fixed, config.getCreatedAt());
        assertNotNull(config.getUpdatedAt());
    }

    @Test
    void preUpdateSetsUpdatedAt() {
        EmailConfig config = new EmailConfig();
        assertNull(config.getUpdatedAt());

        config.preUpdate();

        assertNotNull(config.getUpdatedAt());
    }

    // --- isSmtpConfigured tests ---

    @Test
    void isSmtpConfiguredReturnsFalseWhenDisabled() {
        EmailConfig config = buildSmtpConfig();
        config.setSmtpEnabled(false);

        assertFalse(config.isSmtpConfigured());
    }

    @Test
    void isSmtpConfiguredReturnsTrueWhenFullyConfigured() {
        EmailConfig config = buildSmtpConfig();
        // New config (id == null), so password is required
        config.setSmtpPassword("secret");

        assertTrue(config.isSmtpConfigured());
    }

    @Test
    void isSmtpConfiguredReturnsFalseWhenHostMissing() {
        EmailConfig config = buildSmtpConfig();
        config.setSmtpPassword("secret");
        config.setSmtpHost(null);

        assertFalse(config.isSmtpConfigured());
    }

    @Test
    void isSmtpConfiguredReturnsFalseWhenHostBlank() {
        EmailConfig config = buildSmtpConfig();
        config.setSmtpPassword("secret");
        config.setSmtpHost("   ");

        assertFalse(config.isSmtpConfigured());
    }

    @Test
    void isSmtpConfiguredReturnsFalseWhenUsernameMissing() {
        EmailConfig config = buildSmtpConfig();
        config.setSmtpPassword("secret");
        config.setSmtpUsername(null);

        assertFalse(config.isSmtpConfigured());
    }

    @Test
    void isSmtpConfiguredReturnsFalseWhenFromAddressMissing() {
        EmailConfig config = buildSmtpConfig();
        config.setSmtpPassword("secret");
        config.setSmtpFromAddress(null);

        assertFalse(config.isSmtpConfigured());
    }

    @Test
    void isSmtpConfiguredReturnsFalseWhenNewConfigWithoutPassword() {
        EmailConfig config = buildSmtpConfig();
        // id is null (new config) and no password
        config.setSmtpPassword(null);

        assertFalse(config.isSmtpConfigured());
    }

    @Test
    void isSmtpConfiguredReturnsTrueForExistingConfigWithoutPassword() {
        EmailConfig config = buildSmtpConfig();
        config.setId(1L); // Existing config
        config.setSmtpPassword(null); // Password might be empty in form

        assertTrue(config.isSmtpConfigured());
    }

    // --- isImapConfigured tests ---

    @Test
    void isImapConfiguredReturnsFalseWhenDisabled() {
        EmailConfig config = buildImapConfig();
        config.setImapEnabled(false);

        assertFalse(config.isImapConfigured());
    }

    @Test
    void isImapConfiguredReturnsTrueWhenFullyConfigured() {
        EmailConfig config = buildImapConfig();
        config.setImapPassword("secret");

        assertTrue(config.isImapConfigured());
    }

    @Test
    void isImapConfiguredReturnsFalseWhenHostMissing() {
        EmailConfig config = buildImapConfig();
        config.setImapPassword("secret");
        config.setImapHost(null);

        assertFalse(config.isImapConfigured());
    }

    @Test
    void isImapConfiguredReturnsFalseWhenHostBlank() {
        EmailConfig config = buildImapConfig();
        config.setImapPassword("secret");
        config.setImapHost("  ");

        assertFalse(config.isImapConfigured());
    }

    @Test
    void isImapConfiguredReturnsFalseWhenUsernameMissing() {
        EmailConfig config = buildImapConfig();
        config.setImapPassword("secret");
        config.setImapUsername(null);

        assertFalse(config.isImapConfigured());
    }

    @Test
    void isImapConfiguredReturnsFalseWhenNewConfigWithoutPassword() {
        EmailConfig config = buildImapConfig();
        config.setImapPassword(null);

        assertFalse(config.isImapConfigured());
    }

    @Test
    void isImapConfiguredReturnsTrueForExistingConfigWithoutPassword() {
        EmailConfig config = buildImapConfig();
        config.setId(1L); // Existing config
        config.setImapPassword(null);

        assertTrue(config.isImapConfigured());
    }

    // --- helpers ---

    private EmailConfig buildSmtpConfig() {
        EmailConfig config = new EmailConfig();
        config.setSmtpEnabled(true);
        config.setSmtpHost("smtp.example.com");
        config.setSmtpUsername("user@example.com");
        config.setSmtpFromAddress("noreply@example.com");
        return config;
    }

    private EmailConfig buildImapConfig() {
        EmailConfig config = new EmailConfig();
        config.setImapEnabled(true);
        config.setImapHost("imap.example.com");
        config.setImapUsername("user@example.com");
        return config;
    }
}
