/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.framework;

import com.thoughtworks.xstream.XStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class XstreamStore2Test {

    @Mock
    private TextRepository2 repo;

    @Mock
    private PlaintextSecWrapper sec;

    @InjectMocks
    private XstreamStore2<TestStorable> store;

    private XStream xstream;

    @BeforeEach
    void setUp() throws Exception {
        xstream = new XStream();
        xstream.allowTypesByWildcard(new String[]{"ch.plaintext.**"});

        // Replace the XStream instance in the store via reflection
        java.lang.reflect.Field xstreamField = XstreamStore2.class.getDeclaredField("xstream");
        xstreamField.setAccessible(true);
        xstreamField.set(store, xstream);
    }

    // -------------------------------------------------------------------------
    // readByKeyAndMandant
    // -------------------------------------------------------------------------

    @Test
    void readByKeyAndMandant_found_returnsObject() {
        TestStorable storable = createTestStorable("key1");
        Text2 text = createText2(storable, "mandatA");

        when(repo.findByKeyAndMandat("key1", "mandatA")).thenReturn(text);

        TestStorable result = store.readByKeyAndMandant("key1", "mandatA");
        assertNotNull(result);
        assertEquals("key1", result.getKey());
    }

    @Test
    void readByKeyAndMandant_notFound_returnsNull() {
        when(repo.findByKeyAndMandat("missing", "mandatA")).thenReturn(null);
        assertNull(store.readByKeyAndMandant("missing", "mandatA"));
    }

    @Test
    void readByKeyAndMandant_setsAuditFields() {
        TestStorable storable = createTestStorable("key1");
        Text2 text = createText2(storable, "mandatA");
        text.setLastModifiedBy("editor");
        Date modDate = new Date();
        text.setLastModifiedDate(modDate);

        when(repo.findByKeyAndMandat("key1", "mandatA")).thenReturn(text);

        TestStorable result = store.readByKeyAndMandant("key1", "mandatA");
        assertEquals("editor", result.getLastModifiedBy());
        assertEquals(modDate, result.getLastModifiedDate());
    }

    // -------------------------------------------------------------------------
    // readByIndex
    // -------------------------------------------------------------------------

    @Test
    void readByIndex_found_returnsList() {
        TestStorable s1 = createTestStorable("k1");
        TestStorable s2 = createTestStorable("k2");

        Text2 t1 = createText2(s1, "m");
        Text2 t2 = createText2(s2, "m");

        when(repo.findByIndexAndMandat("idx", "m")).thenReturn(Arrays.asList(t1, t2));

        List<TestStorable> result = store.readByIndex("idx", "m");
        assertEquals(2, result.size());
    }

    @Test
    void readByIndex_nullResult_returnsEmptyList() {
        when(repo.findByIndexAndMandat("idx", "m")).thenReturn(null);
        List<TestStorable> result = store.readByIndex("idx", "m");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // -------------------------------------------------------------------------
    // readByType
    // -------------------------------------------------------------------------

    @Test
    void readByType_found_returnsList() {
        TestStorable s1 = createTestStorable("k1");
        Text2 t1 = createText2(s1, "m");

        when(repo.findByType("TestStorable")).thenReturn(List.of(t1));

        List<TestStorable> result = store.readByType(TestStorable.class);
        assertEquals(1, result.size());
    }

    @Test
    void readByType_nullResult_returnsEmptyList() {
        when(repo.findByType("TestStorable")).thenReturn(null);
        List<TestStorable> result = store.readByType(TestStorable.class);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // -------------------------------------------------------------------------
    // readByTypeAndMandat
    // -------------------------------------------------------------------------

    @Test
    void readByTypeAndMandat_found_returnsList() {
        TestStorable s1 = createTestStorable("k1");
        Text2 t1 = createText2(s1, "mandatA");

        when(repo.findByTypeAndMandat("TestStorable", "mandatA")).thenReturn(List.of(t1));

        List<TestStorable> result = store.readByTypeAndMandat(TestStorable.class, "mandatA");
        assertEquals(1, result.size());
    }

    @Test
    void readByTypeAndMandat_nullResult_returnsEmptyList() {
        when(repo.findByTypeAndMandat("TestStorable", "mandatA")).thenReturn(null);
        List<TestStorable> result = store.readByTypeAndMandat(TestStorable.class, "mandatA");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // -------------------------------------------------------------------------
    // save
    // -------------------------------------------------------------------------

    @Test
    void save_newObject_createsAndSaves() {
        TestStorable storable = createTestStorable("key-new");

        when(repo.findByKeyAndMandat("key-new", "mandatA")).thenReturn(null);

        Text2 savedText = new Text2();
        savedText.setCreatedBy("creator");
        when(repo.save(any(Text2.class))).thenReturn(savedText);

        // After save, readByKeyAndMandant is called internally
        Text2 readBack = createText2(storable, "mandatA");
        // First call returns null (new), subsequent calls for readback
        when(repo.findByKeyAndMandat("key-new", "mandatA"))
                .thenReturn(null)
                .thenReturn(readBack);

        TestStorable result = store.save(storable, "mandatA");
        assertNotNull(result);
        verify(repo, atLeast(1)).save(any(Text2.class));
    }

    @Test
    void save_existingObject_updates() {
        TestStorable storable = createTestStorable("key-existing");
        Text2 existing = createText2(storable, "mandatA");
        existing.setId(10L);

        when(repo.findByKeyAndMandat("key-existing", "mandatA")).thenReturn(existing);
        when(repo.save(any(Text2.class))).thenReturn(existing);

        TestStorable result = store.save(storable, "mandatA");
        assertNotNull(result);
        verify(repo).save(any(Text2.class));
    }

    // -------------------------------------------------------------------------
    // delete
    // -------------------------------------------------------------------------

    @Test
    void delete_removesById() {
        TestStorable storable = createTestStorable("key-del");
        Text2 text = new Text2();
        text.setId(99L);
        text.setKey("key-del");

        when(repo.findByKey("key-del")).thenReturn(text);

        store.delete(storable);
        verify(repo).deleteById(99L);
    }

    // -------------------------------------------------------------------------
    // Helper: TestStorable implementation
    // -------------------------------------------------------------------------

    /**
     * Simple test implementation of Xstream2Storable for testing XstreamStore2.
     */
    public static class TestStorable implements Xstream2Storable {
        private String key;
        private Date lastModifiedDate;
        private String mandat;
        private String lastModifiedBy;
        private String createdBy;

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

    private TestStorable createTestStorable(String key) {
        TestStorable s = new TestStorable();
        s.setKey(key);
        s.setMandat("default");
        return s;
    }

    private Text2 createText2(TestStorable storable, String mandat) {
        Text2 text = new Text2();
        text.setKey(storable.getKey());
        text.setValue(xstream.toXML(storable));
        text.setMandat(mandat);
        text.setType("TestStorable");
        return text;
    }
}
