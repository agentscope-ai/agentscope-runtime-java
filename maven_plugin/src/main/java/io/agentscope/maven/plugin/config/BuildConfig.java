package io.agentscope.maven.plugin.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Build configuration for Docker image
 */
public class BuildConfig {
    private String imageName;
    private String imageTag = "latest";
    private String baseImage = "eclipse-temurin:17-jre";
    private int port = 8080;
    private boolean pushToRegistry = false;
    private boolean deployToK8s = false;
    private String buildContextDir = "/tmp/docker-build";
    private int buildTimeout = 600; // 10 minutes
    private int pushTimeout = 300; // 5 minutes
    private boolean cleanupAfterBuild = true;
    private Map<String, String> environment = new HashMap<>();

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getImageTag() {
        return imageTag;
    }

    public void setImageTag(String imageTag) {
        this.imageTag = imageTag;
    }

    public String getBaseImage() {
        return baseImage;
    }

    public void setBaseImage(String baseImage) {
        this.baseImage = baseImage;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isPushToRegistry() {
        return pushToRegistry;
    }

    public void setPushToRegistry(boolean pushToRegistry) {
        this.pushToRegistry = pushToRegistry;
    }

    public boolean isDeployToK8s() {
        return deployToK8s;
    }

    public void setDeployToK8s(boolean deployToK8s) {
        this.deployToK8s = deployToK8s;
    }

    public String getBuildContextDir() {
        return buildContextDir;
    }

    public void setBuildContextDir(String buildContextDir) {
        this.buildContextDir = buildContextDir;
    }

    public int getBuildTimeout() {
        return buildTimeout;
    }

    public void setBuildTimeout(int buildTimeout) {
        this.buildTimeout = buildTimeout;
    }

    public int getPushTimeout() {
        return pushTimeout;
    }

    public void setPushTimeout(int pushTimeout) {
        this.pushTimeout = pushTimeout;
    }

    public boolean isCleanupAfterBuild() {
        return cleanupAfterBuild;
    }

    public void setCleanupAfterBuild(boolean cleanupAfterBuild) {
        this.cleanupAfterBuild = cleanupAfterBuild;
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }

    public void setEnvironment(Map<String, String> environment) {
        this.environment = environment;
    }

    public String getFullImageName() {
        return imageName + ":" + imageTag;
    }
}

