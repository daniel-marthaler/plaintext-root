/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.framework;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Text2Test {

    private Text2 text;

    @BeforeEach
    void setUp() {
        text = new Text2();
    }

    // -------------------------------------------------------------------------
    // Default values
    // -------------------------------------------------------------------------

    @Test
    void defaults_areNull() {
        assertNull(text.getValue());
        assertNull(text.getKey());
        assertNull(text.getIndex());
        assertNull(text.getIndex1());
        assertNull(text.getIndex2());
        assertNull(text.getIndex3());
        assertNull(text.getIndex4());
        assertNull(text.getType());
    }

    // -------------------------------------------------------------------------
    // Setters and getters
    // -------------------------------------------------------------------------

    @Test
    void setAndGetValue() {
        text.setValue("test-value");
        assertEquals("test-value", text.getValue());
    }

    @Test
    void setAndGetKey() {
        text.setKey("test-key");
        assertEquals("test-key", text.getKey());
    }

    @Test
    void setAndGetIndex() {
        text.setIndex("idx");
        assertEquals("idx", text.getIndex());
    }

    @Test
    void setAndGetIndex1() {
        text.setIndex1("idx1");
        assertEquals("idx1", text.getIndex1());
    }

    @Test
    void setAndGetIndex2() {
        text.setIndex2("idx2");
        assertEquals("idx2", text.getIndex2());
    }

    @Test
    void setAndGetIndex3() {
        text.setIndex3("idx3");
        assertEquals("idx3", text.getIndex3());
    }

    @Test
    void setAndGetIndex4() {
        text.setIndex4("idx4");
        assertEquals("idx4", text.getIndex4());
    }

    @Test
    void setAndGetType() {
        text.setType("TestType");
        assertEquals("TestType", text.getType());
    }

    // -------------------------------------------------------------------------
    // Inherits SuperModel behavior
    // -------------------------------------------------------------------------

    @Test
    void inheritsId() {
        text.setId(99L);
        assertEquals(99L, text.getId());
    }

    @Test
    void inheritsMandat() {
        text.setMandat("testMandat");
        assertEquals("testMandat", text.getMandat());
    }

    @Test
    void getKey_returnsText2Key_notSuperModelId() {
        // Text2 has its own 'key' field. Lombok @Data generates getKey() for it,
        // which shadows SuperModel.getKey(). So setId does NOT affect getKey().
        text.setId(42L);
        // key field is still null because setId does not set it
        assertNull(text.getKey());
    }

    @Test
    void setKey_setsText2KeyField() {
        // Text2's Lombok-generated setKey sets the Text2.key field
        text.setKey("custom-key");
        assertEquals("custom-key", text.getKey());
    }

    // -------------------------------------------------------------------------
    // equals / hashCode (Lombok @Data + @EqualsAndHashCode(callSuper = true))
    // -------------------------------------------------------------------------

    @Test
    void equals_sameContent_returnsTrue() {
        Text2 t1 = new Text2();
        t1.setId(1L);
        t1.setValue("val");
        t1.setKey("key");

        Text2 t2 = new Text2();
        t2.setId(1L);
        t2.setValue("val");
        t2.setKey("key");

        assertEquals(t1, t2);
    }

    @Test
    void equals_differentValue_returnsFalse() {
        Text2 t1 = new Text2();
        t1.setId(1L);
        t1.setValue("val1");

        Text2 t2 = new Text2();
        t2.setId(1L);
        t2.setValue("val2");

        assertNotEquals(t1, t2);
    }
}
