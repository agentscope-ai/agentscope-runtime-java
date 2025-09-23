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
package io.agentscope.runtime.sandbox.manager.model;

public class ContainerModel {
    private String sessionId;
    private String containerId;
    private String containerName;
    private String baseUrl;
    private String browserUrl;
    private String frontBrowserWS;
    private String clientBrowserWS;
    private String artifactsSIO;
    private String[] ports;
    private String mountDir;
    private String storagePath;
    private String runtimeToken;
    private String version;
    private String authToken;

    public static Builder builder() {
        return new Builder();
    }

    private ContainerModel(Builder builder) {
        this.sessionId = builder.sessionId;
        this.containerId = builder.containerId;
        this.containerName = builder.containerName;
        this.baseUrl = builder.baseUrl;
        this.browserUrl = builder.browserUrl;
        this.frontBrowserWS = builder.frontBrowserWS;
        this.clientBrowserWS = builder.clientBrowserWS;
        this.artifactsSIO = builder.artifactsSIO;
        this.ports = builder.ports;
        this.mountDir = builder.mountDir;
        this.storagePath = builder.storagePath;
        this.runtimeToken = builder.runtimeToken;
        this.version = builder.version;
        this.authToken = builder.authToken;
    }

    // Getters
    public String getSessionId() {
        return sessionId;
    }

    public String getContainerId() {
        return containerId;
    }

    public String getContainerName() {
        return containerName;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getBrowserUrl() {
        return browserUrl;
    }

    public String getFrontBrowserWS() {
        return frontBrowserWS;
    }

    public String getClientBrowserWS() {
        return clientBrowserWS;
    }

    public String getArtifactsSIO() {
        return artifactsSIO;
    }

    public String[] getPorts() {
        return ports;
    }

    public String getMountDir() {
        return mountDir;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public String getRuntimeToken() {
        return runtimeToken;
    }

    public String getVersion() {
        return version;
    }

    // Setters
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setBrowserUrl(String browserUrl) {
        this.browserUrl = browserUrl;
    }

    public void setFrontBrowserWS(String frontBrowserWS) {
        this.frontBrowserWS = frontBrowserWS;
    }

    public void setClientBrowserWS(String clientBrowserWS) {
        this.clientBrowserWS = clientBrowserWS;
    }

    public void setArtifactsSIO(String artifactsSIO) {
        this.artifactsSIO = artifactsSIO;
    }

    public void setPorts(String[] ports) {
        this.ports = ports;
    }

    public void setMountDir(String mountDir) {
        this.mountDir = mountDir;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public void setRuntimeToken(String runtimeToken) {
        this.runtimeToken = runtimeToken;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String toString() {
        return "manager.ContainerModel{" +
                "sessionId='" + sessionId + '\'' +
                ", containerId='" + containerId + '\'' +
                ", containerName='" + containerName + '\'' +
                ", baseUrl='" + baseUrl + '\'' +
                ", browserUrl='" + browserUrl + '\'' +
                ", frontBrowserWS='" + frontBrowserWS + '\'' +
                ", clientBrowserWS='" + clientBrowserWS + '\'' +
                ", artifactsSIO='" + artifactsSIO + '\'' +
                ", ports=" + String.join(",", ports) +
                ", mountDir='" + mountDir + '\'' +
                ", storagePath='" + storagePath + '\'' +
                ", runtimeToken='" + runtimeToken + '\'' +
                ", version='" + version + '\'' +
                ", authToken='" + authToken + '\'' +
                '}';
    }

    public static class Builder {
        private String sessionId;
        private String containerId;
        private String containerName;
        private String baseUrl;
        private String browserUrl;
        private String frontBrowserWS;
        private String clientBrowserWS;
        private String artifactsSIO;
        private String[] ports;
        private String mountDir;
        private String storagePath;
        private String runtimeToken;
        private String version;
        private String authToken;

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder containerId(String containerId) {
            this.containerId = containerId;
            return this;
        }

        public Builder containerName(String containerName) {
            this.containerName = containerName;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder browserUrl(String browserUrl) {
            this.browserUrl = browserUrl;
            return this;
        }

        public Builder frontBrowserWS(String frontBrowserWS) {
            this.frontBrowserWS = frontBrowserWS;
            return this;
        }

        public Builder clientBrowserWS(String clientBrowserWS) {
            this.clientBrowserWS = clientBrowserWS;
            return this;
        }

        public Builder artifactsSIO(String artifactsSIO) {
            this.artifactsSIO = artifactsSIO;
            return this;
        }

        public Builder ports(String[] ports) {
            this.ports = ports;
            return this;
        }

        public Builder mountDir(String mountDir) {
            this.mountDir = mountDir;
            return this;
        }

        public Builder storagePath(String storagePath) {
            this.storagePath = storagePath;
            return this;
        }

        public Builder runtimeToken(String runtimeToken) {
            this.runtimeToken = runtimeToken;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder authToken(String authToken) {
            this.authToken = authToken;
            return this;
        }

        public ContainerModel build() {
            return new ContainerModel(this);
        }
    }
}
