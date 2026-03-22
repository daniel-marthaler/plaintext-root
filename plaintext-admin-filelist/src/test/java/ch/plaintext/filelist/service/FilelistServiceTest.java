/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.filelist.service;

import ch.plaintext.PlaintextSecurity;
import ch.plaintext.filelist.entity.FileMetadata;
import ch.plaintext.filelist.repository.FileMetadataRepository;
import ch.plaintext.filelist.storage.StorageBackend;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FilelistServiceTest {

    @Mock
    private FileMetadataRepository repository;

    @Mock
    private PlaintextSecurity security;

    @Mock
    private StorageBackend vfsBackend;

    private FilelistService service;

    @BeforeEach
    void setUp() {
        service = new FilelistService(repository, security, List.of(vfsBackend));
    }

    // --- getAllFiles ---

    @Test
    void getAllFilesReturnsList() {
        FileMetadata f = new FileMetadata();
        f.setFilename("test.pdf");
        when(repository.findByMandatOrderByCreatedDateDesc("mandatA")).thenReturn(List.of(f));

        List<FileMetadata> result = service.getAllFiles("mandatA");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFilename()).isEqualTo("test.pdf");
    }

    // --- getAllFilesForCurrentUser ---

    @Test
    void getAllFilesForCurrentUserUsesSecurityMandat() {
        when(security.getMandat()).thenReturn("mandatA");
        when(repository.findByMandatOrderByCreatedDateDesc("mandatA")).thenReturn(List.of());

        service.getAllFilesForCurrentUser();

        verify(repository).findByMandatOrderByCreatedDateDesc("mandatA");
    }

    @Test
    void getAllFilesForCurrentUserThrowsForNoAuth() {
        when(security.getMandat()).thenReturn("NO_AUTH");

        assertThatThrownBy(() -> service.getAllFilesForCurrentUser())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("invalid mandat");
    }

    @Test
    void getAllFilesForCurrentUserThrowsForNoUser() {
        when(security.getMandat()).thenReturn("NO_USER");

        assertThatThrownBy(() -> service.getAllFilesForCurrentUser())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getAllFilesForCurrentUserThrowsForError() {
        when(security.getMandat()).thenReturn("ERROR");

        assertThatThrownBy(() -> service.getAllFilesForCurrentUser())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getAllFilesForCurrentUserThrowsForNullMandat() {
        when(security.getMandat()).thenReturn(null);

        assertThatThrownBy(() -> service.getAllFilesForCurrentUser())
                .isInstanceOf(IllegalStateException.class);
    }

    // --- getByCategory ---

    @Test
    void getByCategoryDelegatesToRepository() {
        when(repository.findByMandatAndCategory("mandatA", "docs")).thenReturn(List.of(new FileMetadata()));

        assertThat(service.getByCategory("mandatA", "docs")).hasSize(1);
    }

    // --- getAllCategories ---

    @Test
    void getAllCategoriesDelegatesToRepository() {
        when(repository.findDistinctCategoriesByMandat("mandatA")).thenReturn(List.of("docs", "images"));

        assertThat(service.getAllCategories("mandatA")).containsExactly("docs", "images");
    }

    // --- getTotalStorageUsed ---

    @Test
    void getTotalStorageUsedReturnsValueFromRepository() {
        when(repository.calculateTotalStorageUsed("mandatA")).thenReturn(1024L);

        assertThat(service.getTotalStorageUsed("mandatA")).isEqualTo(1024L);
    }

    @Test
    void getTotalStorageUsedReturnsZeroForNull() {
        when(repository.calculateTotalStorageUsed("mandatA")).thenReturn(null);

        assertThat(service.getTotalStorageUsed("mandatA")).isZero();
    }

    // --- findById ---

    @Test
    void findByIdReturnsFile() {
        FileMetadata f = new FileMetadata();
        f.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(f));

        assertThat(service.findById(1L)).isPresent();
    }

    @Test
    void findByIdReturnsEmpty() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThat(service.findById(999L)).isEmpty();
    }

    // --- uploadFile ---

    @Test
    void uploadFileStoresFileAndSavesMetadata() throws Exception {
        when(security.getMandat()).thenReturn("mandatA");
        when(security.getUser()).thenReturn("testuser");
        when(vfsBackend.getBackendType()).thenReturn("VFS");
        when(vfsBackend.store(any(), eq("test.pdf"), eq("mandatA"))).thenReturn("/path/to/test.pdf");
        when(repository.save(any(FileMetadata.class))).thenAnswer(inv -> inv.getArgument(0));

        InputStream is = new ByteArrayInputStream("content".getBytes());
        FileMetadata result = service.uploadFile(is, "test.pdf", "application/pdf", 7L, "docs", "VFS");

        assertThat(result.getFilename()).isEqualTo("test.pdf");
        assertThat(result.getMandat()).isEqualTo("mandatA");
        assertThat(result.getFilePath()).isEqualTo("/path/to/test.pdf");
        assertThat(result.getStorageBackend()).isEqualTo("VFS");
        assertThat(result.getContentType()).isEqualTo("application/pdf");
        assertThat(result.getFileSize()).isEqualTo(7L);
        assertThat(result.getCategory()).isEqualTo("docs");
        assertThat(result.getUploadedBy()).isEqualTo("testuser");
    }

    @Test
    void uploadFileThrowsForUnknownBackend() {
        when(security.getMandat()).thenReturn("mandatA");
        when(vfsBackend.getBackendType()).thenReturn("VFS");

        InputStream is = new ByteArrayInputStream("content".getBytes());

        assertThatThrownBy(() -> service.uploadFile(is, "test.pdf", "application/pdf", 7L, "docs", "S3"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown storage backend");
    }

    // --- downloadFile ---

    @Test
    void downloadFileThrowsWhenFileNotFound() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.downloadFile(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File not found");
    }

    @Test
    void downloadFileReturnsInputStreamAndUpdatesStats() throws Exception {
        FileMetadata metadata = new FileMetadata();
        metadata.setId(1L);
        metadata.setFilePath("/path/test.pdf");
        metadata.setStorageBackend("VFS");
        metadata.setAccessCount(2);
        when(repository.findById(1L)).thenReturn(Optional.of(metadata));
        when(vfsBackend.getBackendType()).thenReturn("VFS");
        InputStream mockStream = new ByteArrayInputStream("data".getBytes());
        when(vfsBackend.retrieve("/path/test.pdf")).thenReturn(mockStream);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InputStream result = service.downloadFile(1L);

        assertThat(result).isNotNull();
        assertThat(metadata.getAccessCount()).isEqualTo(3);
        verify(repository).save(metadata);
    }

    // --- deleteFile ---

    @Test
    void deleteFileThrowsWhenFileNotFound() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteFile(999L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteFileRemovesFromBackendAndRepository() throws Exception {
        FileMetadata metadata = new FileMetadata();
        metadata.setId(1L);
        metadata.setFilePath("/path/test.pdf");
        metadata.setStorageBackend("VFS");
        metadata.setFilename("test.pdf");
        when(repository.findById(1L)).thenReturn(Optional.of(metadata));
        when(vfsBackend.getBackendType()).thenReturn("VFS");

        service.deleteFile(1L);

        verify(vfsBackend).delete("/path/test.pdf");
        verify(repository).deleteById(1L);
    }

    // --- updateMetadata ---

    @Test
    void updateMetadataSavesToRepository() {
        FileMetadata metadata = new FileMetadata();
        metadata.setId(1L);
        when(repository.save(metadata)).thenReturn(metadata);

        FileMetadata result = service.updateMetadata(metadata);

        assertThat(result).isSameAs(metadata);
        verify(repository).save(metadata);
    }

    // --- getAvailableStorageBackends ---

    @Test
    void getAvailableStorageBackendsReturnsList() {
        when(vfsBackend.getBackendType()).thenReturn("VFS");

        List<String> result = service.getAvailableStorageBackends();

        assertThat(result).containsExactly("VFS");
    }
}
