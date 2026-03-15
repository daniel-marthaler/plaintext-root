package ch.plaintext.email;

import ch.plaintext.email.model.Email;
import ch.plaintext.email.model.EmailConfig;
import ch.plaintext.email.persistence.EmailConfigRepository;
import ch.plaintext.email.persistence.EmailRepository;
import ch.plaintext.email.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private EmailRepository emailRepository;

    @Mock
    private EmailConfigRepository emailConfigRepository;

    @InjectMocks
    private EmailService emailService;

    private Email testEmail;
    private EmailConfig testConfig;

    @BeforeEach
    void setUp() {
        testEmail = new Email();
        testEmail.setId(1L);
        testEmail.setMandat("TEST_MANDAT");
        testEmail.setToAddress("test@example.com");
        testEmail.setSubject("Test Subject");
        testEmail.setBody("Test Body");
        testEmail.setStatus(Email.EmailStatus.DRAFT);
        testEmail.setDirection(Email.EmailDirection.OUTGOING);

        testConfig = new EmailConfig();
        testConfig.setId(1L);
        testConfig.setMandat("TEST_MANDAT");
        testConfig.setConfigName("default");
        testConfig.setSmtpFromAddress("from@example.com");
        testConfig.setSmtpHost("smtp.example.com");
        testConfig.setSmtpUsername("user");
        testConfig.setSmtpPassword("pass");
    }

    @Test
    void createDraft_withValidData_shouldCreateEmail() {
        // Given
        when(emailConfigRepository.findFirstByMandatOrderByConfigNameAsc("TEST_MANDAT")).thenReturn(Optional.of(testConfig));
        when(emailRepository.save(any(Email.class))).thenAnswer(invocation -> {
            Email email = invocation.getArgument(0);
            email.setId(1L);
            return email;
        });

        // When
        Email result = emailService.createDraft(
                "TEST_MANDAT",
                "to@example.com",
                "Subject",
                "Body",
                true
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMandat()).isEqualTo("TEST_MANDAT");
        assertThat(result.getToAddress()).isEqualTo("to@example.com");
        assertThat(result.getSubject()).isEqualTo("Subject");
        assertThat(result.getBody()).isEqualTo("Body");
        assertThat(result.isHtml()).isTrue();
        assertThat(result.getStatus()).isEqualTo(Email.EmailStatus.DRAFT);
        assertThat(result.getDirection()).isEqualTo(Email.EmailDirection.OUTGOING);
        assertThat(result.getFromAddress()).isEqualTo("from@example.com");

        verify(emailConfigRepository).findFirstByMandatOrderByConfigNameAsc("TEST_MANDAT");
        verify(emailRepository).save(any(Email.class));
    }

    @Test
    void createDraft_withoutConfig_shouldCreateEmailWithoutFromAddress() {
        // Given
        when(emailConfigRepository.findFirstByMandatOrderByConfigNameAsc("TEST_MANDAT")).thenReturn(Optional.empty());
        when(emailRepository.save(any(Email.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Email result = emailService.createDraft(
                "TEST_MANDAT",
                "to@example.com",
                "Subject",
                "Body",
                false
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFromAddress()).isEqualTo("noreply@plaintext.ch"); // Uses default when no config
        assertThat(result.isHtml()).isFalse();
        verify(emailRepository).save(any(Email.class));
    }

    @Test
    void queueEmail_withDraftStatus_shouldChangeStatusToQueued() {
        // Given
        testEmail.setStatus(Email.EmailStatus.DRAFT);
        when(emailRepository.findById(1L)).thenReturn(Optional.of(testEmail));
        when(emailRepository.save(any(Email.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Email result = emailService.queueEmail(1L);

        // Then
        assertThat(result.getStatus()).isEqualTo(Email.EmailStatus.QUEUED);
        verify(emailRepository).findById(1L);
        verify(emailRepository).save(testEmail);
    }

    @Test
    void queueEmail_withNonDraftStatus_shouldThrowException() {
        // Given
        testEmail.setStatus(Email.EmailStatus.SENT);
        when(emailRepository.findById(1L)).thenReturn(Optional.of(testEmail));

        // When/Then
        assertThatThrownBy(() -> emailService.queueEmail(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Can only queue emails with status DRAFT");

        verify(emailRepository).findById(1L);
        verify(emailRepository, never()).save(any(Email.class));
    }

    @Test
    void queueEmail_withNonExistentEmail_shouldThrowException() {
        // Given
        when(emailRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> emailService.queueEmail(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email not found: 999");

        verify(emailRepository).findById(999L);
    }

    @Test
    void markAsSent_withValidEmail_shouldUpdateStatusAndSetSentAt() {
        // Given
        when(emailRepository.findById(1L)).thenReturn(Optional.of(testEmail));
        when(emailRepository.save(any(Email.class))).thenAnswer(invocation -> invocation.getArgument(0));
        String messageId = "<test@example.com>";

        // When
        emailService.markAsSent(1L, messageId);

        // Then
        ArgumentCaptor<Email> emailCaptor = ArgumentCaptor.forClass(Email.class);
        verify(emailRepository).save(emailCaptor.capture());
        Email savedEmail = emailCaptor.getValue();

        assertThat(savedEmail.getStatus()).isEqualTo(Email.EmailStatus.SENT);
        assertThat(savedEmail.getSentAt()).isNotNull();
        assertThat(savedEmail.getMessageId()).isEqualTo(messageId);
    }

    @Test
    void markAsSent_withNonExistentEmail_shouldThrowException() {
        // Given
        when(emailRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> emailService.markAsSent(999L, "<test@example.com>"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email not found: 999");
    }

    @Test
    void markAsFailed_withValidEmail_shouldUpdateStatusAndIncrementRetryCount() {
        // Given
        testEmail.setRetryCount(0);
        when(emailRepository.findById(1L)).thenReturn(Optional.of(testEmail));
        when(emailRepository.save(any(Email.class))).thenAnswer(invocation -> invocation.getArgument(0));
        String errorMessage = "Connection failed";

        // When
        emailService.markAsFailed(1L, errorMessage);

        // Then
        ArgumentCaptor<Email> emailCaptor = ArgumentCaptor.forClass(Email.class);
        verify(emailRepository).save(emailCaptor.capture());
        Email savedEmail = emailCaptor.getValue();

        assertThat(savedEmail.getStatus()).isEqualTo(Email.EmailStatus.FAILED);
        assertThat(savedEmail.getErrorMessage()).isEqualTo(errorMessage);
        assertThat(savedEmail.getRetryCount()).isEqualTo(1);
    }

    @Test
    void markAsFailed_multipleRetries_shouldIncrementRetryCount() {
        // Given
        testEmail.setRetryCount(2);
        when(emailRepository.findById(1L)).thenReturn(Optional.of(testEmail));
        when(emailRepository.save(any(Email.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        emailService.markAsFailed(1L, "Error");

        // Then
        ArgumentCaptor<Email> emailCaptor = ArgumentCaptor.forClass(Email.class);
        verify(emailRepository).save(emailCaptor.capture());
        assertThat(emailCaptor.getValue().getRetryCount()).isEqualTo(3);
    }

    @Test
    void getEmailsForMandate_shouldReturnEmailsList() {
        // Given
        Email email1 = new Email();
        email1.setId(1L);
        email1.setMandat("TEST_MANDAT");

        Email email2 = new Email();
        email2.setId(2L);
        email2.setMandat("TEST_MANDAT");

        List<Email> expectedEmails = Arrays.asList(email1, email2);
        when(emailRepository.findByMandatOrderByCreatedAtDesc("TEST_MANDAT")).thenReturn(expectedEmails);

        // When
        List<Email> result = emailService.getEmailsForMandate("TEST_MANDAT");

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyElementsOf(expectedEmails);
        verify(emailRepository).findByMandatOrderByCreatedAtDesc("TEST_MANDAT");
    }

    @Test
    void getQueuedEmails_shouldReturnEmailsWithRetryCountLessThan3() {
        // Given
        Email email1 = new Email();
        email1.setId(1L);
        email1.setStatus(Email.EmailStatus.QUEUED);
        email1.setRetryCount(0);

        Email email2 = new Email();
        email2.setId(2L);
        email2.setStatus(Email.EmailStatus.QUEUED);
        email2.setRetryCount(2);

        List<Email> expectedEmails = Arrays.asList(email1, email2);
        when(emailRepository.findByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
                Email.EmailStatus.QUEUED, 3)).thenReturn(expectedEmails);

        // When
        List<Email> result = emailService.getQueuedEmails();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyElementsOf(expectedEmails);
        verify(emailRepository).findByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
                Email.EmailStatus.QUEUED, 3);
    }

    @Test
    void getConfigForMandate_shouldReturnConfig() {
        // Given
        when(emailConfigRepository.findFirstByMandatOrderByConfigNameAsc("TEST_MANDAT")).thenReturn(Optional.of(testConfig));

        // When
        Optional<EmailConfig> result = emailService.getConfigForMandate("TEST_MANDAT");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testConfig);
        verify(emailConfigRepository).findFirstByMandatOrderByConfigNameAsc("TEST_MANDAT");
    }

    @Test
    void getConfigForMandate_withNonExistentMandate_shouldReturnEmpty() {
        // Given
        when(emailConfigRepository.findFirstByMandatOrderByConfigNameAsc("UNKNOWN")).thenReturn(Optional.empty());

        // When
        Optional<EmailConfig> result = emailService.getConfigForMandate("UNKNOWN");

        // Then
        assertThat(result).isEmpty();
        verify(emailConfigRepository).findFirstByMandatOrderByConfigNameAsc("UNKNOWN");
    }

    @Test
    void saveConfig_shouldSaveAndReturnConfig() {
        // Given
        when(emailConfigRepository.save(testConfig)).thenReturn(testConfig);

        // When
        EmailConfig result = emailService.saveConfig(testConfig);

        // Then
        assertThat(result).isEqualTo(testConfig);
        verify(emailConfigRepository).save(testConfig);
    }

    @Test
    void deleteEmail_shouldCallRepository() {
        // Given
        doNothing().when(emailRepository).deleteById(1L);

        // When
        emailService.deleteEmail(1L);

        // Then
        verify(emailRepository).deleteById(1L);
    }

    @Test
    void getQueuedCount_shouldReturnCount() {
        // Given
        when(emailRepository.countByMandatAndStatus("TEST_MANDAT", Email.EmailStatus.QUEUED))
                .thenReturn(5L);

        // When
        long result = emailService.getQueuedCount("TEST_MANDAT");

        // Then
        assertThat(result).isEqualTo(5L);
        verify(emailRepository).countByMandatAndStatus("TEST_MANDAT", Email.EmailStatus.QUEUED);
    }

    @Test
    void getSentCount_shouldReturnCount() {
        // Given
        when(emailRepository.countByMandatAndStatus("TEST_MANDAT", Email.EmailStatus.SENT))
                .thenReturn(10L);

        // When
        long result = emailService.getSentCount("TEST_MANDAT");

        // Then
        assertThat(result).isEqualTo(10L);
        verify(emailRepository).countByMandatAndStatus("TEST_MANDAT", Email.EmailStatus.SENT);
    }

    @Test
    void getFailedCount_shouldReturnCount() {
        // Given
        when(emailRepository.countByMandatAndStatus("TEST_MANDAT", Email.EmailStatus.FAILED))
                .thenReturn(3L);

        // When
        long result = emailService.getFailedCount("TEST_MANDAT");

        // Then
        assertThat(result).isEqualTo(3L);
        verify(emailRepository).countByMandatAndStatus("TEST_MANDAT", Email.EmailStatus.FAILED);
    }

    @Test
    void getQueuedCount_withZeroEmails_shouldReturnZero() {
        // Given
        when(emailRepository.countByMandatAndStatus("TEST_MANDAT", Email.EmailStatus.QUEUED))
                .thenReturn(0L);

        // When
        long result = emailService.getQueuedCount("TEST_MANDAT");

        // Then
        assertThat(result).isZero();
    }
}
