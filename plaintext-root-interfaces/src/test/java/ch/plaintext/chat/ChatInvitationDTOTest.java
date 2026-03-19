/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.chat;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class ChatInvitationDTOTest {

    @Test
    void settersAndGetters_workCorrectly() {
        ChatInvitationDTO dto = new ChatInvitationDTO();
        Date now = new Date();

        dto.setId(1L);
        dto.setChatId(10L);
        dto.setChatName("Dev Chat");
        dto.setInviterEmail("admin@test.ch");
        dto.setInviteeEmail("user@test.ch");
        dto.setInvitedAt(now);
        dto.setStatus("PENDING");
        dto.setRespondedAt(null);

        assertEquals(1L, dto.getId());
        assertEquals(10L, dto.getChatId());
        assertEquals("Dev Chat", dto.getChatName());
        assertEquals("admin@test.ch", dto.getInviterEmail());
        assertEquals("user@test.ch", dto.getInviteeEmail());
        assertEquals(now, dto.getInvitedAt());
        assertEquals("PENDING", dto.getStatus());
        assertNull(dto.getRespondedAt());
    }
}
