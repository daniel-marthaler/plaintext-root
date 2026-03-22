/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.filelist.storage;

import ch.plaintext.settings.ISettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VfsStorageBackendTest {

    @Mock
    private ISettingsService settingsService;

    private VfsStorageBackend backend;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        backend = new VfsStorageBackend(settingsService);
    }

    // --- getBackendType ---

    @Test
    void getBackendTypeReturnsVFS() {
        assertThat(backend.getBackendType()).isEqualTo("VFS");
    }

    // --- store ---

    @Test
    void storeCreatesFileInMandatDirectory() throws Exception {
        when(settingsService.getString("filelist.vfs.basepath", "mandatA")).thenReturn(tempDir.toString());

        InputStream is = new ByteArrayInputStream("hello world".getBytes());
        String path = backend.store(is, "test.txt", "mandatA");

        assertThat(path).isNotNull();
        assertThat(new File(path)).exists();
        assertThat(Files.readString(Path.of(path))).isEqualTo("hello world");
    }

    @Test
    void storeSanitizesFilename() throws Exception {
        when(settingsService.getString("filelist.vfs.basepath", "mandatA")).thenReturn(tempDir.toString());

        InputStream is = new ByteArrayInputStream("content".getBytes());
        String path = backend.store(is, "my file (1).txt", "mandatA");

        assertThat(path).isNotNull();
        // Spaces and parens should be replaced with underscores
        assertThat(new File(path).getName()).doesNotContain(" ", "(", ")");
    }

    @Test
    void storeHandlesDuplicateFilenames() throws Exception {
        when(settingsService.getString("filelist.vfs.basepath", "mandatA")).thenReturn(tempDir.toString());

        // Create first file
        InputStream is1 = new ByteArrayInputStream("first".getBytes());
        String path1 = backend.store(is1, "test.txt", "mandatA");

        // Create second file with same name
        InputStream is2 = new ByteArrayInputStream("second".getBytes());
        String path2 = backend.store(is2, "test.txt", "mandatA");

        assertThat(path1).isNotEqualTo(path2);
        assertThat(new File(path1)).exists();
        assertThat(new File(path2)).exists();
    }

    @Test
    void storeUsesDefaultPathWhenSettingIsNull() throws Exception {
        when(settingsService.getString("filelist.vfs.basepath", "mandatA")).thenReturn(null);

        InputStream is = new ByteArrayInputStream("content".getBytes());
        // This will use the default path, which may or may not work depending on filesystem permissions.
        // For now, we just verify it doesn't throw an NPE on null setting
        // The actual file creation would go to the default path
        try {
            backend.store(is, "test.txt", "mandatA");
        } catch (Exception e) {
            // It's OK if it fails due to filesystem permissions -
            // what matters is that the null case is handled
            assertThat(e).isNotInstanceOf(NullPointerException.class);
        }
    }

    // --- retrieve ---

    @Test
    void retrieveReturnsInputStreamForExistingFile() throws Exception {
        // Create a temp file
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "content");

        InputStream is = backend.retrieve(testFile.toString());

        assertThat(is).isNotNull();
        assertThat(new String(is.readAllBytes())).isEqualTo("content");
        is.close();
    }

    @Test
    void retrieveThrowsForNonExistentFile() {
        assertThatThrownBy(() -> backend.retrieve("/nonexistent/file.txt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File not found");
    }

    // --- delete ---

    @Test
    void deleteRemovesExistingFile() throws Exception {
        Path testFile = tempDir.resolve("delete_me.txt");
        Files.writeString(testFile, "to be deleted");

        backend.delete(testFile.toString());

        assertThat(testFile.toFile()).doesNotExist();
    }

    @Test
    void deleteDoesNotThrowForNonExistentFile() throws Exception {
        backend.delete(tempDir.resolve("nonexistent.txt").toString());
        // Should not throw
    }

    // --- exists ---

    @Test
    void existsReturnsTrueForExistingFile() throws Exception {
        Path testFile = tempDir.resolve("exists.txt");
        Files.writeString(testFile, "content");

        assertThat(backend.exists(testFile.toString())).isTrue();
    }

    @Test
    void existsReturnsFalseForNonExistentFile() {
        assertThat(backend.exists("/nonexistent/file.txt")).isFalse();
    }
}
