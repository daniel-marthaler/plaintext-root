/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.email.listener;

import ch.plaintext.PlaintextEmailReceiver;
import ch.plaintext.email.model.Email;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailTestListenerTest {

    @Mock
    private PlaintextEmailReceiver emailReceiver;

    @InjectMocks
    private EmailTestListener emailTestListener;

    @Test
    void getListenerName() {
        assertEquals("Email Test Listener", emailTestListener.getListenerName());
    }

    @Test
    void getConfigNamesToListenTo() {
        List<String> configs = emailTestListener.getConfigNamesToListenTo();

        assertNotNull(configs);
        assertEquals(1, configs.size());
        assertEquals("maintenance", configs.get(0));
    }

    @Test
    void onEmailReceivedReadsEmailDetails() {
        Email email = new Email();
        email.setId(1L);
        email.setSubject("Test Subject");
        email.setFromAddress("from@example.com");
        email.setToAddress("to@example.com");
        email.setDirection(Email.EmailDirection.INCOMING);
        email.setStatus(Email.EmailStatus.RECEIVED);
        email.setBody("Hello World");
        email.setHtml(false);

        when(emailReceiver.readEmail(1L)).thenReturn(Optional.of(email));

        // Should not throw
        assertDoesNotThrow(() ->
                emailTestListener.onEmailReceived(1L, "test-mandat", "maintenance"));

        // readEmail is called twice (once for subject, once for details)
        verify(emailReceiver, times(2)).readEmail(1L);
    }

    @Test
    void onEmailReceivedHandlesEmptyOptional() {
        when(emailReceiver.readEmail(999L)).thenReturn(Optional.empty());

        assertDoesNotThrow(() ->
                emailTestListener.onEmailReceived(999L, "test-mandat", "maintenance"));
    }

    @Test
    void onEmailReceivedHandlesNullBody() {
        Email email = new Email();
        email.setId(1L);
        email.setSubject("Subject");
        email.setBody(null);

        when(emailReceiver.readEmail(1L)).thenReturn(Optional.of(email));

        assertDoesNotThrow(() ->
                emailTestListener.onEmailReceived(1L, "test-mandat", "maintenance"));
    }

    @Test
    void onEmailReceivedHandlesLongBody() {
        Email email = new Email();
        email.setId(1L);
        email.setSubject("Subject");
        email.setBody("A".repeat(500)); // Body longer than 200 chars

        when(emailReceiver.readEmail(1L)).thenReturn(Optional.of(email));

        assertDoesNotThrow(() ->
                emailTestListener.onEmailReceived(1L, "test-mandat", "maintenance"));
    }
}
