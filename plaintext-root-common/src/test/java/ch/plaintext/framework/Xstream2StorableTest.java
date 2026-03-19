/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.framework;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Xstream2StorableTest {

    /**
     * Concrete test implementation with private fields for testing getFields().
     */
    static class TestImpl implements Xstream2Storable {
        private String key;
        private Date lastModifiedDate;
        private String mandat;
        private String lastModifiedBy;
        private String createdBy;
        private String privateTestField;
        public String publicTestField;

        @Override
        public String getKey() { return key; }

        @Override
        public void setKey(String in) { this.key = in; }

        @Override
        public Date getLastModifiedDate() { return lastModifiedDate; }

        @Override
        public void setLastModifiedDate(Date date) { this.lastModifiedDate = date; }

        @Override
        public String getMandat() { return mandat; }

        @Override
        public void setMandat(String mandat) { this.mandat = mandat; }

        @Override
        public String getLastModifiedBy() { return lastModifiedBy; }

        @Override
        public void setLastModifiedBy(String in) { this.lastModifiedBy = in; }

        @Override
        public String getCreatedBy() { return createdBy; }

        @Override
        public void setCreatedBy(String in) { this.createdBy = in; }
    }

    // -------------------------------------------------------------------------
    // Default method: getDruckmodus
    // -------------------------------------------------------------------------

    @Test
    void getDruckmodus_defaultIsFalse() {
        TestImpl impl = new TestImpl();
        assertFalse(impl.getDruckmodus());
    }

    @Test
    void setDruckmodus_defaultDoesNothing() {
        TestImpl impl = new TestImpl();
        impl.setDruckmodus(true);
        // Default implementation is a no-op, so getDruckmodus still returns false
        assertFalse(impl.getDruckmodus());
    }

    // -------------------------------------------------------------------------
    // Default index methods
    // -------------------------------------------------------------------------

    @Test
    void getIndex_defaultReturnsDefault() {
        assertEquals("default", new TestImpl().getIndex());
    }

    @Test
    void getIndex1_defaultReturnsDefault() {
        assertEquals("default", new TestImpl().getIndex1());
    }

    @Test
    void getIndex2_defaultReturnsDefault() {
        assertEquals("default", new TestImpl().getIndex2());
    }

    @Test
    void getIndex3_defaultReturnsDefault() {
        assertEquals("default", new TestImpl().getIndex3());
    }

    @Test
    void getIndex4_defaultReturnsDefault() {
        assertEquals("default", new TestImpl().getIndex4());
    }

    // -------------------------------------------------------------------------
    // Default method: getFields
    // -------------------------------------------------------------------------

    @Test
    void getFields_returnsOnlyPrivateFields() {
        TestImpl impl = new TestImpl();
        List<Field> fields = impl.getFields();
        assertNotNull(fields);
        // Should contain privateTestField but not publicTestField
        List<String> names = fields.stream().map(Field::getName).toList();
        assertTrue(names.contains("privateTestField"));
        assertFalse(names.contains("publicTestField"));
    }

    @Test
    void getFields_allReturnedFieldsArePrivate() {
        TestImpl impl = new TestImpl();
        List<Field> fields = impl.getFields();
        for (Field f : fields) {
            assertTrue(java.lang.reflect.Modifier.isPrivate(f.getModifiers()),
                    "Expected private but got: " + f.getName());
        }
    }
}
