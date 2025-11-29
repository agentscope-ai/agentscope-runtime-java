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

package io.agentscope.maven.plugin;

import io.agentscope.maven.plugin.config.AgentRunConfig;
import io.agentscope.maven.plugin.config.BuildConfig;
import io.agentscope.maven.plugin.config.EnvironmentConfig;
import io.agentscope.maven.plugin.config.K8sConfig;
import io.agentscope.maven.plugin.config.ModelStudioConfig;
import io.agentscope.maven.plugin.config.OssConfig;
import io.agentscope.maven.plugin.config.RegistryConfig;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Configuration loader from YAML file
 */
public class ConfigLoader {
    private final MavenProject project;
    private final String configFile;
    private final Log log;

    public ConfigLoader(MavenProject project, String configFile, Log log) {
        this.project = project;
        this.configFile = configFile;
        this.log = log;
    }

    @SuppressWarnings("unchecked")
    public BuildConfig loadBuildConfig() {
        BuildConfig config = new BuildConfig();
        Map<String, Object> yamlConfig = loadYamlConfig();
        
        if (yamlConfig != null && yamlConfig.containsKey("build")) {
            Map<String, Object> buildConfig = (Map<String, Object>) yamlConfig.get("build");
            if (buildConfig.containsKey("imageName")) {
                config.setImageName((String) buildConfig.get("imageName"));
            }
            if (buildConfig.containsKey("imageTag")) {
                config.setImageTag((String) buildConfig.get("imageTag"));
            }
            if (buildConfig.containsKey("baseImage")) {
                config.setBaseImage((String) buildConfig.get("baseImage"));
            }
            if (buildConfig.containsKey("port")) {
                config.setPort(((Number) buildConfig.get("port")).intValue());
            }
            if (buildConfig.containsKey("pushToRegistry")) {
                config.setPushToRegistry((Boolean) buildConfig.get("pushToRegistry"));
            }
            if (buildConfig.containsKey("deployToK8s")) {
                config.setDeployToK8s((Boolean) buildConfig.get("deployToK8s"));
            }
            if (buildConfig.containsKey("deployToModelStudio")) {
                config.setDeployToModelStudio((Boolean) buildConfig.get("deployToModelStudio"));
            }
            if (buildConfig.containsKey("deployToAgentRun")) {
                config.setDeployToAgentRun((Boolean) buildConfig.get("deployToAgentRun"));
            }
            if (buildConfig.containsKey("buildContextDir")) {
                config.setBuildContextDir((String) buildConfig.get("buildContextDir"));
            }
            if (buildConfig.containsKey("environment")) {
                config.setEnvironment((Map<String, String>) buildConfig.get("environment"));
            }
        }
        
        return config;
    }

    @SuppressWarnings("unchecked")
    public RegistryConfig loadRegistryConfig() {
        RegistryConfig config = new RegistryConfig();
        Map<String, Object> yamlConfig = loadYamlConfig();
        
        if (yamlConfig != null && yamlConfig.containsKey("registry")) {
            Map<String, Object> registryConfig = (Map<String, Object>) yamlConfig.get("registry");
            if (registryConfig.containsKey("url")) {
                config.setRegistryUrl((String) registryConfig.get("url"));
            }
            if (registryConfig.containsKey("username")) {
                config.setUsername((String) registryConfig.get("username"));
            }
            if (registryConfig.containsKey("password")) {
                config.setPassword((String) registryConfig.get("password"));
            }
            if (registryConfig.containsKey("namespace")) {
                config.setNamespace((String) registryConfig.get("namespace"));
            }
        }
        
        return config;
    }

    @SuppressWarnings("unchecked")
    public K8sConfig loadK8sConfig() {
        K8sConfig config = new K8sConfig();
        Map<String, Object> yamlConfig = loadYamlConfig();
        
        if (yamlConfig != null && yamlConfig.containsKey("kubernetes")) {
            Map<String, Object> k8sConfig = (Map<String, Object>) yamlConfig.get("kubernetes");
            if (k8sConfig.containsKey("namespace")) {
                config.setK8sNamespace((String) k8sConfig.get("namespace"));
            }
            if (k8sConfig.containsKey("kubeconfigPath")) {
                config.setKubeconfigPath((String) k8sConfig.get("kubeconfigPath"));
            }
            if (k8sConfig.containsKey("replicas")) {
                config.setReplicas(((Number) k8sConfig.get("replicas")).intValue());
            }
            if (k8sConfig.containsKey("runtimeConfig")) {
                config.setRuntimeConfig((Map<String, String>) k8sConfig.get("runtimeConfig"));
            }
        }
        
        return config;
    }

    @SuppressWarnings("unchecked")
    public ModelStudioConfig loadModelStudioConfig() {
        ModelStudioConfig config = new ModelStudioConfig();
        Map<String, Object> yamlConfig = loadYamlConfig();

        if (yamlConfig != null && yamlConfig.containsKey("modelstudio")) {
            Map<String, Object> modelStudioConfig = (Map<String, Object>) yamlConfig.get("modelstudio");
            if (modelStudioConfig.containsKey("region")) {
                config.setRegion((String) modelStudioConfig.get("region"));
            }
            if (modelStudioConfig.containsKey("endpoint")) {
                config.setEndpoint((String) modelStudioConfig.get("endpoint"));
            }
            if (modelStudioConfig.containsKey("workspaceId")) {
                config.setWorkspaceId((String) modelStudioConfig.get("workspaceId"));
            }
            if (modelStudioConfig.containsKey("accessKeyId")) {
                config.setAccessKeyId((String) modelStudioConfig.get("accessKeyId"));
            }
            if (modelStudioConfig.containsKey("accessKeySecret")) {
                config.setAccessKeySecret((String) modelStudioConfig.get("accessKeySecret"));
            }
            if (modelStudioConfig.containsKey("dashscopeApiKey")) {
                config.setDashscopeApiKey((String) modelStudioConfig.get("dashscopeApiKey"));
            }
            if (modelStudioConfig.containsKey("telemetryEnabled")) {
                config.setTelemetryEnabled((Boolean) modelStudioConfig.get("telemetryEnabled"));
            }
            if (modelStudioConfig.containsKey("deployName")) {
                config.setDeployName((String) modelStudioConfig.get("deployName"));
            }
            if (modelStudioConfig.containsKey("agentId")) {
                config.setAgentId((String) modelStudioConfig.get("agentId"));
            }
            if (modelStudioConfig.containsKey("agentDescription")) {
                config.setAgentDescription((String) modelStudioConfig.get("agentDescription"));
            }
            if (modelStudioConfig.containsKey("serviceName")) {
                config.setServiceName((String) modelStudioConfig.get("serviceName"));
            }
            if (modelStudioConfig.containsKey("functionName")) {
                config.setFunctionName((String) modelStudioConfig.get("functionName"));
            }
            if (modelStudioConfig.containsKey("artifactBucket")) {
                config.setArtifactBucket((String) modelStudioConfig.get("artifactBucket"));
            }
            if (modelStudioConfig.containsKey("memorySize")) {
                config.setMemorySize(((Number) modelStudioConfig.get("memorySize")).intValue());
            }
            if (modelStudioConfig.containsKey("timeoutSeconds")) {
                config.setTimeoutSeconds(((Number) modelStudioConfig.get("timeoutSeconds")).intValue());
            }
            if (modelStudioConfig.containsKey("metadata")) {
                config.setMetadata((Map<String, String>) modelStudioConfig.get("metadata"));
            }
        }

        return config;
    }

    @SuppressWarnings("unchecked")
    public AgentRunConfig loadAgentRunConfig() {
        AgentRunConfig config = new AgentRunConfig();
        Map<String, Object> yamlConfig = loadYamlConfig();

        if (yamlConfig != null && yamlConfig.containsKey("agentrun")) {
            Map<String, Object> agentRunConfig = (Map<String, Object>) yamlConfig.get("agentrun");
            if (agentRunConfig.containsKey("region")) {
                config.setRegion((String) agentRunConfig.get("region"));
            }
            if (agentRunConfig.containsKey("endpoint")) {
                config.setEndpoint((String) agentRunConfig.get("endpoint"));
            }
            if (agentRunConfig.containsKey("accessKeyId")) {
                config.setAccessKeyId((String) agentRunConfig.get("accessKeyId"));
            }
            if (agentRunConfig.containsKey("accessKeySecret")) {
                config.setAccessKeySecret((String) agentRunConfig.get("accessKeySecret"));
            }
            if (agentRunConfig.containsKey("runtimeNamePrefix")) {
                config.setRuntimeNamePrefix((String) agentRunConfig.get("runtimeNamePrefix"));
            }
            if (agentRunConfig.containsKey("artifactBucket")) {
                config.setArtifactBucket((String) agentRunConfig.get("artifactBucket"));
            }
            if (agentRunConfig.containsKey("cpu")) {
                config.setCpu(((Number) agentRunConfig.get("cpu")).intValue());
            }
            if (agentRunConfig.containsKey("memorySize")) {
                config.setMemorySize(((Number) agentRunConfig.get("memorySize")).intValue());
            }
            if (agentRunConfig.containsKey("timeoutSeconds")) {
                config.setTimeoutSeconds(((Number) agentRunConfig.get("timeoutSeconds")).intValue());
            }
            if (agentRunConfig.containsKey("executionRoleArn")) {
                config.setExecutionRoleArn((String) agentRunConfig.get("executionRoleArn"));
            }
            if (agentRunConfig.containsKey("logProject")) {
                config.setLogProject((String) agentRunConfig.get("logProject"));
            }
            if (agentRunConfig.containsKey("logStore")) {
                config.setLogStore((String) agentRunConfig.get("logStore"));
            }
            if (agentRunConfig.containsKey("networkMode")) {
                config.setNetworkMode((String) agentRunConfig.get("networkMode"));
            }
            if (agentRunConfig.containsKey("vpcId")) {
                config.setVpcId((String) agentRunConfig.get("vpcId"));
            }
            if (agentRunConfig.containsKey("securityGroupId")) {
                config.setSecurityGroupId((String) agentRunConfig.get("securityGroupId"));
            }
            if (agentRunConfig.containsKey("vswitchIds")) {
                config.setVswitchIds((List<String>) agentRunConfig.get("vswitchIds"));
            }
            if (agentRunConfig.containsKey("sessionConcurrencyLimit")) {
                config.setSessionConcurrencyLimit(((Number) agentRunConfig.get("sessionConcurrencyLimit")).intValue());
            }
            if (agentRunConfig.containsKey("sessionIdleTimeoutSeconds")) {
                config.setSessionIdleTimeoutSeconds(((Number) agentRunConfig.get("sessionIdleTimeoutSeconds")).intValue());
            }
            if (agentRunConfig.containsKey("existingRuntimeId")) {
                config.setExistingRuntimeId((String) agentRunConfig.get("existingRuntimeId"));
            }
            if (agentRunConfig.containsKey("metadata")) {
                config.setMetadata((Map<String, String>) agentRunConfig.get("metadata"));
            }
            if (agentRunConfig.containsKey("language")) {
                config.setCodeLanguage((String) agentRunConfig.get("language"));
            } else if (agentRunConfig.containsKey("codeLanguage")) {
                config.setCodeLanguage((String) agentRunConfig.get("codeLanguage"));
            }
            if (agentRunConfig.containsKey("environment")) {
                config.setEnvironment((Map<String, String>) agentRunConfig.get("environment"));
            }
        }
        
        return config;
    }

    /**
     * Load top-level OSS configuration from YAML.
     */
    @SuppressWarnings("unchecked")
    public OssConfig loadOssConfig() {
        OssConfig config = new OssConfig();
        Map<String, Object> yamlConfig = loadYamlConfig();

        if (yamlConfig != null && yamlConfig.containsKey("oss")) {
            Map<String, Object> ossConfig = (Map<String, Object>) yamlConfig.get("oss");
            if (ossConfig.containsKey("region")) {
                config.setRegion((String) ossConfig.get("region"));
            }
            if (ossConfig.containsKey("endpoint")) {
                config.setEndpoint((String) ossConfig.get("endpoint"));
            }
            if (ossConfig.containsKey("accessKeyId")) {
                config.setAccessKeyId((String) ossConfig.get("accessKeyId"));
            }
            if (ossConfig.containsKey("accessKeySecret")) {
                config.setAccessKeySecret((String) ossConfig.get("accessKeySecret"));
            }
            if (ossConfig.containsKey("bucket")) {
                config.setBucket((String) ossConfig.get("bucket"));
            }
        }

        return config;
    }

    /**
     * Load top-level environment configuration from YAML.
     */
    @SuppressWarnings("unchecked")
    public EnvironmentConfig loadEnvironmentConfig() {
        EnvironmentConfig config = new EnvironmentConfig();
        Map<String, Object> yamlConfig = loadYamlConfig();

        if (yamlConfig != null && yamlConfig.containsKey("environment")) {
            Object envObj = yamlConfig.get("environment");
            if (envObj instanceof Map) {
                config.setVariables((Map<String, String>) envObj);
            }
        }

        return config;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadYamlConfig() {
        File configFileObj;
        
        // Handle both relative and absolute paths
        if (configFile == null || configFile.isEmpty()) {
            log.warn("Configuration file path is empty, skipping YAML config loading");
            return null;
        }
        
        // Resolve Maven properties if they weren't already resolved
        String resolvedConfigFile = configFile;
        if (resolvedConfigFile.contains("${project.basedir}")) {
            resolvedConfigFile = resolvedConfigFile.replace("${project.basedir}", project.getBasedir().getAbsolutePath());
            log.debug("Resolved Maven property in config file path: " + resolvedConfigFile);
        }
        
        // If configFile is already an absolute path, use it directly
        File configFileCandidate = new File(resolvedConfigFile);
        if (configFileCandidate.isAbsolute()) {
            configFileObj = configFileCandidate;
        } else {
            // Otherwise, resolve relative to project base directory
            configFileObj = new File(project.getBasedir(), resolvedConfigFile);
        }
        
        log.info("Loading configuration from: " + configFileObj.getAbsolutePath());
        
        if (!configFileObj.exists()) {
            log.warn("Configuration file not found: " + configFileObj.getAbsolutePath());
            log.warn("Project base directory: " + project.getBasedir().getAbsolutePath());
            log.warn("Original config file path: " + configFile);
            log.warn("Resolved config file path: " + resolvedConfigFile);
            return null;
        }
        
        if (!configFileObj.isFile()) {
            log.warn("Configuration path is not a file: " + configFileObj.getAbsolutePath());
            return null;
        }
        
        if (!configFileObj.canRead()) {
            log.warn("Configuration file is not readable: " + configFileObj.getAbsolutePath());
            return null;
        }

        try (InputStream inputStream = new FileInputStream(configFileObj)) {
            Yaml yaml = new Yaml();
            Map<String, Object> config = yaml.load(inputStream);
            if (config != null) {
                log.info("Successfully loaded configuration file with " + config.size() + " top-level keys: " + config.keySet());
            } else {
                log.warn("Configuration file is empty or invalid");
            }
            return config;
        } catch (Exception e) {
            log.error("Failed to load configuration file: " + configFileObj.getAbsolutePath(), e);
            throw new RuntimeException("Failed to load configuration file: " + configFile, e);
        }
    }
}



