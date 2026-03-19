/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.email.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EmailAttachmentTest {

    @Test
    void defaultValues() {
        EmailAttachment attachment = new EmailAttachment();

        assertNull(attachment.getId());
        assertNull(attachment.getEmail());
        assertNull(attachment.getFilename());
        assertNull(attachment.getContentType());
        assertNull(attachment.getSizeBytes());
        assertNull(attachment.getData());
        assertNull(attachment.getCreatedAt());
    }

    @Test
    void prePersistSetsCreatedAt() {
        EmailAttachment attachment = new EmailAttachment();
        assertNull(attachment.getCreatedAt());

        attachment.prePersist();

        assertNotNull(attachment.getCreatedAt());
    }

    @Test
    void prePersistDoesNotOverwriteExistingCreatedAt() {
        EmailAttachment attachment = new EmailAttachment();
        LocalDateTime fixed = LocalDateTime.of(2024, 3, 20, 14, 0);
        attachment.setCreatedAt(fixed);

        attachment.prePersist();

        assertEquals(fixed, attachment.getCreatedAt());
    }

    @Test
    void settersAndGetters() {
        EmailAttachment attachment = new EmailAttachment();
        byte[] data = {0x01, 0x02, 0x03};

        attachment.setId(10L);
        attachment.setFilename("report.pdf");
        attachment.setContentType("application/pdf");
        attachment.setSizeBytes(1024L);
        attachment.setData(data);

        assertEquals(10L, attachment.getId());
        assertEquals("report.pdf", attachment.getFilename());
        assertEquals("application/pdf", attachment.getContentType());
        assertEquals(1024L, attachment.getSizeBytes());
        assertArrayEquals(data, attachment.getData());
    }

    @Test
    void toStringExcludesEmailAndData() {
        EmailAttachment attachment = new EmailAttachment();
        attachment.setFilename("test.txt");
        attachment.setData(new byte[]{1, 2, 3});
        Email email = new Email();
        email.setId(1L);
        attachment.setEmail(email);

        String str = attachment.toString();

        // @ToString(exclude = {"email", "data"}) - should not contain references to those fields
        assertNotNull(str);
        assertTrue(str.contains("test.txt"));
    }
}
