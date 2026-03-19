/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.email.service;

import ch.plaintext.email.model.Email;
import ch.plaintext.email.model.EmailConfig;
import ch.plaintext.email.persistence.EmailConfigRepository;
import ch.plaintext.email.persistence.EmailRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private EmailRepository emailRepository;

    @Mock
    private EmailConfigRepository emailConfigRepository;

    @InjectMocks
    private EmailService emailService;

    private static final String MANDAT = "test-mandat";

    // --- createDraft tests ---

    @Nested
    class CreateDraft {

        @Test
        void createsDraftWithConfigFromAddress() {
            EmailConfig config = new EmailConfig();
            config.setSmtpFromAddress("sender@example.com");
            when(emailConfigRepository.findFirstByMandatOrderByConfigNameAsc(MANDAT))
                    .thenReturn(Optional.of(config));
            when(emailRepository.save(any(Email.class))).thenAnswer(inv -> inv.getArgument(0));

            Email result = emailService.createDraft(MANDAT, "to@example.com", "Subject", "Body", false);

            assertEquals(MANDAT, result.getMandat());
            assertEquals("to@example.com", result.getToAddress());
            assertEquals("Subject", result.getSubject());
            assertEquals("Body", result.getBody());
            assertFalse(result.isHtml());
            assertEquals(Email.EmailStatus.DRAFT, result.getStatus());
            assertEquals(Email.EmailDirection.OUTGOING, result.getDirection());
            assertEquals("sender@example.com", result.getFromAddress());
        }

        @Test
        void createsDraftWithDefaultFromAddressWhenNoConfig() {
            when(emailConfigRepository.findFirstByMandatOrderByConfigNameAsc(MANDAT))
                    .thenReturn(Optional.empty());
            when(emailRepository.save(any(Email.class))).thenAnswer(inv -> inv.getArgument(0));

            Email result = emailService.createDraft(MANDAT, "to@example.com", "Subject", "Body", true);

            assertEquals("noreply@plaintext.ch", result.getFromAddress());
        }

        @Test
        void createsDraftWithSpecificConfigName() {
            EmailConfig config = new EmailConfig();
            config.setSmtpFromAddress("config@example.com");
            when(emailConfigRepository.findByMandatAndConfigName(MANDAT, "myconfig"))
                    .thenReturn(Optional.of(config));
            when(emailRepository.save(any(Email.class))).thenAnswer(inv -> inv.getArgument(0));

            Email result = emailService.createDraft(MANDAT, "myconfig", "to@example.com",
                    "cc@example.com", "bcc@example.com", "Subject", "Body", false);

            assertEquals("myconfig", result.getConfigName());
            assertEquals("config@example.com", result.getFromAddress());
            assertEquals("cc@example.com", result.getCcAddress());
            assertEquals("bcc@example.com", result.getBccAddress());
        }

        @Test
        void createsDraftWithCcAndBcc() {
            when(emailConfigRepository.findFirstByMandatOrderByConfigNameAsc(MANDAT))
                    .thenReturn(Optional.empty());
            when(emailRepository.save(any(Email.class))).thenAnswer(inv -> inv.getArgument(0));

            Email result = emailService.createDraft(MANDAT, "to@example.com",
                    "cc@example.com", "bcc@example.com", "Subject", "Body", true);

            assertEquals("cc@example.com", result.getCcAddress());
            assertEquals("bcc@example.com", result.getBccAddress());
        }
    }

    // --- queueEmail tests ---

    @Nested
    class QueueEmail {

        @Test
        void queuesEmailInDraftStatus() {
            Email email = new Email();
            email.setId(1L);
            email.setStatus(Email.EmailStatus.DRAFT);
            when(emailRepository.findById(1L)).thenReturn(Optional.of(email));
            when(emailRepository.save(any(Email.class))).thenAnswer(inv -> inv.getArgument(0));

            Email result = emailService.queueEmail(1L);

            assertEquals(Email.EmailStatus.QUEUED, result.getStatus());
        }

        @Test
        void throwsWhenEmailNotFound() {
            when(emailRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> emailService.queueEmail(999L));
        }

        @Test
        void throwsWhenEmailNotInDraftStatus() {
            Email email = new Email();
            email.setId(1L);
            email.setStatus(Email.EmailStatus.SENT);
            when(emailRepository.findById(1L)).thenReturn(Optional.of(email));

            assertThrows(IllegalStateException.class, () -> emailService.queueEmail(1L));
        }
    }

    // --- markAsSent tests ---

    @Nested
    class MarkAsSent {

        @Test
        void marksEmailAsSent() {
            Email email = new Email();
            email.setId(1L);
            when(emailRepository.findById(1L)).thenReturn(Optional.of(email));
            when(emailRepository.save(any(Email.class))).thenAnswer(inv -> inv.getArgument(0));

            emailService.markAsSent(1L, "<msg-id@example.com>");

            ArgumentCaptor<Email> captor = ArgumentCaptor.forClass(Email.class);
            verify(emailRepository).save(captor.capture());
            Email saved = captor.getValue();
            assertEquals(Email.EmailStatus.SENT, saved.getStatus());
            assertNotNull(saved.getSentAt());
            assertEquals("<msg-id@example.com>", saved.getMessageId());
        }

        @Test
        void throwsWhenEmailNotFound() {
            when(emailRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> emailService.markAsSent(999L, "msg-id"));
        }
    }

    // --- markAsFailed tests ---

    @Nested
    class MarkAsFailed {

        @Test
        void marksEmailAsFailed() {
            Email email = new Email();
            email.setId(1L);
            email.setRetryCount(0);
            when(emailRepository.findById(1L)).thenReturn(Optional.of(email));
            when(emailRepository.save(any(Email.class))).thenAnswer(inv -> inv.getArgument(0));

            emailService.markAsFailed(1L, "Connection refused");

            ArgumentCaptor<Email> captor = ArgumentCaptor.forClass(Email.class);
            verify(emailRepository).save(captor.capture());
            Email saved = captor.getValue();
            assertEquals(Email.EmailStatus.FAILED, saved.getStatus());
            assertEquals("Connection refused", saved.getErrorMessage());
            assertEquals(1, saved.getRetryCount());
        }

        @Test
        void incrementsRetryCount() {
            Email email = new Email();
            email.setId(1L);
            email.setRetryCount(2);
            when(emailRepository.findById(1L)).thenReturn(Optional.of(email));
            when(emailRepository.save(any(Email.class))).thenAnswer(inv -> inv.getArgument(0));

            emailService.markAsFailed(1L, "Timeout");

            ArgumentCaptor<Email> captor = ArgumentCaptor.forClass(Email.class);
            verify(emailRepository).save(captor.capture());
            assertEquals(3, captor.getValue().getRetryCount());
        }
    }

    // --- query methods ---

    @Nested
    class QueryMethods {

        @Test
        void getEmailsForMandate() {
            List<Email> emails = List.of(new Email(), new Email());
            when(emailRepository.findByMandatOrderByCreatedAtDesc(MANDAT)).thenReturn(emails);

            List<Email> result = emailService.getEmailsForMandate(MANDAT);

            assertEquals(2, result.size());
        }

        @Test
        void getQueuedEmails() {
            List<Email> emails = List.of(new Email());
            when(emailRepository.findByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
                    Email.EmailStatus.QUEUED, 3)).thenReturn(emails);

            List<Email> result = emailService.getQueuedEmails();

            assertEquals(1, result.size());
        }

        @Test
        void getQueuedEmailsForMandate() {
            List<Email> emails = List.of(new Email());
            when(emailRepository.findByMandatAndStatusAndRetryCountLessThanOrderByCreatedAtAsc(
                    MANDAT, Email.EmailStatus.QUEUED, 3)).thenReturn(emails);

            List<Email> result = emailService.getQueuedEmailsForMandate(MANDAT);

            assertEquals(1, result.size());
        }

        @Test
        void getQueuedCount() {
            when(emailRepository.countByMandatAndStatus(MANDAT, Email.EmailStatus.QUEUED)).thenReturn(5L);

            assertEquals(5L, emailService.getQueuedCount(MANDAT));
        }

        @Test
        void getSentCount() {
            when(emailRepository.countByMandatAndStatus(MANDAT, Email.EmailStatus.SENT)).thenReturn(10L);

            assertEquals(10L, emailService.getSentCount(MANDAT));
        }

        @Test
        void getFailedCount() {
            when(emailRepository.countByMandatAndStatus(MANDAT, Email.EmailStatus.FAILED)).thenReturn(2L);

            assertEquals(2L, emailService.getFailedCount(MANDAT));
        }

        @Test
        void findById() {
            Email email = new Email();
            email.setId(42L);
            when(emailRepository.findById(42L)).thenReturn(Optional.of(email));

            Optional<Email> result = emailService.findById(42L);

            assertTrue(result.isPresent());
            assertEquals(42L, result.get().getId());
        }

        @Test
        void findByIdReturnsEmptyWhenNotFound() {
            when(emailRepository.findById(999L)).thenReturn(Optional.empty());

            assertTrue(emailService.findById(999L).isEmpty());
        }
    }

    // --- config methods ---

    @Nested
    class ConfigMethods {

        @Test
        void getConfigForMandate() {
            EmailConfig config = new EmailConfig();
            config.setConfigName("default");
            when(emailConfigRepository.findFirstByMandatOrderByConfigNameAsc(MANDAT))
                    .thenReturn(Optional.of(config));

            Optional<EmailConfig> result = emailService.getConfigForMandate(MANDAT);

            assertTrue(result.isPresent());
            assertEquals("default", result.get().getConfigName());
        }

        @Test
        void getConfigForMandateFallsBackOnException() {
            when(emailConfigRepository.findFirstByMandatOrderByConfigNameAsc(MANDAT))
                    .thenThrow(new RuntimeException("Non-unique result"));
            EmailConfig config = new EmailConfig();
            config.setConfigName("first");
            when(emailConfigRepository.findByMandatOrderByConfigNameAsc(MANDAT))
                    .thenReturn(List.of(config));

            Optional<EmailConfig> result = emailService.getConfigForMandate(MANDAT);

            assertTrue(result.isPresent());
            assertEquals("first", result.get().getConfigName());
        }

        @Test
        void getConfigForMandateReturnsEmptyOnFallbackWithEmptyList() {
            when(emailConfigRepository.findFirstByMandatOrderByConfigNameAsc(MANDAT))
                    .thenThrow(new RuntimeException("Non-unique result"));
            when(emailConfigRepository.findByMandatOrderByConfigNameAsc(MANDAT))
                    .thenReturn(Collections.emptyList());

            Optional<EmailConfig> result = emailService.getConfigForMandate(MANDAT);

            assertTrue(result.isEmpty());
        }

        @Test
        void getConfigByName() {
            EmailConfig config = new EmailConfig();
            config.setConfigName("maintenance");
            when(emailConfigRepository.findByMandatAndConfigName(MANDAT, "maintenance"))
                    .thenReturn(Optional.of(config));

            Optional<EmailConfig> result = emailService.getConfigByName(MANDAT, "maintenance");

            assertTrue(result.isPresent());
        }

        @Test
        void getConfigsForMandate() {
            List<EmailConfig> configs = List.of(new EmailConfig(), new EmailConfig());
            when(emailConfigRepository.findByMandatOrderByConfigNameAsc(MANDAT)).thenReturn(configs);

            assertEquals(2, emailService.getConfigsForMandate(MANDAT).size());
        }

        @Test
        void getConfigNamesForMandate() {
            EmailConfig c1 = new EmailConfig();
            c1.setConfigName("alpha");
            EmailConfig c2 = new EmailConfig();
            c2.setConfigName("beta");
            when(emailConfigRepository.findByMandatOrderByConfigNameAsc(MANDAT))
                    .thenReturn(List.of(c1, c2));

            List<String> names = emailService.getConfigNamesForMandate(MANDAT);

            assertEquals(List.of("alpha", "beta"), names);
        }

        @Test
        void configExists() {
            when(emailConfigRepository.existsByMandatAndConfigName(MANDAT, "default")).thenReturn(true);

            assertTrue(emailService.configExists(MANDAT, "default"));
        }

        @Test
        void findConfigByNameAcrossAllMandates() {
            EmailConfig config = new EmailConfig();
            config.setConfigName("maintenance");
            config.setMandat("mandat-a");
            when(emailConfigRepository.findAll()).thenReturn(List.of(config));

            Optional<EmailConfig> result = emailService.findConfigByNameAcrossAllMandates("maintenance");

            assertTrue(result.isPresent());
            assertEquals("mandat-a", result.get().getMandat());
        }

        @Test
        void findConfigByNameAcrossAllMandatesReturnsEmptyWhenNotFound() {
            when(emailConfigRepository.findAll()).thenReturn(Collections.emptyList());

            assertTrue(emailService.findConfigByNameAcrossAllMandates("nonexistent").isEmpty());
        }

        @Test
        void getActiveImapConfigCount() {
            EmailConfig c1 = new EmailConfig();
            c1.setImapEnabled(true);
            EmailConfig c2 = new EmailConfig();
            c2.setImapEnabled(false);
            EmailConfig c3 = new EmailConfig();
            c3.setImapEnabled(true);
            when(emailConfigRepository.findAll()).thenReturn(List.of(c1, c2, c3));

            assertEquals(2, emailService.getActiveImapConfigCount());
        }
    }

    // --- saveConfig tests ---

    @Nested
    class SaveConfig {

        @Test
        void savesNewConfig() {
            EmailConfig config = new EmailConfig();
            config.setMandat(MANDAT);
            config.setConfigName("newconfig");
            when(emailConfigRepository.existsByMandatAndConfigName(MANDAT, "newconfig"))
                    .thenReturn(false);
            when(emailConfigRepository.save(any(EmailConfig.class))).thenAnswer(inv -> inv.getArgument(0));

            EmailConfig result = emailService.saveConfig(config);

            assertEquals("newconfig", result.getConfigName());
            verify(emailConfigRepository).save(config);
        }

        @Test
        void trimsConfigName() {
            EmailConfig config = new EmailConfig();
            config.setMandat(MANDAT);
            config.setConfigName("  padded  ");
            when(emailConfigRepository.existsByMandatAndConfigName(MANDAT, "padded"))
                    .thenReturn(false);
            when(emailConfigRepository.save(any(EmailConfig.class))).thenAnswer(inv -> inv.getArgument(0));

            EmailConfig result = emailService.saveConfig(config);

            assertEquals("padded", result.getConfigName());
        }

        @Test
        void throwsWhenConfigNameEmpty() {
            EmailConfig config = new EmailConfig();
            config.setConfigName("");

            assertThrows(IllegalArgumentException.class, () -> emailService.saveConfig(config));
        }

        @Test
        void throwsWhenConfigNameNull() {
            EmailConfig config = new EmailConfig();
            config.setConfigName(null);

            assertThrows(IllegalArgumentException.class, () -> emailService.saveConfig(config));
        }

        @Test
        void throwsWhenDuplicateNameForNewConfig() {
            EmailConfig config = new EmailConfig();
            config.setMandat(MANDAT);
            config.setConfigName("duplicate");
            when(emailConfigRepository.existsByMandatAndConfigName(MANDAT, "duplicate"))
                    .thenReturn(true);

            assertThrows(IllegalArgumentException.class, () -> emailService.saveConfig(config));
        }

        @Test
        void allowsSameNameForExistingConfigWithUnchangedName() {
            EmailConfig existing = new EmailConfig();
            existing.setId(1L);
            existing.setConfigName("original");
            existing.setMandat(MANDAT);

            when(emailConfigRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(emailConfigRepository.save(any(EmailConfig.class))).thenAnswer(inv -> inv.getArgument(0));

            EmailConfig toSave = new EmailConfig();
            toSave.setId(1L);
            toSave.setMandat(MANDAT);
            toSave.setConfigName("original");

            EmailConfig result = emailService.saveConfig(toSave);

            assertEquals("original", result.getConfigName());
        }

        @Test
        void throwsWhenRenamingToExistingName() {
            EmailConfig existing = new EmailConfig();
            existing.setId(1L);
            existing.setConfigName("original");
            existing.setMandat(MANDAT);

            when(emailConfigRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(emailConfigRepository.existsByMandatAndConfigName(MANDAT, "taken"))
                    .thenReturn(true);

            EmailConfig toSave = new EmailConfig();
            toSave.setId(1L);
            toSave.setMandat(MANDAT);
            toSave.setConfigName("taken");

            assertThrows(IllegalArgumentException.class, () -> emailService.saveConfig(toSave));
        }
    }

    // --- deleteConfig ---

    @Test
    void deleteConfig() {
        emailService.deleteConfig(42L);

        verify(emailConfigRepository).deleteById(42L);
    }

    // --- updateDraft tests ---

    @Nested
    class UpdateDraft {

        @Test
        void updatesDraftEmail() {
            Email email = new Email();
            email.setId(1L);
            email.setMandat(MANDAT);
            email.setStatus(Email.EmailStatus.DRAFT);
            when(emailRepository.findById(1L)).thenReturn(Optional.of(email));
            when(emailRepository.save(any(Email.class))).thenAnswer(inv -> inv.getArgument(0));

            Email result = emailService.updateDraft(1L, "new-to@example.com",
                    "new-cc@example.com", "New Subject", "New Body", true);

            assertEquals("new-to@example.com", result.getToAddress());
            assertEquals("new-cc@example.com", result.getCcAddress());
            assertEquals("New Subject", result.getSubject());
            assertEquals("New Body", result.getBody());
            assertTrue(result.isHtml());
        }

        @Test
        void throwsWhenNotDraft() {
            Email email = new Email();
            email.setId(1L);
            email.setStatus(Email.EmailStatus.SENT);
            when(emailRepository.findById(1L)).thenReturn(Optional.of(email));

            assertThrows(IllegalStateException.class,
                    () -> emailService.updateDraft(1L, "to", "cc", "subj", "body", false));
        }

        @Test
        void throwsWhenEmailNotFound() {
            when(emailRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> emailService.updateDraft(999L, "to", "cc", "subj", "body", false));
        }
    }

    // --- deleteEmail ---

    @Test
    void deleteEmail() {
        emailService.deleteEmail(42L);

        verify(emailRepository).deleteById(42L);
    }

    // --- PlaintextEmailReceiver interface methods ---

    @Nested
    class ReceiverInterface {

        @Test
        void readEmail() {
            Email email = new Email();
            email.setId(1L);
            when(emailRepository.findById(1L)).thenReturn(Optional.of(email));

            Optional<Object> result = emailService.readEmail(1L);

            assertTrue(result.isPresent());
            assertInstanceOf(Email.class, result.get());
        }

        @Test
        void readEmailReturnsEmptyWhenNotFound() {
            when(emailRepository.findById(999L)).thenReturn(Optional.empty());

            assertTrue(emailService.readEmail(999L).isEmpty());
        }

        @Test
        void readEmailsForMandate() {
            when(emailRepository.findByMandatOrderByCreatedAtDesc(MANDAT))
                    .thenReturn(List.of(new Email(), new Email()));

            List<Object> result = emailService.readEmailsForMandate(MANDAT);

            assertEquals(2, result.size());
        }

        @Test
        void readQueuedEmails() {
            when(emailRepository.findByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
                    Email.EmailStatus.QUEUED, 3))
                    .thenReturn(List.of(new Email()));

            assertEquals(1, emailService.readQueuedEmails().size());
        }

        @Test
        void readQueuedEmailsForMandate() {
            when(emailRepository.findByMandatAndStatusAndRetryCountLessThanOrderByCreatedAtAsc(
                    MANDAT, Email.EmailStatus.QUEUED, 3))
                    .thenReturn(List.of(new Email()));

            assertEquals(1, emailService.readQueuedEmailsForMandate(MANDAT).size());
        }

        @Test
        void readIncomingEmailsForMandate() {
            when(emailRepository.findByMandatAndDirectionOrderByCreatedAtDesc(
                    MANDAT, Email.EmailDirection.INCOMING))
                    .thenReturn(List.of(new Email()));

            assertEquals(1, emailService.readIncomingEmailsForMandate(MANDAT).size());
        }

        @Test
        void readOutgoingEmailsForMandate() {
            when(emailRepository.findByMandatAndDirectionOrderByCreatedAtDesc(
                    MANDAT, Email.EmailDirection.OUTGOING))
                    .thenReturn(List.of(new Email(), new Email()));

            assertEquals(2, emailService.readOutgoingEmailsForMandate(MANDAT).size());
        }
    }
}
