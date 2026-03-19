/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.email.cron;

import ch.plaintext.email.model.Email;
import ch.plaintext.email.model.EmailConfig;
import ch.plaintext.email.service.EmailReceiveService;
import ch.plaintext.email.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailReceiveCronTest {

    @Mock
    private EmailService emailService;

    @Mock
    private EmailReceiveService emailReceiveService;

    @InjectMocks
    private EmailReceiveCron emailReceiveCron;

    @Test
    void isGlobalReturnsFalseByDefault() {
        // EmailReceiveCron does not override isGlobal(), so default is false
        assertFalse(emailReceiveCron.isGlobal());
    }

    @Test
    void getDisplayName() {
        assertEquals("Email Empfang", emailReceiveCron.getDisplayName());
    }

    @Test
    void getDefaultCronExpression() {
        assertEquals("*/10 * * * *", emailReceiveCron.getDefaultCronExpression());
    }

    @Test
    void runReceivesEmailsWhenImapConfigured() {
        EmailConfig config = new EmailConfig();
        config.setImapEnabled(true);
        config.setImapHost("imap.example.com");
        config.setImapUsername("user");
        config.setId(1L); // existing config

        when(emailService.getConfigForMandate("test")).thenReturn(Optional.of(config));
        when(emailReceiveService.receiveEmails("test")).thenReturn(List.of(new Email()));

        emailReceiveCron.run("test");

        verify(emailReceiveService).receiveEmails("test");
    }

    @Test
    void runSkipsWhenNoConfig() {
        when(emailService.getConfigForMandate("test")).thenReturn(Optional.empty());

        emailReceiveCron.run("test");

        verify(emailReceiveService, never()).receiveEmails(anyString());
    }

    @Test
    void runSkipsWhenImapNotConfigured() {
        EmailConfig config = new EmailConfig();
        config.setImapEnabled(false);

        when(emailService.getConfigForMandate("test")).thenReturn(Optional.of(config));

        emailReceiveCron.run("test");

        verify(emailReceiveService, never()).receiveEmails(anyString());
    }

    @Test
    void runHandlesException() {
        when(emailService.getConfigForMandate("test"))
                .thenThrow(new RuntimeException("DB error"));

        assertDoesNotThrow(() -> emailReceiveCron.run("test"));
    }
}
