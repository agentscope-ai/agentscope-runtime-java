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
    public void pushImage(String imageName, RegistryConfig registryConfig) throws IOException {
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
                AuthConfig authConfig = new AuthConfig()
                        .withUsername(registryConfig.getUsername())
                        .withPassword(registryConfig.getPassword())
                        .withRegistryAddress(registryConfig.getRegistryUrl());
                pushImageCmd.withAuthConfig(authConfig);
            }

            pushImageCmd.exec(new ResultCallback.Adapter<PushResponseItem>() {
                @Override
                public void onNext(PushResponseItem item) {
                    if (item.getStatus() != null) {
                        log.info("Push progress: " + item.getStatus());
                    }
                    if (item.getErrorDetail() != null) {
                        log.error("Push error: " + item.getErrorDetail().getMessage());
                    }
                }
            }).awaitCompletion(5, TimeUnit.MINUTES);

            log.info("Image pushed successfully: " + fullImageName);
        } catch (Exception e) {
            log.error("Failed to push image", e);
            throw new IOException("Failed to push image to registry", e);
        }
    }
}

