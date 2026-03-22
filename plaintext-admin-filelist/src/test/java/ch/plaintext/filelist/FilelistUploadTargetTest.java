/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.filelist;

import ch.plaintext.filelist.entity.FileMetadata;
import ch.plaintext.filelist.repository.FileMetadataRepository;
import ch.plaintext.filelist.storage.StorageBackend;
import ch.plaintext.upload.IUploadTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FilelistUploadTargetTest {

    @Mock
    private FileMetadataRepository repository;

    @Mock
    private StorageBackend vfsBackend;

    @Mock
    private MultipartFile multipartFile;

    private FilelistUploadTarget target;

    @BeforeEach
    void setUp() {
        target = new FilelistUploadTarget(repository, List.of(vfsBackend));
    }

    // --- getServiceName ---

    @Test
    void getServiceNameReturnsFilelist() {
        assertThat(target.getServiceName()).isEqualTo("filelist");
    }

    // --- getDescription ---

    @Test
    void getDescriptionIsNotEmpty() {
        assertThat(target.getDescription()).isNotEmpty();
    }

    // --- getAcceptedFileTypes ---

    @Test
    void getAcceptedFileTypesAcceptsAll() {
        assertThat(target.getAcceptedFileTypes()).containsExactly("*");
    }

    // --- handleUpload ---

    @Test
    void handleUploadSuccessfully() throws Exception {
        when(vfsBackend.getBackendType()).thenReturn("VFS");
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream("data".getBytes()));
        when(multipartFile.getOriginalFilename()).thenReturn("test.pdf");
        when(multipartFile.getContentType()).thenReturn("application/pdf");
        when(multipartFile.getSize()).thenReturn(1024L);
        when(vfsBackend.store(any(), eq("test.pdf"), eq("mandatA"))).thenReturn("/path/test.pdf");

        FileMetadata saved = new FileMetadata();
        saved.setId(42L);
        when(repository.save(any(FileMetadata.class))).thenReturn(saved);

        IUploadTarget.UploadResult result = target.handleUpload(multipartFile, "mandatA",
                Map.of("category", "docs", "backend", "VFS"));

        assertThat(result.success()).isTrue();
        assertThat(result.details()).containsEntry("fileId", 42L);
        assertThat(result.details()).containsEntry("category", "docs");

        ArgumentCaptor<FileMetadata> captor = ArgumentCaptor.forClass(FileMetadata.class);
        verify(repository).save(captor.capture());
        FileMetadata metadata = captor.getValue();
        assertThat(metadata.getMandat()).isEqualTo("mandatA");
        assertThat(metadata.getFilename()).isEqualTo("test.pdf");
        assertThat(metadata.getContentType()).isEqualTo("application/pdf");
        assertThat(metadata.getUploadedBy()).isEqualTo("api-upload");
    }

    @Test
    void handleUploadDefaultsCategoryToUpload() throws Exception {
        when(vfsBackend.getBackendType()).thenReturn("VFS");
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream("data".getBytes()));
        when(multipartFile.getOriginalFilename()).thenReturn("test.txt");
        when(multipartFile.getContentType()).thenReturn("text/plain");
        when(multipartFile.getSize()).thenReturn(100L);
        when(vfsBackend.store(any(), any(), any())).thenReturn("/path/test.txt");

        FileMetadata saved = new FileMetadata();
        saved.setId(1L);
        when(repository.save(any())).thenReturn(saved);

        IUploadTarget.UploadResult result = target.handleUpload(multipartFile, "mandatA", Map.of());

        assertThat(result.success()).isTrue();

        ArgumentCaptor<FileMetadata> captor = ArgumentCaptor.forClass(FileMetadata.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCategory()).isEqualTo("upload");
    }

    @Test
    void handleUploadReturnsErrorForUnknownBackend() throws Exception {
        when(vfsBackend.getBackendType()).thenReturn("VFS");

        IUploadTarget.UploadResult result = target.handleUpload(multipartFile, "mandatA",
                Map.of("backend", "S3"));

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("Failed to store file");
    }

    @Test
    void handleUploadReturnsErrorOnStorageException() throws Exception {
        when(vfsBackend.getBackendType()).thenReturn("VFS");
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream("data".getBytes()));
        when(multipartFile.getOriginalFilename()).thenReturn("test.txt");
        when(vfsBackend.store(any(), any(), any())).thenThrow(new RuntimeException("Disk full"));

        IUploadTarget.UploadResult result = target.handleUpload(multipartFile, "mandatA",
                Map.of("backend", "VFS"));

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("Disk full");
    }
}
