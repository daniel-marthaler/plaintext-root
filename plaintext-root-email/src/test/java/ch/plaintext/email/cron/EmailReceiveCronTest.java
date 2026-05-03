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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailReceiveCronTest {

    private static final String MANDAT = "test";

    @Mock
    private EmailConfigRepository emailConfigRepository;

    @Mock
    private EmailReceiveService emailReceiveService;

    @InjectMocks
    private EmailReceiveCron emailReceiveCron;

    @Test
    void isGlobalReturnsFalseByDefault() {
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
    void runSkipsWhenNoConfigs() {
        when(emailConfigRepository.findByMandatAndImapEnabledTrue(MANDAT))
                .thenReturn(Collections.emptyList());

        emailReceiveCron.run(MANDAT);

        verify(emailReceiveService, never()).receiveEmailsFromConfig(any(EmailConfig.class));
    }

    @Test
    void runIteratesAllImapEnabledConfigs() {
        EmailConfig c1 = fullImap("kalenderimport", 1L);
        EmailConfig c2 = fullImap("postkonto", 2L);

        when(emailConfigRepository.findByMandatAndImapEnabledTrue(MANDAT)).thenReturn(List.of(c1, c2));
        when(emailReceiveService.receiveEmailsFromConfig(c1)).thenReturn(List.of(new Email()));
        when(emailReceiveService.receiveEmailsFromConfig(c2)).thenReturn(List.of(new Email(), new Email()));

        emailReceiveCron.run(MANDAT);

        verify(emailReceiveService).receiveEmailsFromConfig(c1);
        verify(emailReceiveService).receiveEmailsFromConfig(c2);
    }

    @Test
    void runSkipsConfigsThatAreNotProperlyConfigured() {
        EmailConfig incomplete = new EmailConfig();
        incomplete.setImapEnabled(true);
        incomplete.setImapHost(null); // missing host -> isImapConfigured() == false
        incomplete.setConfigName("incomplete");

        when(emailConfigRepository.findByMandatAndImapEnabledTrue(MANDAT)).thenReturn(List.of(incomplete));

        emailReceiveCron.run(MANDAT);

        verify(emailReceiveService, never()).receiveEmailsFromConfig(any(EmailConfig.class));
    }

    @Test
    void runContinuesAfterIndividualConfigFailure() {
        EmailConfig c1 = fullImap("kalenderimport", 1L);
        EmailConfig c2 = fullImap("postkonto", 2L);

        when(emailConfigRepository.findByMandatAndImapEnabledTrue(MANDAT)).thenReturn(List.of(c1, c2));
        when(emailReceiveService.receiveEmailsFromConfig(c1))
                .thenThrow(new RuntimeException("IMAP error"));

        assertDoesNotThrow(() -> emailReceiveCron.run(MANDAT));

        verify(emailReceiveService).receiveEmailsFromConfig(c2);
    }

    @Test
    void runHandlesRepositoryException() {
        when(emailConfigRepository.findByMandatAndImapEnabledTrue(MANDAT))
                .thenThrow(new RuntimeException("DB error"));

        assertDoesNotThrow(() -> emailReceiveCron.run(MANDAT));

        verify(emailReceiveService, never()).receiveEmailsFromConfig(any(EmailConfig.class));
    }

    private EmailConfig fullImap(String name, Long id) {
        EmailConfig c = new EmailConfig();
        c.setId(id);
        c.setMandat(MANDAT);
        c.setConfigName(name);
        c.setImapEnabled(true);
        c.setImapHost("imap.example.com");
        c.setImapPort(993);
        c.setImapUsername("user");
        c.setImapPassword("pw");
        c.setImapFolder("INBOX");
        return c;
    }
}
