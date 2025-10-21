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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.logging.Logger;

/**
 * 存储管理器，负责处理本地和云存储的文件下载
 */
public class StorageManager {
    private static final Logger logger = Logger.getLogger(StorageManager.class.getName());
    
    private final FileSystemConfig fileSystemConfig;
    private OSS ossClient;

    public StorageManager(FileSystemConfig fileSystemConfig) {
        this.fileSystemConfig = fileSystemConfig;
        
        // 如果是 OSS 存储，初始化 OSS 客户端
        if (fileSystemConfig.getFileSystemType() == FileSystemType.OSS && fileSystemConfig instanceof OssConfig ossConfig) {
            this.ossClient = new OSSClientBuilder().build(
                ossConfig.getOssEndpoint(),
                ossConfig.getOssAccessKeyId(),
                ossConfig.getOssAccessKeySecret()
            );
            logger.info("OSS client initialized with endpoint: " + ossConfig.getOssEndpoint());
        }
    }

    /**
     * 路径连接，类似 Python 的 os.path.join
     *
     * @param parts 路径部分
     * @return 连接后的路径
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
     * 从存储下载文件夹到本地目录
     *
     * @param storagePath 存储路径（OSS 对象前缀或本地路径）
     * @param localDir    本地目标目录
     * @return 是否成功下载
     */
    public boolean downloadFolder(String storagePath, String localDir) {
        if (storagePath == null || storagePath.isEmpty()) {
            logger.warning("Storage path is empty, skipping download");
            return false;
        }

        if (localDir == null || localDir.isEmpty()) {
            logger.warning("Local directory is empty, skipping download");
            return false;
        }

        try {
            if (fileSystemConfig.getFileSystemType() == FileSystemType.OSS) {
                return downloadFromOss(storagePath, localDir);
            } else {
                // LOCAL 类型：从本地路径复制到目标路径
                return copyLocalFolder(storagePath, localDir);
            }
        } catch (Exception e) {
            logger.severe("Failed to download folder from " + storagePath + " to " + localDir + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * 从 OSS 下载文件夹
     *
     * @param ossPrefix  OSS 对象前缀
     * @param localDir   本地目标目录
     * @return 是否成功下载
     */
    private boolean downloadFromOss(String ossPrefix, String localDir) {
        if (ossClient == null || !(fileSystemConfig instanceof OssConfig ossConfig)) {
            logger.severe("OSS client not initialized");
            return false;
        }

        try {
            // 确保本地目录存在
            File localDirectory = new File(localDir);
            if (!localDirectory.exists()) {
                localDirectory.mkdirs();
            }

            String bucketName = ossConfig.getOssBucketName();
            
            // 确保前缀以 / 结尾
            String prefix = ossPrefix.endsWith("/") ? ossPrefix : ossPrefix + "/";
            
            logger.info("Downloading from OSS bucket: " + bucketName + ", prefix: " + prefix + " to " + localDir);

            // 列出所有对象
            ListObjectsRequest listObjectsRequest = new ListObjectsRequest(bucketName)
                .withPrefix(prefix)
                .withMaxKeys(1000);

            ObjectListing objectListing;
            int downloadedCount = 0;

            do {
                objectListing = ossClient.listObjects(listObjectsRequest);
                
                for (OSSObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                    String objectKey = objectSummary.getKey();
                    
                    // 跳过目录标记对象
                    if (objectKey.endsWith("/")) {
                        continue;
                    }
                    
                    // 计算相对路径
                    String relativePath = objectKey.substring(prefix.length());
                    File localFile = new File(localDir, relativePath);
                    
                    // 确保父目录存在
                    File parentDir = localFile.getParentFile();
                    if (parentDir != null && !parentDir.exists()) {
                        parentDir.mkdirs();
                    }
                    
                    // 下载文件
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
                        logger.fine("Downloaded: " + objectKey + " to " + localFile.getAbsolutePath());
                    } catch (Exception e) {
                        logger.warning("Failed to download " + objectKey + ": " + e.getMessage());
                    }
                }
                
                listObjectsRequest.setMarker(objectListing.getNextMarker());
            } while (objectListing.isTruncated());

            logger.info("Downloaded " + downloadedCount + " files from OSS");
            return true;

        } catch (Exception e) {
            logger.severe("Failed to download from OSS: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 从本地路径复制文件夹
     *
     * @param sourcePath 源路径
     * @param targetPath 目标路径
     * @return 是否成功复制
     */
    private boolean copyLocalFolder(String sourcePath, String targetPath) {
        try {
            Path source = Paths.get(sourcePath);
            Path target = Paths.get(targetPath);

            if (!Files.exists(source)) {
                logger.warning("Source path does not exist: " + sourcePath);
                return false;
            }

            // 确保目标目录存在
            if (!Files.exists(target)) {
                Files.createDirectories(target);
            }

            // 如果源是目录，递归复制
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
                            logger.warning("Failed to copy " + srcPath + ": " + e.getMessage());
                        }
                    });
            } else {
                // 如果源是文件，直接复制
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            }

            logger.info("Copied folder from " + sourcePath + " to " + targetPath);
            return true;

        } catch (Exception e) {
            logger.severe("Failed to copy local folder: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 关闭 OSS 客户端
     */
    public void close() {
        if (ossClient != null) {
            try {
                ossClient.shutdown();
                logger.info("OSS client closed");
            } catch (Exception e) {
                logger.warning("Failed to close OSS client: " + e.getMessage());
            }
        }
    }
}

