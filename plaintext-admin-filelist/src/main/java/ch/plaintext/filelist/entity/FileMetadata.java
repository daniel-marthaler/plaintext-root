/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.filelist.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * FileMetadata Entity - stores file metadata with support for multiple storage backends.
 */
@Entity
@Table(name = "file_metadata", indexes = {
    @Index(name = "idx_file_metadata_mandat", columnList = "mandat"),
    @Index(name = "idx_file_metadata_category", columnList = "category"),
    @Index(name = "idx_file_metadata_storage_backend", columnList = "storage_backend")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mandat", nullable = false, length = 100)
    private String mandat;

    @Column(name = "filename", nullable = false, length = 500)
    private String filename;

    @Column(name = "original_filename", length = 500)
    private String originalFilename;

    @Column(name = "file_path", nullable = false, length = 2000)
    private String filePath; // Storage-specific path

    @Column(name = "storage_backend", nullable = false, length = 50)
    private String storageBackend; // VFS, NEXTCLOUD, S3, etc.

    @Column(name = "content_type", length = 255)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize; // bytes

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "beschreibung", length = 2000)
    private String beschreibung;

    @Column(name = "tags", length = 1000)
    private String tags; // Comma-separated tags

    @Column(name = "checksum", length = 64)
    private String checksum; // SHA-256 or MD5

    @Column(name = "uploaded_by", length = 255)
    private String uploadedBy;

    @Column(name = "access_count")
    private Integer accessCount = 0;

    @Column(name = "last_accessed")
    private LocalDateTime lastAccessed;

    @CreatedDate
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @CreatedBy
    @Column(name = "created_by", length = 255, updatable = false)
    private String createdBy;

    @LastModifiedDate
    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;

    @LastModifiedBy
    @Column(name = "last_modified_by", length = 255)
    private String lastModifiedBy;

    public String getFileSizeFormatted() {
        if (fileSize == null) {
            return "0 B";
        }
        if (fileSize < 1024) {
            return fileSize + " B";
        }
        if (fileSize < 1024 * 1024) {
            return String.format("%.2f KB", fileSize / 1024.0);
        }
        if (fileSize < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", fileSize / (1024.0 * 1024.0));
        }
        return String.format("%.2f GB", fileSize / (1024.0 * 1024.0 * 1024.0));
    }

    public void incrementAccessCount() {
        this.accessCount = (this.accessCount == null ? 0 : this.accessCount) + 1;
        this.lastAccessed = LocalDateTime.now();
    }
}
