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

package io.agentscope.maven.plugin.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for AgentRun deployments.
 */
public class AgentRunConfig {

    private String region = "cn-hangzhou";
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String runtimeNamePrefix = "agentscope-runtime";
    private String artifactBucket = "agentscope-runtime-agentrun-artifacts";
    private int cpu = 2;
    private int memorySize = 4096;
    private int timeoutSeconds = 120;
    private String executionRoleArn;
    private String logProject;
    private String logStore;
    private String networkMode = "PUBLIC";
    private String vpcId;
    private String securityGroupId;
    private List<String> vswitchIds = new ArrayList<>();
    private Integer sessionConcurrencyLimit = 1;
    private Integer sessionIdleTimeoutSeconds = 600;
    private String existingRuntimeId;
    private Map<String, String> metadata = new HashMap<>();
    private String codeLanguage = "java17";
    private Map<String, String> environment = new HashMap<>();

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    public String getRuntimeNamePrefix() {
        return runtimeNamePrefix;
    }

    public void setRuntimeNamePrefix(String runtimeNamePrefix) {
        this.runtimeNamePrefix = runtimeNamePrefix;
    }

    public String getArtifactBucket() {
        return artifactBucket;
    }

    public void setArtifactBucket(String artifactBucket) {
        this.artifactBucket = artifactBucket;
    }

    public int getCpu() {
        return cpu;
    }

    public void setCpu(int cpu) {
        this.cpu = cpu;
    }

    public int getMemorySize() {
        return memorySize;
    }

    public void setMemorySize(int memorySize) {
        this.memorySize = memorySize;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public String getExecutionRoleArn() {
        return executionRoleArn;
    }

    public void setExecutionRoleArn(String executionRoleArn) {
        this.executionRoleArn = executionRoleArn;
    }

    public String getLogProject() {
        return logProject;
    }

    public void setLogProject(String logProject) {
        this.logProject = logProject;
    }

    public String getLogStore() {
        return logStore;
    }

    public void setLogStore(String logStore) {
        this.logStore = logStore;
    }

    public String getNetworkMode() {
        return networkMode;
    }

    public void setNetworkMode(String networkMode) {
        this.networkMode = networkMode;
    }

    public String getVpcId() {
        return vpcId;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    public String getSecurityGroupId() {
        return securityGroupId;
    }

    public void setSecurityGroupId(String securityGroupId) {
        this.securityGroupId = securityGroupId;
    }

    public List<String> getVswitchIds() {
        return vswitchIds;
    }

    public void setVswitchIds(List<String> vswitchIds) {
        this.vswitchIds = vswitchIds;
    }

    public Integer getSessionConcurrencyLimit() {
        return sessionConcurrencyLimit;
    }

    public void setSessionConcurrencyLimit(Integer sessionConcurrencyLimit) {
        this.sessionConcurrencyLimit = sessionConcurrencyLimit;
    }

    public Integer getSessionIdleTimeoutSeconds() {
        return sessionIdleTimeoutSeconds;
    }

    public void setSessionIdleTimeoutSeconds(Integer sessionIdleTimeoutSeconds) {
        this.sessionIdleTimeoutSeconds = sessionIdleTimeoutSeconds;
    }

    public String getExistingRuntimeId() {
        return existingRuntimeId;
    }

    public void setExistingRuntimeId(String existingRuntimeId) {
        this.existingRuntimeId = existingRuntimeId;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public String getCodeLanguage() {
        return codeLanguage;
    }

    public void setCodeLanguage(String codeLanguage) {
        this.codeLanguage = codeLanguage;
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }

    public void setEnvironment(Map<String, String> environment) {
        this.environment = environment;
    }
}

