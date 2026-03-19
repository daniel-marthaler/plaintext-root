/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.email.cron;

import ch.plaintext.email.model.Email;
import ch.plaintext.email.service.EmailSendService;
import ch.plaintext.email.service.EmailService;
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
class EmailSendCronTest {

    @Mock
    private EmailService emailService;

    @Mock
    private EmailSendService emailSendService;

    @InjectMocks
    private EmailSendCron emailSendCron;

    @Test
    void isGlobalReturnsTrue() {
        assertTrue(emailSendCron.isGlobal());
    }

    @Test
    void getDisplayName() {
        assertEquals("Email Versand (Global)", emailSendCron.getDisplayName());
    }

    @Test
    void getDefaultCronExpression() {
        assertEquals("*/5 * * * *", emailSendCron.getDefaultCronExpression());
    }

    @Test
    void runSendsQueuedEmails() {
        Email email1 = new Email();
        email1.setId(1L);
        email1.setMandat("test");
        Email email2 = new Email();
        email2.setId(2L);
        email2.setMandat("test");

        when(emailService.getQueuedEmails()).thenReturn(List.of(email1, email2));

        emailSendCron.run("any-mandant");

        verify(emailSendService).sendEmail(email1);
        verify(emailSendService).sendEmail(email2);
    }

    @Test
    void runHandlesEmptyQueue() {
        when(emailService.getQueuedEmails()).thenReturn(Collections.emptyList());

        emailSendCron.run("any-mandant");

        verify(emailSendService, never()).sendEmail(any());
    }

    @Test
    void runContinuesOnIndividualFailure() {
        Email email1 = new Email();
        email1.setId(1L);
        email1.setMandat("test");
        Email email2 = new Email();
        email2.setId(2L);
        email2.setMandat("test");

        when(emailService.getQueuedEmails()).thenReturn(List.of(email1, email2));
        doThrow(new RuntimeException("Send failed")).when(emailSendService).sendEmail(email1);

        emailSendCron.run("any-mandant");

        // Second email should still be attempted even though first failed
        verify(emailSendService).sendEmail(email2);
    }

    @Test
    void runHandlesExceptionGettingQueuedEmails() {
        when(emailService.getQueuedEmails()).thenThrow(new RuntimeException("DB error"));

        // Should not throw
        assertDoesNotThrow(() -> emailSendCron.run("any-mandant"));
    }

    @Test
    void runLogsConfigNameFromEmail() {
        Email emailWithConfig = new Email();
        emailWithConfig.setId(1L);
        emailWithConfig.setMandat("test");
        emailWithConfig.setConfigName("maintenance");

        Email emailWithoutConfig = new Email();
        emailWithoutConfig.setId(2L);
        emailWithoutConfig.setMandat("test");
        emailWithoutConfig.setConfigName(null);

        when(emailService.getQueuedEmails()).thenReturn(List.of(emailWithConfig, emailWithoutConfig));

        emailSendCron.run("any-mandant");

        verify(emailSendService).sendEmail(emailWithConfig);
        verify(emailSendService).sendEmail(emailWithoutConfig);
    }
}
