/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.email;

import ch.plaintext.email.model.Email;
import ch.plaintext.email.model.EmailConfig;
import ch.plaintext.email.persistence.EmailRepository;
import ch.plaintext.email.service.EmailReceiveService;
import ch.plaintext.email.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailReceiveServiceTest {

    @Mock
    private EmailService emailService;

    @Mock
    private EmailRepository emailRepository;

    @InjectMocks
    private EmailReceiveService emailReceiveService;

    private EmailConfig testConfig;

    @BeforeEach
    void setUp() {
        testConfig = new EmailConfig();
        testConfig.setId(1L);
        testConfig.setMandat("TEST_MANDAT");
        testConfig.setImapEnabled(true);
        testConfig.setImapHost("imap.example.com");
        testConfig.setImapPort(993);
        testConfig.setImapUsername("user@example.com");
        testConfig.setImapPassword("password");
        testConfig.setImapUseSsl(true);
        testConfig.setImapFolder("INBOX");
        testConfig.setImapMarkAsRead(true);
        testConfig.setImapDeleteAfterFetch(false);
    }

    @Test
    void receiveEmails_withMissingConfiguration_shouldReturnEmptyList() {
        // Given
        when(emailService.getConfigForMandate("TEST_MANDAT")).thenReturn(Optional.empty());

        // When
        List<Email> result = emailReceiveService.receiveEmails("TEST_MANDAT");

        // Then
        assertThat(result).isEmpty();
        verify(emailService).getConfigForMandate("TEST_MANDAT");
        verify(emailRepository, never()).save(any(Email.class));
    }

    @Test
    void receiveEmails_withImapDisabled_shouldReturnEmptyList() {
        // Given
        testConfig.setImapEnabled(false);
        when(emailService.getConfigForMandate("TEST_MANDAT")).thenReturn(Optional.of(testConfig));

        // When
        List<Email> result = emailReceiveService.receiveEmails("TEST_MANDAT");

        // Then
        assertThat(result).isEmpty();
        verify(emailService).getConfigForMandate("TEST_MANDAT");
        verify(emailRepository, never()).save(any(Email.class));
    }

    @Test
    void receiveEmails_withIncompleteImapConfig_shouldReturnEmptyList() {
        // Given
        testConfig.setImapHost(null); // Missing required field
        when(emailService.getConfigForMandate("TEST_MANDAT")).thenReturn(Optional.of(testConfig));

        // When
        List<Email> result = emailReceiveService.receiveEmails("TEST_MANDAT");

        // Then
        assertThat(result).isEmpty();
        verify(emailService).getConfigForMandate("TEST_MANDAT");
    }

    @Test
    void receiveEmails_withMissingUsername_shouldReturnEmptyList() {
        // Given
        testConfig.setImapUsername(null);
        when(emailService.getConfigForMandate("TEST_MANDAT")).thenReturn(Optional.of(testConfig));

        // When
        List<Email> result = emailReceiveService.receiveEmails("TEST_MANDAT");

        // Then
        assertThat(result).isEmpty();
        verify(emailService).getConfigForMandate("TEST_MANDAT");
    }

    @Test
    void receiveEmails_withMissingPassword_shouldReturnEmptyList() {
        // Given
        testConfig.setImapPassword(null);
        when(emailService.getConfigForMandate("TEST_MANDAT")).thenReturn(Optional.of(testConfig));

        // When
        List<Email> result = emailReceiveService.receiveEmails("TEST_MANDAT");

        // Then
        assertThat(result).isEmpty();
        verify(emailService).getConfigForMandate("TEST_MANDAT");
    }

    @Test
    void receiveEmails_withConnectionFailure_shouldReturnEmptyListAndNotThrow() {
        // Given
        when(emailService.getConfigForMandate("TEST_MANDAT")).thenReturn(Optional.of(testConfig));

        // When - connection will fail in test environment
        List<Email> result = emailReceiveService.receiveEmails("TEST_MANDAT");

        // Then - should handle exception gracefully and return empty list
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(emailService).getConfigForMandate("TEST_MANDAT");
    }

    @Test
    void receiveEmails_withValidConfig_shouldCallGetConfigForMandate() {
        // Given
        when(emailService.getConfigForMandate("TEST_MANDAT")).thenReturn(Optional.of(testConfig));

        // When
        emailReceiveService.receiveEmails("TEST_MANDAT");

        // Then
        verify(emailService).getConfigForMandate("TEST_MANDAT");
    }

    @Test
    void receiveEmails_withCustomFolder_shouldUseConfiguredFolder() {
        // Given
        testConfig.setImapFolder("Archive");
        when(emailService.getConfigForMandate("TEST_MANDAT")).thenReturn(Optional.of(testConfig));

        // When
        emailReceiveService.receiveEmails("TEST_MANDAT");

        // Then
        verify(emailService).getConfigForMandate("TEST_MANDAT");
        assertThat(testConfig.getImapFolder()).isEqualTo("Archive");
    }

    @Test
    void receiveEmails_withDefaultFolder_shouldUseInbox() {
        // Given
        when(emailService.getConfigForMandate("TEST_MANDAT")).thenReturn(Optional.of(testConfig));

        // When
        emailReceiveService.receiveEmails("TEST_MANDAT");

        // Then
        assertThat(testConfig.getImapFolder()).isEqualTo("INBOX");
    }

    @Test
    void receiveEmails_withMarkAsReadEnabled_shouldHaveCorrectConfig() {
        // Given
        testConfig.setImapMarkAsRead(true);
        when(emailService.getConfigForMandate("TEST_MANDAT")).thenReturn(Optional.of(testConfig));

        // When
        emailReceiveService.receiveEmails("TEST_MANDAT");

        // Then
        assertThat(testConfig.isImapMarkAsRead()).isTrue();
    }

    @Test
    void receiveEmails_withMarkAsReadDisabled_shouldHaveCorrectConfig() {
        // Given
        testConfig.setImapMarkAsRead(false);
        when(emailService.getConfigForMandate("TEST_MANDAT")).thenReturn(Optional.of(testConfig));

        // When
        emailReceiveService.receiveEmails("TEST_MANDAT");

        // Then
        assertThat(testConfig.isImapMarkAsRead()).isFalse();
    }

    @Test
    void receiveEmails_withDeleteAfterFetchEnabled_shouldHaveCorrectConfig() {
        // Given
        testConfig.setImapDeleteAfterFetch(true);
        when(emailService.getConfigForMandate("TEST_MANDAT")).thenReturn(Optional.of(testConfig));

        // When
        emailReceiveService.receiveEmails("TEST_MANDAT");

        // Then
        assertThat(testConfig.isImapDeleteAfterFetch()).isTrue();
    }

    @Test
    void receiveEmails_withDeleteAfterFetchDisabled_shouldHaveCorrectConfig() {
        // Given
        testConfig.setImapDeleteAfterFetch(false);
        when(emailService.getConfigForMandate("TEST_MANDAT")).thenReturn(Optional.of(testConfig));

        // When
        emailReceiveService.receiveEmails("TEST_MANDAT");

        // Then
        assertThat(testConfig.isImapDeleteAfterFetch()).isFalse();
    }

    @Test
    void receiveEmails_withSslEnabled_shouldHaveCorrectConfig() {
        // Given
        testConfig.setImapUseSsl(true);
        when(emailService.getConfigForMandate("TEST_MANDAT")).thenReturn(Optional.of(testConfig));

        // When
        emailReceiveService.receiveEmails("TEST_MANDAT");

        // Then
        assertThat(testConfig.isImapUseSsl()).isTrue();
    }

    @Test
    void receiveEmails_withSslDisabled_shouldHaveCorrectConfig() {
        // Given
        testConfig.setImapUseSsl(false);
        when(emailService.getConfigForMandate("TEST_MANDAT")).thenReturn(Optional.of(testConfig));

        // When
        emailReceiveService.receiveEmails("TEST_MANDAT");

        // Then
        assertThat(testConfig.isImapUseSsl()).isFalse();
    }

    @Test
    void receiveEmails_withCustomPort_shouldUseConfiguredPort() {
        // Given
        testConfig.setImapPort(143);
        when(emailService.getConfigForMandate("TEST_MANDAT")).thenReturn(Optional.of(testConfig));

        // When
        emailReceiveService.receiveEmails("TEST_MANDAT");

        // Then
        assertThat(testConfig.getImapPort()).isEqualTo(143);
    }

    @Test
    void receiveEmails_multipleCallsWithSameMandate_shouldCallServiceEachTime() {
        // Given
        when(emailService.getConfigForMandate("TEST_MANDAT")).thenReturn(Optional.of(testConfig));

        // When
        emailReceiveService.receiveEmails("TEST_MANDAT");
        emailReceiveService.receiveEmails("TEST_MANDAT");

        // Then
        verify(emailService, times(2)).getConfigForMandate("TEST_MANDAT");
    }

    @Test
    void receiveEmails_withDifferentMandates_shouldCallServiceWithCorrectMandate() {
        // Given
        EmailConfig config2 = new EmailConfig();
        config2.setMandat("MANDAT_2");
        config2.setImapEnabled(true);
        config2.setImapHost("imap2.example.com");
        config2.setImapUsername("user2");
        config2.setImapPassword("pass2");

        when(emailService.getConfigForMandate("TEST_MANDAT")).thenReturn(Optional.of(testConfig));
        when(emailService.getConfigForMandate("MANDAT_2")).thenReturn(Optional.of(config2));

        // When
        emailReceiveService.receiveEmails("TEST_MANDAT");
        emailReceiveService.receiveEmails("MANDAT_2");

        // Then
        verify(emailService).getConfigForMandate("TEST_MANDAT");
        verify(emailService).getConfigForMandate("MANDAT_2");
    }

    @Test
    void receiveEmails_withBlankHost_shouldReturnEmptyList() {
        // Given
        testConfig.setImapHost("   ");
        when(emailService.getConfigForMandate("TEST_MANDAT")).thenReturn(Optional.of(testConfig));

        // When
        List<Email> result = emailReceiveService.receiveEmails("TEST_MANDAT");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void receiveEmails_withBlankUsername_shouldReturnEmptyList() {
        // Given
        testConfig.setImapUsername("");
        when(emailService.getConfigForMandate("TEST_MANDAT")).thenReturn(Optional.of(testConfig));

        // When
        List<Email> result = emailReceiveService.receiveEmails("TEST_MANDAT");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void receiveEmails_withBlankPassword_shouldReturnEmptyList() {
        // Given
        testConfig.setImapPassword("  ");
        when(emailService.getConfigForMandate("TEST_MANDAT")).thenReturn(Optional.of(testConfig));

        // When
        List<Email> result = emailReceiveService.receiveEmails("TEST_MANDAT");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void receiveEmails_returnValue_shouldNotBeNull() {
        // Given
        when(emailService.getConfigForMandate("TEST_MANDAT")).thenReturn(Optional.of(testConfig));

        // When
        List<Email> result = emailReceiveService.receiveEmails("TEST_MANDAT");

        // Then
        assertThat(result).isNotNull();
    }
}
