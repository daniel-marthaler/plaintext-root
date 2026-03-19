/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.chat;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class ChatMessageDTOTest {

    @Test
    void settersAndGetters_workCorrectly() {
        ChatMessageDTO dto = new ChatMessageDTO();
        Date now = new Date();

        dto.setId(10L);
        dto.setChatId(1L);
        dto.setSenderEmail("sender@test.ch");
        dto.setMessageText("Hello World");
        dto.setSentAt(now);
        dto.setIsRead(false);

        assertEquals(10L, dto.getId());
        assertEquals(1L, dto.getChatId());
        assertEquals("sender@test.ch", dto.getSenderEmail());
        assertEquals("Hello World", dto.getMessageText());
        assertEquals(now, dto.getSentAt());
        assertFalse(dto.getIsRead());
    }

    @Test
    void equalsAndHashCode() {
        ChatMessageDTO dto1 = new ChatMessageDTO();
        dto1.setId(1L);
        dto1.setMessageText("Test");

        ChatMessageDTO dto2 = new ChatMessageDTO();
        dto2.setId(1L);
        dto2.setMessageText("Test");

        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }
}
