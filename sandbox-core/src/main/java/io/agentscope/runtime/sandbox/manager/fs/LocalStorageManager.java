package io.agentscope.runtime.sandbox.manager.fs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class LocalStorageManager extends StorageManager {
    Logger logger = LoggerFactory.getLogger(LocalStorageManager.class);

    public LocalStorageManager(FileSystemStarter fileSystemStarter) {
        super(fileSystemStarter);
    }

    @Override
    public boolean downloadFolder(String storagePath, String localDir) {
        if (storagePath == null || storagePath.isEmpty()) {
            logger.warn("Storage path is empty, skipping download");
            return false;
        }

        if (localDir == null || localDir.isEmpty()) {
            logger.warn("Local directory is empty, skipping download");
            return false;
        }

        try {
            return copyLocalFolder(storagePath, localDir);
        } catch (Exception e) {
            logger.error("Failed to download folder from {} to {}: {}", storagePath, localDir, e.getMessage());
            return false;
        }
    }

    private boolean copyLocalFolder(String sourcePath, String targetPath) {
        try {
            Path source = Paths.get(sourcePath);
            Path target = Paths.get(targetPath);

            if (!Files.exists(source)) {
                logger.warn("Source path does not exist: {}", sourcePath);
                return false;
            }

            // Ensure target directory exists
            if (!Files.exists(target)) {
                Files.createDirectories(target);
            }

            // If source is directory, copy recursively
            if (Files.isDirectory(source)) {
                Files.walk(source).forEach(srcPath -> {
                    try {
                        Path destPath = target.resolve(source.relativize(srcPath));
                        if (Files.isDirectory(srcPath)) {
                            if (!Files.exists(destPath)) {
                                Files.createDirectories(destPath);
                            }
                        } else {
                            Files.copy(srcPath, destPath, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException e) {
                        logger.warn("Failed to copy {}: {}", srcPath, e.getMessage());
                    }
                });
            } else {
                // If source is file, copy directly
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            }

            logger.info("Copied folder from {} to {}", sourcePath, targetPath);
            return true;

        } catch (Exception e) {
            logger.error("Failed to copy local folder: {}", e.getMessage());
            return false;
        }
    }


    @Override
    public boolean uploadFolder(String localDir, String storagePath) {
        if (localDir == null || localDir.isEmpty()) {
            logger.warn("Local directory is empty, skipping upload");
            return false;
        }

        if (storagePath == null || storagePath.isEmpty()) {
            logger.warn("Storage path is empty, skipping upload");
            return false;
        }

        // Check if local directory exists
        File localDirectory = new File(localDir);
        if (!localDirectory.exists()) {
            logger.warn("Local directory does not exist: {}", localDir);
            return false;
        }

        try {
            return copyLocalFolder(localDir, storagePath);
        } catch (Exception e) {
            logger.error("Failed to upload folder from {} to {}: {}", localDir, storagePath, e.getMessage());
            return false;
        }
    }
}
