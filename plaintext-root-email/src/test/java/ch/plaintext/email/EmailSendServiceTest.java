package ch.plaintext.email;

import ch.plaintext.email.model.Email;
import ch.plaintext.email.model.EmailConfig;
import ch.plaintext.email.service.EmailSendService;
import ch.plaintext.email.service.EmailService;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailSendServiceTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EmailSendService emailSendService;

    private Email testEmail;
    private EmailConfig testConfig;

    @BeforeEach
    void setUp() {
        testEmail = new Email();
        testEmail.setId(1L);
        testEmail.setMandat("TEST_MANDAT");
        testEmail.setToAddress("to@example.com");
        testEmail.setFromAddress("from@example.com");
        testEmail.setSubject("Test Subject");
        testEmail.setBody("Test Body");
        testEmail.setHtml(false);
        testEmail.setStatus(Email.EmailStatus.QUEUED);

        testConfig = new EmailConfig();
        testConfig.setId(1L);
        testConfig.setMandat("TEST_MANDAT");
        testConfig.setConfigName("default");
        testConfig.setSmtpEnabled(true);
        testConfig.setSmtpHost("smtp.example.com");
        testConfig.setSmtpPort(587);
        testConfig.setSmtpUsername("user@example.com");
        testConfig.setSmtpPassword("password");
        testConfig.setSmtpFromAddress("from@example.com");
        testConfig.setSmtpFromName("Test Sender");
        testConfig.setSmtpUseTls(true);
        testConfig.setSmtpUseSsl(false);
    }

    @Test
    void sendEmail_withValidConfiguration_shouldSendSuccessfully() {
        // Given
        when(emailService.getConfigForMandate("TEST_MANDAT")).thenReturn(Optional.of(testConfig));

        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            transportMock.when(() -> Transport.send(any(MimeMessage.class))).then(invocation -> null);

            // When
            emailSendService.sendEmail(testEmail);

            // Then
            transportMock.verify(() -> Transport.send(any(MimeMessage.class)), times(1));
            verify(emailService).getConfigForMandate("TEST_MANDAT");
            verify(emailService).markAsSent(eq(1L), nullable(String.class));
            verify(emailService, never()).markAsFailed(anyLong(), anyString());
        }
    }

    @Test
    void sendEmail_withMissingConfiguration_shouldMarkAsFailed() {
        // Given
        when(emailService.getConfigForMandate("TEST_MANDAT")).thenReturn(Optional.empty());

        // When
        emailSendService.sendEmail(testEmail);

        // Then
        verify(emailService).getConfigForMandate("TEST_MANDAT");
        verify(emailService).markAsFailed(eq(1L), contains("No email configuration found"));
        verify(emailService, never()).markAsSent(anyLong(), anyString());
    }

    @Test
    void sendEmail_withUnconfiguredSmtp_shouldMarkAsFailed() {
        // Given
        testConfig.setSmtpHost(null);
        when(emailService.getConfigForMandate("TEST_MANDAT")).thenReturn(Optional.of(testConfig));

        // When
        emailSendService.sendEmail(testEmail);

        // Then
        verify(emailService).getConfigForMandate("TEST_MANDAT");
        verify(emailService).markAsFailed(eq(1L), contains("SMTP is not configured"));
        verify(emailService, never()).markAsSent(anyLong(), anyString());
    }

    @Test
    void sendEmail_withTransportException_shouldMarkAsFailed() {
        // Given
        when(emailService.getConfigForMandate("TEST_MANDAT")).thenReturn(Optional.of(testConfig));

        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            transportMock.when(() -> Transport.send(any(MimeMessage.class)))
                    .thenThrow(new MessagingException("SMTP connection failed"));

            // When
            emailSendService.sendEmail(testEmail);

            // Then
            verify(emailService).markAsFailed(eq(1L), contains("SMTP connection failed"));
            verify(emailService, never()).markAsSent(anyLong(), anyString());
        }
    }

    @Test
    void sendEmail_withHtmlContent_shouldSendSuccessfully() {
        // Given
        testEmail.setHtml(true);
        testEmail.setBody("<html><body><h1>Test</h1></body></html>");
        when(emailService.getConfigForMandate("TEST_MANDAT")).thenReturn(Optional.of(testConfig));

        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            transportMock.when(() -> Transport.send(any(MimeMessage.class))).then(invocation -> null);

            // When
            emailSendService.sendEmail(testEmail);

            // Then
            transportMock.verify(() -> Transport.send(any(MimeMessage.class)), times(1));
            verify(emailService).markAsSent(eq(1L), nullable(String.class));
            verify(emailService, never()).markAsFailed(anyLong(), anyString());
            assertThat(testEmail.isHtml()).isTrue();
        }
    }

    @Test
    void sendEmail_withPlainTextContent_shouldSendSuccessfully() {
        // Given
        testEmail.setHtml(false);
        testEmail.setBody("Plain text body");
        when(emailService.getConfigForMandate("TEST_MANDAT")).thenReturn(Optional.of(testConfig));

        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            transportMock.when(() -> Transport.send(any(MimeMessage.class))).then(invocation -> null);

            // When
            emailSendService.sendEmail(testEmail);

            // Then
            transportMock.verify(() -> Transport.send(any(MimeMessage.class)), times(1));
            verify(emailService).markAsSent(eq(1L), nullable(String.class));
            verify(emailService, never()).markAsFailed(anyLong(), anyString());
            assertThat(testEmail.isHtml()).isFalse();
        }
    }

    @Test
    void sendEmail_withCcRecipients_shouldAddCcAddresses() {
        // Given
        testEmail.setCcAddress("cc1@example.com, cc2@example.com");
        when(emailService.getConfigForMandate("TEST_MANDAT")).thenReturn(Optional.of(testConfig));

        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
            transportMock.when(() -> Transport.send(any(MimeMessage.class))).then(invocation -> null);

            // When
            emailSendService.sendEmail(testEmail);

            // Then
            transportMock.verify(() -> Transport.send(messageCaptor.capture()), times(1));
            MimeMessage sentMessage = messageCaptor.getValue();

            Address[] ccRecipients = sentMessage.getRecipients(Message.RecipientType.CC);
            assertThat(ccRecipients).isNotNull();
            assertThat(ccRecipients).hasSize(2);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void sendEmail_withBccRecipients_shouldAddBccAddresses() {
        // Given
        testEmail.setBccAddress("bcc@example.com");
        when(emailService.getConfigForMandate("TEST_MANDAT")).thenReturn(Optional.of(testConfig));

        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
            transportMock.when(() -> Transport.send(any(MimeMessage.class))).then(invocation -> null);

            // When
            emailSendService.sendEmail(testEmail);

            // Then
            transportMock.verify(() -> Transport.send(messageCaptor.capture()), times(1));
            MimeMessage sentMessage = messageCaptor.getValue();

            Address[] bccRecipients = sentMessage.getRecipients(Message.RecipientType.BCC);
            assertThat(bccRecipients).isNotNull();
            assertThat(bccRecipients).hasSize(1);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void sendEmail_withTlsEnabled_shouldConfigureTls() {
        // Given
        testConfig.setSmtpUseTls(true);
        testConfig.setSmtpUseSsl(false);
        when(emailService.getConfigForMandate("TEST_MANDAT")).thenReturn(Optional.of(testConfig));

        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            transportMock.when(() -> Transport.send(any(MimeMessage.class))).then(invocation -> null);

            // When
            emailSendService.sendEmail(testEmail);

            // Then
            transportMock.verify(() -> Transport.send(any(MimeMessage.class)), times(1));
            verify(emailService).markAsSent(eq(1L), nullable(String.class));
        }
    }

    @Test
    void sendEmail_withSslEnabled_shouldConfigureSsl() {
        // Given
        testConfig.setSmtpUseTls(false);
        testConfig.setSmtpUseSsl(true);
        when(emailService.getConfigForMandate("TEST_MANDAT")).thenReturn(Optional.of(testConfig));

        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            transportMock.when(() -> Transport.send(any(MimeMessage.class))).then(invocation -> null);

            // When
            emailSendService.sendEmail(testEmail);

            // Then
            transportMock.verify(() -> Transport.send(any(MimeMessage.class)), times(1));
            verify(emailService).markAsSent(eq(1L), nullable(String.class));
        }
    }

    @Test
    void sendEmail_withFromName_shouldSetFromWithName() {
        // Given
        testConfig.setSmtpFromName("Test Sender");
        when(emailService.getConfigForMandate("TEST_MANDAT")).thenReturn(Optional.of(testConfig));

        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
            transportMock.when(() -> Transport.send(any(MimeMessage.class))).then(invocation -> null);

            // When
            emailSendService.sendEmail(testEmail);

            // Then
            transportMock.verify(() -> Transport.send(messageCaptor.capture()), times(1));
            MimeMessage sentMessage = messageCaptor.getValue();

            Address[] fromAddresses = sentMessage.getFrom();
            assertThat(fromAddresses).isNotNull();
            assertThat(fromAddresses).hasSize(1);
            assertThat(fromAddresses[0].toString()).contains("Test Sender");
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void sendEmail_withoutFromName_shouldSetFromWithoutName() {
        // Given
        testConfig.setSmtpFromName(null);
        when(emailService.getConfigForMandate("TEST_MANDAT")).thenReturn(Optional.of(testConfig));

        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
            transportMock.when(() -> Transport.send(any(MimeMessage.class))).then(invocation -> null);

            // When
            emailSendService.sendEmail(testEmail);

            // Then
            transportMock.verify(() -> Transport.send(messageCaptor.capture()), times(1));
            MimeMessage sentMessage = messageCaptor.getValue();

            Address[] fromAddresses = sentMessage.getFrom();
            assertThat(fromAddresses).isNotNull();
            assertThat(fromAddresses).hasSize(1);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void sendEmail_withNullFromAddressInEmail_shouldUseConfigFromAddress() {
        // Given
        testEmail.setFromAddress(null);
        when(emailService.getConfigForMandate("TEST_MANDAT")).thenReturn(Optional.of(testConfig));

        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
            transportMock.when(() -> Transport.send(any(MimeMessage.class))).then(invocation -> null);

            // When
            emailSendService.sendEmail(testEmail);

            // Then
            transportMock.verify(() -> Transport.send(messageCaptor.capture()), times(1));
            MimeMessage sentMessage = messageCaptor.getValue();

            Address[] fromAddresses = sentMessage.getFrom();
            assertThat(fromAddresses).isNotNull();
            assertThat(fromAddresses[0].toString()).contains("from@example.com");
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void sendEmail_withSubject_shouldSetSubject() {
        // Given
        testEmail.setSubject("Important Test Subject");
        when(emailService.getConfigForMandate("TEST_MANDAT")).thenReturn(Optional.of(testConfig));

        try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
            ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
            transportMock.when(() -> Transport.send(any(MimeMessage.class))).then(invocation -> null);

            // When
            emailSendService.sendEmail(testEmail);

            // Then
            transportMock.verify(() -> Transport.send(messageCaptor.capture()), times(1));
            MimeMessage sentMessage = messageCaptor.getValue();

            assertThat(sentMessage.getSubject()).isEqualTo("Important Test Subject");
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }
}
