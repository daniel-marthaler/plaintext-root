/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.email.cron;

import ch.plaintext.email.model.Email;
import ch.plaintext.email.model.EmailConfig;
import ch.plaintext.email.persistence.EmailConfigRepository;
import ch.plaintext.email.service.EmailReceiveService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailReceiveTestCronTest {

    @Mock
    private EmailConfigRepository emailConfigRepository;

    @Mock
    private EmailReceiveService emailReceiveService;

    @InjectMocks
    private EmailReceiveTestCron emailReceiveTestCron;

    @Test
    void isGlobalReturnsTrue() {
        assertTrue(emailReceiveTestCron.isGlobal());
    }

    @Test
    void getDisplayName() {
        assertEquals("Email Test Empfang", emailReceiveTestCron.getDisplayName());
    }

    @Test
    void getDefaultCronExpressionReturnsNull() {
        assertNull(emailReceiveTestCron.getDefaultCronExpression());
    }

    @Test
    void runSkipsWhenNoImapConfigs() {
        when(emailConfigRepository.findByImapEnabledTrue()).thenReturn(Collections.emptyList());

        emailReceiveTestCron.run("any");

        verify(emailReceiveService, never()).receiveEmailsFromConfig(any());
    }

    @Test
    void runProcessesImapEnabledConfigs() {
        EmailConfig config = new EmailConfig();
        config.setImapEnabled(true);
        config.setImapHost("imap.example.com");
        config.setImapUsername("user");
        config.setImapPassword("pass");
        config.setId(1L);
        config.setConfigName("maintenance");
        config.setMandat("test");

        when(emailConfigRepository.findByImapEnabledTrue()).thenReturn(List.of(config));
        when(emailReceiveService.receiveEmailsFromConfig(config))
                .thenReturn(List.of(new Email()));

        emailReceiveTestCron.run("any");

        verify(emailReceiveService).receiveEmailsFromConfig(config);
    }

    @Test
    void runSkipsNotProperlyConfiguredConfigs() {
        EmailConfig config = new EmailConfig();
        config.setImapEnabled(true);
        config.setImapHost(null); // Not properly configured
        config.setConfigName("broken");
        config.setMandat("test");

        when(emailConfigRepository.findByImapEnabledTrue()).thenReturn(List.of(config));

        emailReceiveTestCron.run("any");

        verify(emailReceiveService, never()).receiveEmailsFromConfig(any());
    }

    @Test
    void runContinuesOnIndividualConfigFailure() {
        EmailConfig config1 = new EmailConfig();
        config1.setImapEnabled(true);
        config1.setImapHost("imap1.example.com");
        config1.setImapUsername("user1");
        config1.setImapPassword("pass1");
        config1.setId(1L);
        config1.setConfigName("config1");
        config1.setMandat("test");

        EmailConfig config2 = new EmailConfig();
        config2.setImapEnabled(true);
        config2.setImapHost("imap2.example.com");
        config2.setImapUsername("user2");
        config2.setImapPassword("pass2");
        config2.setId(2L);
        config2.setConfigName("config2");
        config2.setMandat("test");

        when(emailConfigRepository.findByImapEnabledTrue()).thenReturn(List.of(config1, config2));
        when(emailReceiveService.receiveEmailsFromConfig(config1))
                .thenThrow(new RuntimeException("Connection failed"));
        when(emailReceiveService.receiveEmailsFromConfig(config2))
                .thenReturn(List.of(new Email()));

        emailReceiveTestCron.run("any");

        verify(emailReceiveService).receiveEmailsFromConfig(config1);
        verify(emailReceiveService).receiveEmailsFromConfig(config2);
    }

    @Test
    void runHandlesTopLevelException() {
        when(emailConfigRepository.findByImapEnabledTrue())
                .thenThrow(new RuntimeException("DB error"));

        assertDoesNotThrow(() -> emailReceiveTestCron.run("any"));
    }
}
