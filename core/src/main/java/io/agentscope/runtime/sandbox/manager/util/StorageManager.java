/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agentscope.runtime.sandbox.manager.util;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ListObjectsRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import io.agentscope.runtime.sandbox.manager.model.fs.FileSystemConfig;
import io.agentscope.runtime.sandbox.manager.model.fs.FileSystemType;
import io.agentscope.runtime.sandbox.manager.model.fs.OssConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Storage Manager responsible for handling file downloads from local and cloud storage
 */
public class StorageManager {
    private static final Logger logger = LoggerFactory.getLogger(StorageManager.class);

    private final FileSystemConfig fileSystemConfig;
    private OSS ossClient;

    public StorageManager(FileSystemConfig fileSystemConfig) {
        this.fileSystemConfig = fileSystemConfig;

        // If OSS storage, initialize OSS client
        if (fileSystemConfig.getFileSystemType() == FileSystemType.OSS && fileSystemConfig instanceof OssConfig ossConfig) {
            this.ossClient = new OSSClientBuilder().build(
                    ossConfig.getOssEndpoint(),
                    ossConfig.getOssAccessKeyId(),
                    ossConfig.getOssAccessKeySecret()
            );
            logger.info("OSS client initialized with endpoint: {}", ossConfig.getOssEndpoint());
        }
    }

    /**
     * Path join
     *
     * @param parts path components
     * @return joined path
     */
    public String pathJoin(String... parts) {
        if (parts == null || parts.length == 0) {
            return "";
        }

        Path path = Paths.get(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            path = path.resolve(parts[i]);
        }

        return path.toString();
    }

    /**
     * Download folder from storage to local directory
     *
     * @param storagePath storage path (OSS object prefix or local path)
     * @param localDir    local target directory
     * @return whether download succeeded
     */
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
            if (fileSystemConfig.getFileSystemType() == FileSystemType.OSS) {
                return downloadFromOss(storagePath, localDir);
            } else {
                // LOCAL type: copy from local path to target path
                return copyLocalFolder(storagePath, localDir);
            }
        } catch (Exception e) {
            logger.error("Failed to download folder from {} to {}: {}", storagePath, localDir, e.getMessage());
            return false;
        }
    }

    /**
     * Download folder from OSS
     *
     * @param ossPrefix OSS object prefix
     * @param localDir  local target directory
     * @return whether download succeeded
     */
    private boolean downloadFromOss(String ossPrefix, String localDir) {
        if (ossClient == null || !(fileSystemConfig instanceof OssConfig ossConfig)) {
            logger.error("OSS client not initialized");
            return false;
        }

        try {
            // Ensure local directory exists
            File localDirectory = new File(localDir);
            if (!localDirectory.exists()) {
                localDirectory.mkdirs();
            }

            String bucketName = ossConfig.getOssBucketName();

            // Ensure prefix ends with /
            String prefix = ossPrefix.endsWith("/") ? ossPrefix : ossPrefix + "/";

            logger.info("Downloading from OSS bucket: {}, prefix: {} to {}", bucketName, prefix, localDir);

            // List all objects
            ListObjectsRequest listObjectsRequest = new ListObjectsRequest(bucketName)
                    .withPrefix(prefix)
                    .withMaxKeys(1000);

            ObjectListing objectListing;
            int downloadedCount = 0;

            do {
                objectListing = ossClient.listObjects(listObjectsRequest);

                for (OSSObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                    String objectKey = objectSummary.getKey();

                    // Skip directory marker objects
                    if (objectKey.endsWith("/")) {
                        continue;
                    }

                    // Calculate relative path
                    String relativePath = objectKey.substring(prefix.length());
                    File localFile = new File(localDir, relativePath);

                    // Ensure parent directory exists
                    File parentDir = localFile.getParentFile();
                    if (parentDir != null && !parentDir.exists()) {
                        parentDir.mkdirs();
                    }

                    // Download file
                    try {
                        OSSObject ossObject = ossClient.getObject(bucketName, objectKey);
                        try (InputStream inputStream = ossObject.getObjectContent();
                             FileOutputStream outputStream = new FileOutputStream(localFile)) {
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                            }
                        }
                        downloadedCount++;
                        logger.info("Downloaded: {} to {}", objectKey, localFile.getAbsolutePath());
                    } catch (Exception e) {
                        logger.warn("Failed to download {}: {}", objectKey, e.getMessage());
                    }
                }

                listObjectsRequest.setMarker(objectListing.getNextMarker());
            } while (objectListing.isTruncated());

            logger.info("Downloaded {} files from OSS", downloadedCount);
            return true;

        } catch (Exception e) {
            logger.error("Failed to download from OSS: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Copy folder from local path
     *
     * @param sourcePath source path
     * @param targetPath target path
     * @return whether copy succeeded
     */
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
                Files.walk(source)
                        .forEach(srcPath -> {
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

    /**
     * Upload local folder to storage
     *
     * @param localDir    local source directory
     * @param storagePath storage path (OSS object prefix or local path)
     * @return whether upload succeeded
     */
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
            if (fileSystemConfig.getFileSystemType() == FileSystemType.OSS) {
                return uploadToOss(localDir, storagePath);
            } else {
                return copyLocalFolder(localDir, storagePath);
            }
        } catch (Exception e) {
            logger.error("Failed to upload folder from {} to {}: {}", localDir, storagePath, e.getMessage());
            return false;
        }
    }

    /**
     * Upload local folder to OSS
     *
     * @param localDir  local source directory
     * @param ossPrefix OSS object prefix
     * @return whether upload succeeded
     */
    private boolean uploadToOss(String localDir, String ossPrefix) {
        if (ossClient == null || !(fileSystemConfig instanceof OssConfig ossConfig)) {
            logger.error("OSS client not initialized");
            return false;
        }

        try {
            File localDirectory = new File(localDir);
            if (!localDirectory.exists() || !localDirectory.isDirectory()) {
                logger.warn("Local directory does not exist or is not a directory: {}", localDir);
                return false;
            }

            String bucketName = ossConfig.getOssBucketName();

            // Ensure prefix ends with /
            String prefix = ossPrefix.endsWith("/") ? ossPrefix : ossPrefix + "/";

            logger.info("Uploading to OSS bucket: {}, prefix: {} from {}", bucketName, prefix, localDir);

            int uploadedCount = 0;
            uploadedCount = uploadDirectoryToOss(localDirectory, bucketName, prefix, "");

            logger.info("Uploaded {} files to OSS", uploadedCount);
            return true;

        } catch (Exception e) {
            logger.error("Failed to upload to OSS: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Recursively upload directory to OSS
     *
     * @param directory    current directory
     * @param bucketName   OSS bucket name
     * @param basePrefix   base prefix
     * @param relativePath relative path
     * @return number of files uploaded
     */
    private int uploadDirectoryToOss(File directory, String bucketName, String basePrefix, String relativePath) {
        int uploadedCount = 0;

        File[] files = directory.listFiles();
        if (files == null) {
            return 0;
        }

        for (File file : files) {
            String currentRelativePath = relativePath.isEmpty() ? file.getName() : relativePath + "/" + file.getName();

            if (file.isDirectory()) {
                // Recursively upload subdirectories
                uploadedCount += uploadDirectoryToOss(file, bucketName, basePrefix, currentRelativePath);
            } else {
                // Upload file
                String objectKey = basePrefix + currentRelativePath;

                try {
                    // Upload file to OSS
                    ossClient.putObject(bucketName, objectKey, file);
                    uploadedCount++;
                    logger.info("Uploaded: {} to {}", file.getAbsolutePath(), objectKey);
                } catch (Exception e) {
                    logger.warn("Failed to upload {} to {}: {}", file.getAbsolutePath(), objectKey, e.getMessage());
                }
            }
        }

        return uploadedCount;
    }

    /**
     * Close OSS client
     */
    public void close() {
        if (ossClient != null) {
            try {
                ossClient.shutdown();
                logger.info("OSS client closed");
            } catch (Exception e) {
                logger.warn("Failed to close OSS client: {}", e.getMessage());
            }
        }
    }
}

