/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.email.cron;

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
class DynamicEmailReceiveCronServiceTest {

    @Mock
    private EmailConfigRepository emailConfigRepository;

    @Mock
    private EmailReceiveService emailReceiveService;

    @InjectMocks
    private DynamicEmailReceiveCronService cronService;

    @Test
    void isGlobalReturnsTrue() {
        assertTrue(cronService.isGlobal());
    }

    @Test
    void getDisplayName() {
        assertEquals("E-Mail Empfang (Global)", cronService.getDisplayName());
    }

    @Test
    void getDefaultCronExpression() {
        assertEquals("0 */5 * * *", cronService.getDefaultCronExpression());
    }

    @Test
    void runSkipsWhenNoImapConfigs() {
        when(emailConfigRepository.findByImapEnabledTrue()).thenReturn(Collections.emptyList());

        cronService.run("any");

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
        config.setConfigName("default");
        config.setMandat("test");

        when(emailConfigRepository.findByImapEnabledTrue()).thenReturn(List.of(config));

        cronService.run("test");

        verify(emailReceiveService).receiveEmailsFromConfig(config);
    }

    @Test
    void runSkipsNotProperlyConfiguredConfigs() {
        EmailConfig config = new EmailConfig();
        config.setImapEnabled(true);
        config.setImapHost(null); // Not properly configured
        config.setConfigName("incomplete");
        config.setMandat("test");

        when(emailConfigRepository.findByImapEnabledTrue()).thenReturn(List.of(config));

        cronService.run("test");

        verify(emailReceiveService, never()).receiveEmailsFromConfig(any());
    }

    @Test
    void runContinuesOnIndividualFailure() {
        EmailConfig config1 = createFullImapConfig("config1", 1L);
        EmailConfig config2 = createFullImapConfig("config2", 2L);

        when(emailConfigRepository.findByImapEnabledTrue()).thenReturn(List.of(config1, config2));
        doThrow(new RuntimeException("IMAP error")).when(emailReceiveService).receiveEmailsFromConfig(config1);

        cronService.run("test");

        // config2 should still be processed
        verify(emailReceiveService).receiveEmailsFromConfig(config2);
    }

    @Test
    void getActiveConfigCount() {
        EmailConfig c1 = new EmailConfig();
        c1.setImapEnabled(true);
        EmailConfig c2 = new EmailConfig();
        c2.setImapEnabled(true);

        when(emailConfigRepository.findByImapEnabledTrue()).thenReturn(List.of(c1, c2));

        assertEquals(2, cronService.getActiveConfigCount());
    }

    @Test
    void getActiveConfigCountReturnsZeroWhenNone() {
        when(emailConfigRepository.findByImapEnabledTrue()).thenReturn(Collections.emptyList());

        assertEquals(0, cronService.getActiveConfigCount());
    }

    private EmailConfig createFullImapConfig(String name, Long id) {
        EmailConfig config = new EmailConfig();
        config.setId(id);
        config.setImapEnabled(true);
        config.setImapHost("imap.example.com");
        config.setImapUsername("user");
        config.setImapPassword("pass");
        config.setConfigName(name);
        config.setMandat("test");
        return config;
    }
}
