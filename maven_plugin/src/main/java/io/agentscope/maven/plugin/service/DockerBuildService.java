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

package io.agentscope.maven.plugin.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.BuildImageCmd;
import com.github.dockerjava.api.command.PushImageCmd;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.PushResponseItem;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.core.command.BuildImageResultCallback;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import io.agentscope.maven.plugin.config.BuildConfig;
import io.agentscope.maven.plugin.config.RegistryConfig;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Service for building and pushing Docker images
 */
public class DockerBuildService {

    private final Log log;
    private DockerClient dockerClient;

    public DockerBuildService(Log log) {
        this.log = log;
        try {
            DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
            DockerHttpClient httpClient = new ZerodepDockerHttpClient.Builder()
                    .dockerHost(config.getDockerHost())
                    .sslConfig(config.getSSLConfig())
                    .build();
            dockerClient = DockerClientImpl.getInstance(config, httpClient);
            dockerClient.infoCmd().exec();
            log.info("Docker client initialized successfully");
        } catch (Exception e) {
            log.warn("Failed to initialize Docker client: " + e.getMessage());
            log.warn("Make sure Docker is running and accessible");
        }
    }

    /**
     * Build Docker image
     */
    public String buildImage(BuildConfig buildConfig, File jarFile, File dockerfile) throws IOException {
        if (dockerClient == null) {
            throw new IllegalStateException("Docker client is not initialized. Please ensure Docker is running.");
        }

        File buildContextDir = new File(buildConfig.getBuildContextDir());
        
        // Copy JAR file to build context
        File targetJar = new File(buildContextDir, jarFile.getName());
        Files.copy(jarFile.toPath(), targetJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
        log.info("Copied JAR file to build context: " + targetJar.getAbsolutePath());

        String imageName = buildConfig.getFullImageName();
        log.info("Building Docker image: " + imageName);

        try {
            BuildImageCmd buildImageCmd = dockerClient.buildImageCmd()
                    .withDockerfile(dockerfile)
                    .withBaseDirectory(buildContextDir)
                    .withTag(imageName);

            BuildImageResultCallback callback = new BuildImageResultCallback() {
                @Override
                public void onNext(com.github.dockerjava.api.model.BuildResponseItem item) {
                    super.onNext(item);
                    if (item.getStream() != null) {
                        log.debug(item.getStream().trim());
                    }
                }
            };

            String imageId = buildImageCmd.exec(callback).awaitImageId();
            log.info("Docker image built successfully. Image ID: " + imageId);

            // Cleanup if configured
            if (buildConfig.isCleanupAfterBuild()) {
                // Keep the JAR and Dockerfile for potential reuse
                log.debug("Build context kept at: " + buildContextDir.getAbsolutePath());
            }

            return imageName;
        } catch (Exception e) {
            log.error("Failed to build Docker image", e);
            throw new IOException("Docker build failed", e);
        }
    }

    /**
     * Push image to registry
     */
    public String pushImage(String imageName, RegistryConfig registryConfig) throws IOException {
        if (dockerClient == null) {
            throw new IllegalStateException("Docker client is not initialized");
        }

        String fullImageName = imageName;
        
        // Tag image with registry URL if provided
        if (registryConfig.getFullUrl() != null && !registryConfig.getFullUrl().isEmpty()) {
            String registryImageName = registryConfig.getFullUrl() + "/" + imageName;
            log.info("Tagging image: " + imageName + " -> " + registryImageName);
            
            try {
                // Parse image name into repository and tag
                String[] parts = imageName.split(":");
                String sourceRepo = parts[0];
                String sourceTag = parts.length > 1 ? parts[1] : "latest";
                
                String[] targetParts = registryImageName.split(":");
                String targetRepo = targetParts[0];
                String targetTag = targetParts.length > 1 ? targetParts[1] : "latest";
                
                dockerClient.tagImageCmd(imageName, targetRepo, targetTag).exec();
                fullImageName = registryImageName;
            } catch (Exception e) {
                log.error("Failed to tag image", e);
                throw new IOException("Failed to tag image", e);
            }
        }

        log.info("Pushing image to registry: " + fullImageName);

        try {
            PushImageCmd pushImageCmd = dockerClient.pushImageCmd(fullImageName);
            
            // Add authentication if provided
            if (registryConfig.getUsername() != null && registryConfig.getPassword() != null) {
                String registryUrl = registryConfig.getRegistryUrl();
                // For Docker Hub, use index.docker.io/v1/ as registry address
                // For other registries, use the provided URL
                String registryAddress = registryUrl;
                if (registryUrl != null && (registryUrl.equals("docker.io") || registryUrl.equals("index.docker.io"))) {
                    registryAddress = "https://index.docker.io/v1/";
                } else if (registryUrl != null && !registryUrl.startsWith("http://") && !registryUrl.startsWith("https://")) {
                    // For registries like registry.cn-hangzhou.aliyuncs.com, use https:// prefix
                    registryAddress = "https://" + registryUrl;
                }
                
                log.info("Authenticating to registry: " + registryAddress + " with username: " + registryConfig.getUsername());
                
                AuthConfig authConfig = new AuthConfig()
                        .withUsername(registryConfig.getUsername())
                        .withPassword(registryConfig.getPassword())
                        .withRegistryAddress(registryAddress);
                pushImageCmd.withAuthConfig(authConfig);
            } else {
                log.warn("No authentication credentials provided. Trying to push without authentication...");
                log.warn("If push fails, please ensure you have configured username and password in deployer.yml");
            }

            final boolean[] hasError = {false};
            final String[] errorMessage = {null};
            
            pushImageCmd.exec(new ResultCallback.Adapter<PushResponseItem>() {
                @Override
                public void onNext(PushResponseItem item) {
                    if (item.getStatus() != null) {
                        log.info("Push progress: " + item.getStatus());
                    }
                    if (item.getErrorDetail() != null) {
                        String error = item.getErrorDetail().getMessage();
                        log.error("Push error: " + error);
                        hasError[0] = true;
                        errorMessage[0] = error;
                    }
                }
            }).awaitCompletion(5, TimeUnit.MINUTES);

            if (hasError[0]) {
                String error = errorMessage[0] != null ? errorMessage[0] : "Unknown error";
                String registryUrl = registryConfig.getRegistryUrl();
                boolean isPersonalRegistry = registryUrl != null && registryUrl.contains("personal.cr.aliyuncs.com");
                
                log.error("=========================================");
                log.error("镜像推送失败！");
                log.error("错误信息: " + error);
                log.error("=========================================");
                log.error("当前配置：");
                log.error("1. registry.url: " + registryUrl);
                log.error("2. registry.username: " + registryConfig.getUsername());
                log.error("3. registry.namespace: " + registryConfig.getNamespace());
                log.error("4. 最终镜像名: " + fullImageName);
                log.error("=========================================");
                
                if (isPersonalRegistry) {
                    log.error("检测到阿里云个人版容器镜像服务！");
                    log.error("=========================================");
                    log.error("个人版容器镜像服务注意事项：");
                    log.error("1. 命名空间通常是用户名，或者可以留空");
                    log.error("2. 如果使用命名空间，确保在阿里云控制台已创建该命名空间");
                    log.error("3. 建议尝试以下配置：");
                    log.error("   - 方案A: 不设置 namespace，镜像名格式: " + registryUrl + "/" + 
                             imageName.split(":")[0] + ":" + (imageName.contains(":") ? imageName.split(":")[1] : "latest"));
                    log.error("   - 方案B: namespace 设置为用户名，镜像名格式: " + registryUrl + "/" + 
                             registryConfig.getUsername() + "/" + imageName.split(":")[0] + ":" + 
                             (imageName.contains(":") ? imageName.split(":")[1] : "latest"));
                    log.error("=========================================");
                }
                
                log.error("排查步骤：");
                log.error("1. 验证登录: docker login " + registryUrl);
                log.error("2. 检查命名空间: 登录阿里云控制台确认命名空间是否存在");
                log.error("3. 测试推送: 手动执行以下命令测试推送：");
                log.error("   docker tag " + imageName + " " + fullImageName);
                log.error("   docker push " + fullImageName);
                log.error("4. 如果手动推送成功但工具失败，可能是认证配置问题");
                log.error("=========================================");
                throw new IOException("Failed to push image to registry: " + error);
            }

            log.info("Image pushed successfully: " + fullImageName);
            return fullImageName;
        } catch (Exception e) {
            log.error("Failed to push image", e);
            log.error("请检查 deployer.yml 中的 registry 配置是否正确");
            throw new IOException("Failed to push image to registry", e);
        }
    }
}

