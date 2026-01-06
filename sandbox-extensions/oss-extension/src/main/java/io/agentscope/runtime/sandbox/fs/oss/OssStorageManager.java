package io.agentscope.runtime.sandbox.fs.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ListObjectsRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import io.agentscope.runtime.sandbox.manager.fs.StorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class OssStorageManager extends StorageManager {
    Logger logger = LoggerFactory.getLogger(OssStorageManager.class);
    private final OSS ossClient;

    public OssStorageManager(OssStarter ossStarter) {
        super(ossStarter);
        this.ossClient = new OSSClientBuilder().build(
                ossStarter.getOssEndpoint(),
                ossStarter.getOssAccessKeyId(),
                ossStarter.getOssAccessKeySecret()
        );
        logger.info("OSS client initialized with endpoint: {}", ossStarter.getOssEndpoint());
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
            return downloadFromOss(storagePath, localDir);
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
        if (ossClient == null || !(fileSystemStarter instanceof OssStarter ossStarter)) {
            logger.error("OSS client not initialized");
            return false;
        }

        try {
            // Ensure local directory exists
            File localDirectory = new File(localDir);
            if (!localDirectory.exists()) {
                boolean ignored = localDirectory.mkdirs();
            }

            String bucketName = ossStarter.getOssBucketName();

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
                        boolean ignores = parentDir.mkdirs();
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


    @Override
    public boolean uploadFolder(String localDir, String storagePath) {
        return false;
    }
}
