package ch.plaintext.filelist.storage;

import ch.plaintext.settings.ISettingsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * VFS (Virtual File System / Local Filesystem) storage backend.
 */
@Component
@Slf4j
public class VfsStorageBackend implements StorageBackend {

    private final ISettingsService settingsService;
    private static final String BASE_PATH_KEY = "filelist.vfs.basepath";
    private static final String DEFAULT_BASE_PATH = System.getProperty("user.home") + "/plaintext-app/files";

    public VfsStorageBackend(ISettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Override
    public String store(InputStream inputStream, String filename, String mandat) throws Exception {
        String basePath = settingsService.getString(BASE_PATH_KEY, mandat);
        if (basePath == null || basePath.isEmpty()) {
            basePath = DEFAULT_BASE_PATH;
        }

        Path mandatDir = Paths.get(basePath, mandat);
        Files.createDirectories(mandatDir);

        String sanitizedFilename = sanitizeFilename(filename);
        Path targetPath = mandatDir.resolve(sanitizedFilename);

        // Handle duplicate filenames
        int counter = 1;
        while (Files.exists(targetPath)) {
            String nameWithoutExt = sanitizedFilename.replaceFirst("[.][^.]+$", "");
            String ext = sanitizedFilename.substring(nameWithoutExt.length());
            sanitizedFilename = nameWithoutExt + "_" + counter + ext;
            targetPath = mandatDir.resolve(sanitizedFilename);
            counter++;
        }

        Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        log.info("Stored file: {}", targetPath);

        return targetPath.toString();
    }

    @Override
    public InputStream retrieve(String filePath) throws Exception {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IllegalArgumentException("File not found: " + filePath);
        }
        return new FileInputStream(file);
    }

    @Override
    public void delete(String filePath) throws Exception {
        File file = new File(filePath);
        if (file.exists()) {
            FileUtils.forceDelete(file);
            log.info("Deleted file: {}", filePath);
        }
    }

    @Override
    public boolean exists(String filePath) {
        return new File(filePath).exists();
    }

    @Override
    public String getBackendType() {
        return "VFS";
    }

    private String sanitizeFilename(String filename) {
        // Remove potentially dangerous characters
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
