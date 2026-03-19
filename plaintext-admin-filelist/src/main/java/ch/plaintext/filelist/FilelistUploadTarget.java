/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.filelist;

import ch.plaintext.upload.IUploadTarget;
import ch.plaintext.filelist.entity.FileMetadata;
import ch.plaintext.filelist.repository.FileMetadataRepository;
import ch.plaintext.filelist.storage.StorageBackend;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class FilelistUploadTarget implements IUploadTarget {

    private final FileMetadataRepository repository;
    private final List<StorageBackend> storageBackends;

    @Override
    public String getServiceName() {
        return "filelist";
    }

    @Override
    public String getDescription() {
        return "Generic file storage - Accepts any file type, stores in the file management system";
    }

    @Override
    public List<String> getAcceptedFileTypes() {
        return List.of("*");
    }

    @Override
    public UploadResult handleUpload(MultipartFile file, String mandat, Map<String, String> params) {
        try {
            String category = params.getOrDefault("category", "upload");
            String backendType = params.getOrDefault("backend", "VFS");

            StorageBackend backend = storageBackends.stream()
                    .filter(b -> b.getBackendType().equalsIgnoreCase(backendType))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown storage backend: " + backendType));

            String filePath = backend.store(file.getInputStream(), file.getOriginalFilename(), mandat);

            FileMetadata metadata = new FileMetadata();
            metadata.setMandat(mandat);
            metadata.setFilename(file.getOriginalFilename());
            metadata.setOriginalFilename(file.getOriginalFilename());
            metadata.setFilePath(filePath);
            metadata.setStorageBackend(backend.getBackendType());
            metadata.setContentType(file.getContentType());
            metadata.setFileSize(file.getSize());
            metadata.setCategory(category);
            metadata.setUploadedBy("api-upload");

            FileMetadata saved = repository.save(metadata);
            log.info("Filelist upload: Saved file '{}' (id={}) for mandat={}", file.getOriginalFilename(), saved.getId(), mandat);

            return UploadResult.ok("File stored successfully",
                    Map.of("fileId", saved.getId(),
                           "category", category,
                           "storageBackend", backend.getBackendType()));
        } catch (Exception e) {
            log.error("Filelist upload error: {}", e.getMessage(), e);
            return UploadResult.error("Failed to store file: " + e.getMessage());
        }
    }
}
