/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.email.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EmailTest {

    @Test
    void defaultValues() {
        Email email = new Email();

        assertFalse(email.isHtml());
        assertEquals(Email.EmailStatus.DRAFT, email.getStatus());
        assertEquals(Email.EmailDirection.OUTGOING, email.getDirection());
        assertEquals(0, email.getRetryCount());
        assertEquals(3, email.getMaxRetries());
        assertNotNull(email.getAttachments());
        assertTrue(email.getAttachments().isEmpty());
    }

    @Test
    void prePersistSetsCreatedAt() {
        Email email = new Email();
        assertNull(email.getCreatedAt());

        email.prePersist();

        assertNotNull(email.getCreatedAt());
    }

    @Test
    void prePersistDoesNotOverwriteExistingCreatedAt() {
        Email email = new Email();
        LocalDateTime fixed = LocalDateTime.of(2024, 1, 1, 12, 0);
        email.setCreatedAt(fixed);

        email.prePersist();

        assertEquals(fixed, email.getCreatedAt());
    }

    @Test
    void addAttachment() {
        Email email = new Email();
        EmailAttachment attachment = new EmailAttachment();
        attachment.setFilename("test.pdf");

        email.addAttachment(attachment);

        assertEquals(1, email.getAttachments().size());
        assertSame(attachment, email.getAttachments().get(0));
        assertSame(email, attachment.getEmail());
    }

    @Test
    void removeAttachment() {
        Email email = new Email();
        EmailAttachment attachment = new EmailAttachment();
        attachment.setFilename("test.pdf");
        email.addAttachment(attachment);

        email.removeAttachment(attachment);

        assertTrue(email.getAttachments().isEmpty());
        assertNull(attachment.getEmail());
    }

    @Test
    void addMultipleAttachments() {
        Email email = new Email();
        EmailAttachment a1 = new EmailAttachment();
        a1.setFilename("file1.pdf");
        EmailAttachment a2 = new EmailAttachment();
        a2.setFilename("file2.jpg");

        email.addAttachment(a1);
        email.addAttachment(a2);

        assertEquals(2, email.getAttachments().size());
    }

    @Test
    void emailStatusEnumValues() {
        Email.EmailStatus[] values = Email.EmailStatus.values();
        assertEquals(6, values.length);
        assertNotNull(Email.EmailStatus.valueOf("DRAFT"));
        assertNotNull(Email.EmailStatus.valueOf("QUEUED"));
        assertNotNull(Email.EmailStatus.valueOf("SENDING"));
        assertNotNull(Email.EmailStatus.valueOf("SENT"));
        assertNotNull(Email.EmailStatus.valueOf("FAILED"));
        assertNotNull(Email.EmailStatus.valueOf("RECEIVED"));
    }

    @Test
    void emailDirectionEnumValues() {
        Email.EmailDirection[] values = Email.EmailDirection.values();
        assertEquals(2, values.length);
        assertNotNull(Email.EmailDirection.valueOf("INCOMING"));
        assertNotNull(Email.EmailDirection.valueOf("OUTGOING"));
    }

    @Test
    void settersAndGetters() {
        Email email = new Email();
        email.setId(42L);
        email.setMandat("test-mandat");
        email.setConfigName("default");
        email.setFromAddress("from@example.com");
        email.setToAddress("to@example.com");
        email.setCcAddress("cc@example.com");
        email.setBccAddress("bcc@example.com");
        email.setSubject("Test Subject");
        email.setBody("Test Body");
        email.setHtml(true);
        email.setStatus(Email.EmailStatus.SENT);
        email.setDirection(Email.EmailDirection.INCOMING);
        email.setErrorMessage("Some error");
        email.setRetryCount(2);
        email.setMaxRetries(5);
        email.setMessageId("<msg-id@example.com>");

        LocalDateTime now = LocalDateTime.now();
        email.setCreatedAt(now);
        email.setSentAt(now);
        email.setReceivedAt(now);

        assertEquals(42L, email.getId());
        assertEquals("test-mandat", email.getMandat());
        assertEquals("default", email.getConfigName());
        assertEquals("from@example.com", email.getFromAddress());
        assertEquals("to@example.com", email.getToAddress());
        assertEquals("cc@example.com", email.getCcAddress());
        assertEquals("bcc@example.com", email.getBccAddress());
        assertEquals("Test Subject", email.getSubject());
        assertEquals("Test Body", email.getBody());
        assertTrue(email.isHtml());
        assertEquals(Email.EmailStatus.SENT, email.getStatus());
        assertEquals(Email.EmailDirection.INCOMING, email.getDirection());
        assertEquals("Some error", email.getErrorMessage());
        assertEquals(2, email.getRetryCount());
        assertEquals(5, email.getMaxRetries());
        assertEquals("<msg-id@example.com>", email.getMessageId());
        assertEquals(now, email.getCreatedAt());
        assertEquals(now, email.getSentAt());
        assertEquals(now, email.getReceivedAt());
    }
}
