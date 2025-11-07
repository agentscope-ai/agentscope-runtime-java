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

import io.agentscope.maven.plugin.config.BuildConfig;
import io.agentscope.maven.plugin.config.K8sConfig;
import io.agentscope.maven.plugin.config.RegistryConfig;
import org.apache.maven.project.MavenProject;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;

/**
 * Configuration loader from YAML file
 */
public class ConfigLoader {
    private final MavenProject project;
    private final String configFile;

    public ConfigLoader(MavenProject project, String configFile) {
        this.project = project;
        this.configFile = configFile;
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
    private Map<String, Object> loadYamlConfig() {
        File configFileObj = new File(project.getBasedir(), configFile);
        if (!configFileObj.exists()) {
            return null;
        }

        try (InputStream inputStream = new FileInputStream(configFileObj)) {
            Yaml yaml = new Yaml();
            return yaml.load(inputStream);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load configuration file: " + configFile, e);
        }
    }
}



