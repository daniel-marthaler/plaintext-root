/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.email.cron;

import ch.plaintext.PlaintextEmailSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailTestCronTest {

    @Mock
    private PlaintextEmailSender emailSender;

    @InjectMocks
    private EmailTestCron emailTestCron;

    @Test
    void isGlobalReturnsTrue() {
        assertTrue(emailTestCron.isGlobal());
    }

    @Test
    void getDisplayName() {
        assertEquals("Email Test Versand", emailTestCron.getDisplayName());
    }

    @Test
    void getDefaultCronExpressionReturnsNull() {
        // Disabled by default
        assertNull(emailTestCron.getDefaultCronExpression());
    }

    @Test
    void runSendsTestEmail() {
        when(emailSender.sendEmail(eq("maintenance"), eq("daniel@marthaler.io"),
                anyString(), anyString(), eq(true)))
                .thenReturn(100L);

        emailTestCron.run("any-mandant");

        verify(emailSender).sendEmail(
                eq("maintenance"),
                eq("daniel@marthaler.io"),
                contains("Test-Email von Plaintext"),
                contains("Plaintext Email Test"),
                eq(true)
        );
    }

    @Test
    void runHandlesException() {
        when(emailSender.sendEmail(anyString(), anyString(), anyString(), anyString(), anyBoolean()))
                .thenThrow(new RuntimeException("Send failed"));

        assertDoesNotThrow(() -> emailTestCron.run("any-mandant"));
    }

    @Test
    void testEmailBodyContainsHtml() {
        when(emailSender.sendEmail(eq("maintenance"), eq("daniel@marthaler.io"),
                anyString(), anyString(), eq(true)))
                .thenReturn(1L);

        emailTestCron.run("test");

        verify(emailSender).sendEmail(eq("maintenance"), eq("daniel@marthaler.io"),
                anyString(),
                argThat(body -> body.contains("<!DOCTYPE html>") && body.contains("Plaintext Email Test")),
                eq(true));
    }
}
