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
class PlaintextEmailSenderImplTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private PlaintextEmailSenderImpl sender;

    @Test
    void sendEmailQueuesEmailSuccessfully() {
        EmailConfig config = new EmailConfig();
        config.setMandat("mandat-1");
        config.setConfigName("maintenance");

        Email draft = new Email();
        draft.setId(10L);
        draft.setStatus(Email.EmailStatus.DRAFT);

        Email queued = new Email();
        queued.setId(10L);
        queued.setStatus(Email.EmailStatus.QUEUED);

        when(emailService.findConfigByNameAcrossAllMandates("maintenance"))
                .thenReturn(Optional.of(config));
        when(emailService.createDraft("mandat-1", "maintenance", "to@example.com",
                null, null, "Subject", "Body", false))
                .thenReturn(draft);
        when(emailService.queueEmail(10L)).thenReturn(queued);

        Long result = sender.sendEmail("maintenance", "to@example.com", "Subject", "Body", false);

        assertEquals(10L, result);
        verify(emailService).createDraft("mandat-1", "maintenance", "to@example.com",
                null, null, "Subject", "Body", false);
        verify(emailService).queueEmail(10L);
    }

    @Test
    void sendEmailWithCcAndBcc() {
        EmailConfig config = new EmailConfig();
        config.setMandat("mandat-1");
        config.setConfigName("maintenance");

        Email draft = new Email();
        draft.setId(20L);

        Email queued = new Email();
        queued.setId(20L);

        when(emailService.findConfigByNameAcrossAllMandates("maintenance"))
                .thenReturn(Optional.of(config));
        when(emailService.createDraft("mandat-1", "maintenance", "to@example.com",
                "cc@example.com", "bcc@example.com", "Subject", "Body", true))
                .thenReturn(draft);
        when(emailService.queueEmail(20L)).thenReturn(queued);

        Long result = sender.sendEmail("maintenance", "to@example.com",
                "cc@example.com", "bcc@example.com", "Subject", "Body", true);

        assertEquals(20L, result);
    }

    @Test
    void sendEmailReturnsNullWhenConfigNotFound() {
        when(emailService.findConfigByNameAcrossAllMandates("nonexistent"))
                .thenReturn(Optional.empty());

        Long result = sender.sendEmail("nonexistent", "to@example.com",
                "Subject", "Body", false);

        assertNull(result);
        verify(emailService, never()).createDraft(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test
    void sendEmailThrowsWhenConfigNameNull() {
        assertThrows(IllegalArgumentException.class,
                () -> sender.sendEmail(null, "to@example.com", "Subject", "Body", false));
    }

    @Test
    void sendEmailThrowsWhenConfigNameEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> sender.sendEmail("", "to@example.com", "Subject", "Body", false));
    }

    @Test
    void sendEmailThrowsWhenConfigNameBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> sender.sendEmail("   ", "to@example.com", "Subject", "Body", false));
    }

    @Test
    void sendEmailThrowsWhenRecipientNull() {
        assertThrows(IllegalArgumentException.class,
                () -> sender.sendEmail("config", null, "Subject", "Body", false));
    }

    @Test
    void sendEmailThrowsWhenRecipientEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> sender.sendEmail("config", "", "Subject", "Body", false));
    }

    @Test
    void sendEmailWrapsUnexpectedException() {
        EmailConfig config = new EmailConfig();
        config.setMandat("mandat-1");
        config.setConfigName("config");

        when(emailService.findConfigByNameAcrossAllMandates("config"))
                .thenReturn(Optional.of(config));
        when(emailService.createDraft(anyString(), anyString(), anyString(),
                isNull(), isNull(), anyString(), anyString(), anyBoolean()))
                .thenThrow(new RuntimeException("DB error"));

        assertThrows(IllegalStateException.class,
                () -> sender.sendEmail("config", "to@example.com", "Subject", "Body", false));
    }

    @Test
    void sendEmailWithCcAndBccDelegatesToOverload() {
        EmailConfig config = new EmailConfig();
        config.setMandat("mandat-1");
        config.setConfigName("config");

        Email draft = new Email();
        draft.setId(30L);

        Email queued = new Email();
        queued.setId(30L);

        when(emailService.findConfigByNameAcrossAllMandates("config"))
                .thenReturn(Optional.of(config));
        when(emailService.createDraft("mandat-1", "config", "to@example.com",
                null, null, "Subject", "Body", false))
                .thenReturn(draft);
        when(emailService.queueEmail(30L)).thenReturn(queued);

        // The simple sendEmail should delegate to the full version with null cc/bcc
        Long result = sender.sendEmail("config", "to@example.com", "Subject", "Body", false);

        assertEquals(30L, result);
    }
}
