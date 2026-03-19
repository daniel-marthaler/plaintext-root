/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.email.service;

import ch.plaintext.email.model.Email;
import ch.plaintext.email.model.EmailConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailSendServiceTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EmailSendService emailSendService;

    @Test
    void sendEmailMarksAsFailedWhenNoConfig() {
        Email email = new Email();
        email.setId(1L);
        email.setMandat("test");
        email.setConfigName("nonexistent");

        when(emailService.getConfigByName("test", "nonexistent"))
                .thenReturn(Optional.empty());

        emailSendService.sendEmail(email);

        verify(emailService).markAsFailed(eq(1L), contains("No email configuration found"));
    }

    @Test
    void sendEmailMarksAsFailedWhenSmtpNotConfigured() {
        Email email = new Email();
        email.setId(2L);
        email.setMandat("test");
        email.setConfigName("myconfig");

        EmailConfig config = new EmailConfig();
        config.setConfigName("myconfig");
        config.setSmtpEnabled(false); // Not configured

        when(emailService.getConfigByName("test", "myconfig"))
                .thenReturn(Optional.of(config));

        emailSendService.sendEmail(email);

        verify(emailService).markAsFailed(eq(2L), contains("SMTP is not configured"));
    }

    @Test
    void sendEmailUsesDefaultConfigWhenNoConfigName() {
        Email email = new Email();
        email.setId(3L);
        email.setMandat("test");
        email.setConfigName(null); // No config name

        when(emailService.getConfigForMandate("test")).thenReturn(Optional.empty());

        emailSendService.sendEmail(email);

        verify(emailService).getConfigForMandate("test");
        verify(emailService).markAsFailed(eq(3L), anyString());
    }

    @Test
    void sendEmailUsesDefaultConfigWhenConfigNameBlank() {
        Email email = new Email();
        email.setId(4L);
        email.setMandat("test");
        email.setConfigName("   "); // Blank config name

        when(emailService.getConfigForMandate("test")).thenReturn(Optional.empty());

        emailSendService.sendEmail(email);

        verify(emailService).getConfigForMandate("test");
        verify(emailService).markAsFailed(eq(4L), anyString());
    }

    @Test
    void sendEmailLooksUpConfigByNameWhenProvided() {
        Email email = new Email();
        email.setId(5L);
        email.setMandat("test");
        email.setConfigName("specific");

        when(emailService.getConfigByName("test", "specific"))
                .thenReturn(Optional.empty());

        emailSendService.sendEmail(email);

        verify(emailService).getConfigByName("test", "specific");
        verify(emailService, never()).getConfigForMandate(anyString());
    }

    @Test
    void sendEmailCatchesTransportExceptionAndMarksFailed() {
        // When Transport.send() fails (which it will without a real SMTP server),
        // the email should be marked as failed.
        Email email = new Email();
        email.setId(6L);
        email.setMandat("test");
        email.setConfigName("myconfig");
        email.setToAddress("to@example.com");
        email.setFromAddress("from@example.com");
        email.setSubject("Test");
        email.setBody("Body");

        EmailConfig config = new EmailConfig();
        config.setConfigName("myconfig");
        config.setSmtpEnabled(true);
        config.setSmtpHost("localhost");
        config.setSmtpPort(25);
        config.setSmtpUsername("user");
        config.setSmtpPassword("pass");
        config.setSmtpFromAddress("from@example.com");
        config.setSmtpUseTls(false);
        config.setSmtpUseSsl(false);

        when(emailService.getConfigByName("test", "myconfig"))
                .thenReturn(Optional.of(config));

        // This will fail because there's no actual SMTP server
        emailSendService.sendEmail(email);

        // Should be marked as failed (Transport.send will throw)
        verify(emailService).markAsFailed(eq(6L), anyString());
        verify(emailService, never()).markAsSent(anyLong(), anyString());
    }
}
