/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.filelist.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class FileMetadataTest {

    // --- getFileSizeFormatted ---

    @Test
    void getFileSizeFormattedReturnsZeroBForNull() {
        FileMetadata file = new FileMetadata();
        file.setFileSize(null);

        assertThat(file.getFileSizeFormatted()).isEqualTo("0 B");
    }

    @Test
    void getFileSizeFormattedReturnsBytes() {
        FileMetadata file = new FileMetadata();
        file.setFileSize(500L);

        assertThat(file.getFileSizeFormatted()).isEqualTo("500 B");
    }

    @Test
    void getFileSizeFormattedReturnsKB() {
        FileMetadata file = new FileMetadata();
        file.setFileSize(2048L);

        assertThat(file.getFileSizeFormatted()).isEqualTo("2.00 KB");
    }

    @Test
    void getFileSizeFormattedReturnsMB() {
        FileMetadata file = new FileMetadata();
        file.setFileSize(5 * 1024 * 1024L);

        assertThat(file.getFileSizeFormatted()).isEqualTo("5.00 MB");
    }

    @Test
    void getFileSizeFormattedReturnsGB() {
        FileMetadata file = new FileMetadata();
        file.setFileSize(2L * 1024 * 1024 * 1024);

        assertThat(file.getFileSizeFormatted()).isEqualTo("2.00 GB");
    }

    @Test
    void getFileSizeFormattedBoundaryAt1024() {
        FileMetadata file = new FileMetadata();
        file.setFileSize(1023L);
        assertThat(file.getFileSizeFormatted()).isEqualTo("1023 B");

        file.setFileSize(1024L);
        assertThat(file.getFileSizeFormatted()).isEqualTo("1.00 KB");
    }

    // --- incrementAccessCount ---

    @Test
    void incrementAccessCountFromNull() {
        FileMetadata file = new FileMetadata();
        file.setAccessCount(null);

        file.incrementAccessCount();

        assertThat(file.getAccessCount()).isEqualTo(1);
        assertThat(file.getLastAccessed()).isNotNull();
    }

    @Test
    void incrementAccessCountFromZero() {
        FileMetadata file = new FileMetadata();
        file.setAccessCount(0);

        file.incrementAccessCount();

        assertThat(file.getAccessCount()).isEqualTo(1);
    }

    @Test
    void incrementAccessCountIncrementsExisting() {
        FileMetadata file = new FileMetadata();
        file.setAccessCount(5);

        file.incrementAccessCount();

        assertThat(file.getAccessCount()).isEqualTo(6);
    }

    @Test
    void incrementAccessCountSetsLastAccessed() {
        FileMetadata file = new FileMetadata();
        file.setAccessCount(0);
        LocalDateTime before = LocalDateTime.now();

        file.incrementAccessCount();

        assertThat(file.getLastAccessed()).isNotNull();
        assertThat(file.getLastAccessed()).isAfterOrEqualTo(before);
    }

    // --- defaults ---

    @Test
    void defaultAccessCountIsZero() {
        FileMetadata file = new FileMetadata();
        assertThat(file.getAccessCount()).isZero();
    }

    @Test
    void noArgsConstructorCreatesEmptyInstance() {
        FileMetadata file = new FileMetadata();
        assertThat(file.getId()).isNull();
        assertThat(file.getFilename()).isNull();
        assertThat(file.getMandat()).isNull();
    }

    @Test
    void toStringDoesNotThrow() {
        FileMetadata file = new FileMetadata();
        file.setId(1L);
        file.setFilename("test.pdf");
        assertThat(file.toString()).contains("test.pdf");
    }
}
