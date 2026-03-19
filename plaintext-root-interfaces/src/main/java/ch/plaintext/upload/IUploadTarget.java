/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.upload;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Interface for services that accept file uploads via the generic /nosec/root/upload endpoint.
 * Implementations are auto-discovered by Spring and listed in the upload services API.
 */
public interface IUploadTarget {

    String getServiceName();

    String getDescription();

    List<String> getAcceptedFileTypes();

    UploadResult handleUpload(MultipartFile file, String mandat, Map<String, String> params);

    record UploadResult(boolean success, String message, Map<String, Object> details) {
        public static UploadResult ok(String message) {
            return new UploadResult(true, message, Map.of());
        }
        public static UploadResult ok(String message, Map<String, Object> details) {
            return new UploadResult(true, message, details);
        }
        public static UploadResult error(String message) {
            return new UploadResult(false, message, Map.of());
        }
    }
}
