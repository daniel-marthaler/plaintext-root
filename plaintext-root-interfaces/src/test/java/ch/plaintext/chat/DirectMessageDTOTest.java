/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.chat;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class DirectMessageDTOTest {

    @Test
    void settersAndGetters_workCorrectly() {
        DirectMessageDTO dto = new DirectMessageDTO();
        Date now = new Date();

        dto.setId(1L);
        dto.setSenderEmail("sender@test.ch");
        dto.setRecipientEmail("recipient@test.ch");
        dto.setMessageText("Hello!");
        dto.setSentAt(now);
        dto.setIsRead(true);

        assertEquals(1L, dto.getId());
        assertEquals("sender@test.ch", dto.getSenderEmail());
        assertEquals("recipient@test.ch", dto.getRecipientEmail());
        assertEquals("Hello!", dto.getMessageText());
        assertEquals(now, dto.getSentAt());
        assertTrue(dto.getIsRead());
    }

    @Test
    void equalsAndHashCode() {
        DirectMessageDTO dto1 = new DirectMessageDTO();
        dto1.setId(1L);
        dto1.setSenderEmail("a@b.ch");

        DirectMessageDTO dto2 = new DirectMessageDTO();
        dto2.setId(1L);
        dto2.setSenderEmail("a@b.ch");

        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }
}
