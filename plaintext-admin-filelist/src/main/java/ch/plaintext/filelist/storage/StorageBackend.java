/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.filelist.storage;

import java.io.InputStream;

/**
 * Interface for different storage backends.
 */
public interface StorageBackend {

    /**
     * Store a file and return the storage path.
     */
    String store(InputStream inputStream, String filename, String mandat) throws Exception;

    /**
     * Retrieve a file as InputStream.
     */
    InputStream retrieve(String filePath) throws Exception;

    /**
     * Delete a file.
     */
    void delete(String filePath) throws Exception;

    /**
     * Check if a file exists.
     */
    boolean exists(String filePath) throws Exception;

    /**
     * Get the backend type name.
     */
    String getBackendType();
}
