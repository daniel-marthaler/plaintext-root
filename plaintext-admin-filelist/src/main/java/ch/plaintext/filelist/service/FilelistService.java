/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.filelist.service;

import ch.plaintext.PlaintextSecurity;
import ch.plaintext.filelist.entity.FileMetadata;
import ch.plaintext.filelist.repository.FileMetadataRepository;
import ch.plaintext.filelist.storage.StorageBackend;
import jakarta.inject.Named;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

@Service
@Named("filelistService")
@Slf4j
public class FilelistService {

    private final FileMetadataRepository repository;
    private final PlaintextSecurity security;
    private final List<StorageBackend> storageBackends;

    public FilelistService(FileMetadataRepository repository, PlaintextSecurity security,
                          List<StorageBackend> storageBackends) {
        this.repository = repository;
        this.security = security;
        this.storageBackends = storageBackends;
    }

    public List<FileMetadata> getAllFiles(String mandat) {
        return repository.findByMandatOrderByCreatedDateDesc(mandat);
    }

    public List<FileMetadata> getAllFilesForCurrentUser() {
        return getAllFiles(getCurrentMandat());
    }

    public List<FileMetadata> getByCategory(String mandat, String category) {
        return repository.findByMandatAndCategory(mandat, category);
    }

    public List<String> getAllCategories(String mandat) {
        return repository.findDistinctCategoriesByMandat(mandat);
    }

    public Long getTotalStorageUsed(String mandat) {
        Long total = repository.calculateTotalStorageUsed(mandat);
        return total != null ? total : 0L;
    }

    public Optional<FileMetadata> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public FileMetadata uploadFile(InputStream inputStream, String filename, String contentType,
                                   Long fileSize, String category, String storageBackendType) throws Exception {
        String mandat = getCurrentMandat();

        StorageBackend backend = getStorageBackend(storageBackendType);
        String filePath = backend.store(inputStream, filename, mandat);

        FileMetadata metadata = new FileMetadata();
        metadata.setMandat(mandat);
        metadata.setFilename(filename);
        metadata.setOriginalFilename(filename);
        metadata.setFilePath(filePath);
        metadata.setStorageBackend(backend.getBackendType());
        metadata.setContentType(contentType);
        metadata.setFileSize(fileSize);
        metadata.setCategory(category);
        metadata.setUploadedBy(security.getUser());

        return repository.save(metadata);
    }

    public InputStream downloadFile(Long fileId) throws Exception {
        Optional<FileMetadata> metadataOpt = repository.findById(fileId);
        if (metadataOpt.isEmpty()) {
            throw new IllegalArgumentException("File not found: " + fileId);
        }

        FileMetadata metadata = metadataOpt.get();
        StorageBackend backend = getStorageBackend(metadata.getStorageBackend());

        // Update access statistics
        metadata.incrementAccessCount();
        repository.save(metadata);

        return backend.retrieve(metadata.getFilePath());
    }

    @Transactional
    public void deleteFile(Long fileId) throws Exception {
        Optional<FileMetadata> metadataOpt = repository.findById(fileId);
        if (metadataOpt.isEmpty()) {
            throw new IllegalArgumentException("File not found: " + fileId);
        }

        FileMetadata metadata = metadataOpt.get();
        StorageBackend backend = getStorageBackend(metadata.getStorageBackend());

        backend.delete(metadata.getFilePath());
        repository.deleteById(fileId);
        log.info("Deleted file: id={}, filename={}", fileId, metadata.getFilename());
    }

    @Transactional
    public FileMetadata updateMetadata(FileMetadata metadata) {
        return repository.save(metadata);
    }

    private StorageBackend getStorageBackend(String backendType) {
        return storageBackends.stream()
                .filter(b -> b.getBackendType().equalsIgnoreCase(backendType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown storage backend: " + backendType));
    }

    public List<String> getAvailableStorageBackends() {
        return storageBackends.stream()
                .map(StorageBackend::getBackendType)
                .toList();
    }

    private String getCurrentMandat() {
        String mandat = security.getMandat();
        if (mandat == null || "NO_AUTH".equals(mandat) || "NO_USER".equals(mandat) || "ERROR".equals(mandat)) {
            throw new IllegalStateException("Cannot access filelist - invalid mandat: " + mandat);
        }
        return mandat;
    }
}
