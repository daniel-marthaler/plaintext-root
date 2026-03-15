package ch.plaintext.email;

import ch.plaintext.email.model.EmailConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EmailConfigModelTest {

    private EmailConfig emailConfig;

    @BeforeEach
    void setUp() {
        emailConfig = new EmailConfig();
    }

    @Test
    void prePersist_shouldSetCreatedAtAndUpdatedAt() {
        // Given
        emailConfig.setCreatedAt(null);
        emailConfig.setUpdatedAt(null);

        // When
        emailConfig.prePersist();

        // Then
        assertThat(emailConfig.getCreatedAt()).isNotNull();
        assertThat(emailConfig.getUpdatedAt()).isNotNull();
        assertThat(emailConfig.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(emailConfig.getUpdatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void prePersist_shouldNotOverrideExistingCreatedAt() {
        // Given
        LocalDateTime existingCreatedAt = LocalDateTime.now().minusDays(7);
        emailConfig.setCreatedAt(existingCreatedAt);

        // When
        emailConfig.prePersist();

        // Then
        assertThat(emailConfig.getCreatedAt()).isEqualTo(existingCreatedAt);
    }

    @Test
    void prePersist_shouldAlwaysUpdateUpdatedAt() {
        // Given
        LocalDateTime oldUpdatedAt = LocalDateTime.now().minusHours(1);
        emailConfig.setUpdatedAt(oldUpdatedAt);

        // When
        emailConfig.prePersist();

        // Then
        assertThat(emailConfig.getUpdatedAt()).isAfter(oldUpdatedAt);
    }

    @Test
    void preUpdate_shouldUpdateUpdatedAt() {
        // Given
        LocalDateTime initialTime = LocalDateTime.now().minusHours(1);
        emailConfig.setUpdatedAt(initialTime);

        // When
        emailConfig.preUpdate();

        // Then
        assertThat(emailConfig.getUpdatedAt()).isAfter(initialTime);
        assertThat(emailConfig.getUpdatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void isSmtpConfigured_withCompleteConfig_shouldReturnTrue() {
        // Given
        emailConfig.setSmtpEnabled(true);
        emailConfig.setSmtpHost("smtp.example.com");
        emailConfig.setSmtpUsername("user@example.com");
        emailConfig.setSmtpPassword("password");
        emailConfig.setSmtpFromAddress("from@example.com");

        // When
        boolean result = emailConfig.isSmtpConfigured();

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isSmtpConfigured_withMissingHost_shouldReturnFalse() {
        // Given
        emailConfig.setSmtpHost(null);
        emailConfig.setSmtpUsername("user@example.com");
        emailConfig.setSmtpPassword("password");

        // When
        boolean result = emailConfig.isSmtpConfigured();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isSmtpConfigured_withBlankHost_shouldReturnFalse() {
        // Given
        emailConfig.setSmtpHost("   ");
        emailConfig.setSmtpUsername("user@example.com");
        emailConfig.setSmtpPassword("password");

        // When
        boolean result = emailConfig.isSmtpConfigured();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isSmtpConfigured_withMissingUsername_shouldReturnFalse() {
        // Given
        emailConfig.setSmtpHost("smtp.example.com");
        emailConfig.setSmtpUsername(null);
        emailConfig.setSmtpPassword("password");

        // When
        boolean result = emailConfig.isSmtpConfigured();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isSmtpConfigured_withBlankUsername_shouldReturnFalse() {
        // Given
        emailConfig.setSmtpHost("smtp.example.com");
        emailConfig.setSmtpUsername("");
        emailConfig.setSmtpPassword("password");

        // When
        boolean result = emailConfig.isSmtpConfigured();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isSmtpConfigured_withMissingPassword_shouldReturnFalse() {
        // Given
        emailConfig.setSmtpHost("smtp.example.com");
        emailConfig.setSmtpUsername("user@example.com");
        emailConfig.setSmtpPassword(null);

        // When
        boolean result = emailConfig.isSmtpConfigured();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isSmtpConfigured_withBlankPassword_shouldReturnFalse() {
        // Given
        emailConfig.setSmtpHost("smtp.example.com");
        emailConfig.setSmtpUsername("user@example.com");
        emailConfig.setSmtpPassword("  ");

        // When
        boolean result = emailConfig.isSmtpConfigured();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isImapConfigured_withCompleteConfigAndEnabled_shouldReturnTrue() {
        // Given
        emailConfig.setImapEnabled(true);
        emailConfig.setImapHost("imap.example.com");
        emailConfig.setImapUsername("user@example.com");
        emailConfig.setImapPassword("password");

        // When
        boolean result = emailConfig.isImapConfigured();

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isImapConfigured_withDisabled_shouldReturnFalse() {
        // Given
        emailConfig.setImapEnabled(false);
        emailConfig.setImapHost("imap.example.com");
        emailConfig.setImapUsername("user@example.com");
        emailConfig.setImapPassword("password");

        // When
        boolean result = emailConfig.isImapConfigured();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isImapConfigured_withMissingHost_shouldReturnFalse() {
        // Given
        emailConfig.setImapEnabled(true);
        emailConfig.setImapHost(null);
        emailConfig.setImapUsername("user@example.com");
        emailConfig.setImapPassword("password");

        // When
        boolean result = emailConfig.isImapConfigured();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isImapConfigured_withBlankHost_shouldReturnFalse() {
        // Given
        emailConfig.setImapEnabled(true);
        emailConfig.setImapHost("   ");
        emailConfig.setImapUsername("user@example.com");
        emailConfig.setImapPassword("password");

        // When
        boolean result = emailConfig.isImapConfigured();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isImapConfigured_withMissingUsername_shouldReturnFalse() {
        // Given
        emailConfig.setImapEnabled(true);
        emailConfig.setImapHost("imap.example.com");
        emailConfig.setImapUsername(null);
        emailConfig.setImapPassword("password");

        // When
        boolean result = emailConfig.isImapConfigured();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isImapConfigured_withMissingPassword_shouldReturnFalse() {
        // Given
        emailConfig.setImapEnabled(true);
        emailConfig.setImapHost("imap.example.com");
        emailConfig.setImapUsername("user@example.com");
        emailConfig.setImapPassword(null);

        // When
        boolean result = emailConfig.isImapConfigured();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void defaultSmtpPort_shouldBe587() {
        // Then
        assertThat(emailConfig.getSmtpPort()).isEqualTo(587);
    }

    @Test
    void defaultSmtpUseTls_shouldBeTrue() {
        // Then
        assertThat(emailConfig.isSmtpUseTls()).isTrue();
    }

    @Test
    void defaultSmtpUseSsl_shouldBeFalse() {
        // Then
        assertThat(emailConfig.isSmtpUseSsl()).isFalse();
    }

    @Test
    void defaultImapEnabled_shouldBeFalse() {
        // Then
        assertThat(emailConfig.isImapEnabled()).isFalse();
    }

    @Test
    void defaultImapPort_shouldBe993() {
        // Then
        assertThat(emailConfig.getImapPort()).isEqualTo(993);
    }

    @Test
    void defaultImapUseSsl_shouldBeTrue() {
        // Then
        assertThat(emailConfig.isImapUseSsl()).isTrue();
    }

    @Test
    void defaultImapFolder_shouldBeInbox() {
        // Then
        assertThat(emailConfig.getImapFolder()).isEqualTo("INBOX");
    }

    @Test
    void defaultImapMarkAsRead_shouldBeTrue() {
        // Then
        assertThat(emailConfig.isImapMarkAsRead()).isTrue();
    }

    @Test
    void defaultImapDeleteAfterFetch_shouldBeFalse() {
        // Then
        assertThat(emailConfig.isImapDeleteAfterFetch()).isFalse();
    }

    @Test
    void setMandat_shouldStoreMandat() {
        // When
        emailConfig.setMandat("TEST_MANDAT");

        // Then
        assertThat(emailConfig.getMandat()).isEqualTo("TEST_MANDAT");
    }

    @Test
    void setSmtpHost_shouldStoreHost() {
        // When
        emailConfig.setSmtpHost("smtp.gmail.com");

        // Then
        assertThat(emailConfig.getSmtpHost()).isEqualTo("smtp.gmail.com");
    }

    @Test
    void setSmtpPort_shouldStorePort() {
        // When
        emailConfig.setSmtpPort(465);

        // Then
        assertThat(emailConfig.getSmtpPort()).isEqualTo(465);
    }

    @Test
    void setSmtpUsername_shouldStoreUsername() {
        // When
        emailConfig.setSmtpUsername("user@example.com");

        // Then
        assertThat(emailConfig.getSmtpUsername()).isEqualTo("user@example.com");
    }

    @Test
    void setSmtpPassword_shouldStorePassword() {
        // When
        emailConfig.setSmtpPassword("secret123");

        // Then
        assertThat(emailConfig.getSmtpPassword()).isEqualTo("secret123");
    }

    @Test
    void setSmtpFromAddress_shouldStoreFromAddress() {
        // When
        emailConfig.setSmtpFromAddress("noreply@example.com");

        // Then
        assertThat(emailConfig.getSmtpFromAddress()).isEqualTo("noreply@example.com");
    }

    @Test
    void setSmtpFromName_shouldStoreFromName() {
        // When
        emailConfig.setSmtpFromName("Example Corp");

        // Then
        assertThat(emailConfig.getSmtpFromName()).isEqualTo("Example Corp");
    }

    @Test
    void setSmtpUseTls_shouldStoreFlag() {
        // When
        emailConfig.setSmtpUseTls(false);

        // Then
        assertThat(emailConfig.isSmtpUseTls()).isFalse();
    }

    @Test
    void setSmtpUseSsl_shouldStoreFlag() {
        // When
        emailConfig.setSmtpUseSsl(true);

        // Then
        assertThat(emailConfig.isSmtpUseSsl()).isTrue();
    }

    @Test
    void setImapEnabled_shouldStoreFlag() {
        // When
        emailConfig.setImapEnabled(true);

        // Then
        assertThat(emailConfig.isImapEnabled()).isTrue();
    }

    @Test
    void setImapHost_shouldStoreHost() {
        // When
        emailConfig.setImapHost("imap.gmail.com");

        // Then
        assertThat(emailConfig.getImapHost()).isEqualTo("imap.gmail.com");
    }

    @Test
    void setImapPort_shouldStorePort() {
        // When
        emailConfig.setImapPort(143);

        // Then
        assertThat(emailConfig.getImapPort()).isEqualTo(143);
    }

    @Test
    void setImapUsername_shouldStoreUsername() {
        // When
        emailConfig.setImapUsername("user@example.com");

        // Then
        assertThat(emailConfig.getImapUsername()).isEqualTo("user@example.com");
    }

    @Test
    void setImapPassword_shouldStorePassword() {
        // When
        emailConfig.setImapPassword("secret456");

        // Then
        assertThat(emailConfig.getImapPassword()).isEqualTo("secret456");
    }

    @Test
    void setImapUseSsl_shouldStoreFlag() {
        // When
        emailConfig.setImapUseSsl(false);

        // Then
        assertThat(emailConfig.isImapUseSsl()).isFalse();
    }

    @Test
    void setImapFolder_shouldStoreFolder() {
        // When
        emailConfig.setImapFolder("Archive");

        // Then
        assertThat(emailConfig.getImapFolder()).isEqualTo("Archive");
    }

    @Test
    void setImapMarkAsRead_shouldStoreFlag() {
        // When
        emailConfig.setImapMarkAsRead(false);

        // Then
        assertThat(emailConfig.isImapMarkAsRead()).isFalse();
    }

    @Test
    void setImapDeleteAfterFetch_shouldStoreFlag() {
        // When
        emailConfig.setImapDeleteAfterFetch(true);

        // Then
        assertThat(emailConfig.isImapDeleteAfterFetch()).isTrue();
    }

    @Test
    void setId_shouldStoreId() {
        // When
        emailConfig.setId(123L);

        // Then
        assertThat(emailConfig.getId()).isEqualTo(123L);
    }

    @Test
    void newEmailConfig_shouldHaveNullId() {
        // Then
        assertThat(emailConfig.getId()).isNull();
    }

    @Test
    void completeSmtpConfiguration_shouldBeValid() {
        // Given
        emailConfig.setMandat("TEST");
        emailConfig.setSmtpEnabled(true);
        emailConfig.setSmtpHost("smtp.example.com");
        emailConfig.setSmtpPort(587);
        emailConfig.setSmtpUsername("user@example.com");
        emailConfig.setSmtpPassword("password");
        emailConfig.setSmtpFromAddress("from@example.com");
        emailConfig.setSmtpFromName("Test Sender");
        emailConfig.setSmtpUseTls(true);

        // Then
        assertThat(emailConfig.isSmtpConfigured()).isTrue();
        assertThat(emailConfig.getSmtpPort()).isEqualTo(587);
        assertThat(emailConfig.isSmtpUseTls()).isTrue();
    }

    @Test
    void completeImapConfiguration_shouldBeValid() {
        // Given
        emailConfig.setMandat("TEST");
        emailConfig.setImapEnabled(true);
        emailConfig.setImapHost("imap.example.com");
        emailConfig.setImapPort(993);
        emailConfig.setImapUsername("user@example.com");
        emailConfig.setImapPassword("password");
        emailConfig.setImapFolder("INBOX");
        emailConfig.setImapMarkAsRead(true);
        emailConfig.setImapDeleteAfterFetch(false);

        // Then
        assertThat(emailConfig.isImapConfigured()).isTrue();
        assertThat(emailConfig.getImapPort()).isEqualTo(993);
        assertThat(emailConfig.getImapFolder()).isEqualTo("INBOX");
    }

    @Test
    void smtpAndImapConfiguration_shouldCoexist() {
        // Given - SMTP config
        emailConfig.setSmtpEnabled(true);
        emailConfig.setSmtpHost("smtp.example.com");
        emailConfig.setSmtpUsername("user@example.com");
        emailConfig.setSmtpPassword("password");
        emailConfig.setSmtpFromAddress("from@example.com");

        // And - IMAP config
        emailConfig.setImapEnabled(true);
        emailConfig.setImapHost("imap.example.com");
        emailConfig.setImapUsername("user@example.com");
        emailConfig.setImapPassword("password");

        // Then
        assertThat(emailConfig.isSmtpConfigured()).isTrue();
        assertThat(emailConfig.isImapConfigured()).isTrue();
    }

    @Test
    void prePersistAndPreUpdate_shouldWorkTogether() {
        // Given - First persist
        emailConfig.prePersist();
        LocalDateTime createdAt = emailConfig.getCreatedAt();
        LocalDateTime initialUpdatedAt = emailConfig.getUpdatedAt();

        // When - Simulate update
        try {
            Thread.sleep(10); // Small delay to ensure time difference
        } catch (InterruptedException e) {
            // Ignore
        }
        emailConfig.preUpdate();

        // Then
        assertThat(emailConfig.getCreatedAt()).isEqualTo(createdAt); // Should not change
        assertThat(emailConfig.getUpdatedAt()).isAfter(initialUpdatedAt); // Should be updated
    }
}
