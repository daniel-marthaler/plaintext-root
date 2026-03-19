/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.chat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChatMessagesPageDTOTest {

    @Test
    void settersAndGetters_workCorrectly() {
        ChatMessagesPageDTO dto = new ChatMessagesPageDTO();

        ChatMessageDTO msg = new ChatMessageDTO();
        msg.setId(1L);

        dto.setMessages(List.of(msg));
        dto.setCurrentPage(0);
        dto.setPageSize(20);
        dto.setTotalElements(100);
        dto.setTotalPages(5);
        dto.setHasNext(true);
        dto.setHasPrevious(false);

        assertEquals(1, dto.getMessages().size());
        assertEquals(0, dto.getCurrentPage());
        assertEquals(20, dto.getPageSize());
        assertEquals(100, dto.getTotalElements());
        assertEquals(5, dto.getTotalPages());
        assertTrue(dto.isHasNext());
        assertFalse(dto.isHasPrevious());
    }

    @Test
    void equalsAndHashCode() {
        ChatMessagesPageDTO dto1 = new ChatMessagesPageDTO();
        dto1.setCurrentPage(0);
        dto1.setTotalPages(5);

        ChatMessagesPageDTO dto2 = new ChatMessagesPageDTO();
        dto2.setCurrentPage(0);
        dto2.setTotalPages(5);

        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }
}
