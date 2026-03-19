/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.email.service;

import ch.plaintext.PlaintextIncomingEmailListener;
import ch.plaintext.email.model.Email;
import ch.plaintext.email.model.EmailConfig;
import ch.plaintext.email.persistence.EmailRepository;
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
class EmailReceiveServiceTest {

    @Mock
    private EmailService emailService;

    @Mock
    private EmailRepository emailRepository;

    @Mock
    private List<PlaintextIncomingEmailListener> emailListeners;

    @InjectMocks
    private EmailReceiveService emailReceiveService;

    @Test
    void receiveEmailsReturnsEmptyListWhenNoConfig() {
        when(emailService.getConfigForMandate("test")).thenReturn(Optional.empty());

        List<Email> result = emailReceiveService.receiveEmails("test");

        assertTrue(result.isEmpty());
    }

    @Test
    void receiveEmailsFromConfigReturnsEmptyWhenImapNotConfigured() {
        EmailConfig config = new EmailConfig();
        config.setImapEnabled(false);
        config.setConfigName("test");

        List<Email> result = emailReceiveService.receiveEmailsFromConfig(config);

        assertTrue(result.isEmpty());
    }

    @Test
    void receiveEmailsFromConfigReturnsEmptyWhenImapEnabledButNotFullyConfigured() {
        EmailConfig config = new EmailConfig();
        config.setImapEnabled(true);
        config.setImapHost(null); // Missing host
        config.setConfigName("test");

        List<Email> result = emailReceiveService.receiveEmailsFromConfig(config);

        assertTrue(result.isEmpty());
    }

    @Test
    void receiveEmailsFromConfigHandlesConnectionFailureGracefully() {
        EmailConfig config = new EmailConfig();
        config.setImapEnabled(true);
        config.setImapHost("nonexistent.example.com");
        config.setImapPort(993);
        config.setImapUsername("user");
        config.setImapPassword("pass");
        config.setImapUseSsl(true);
        config.setImapFolder("INBOX");
        config.setConfigName("broken");

        // This should not throw, but return empty list after logging error
        List<Email> result = emailReceiveService.receiveEmailsFromConfig(config);

        assertTrue(result.isEmpty());
    }

    @Test
    void receiveEmailsCatchesExceptions() {
        when(emailService.getConfigForMandate("test"))
                .thenThrow(new RuntimeException("DB error"));

        // Should not throw
        List<Email> result = emailReceiveService.receiveEmails("test");

        assertTrue(result.isEmpty());
    }

    @Test
    void testImapConnectionThrowsOnInvalidConfig() {
        EmailConfig config = new EmailConfig();
        config.setImapHost("nonexistent.example.com");
        config.setImapPort(993);
        config.setImapUsername("user");
        config.setImapPassword("pass");
        config.setImapUseSsl(true);

        // testImapConnection does NOT catch exceptions, so this should throw
        assertThrows(Exception.class,
                () -> emailReceiveService.testImapConnection(config));
    }
}
