/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.chat;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.Arrays;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class ChatDTOTest {

    @Test
    void defaultMemberEmails_isEmptyList() {
        ChatDTO dto = new ChatDTO();
        assertNotNull(dto.getMemberEmails());
        assertTrue(dto.getMemberEmails().isEmpty());
    }

    @Test
    void settersAndGetters_workCorrectly() {
        ChatDTO dto = new ChatDTO();
        Date now = new Date();

        dto.setId(1L);
        dto.setName("General");
        dto.setOwner("owner@test.ch");
        dto.setMandat("test-mandat");
        dto.setCreatedAt(now);
        dto.setMemberEmails(Arrays.asList("a@test.ch", "b@test.ch"));

        assertEquals(1L, dto.getId());
        assertEquals("General", dto.getName());
        assertEquals("owner@test.ch", dto.getOwner());
        assertEquals("test-mandat", dto.getMandat());
        assertEquals(now, dto.getCreatedAt());
        assertEquals(2, dto.getMemberEmails().size());
    }

    @Test
    void isSerializable() throws Exception {
        ChatDTO dto = new ChatDTO();
        dto.setId(1L);
        dto.setName("Test");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(dto);
        oos.close();

        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
        ChatDTO deserialized = (ChatDTO) ois.readObject();

        assertEquals(dto.getId(), deserialized.getId());
        assertEquals(dto.getName(), deserialized.getName());
    }

    @Test
    void equalsAndHashCode() {
        ChatDTO dto1 = new ChatDTO();
        dto1.setId(1L);
        dto1.setName("Test");

        ChatDTO dto2 = new ChatDTO();
        dto2.setId(1L);
        dto2.setName("Test");

        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    void notEquals_differentId() {
        ChatDTO dto1 = new ChatDTO();
        dto1.setId(1L);

        ChatDTO dto2 = new ChatDTO();
        dto2.setId(2L);

        assertNotEquals(dto1, dto2);
    }
}
