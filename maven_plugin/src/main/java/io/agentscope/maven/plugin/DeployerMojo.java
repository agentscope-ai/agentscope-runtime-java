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
import io.agentscope.maven.plugin.service.DockerBuildService;
import io.agentscope.maven.plugin.service.DockerfileGenerator;
import io.agentscope.maven.plugin.service.AgentRunDeployService;
import io.agentscope.maven.plugin.service.ModelStudioDeployService;
import io.agentscope.maven.plugin.service.K8sDeployService;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.Map;

/**
 * Maven plugin goal to build Docker images and deploy to Kubernetes.
 */
@Mojo(name = "build", requiresProject = true, defaultPhase = org.apache.maven.plugins.annotations.LifecyclePhase.PACKAGE)
public class DeployerMojo extends AbstractMojo {

    @Component
    private MavenProject project;

    /**
     * Configuration file path (YAML format)
     */
    @Parameter(property = "deployer.configFile", defaultValue = "deployer.yml")
    private String configFile;

    /**
     * Image name (overrides config file)
     */
    @Parameter(property = "deployer.imageName")
    private String imageName;

    /**
     * Image tag (overrides config file)
     */
    @Parameter(property = "deployer.imageTag")
    private String imageTag;

    /**
     * Base image for Dockerfile
     */
    @Parameter(property = "deployer.baseImage", defaultValue = "eclipse-temurin:17-jre")
    private String baseImage;

    /**
     * Container port
     */
    @Parameter(property = "deployer.port")
    private Integer port;

    /**
     * Push to registry after build
     */
    @Parameter(property = "deployer.push")
    private Boolean push;

    /**
     * Deploy to Kubernetes after build
     */
    @Parameter(property = "deployer.deploy")
    private Boolean deploy;

    /**
     * Kubernetes namespace
     */
    @Parameter(property = "deployer.k8sNamespace", defaultValue = "agentscope-runtime")
    private String k8sNamespace;

    /**
     * Number of replicas for Kubernetes deployment
     */
    @Parameter(property = "deployer.replicas", defaultValue = "1")
    private int replicas;

    /**
     * Deploy to ModelStudio after build
     */
    @Parameter(property = "deployer.deployToModelStudio")
    private Boolean deployToModelStudio;

    @Parameter(property = "deployer.modelStudioRegion")
    private String modelStudioRegion;

    @Parameter(property = "deployer.modelStudioEndpoint")
    private String modelStudioEndpoint;

    @Parameter(property = "deployer.modelStudioWorkspace")
    private String modelStudioWorkspace;

    @Parameter(property = "deployer.modelStudioAccessKeyId")
    private String modelStudioAccessKeyId;

    @Parameter(property = "deployer.modelStudioAccessKeySecret")
    private String modelStudioAccessKeySecret;

    @Parameter(property = "deployer.modelStudioDashscopeKey")
    private String modelStudioDashscopeKey;

    @Parameter(property = "deployer.modelStudioTelemetry", defaultValue = "true")
    private boolean modelStudioTelemetryEnabled;

    @Parameter(property = "deployer.modelStudioDeployName")
    private String modelStudioDeployName;

    @Parameter(property = "deployer.modelStudioAgentId")
    private String modelStudioAgentId;

    @Parameter(property = "deployer.modelStudioAgentDescription")
    private String modelStudioAgentDescription;

    @Parameter(property = "deployer.modelStudioService")
    private String modelStudioService;

    @Parameter(property = "deployer.modelStudioFunction")
    private String modelStudioFunction;

    @Parameter(property = "deployer.modelStudioBucket")
    private String modelStudioBucket;

    @Parameter(property = "deployer.modelStudioMemory")
    private Integer modelStudioMemory;

    @Parameter(property = "deployer.modelStudioTimeout")
    private Integer modelStudioTimeout;

    @Parameter
    private Map<String, String> modelStudioMetadata;

    /**
     * Deploy to AgentRun after build
     */
    @Parameter(property = "deployer.deployToAgentRun")
    private Boolean deployToAgentRun;

    @Parameter(property = "deployer.agentRunRegion")
    private String agentRunRegion;

    @Parameter(property = "deployer.agentRunEndpoint")
    private String agentRunEndpoint;

    @Parameter(property = "deployer.agentRunAccessKeyId")
    private String agentRunAccessKeyId;

    @Parameter(property = "deployer.agentRunAccessKeySecret")
    private String agentRunAccessKeySecret;

    @Parameter(property = "deployer.agentRunPrefix")
    private String agentRunPrefix;

    @Parameter(property = "deployer.agentRunBucket")
    private String agentRunBucket;

    @Parameter(property = "deployer.agentRunCpu")
    private Integer agentRunCpu;

    @Parameter(property = "deployer.agentRunMemory")
    private Integer agentRunMemory;

    @Parameter(property = "deployer.agentRunTimeout")
    private Integer agentRunTimeout;

    @Parameter(property = "deployer.agentRunExecutionRoleArn")
    private String agentRunExecutionRoleArn;

    @Parameter(property = "deployer.agentRunLogProject")
    private String agentRunLogProject;

    @Parameter(property = "deployer.agentRunLogStore")
    private String agentRunLogStore;

    @Parameter(property = "deployer.agentRunNetworkMode")
    private String agentRunNetworkMode;

    @Parameter(property = "deployer.agentRunVpcId")
    private String agentRunVpcId;

    @Parameter(property = "deployer.agentRunSecurityGroupId")
    private String agentRunSecurityGroupId;

    @Parameter(property = "deployer.agentRunVswitchIds")
    private java.util.List<String> agentRunVswitchIds;

    @Parameter(property = "deployer.agentRunSessionConcurrency")
    private Integer agentRunSessionConcurrency;

    @Parameter(property = "deployer.agentRunSessionIdleTimeout")
    private Integer agentRunSessionIdleTimeout;

    @Parameter(property = "deployer.agentRunExistingRuntimeId")
    private String agentRunExistingRuntimeId;

    @Parameter
    private Map<String, String> agentRunMetadata;

    @Parameter(property = "deployer.agentRunLanguage")
    private String agentRunLanguage;

    /**
     * Environment variables
     */
    @Parameter
    private Map<String, String> environment;

    /**
     * Build context directory
     */
    @Parameter(property = "deployer.buildContextDir", defaultValue = "${project.build.directory}/deployer")
    private String buildContextDir;

    /**
     * Skip plugin execution
     */
    @Parameter(property = "deployer.skip", defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping deployer plugin execution");
            return;
        }

        try {
            // Load configuration from file if exists
            ConfigLoader configLoader = new ConfigLoader(project, configFile, getLog());
            BuildConfig buildConfig = configLoader.loadBuildConfig();
            RegistryConfig registryConfig = configLoader.loadRegistryConfig();
            K8sConfig k8sConfig = configLoader.loadK8sConfig();
            ModelStudioConfig modelStudioConfig = configLoader.loadModelStudioConfig();
            AgentRunConfig agentRunConfig = configLoader.loadAgentRunConfig();
            OssConfig ossConfig = configLoader.loadOssConfig();
            EnvironmentConfig environmentConfig = configLoader.loadEnvironmentConfig();

            // Apply top-level OSS config to AgentRun if not already set
            applyOssConfigToAgentRun(ossConfig, agentRunConfig);

            // Merge top-level environment into build configuration so Docker image picks them up.
            // Priority: top-level overrides build-level when keys clash.
            buildConfig.setEnvironment(
                    mergeEnvironments(buildConfig.getEnvironment(), environmentConfig.getVariables(), null)
            );

            // Merge top-level environment with agentrun-specific environment
            // Priority: agentrun.environment > environment (top-level) > build.environment
            agentRunConfig.setEnvironment(
                    mergeEnvironments(buildConfig.getEnvironment(), environmentConfig.getVariables(), agentRunConfig.getEnvironment())
            );

            // Override with command line parameters
            if (imageName != null && !imageName.isEmpty()) {
                buildConfig.setImageName(imageName);
            }
            if (imageTag != null && !imageTag.isEmpty()) {
                buildConfig.setImageTag(imageTag);
            }
            if (baseImage != null && !baseImage.isEmpty()) {
                buildConfig.setBaseImage(baseImage);
            }
            if (port != null){
                buildConfig.setPort(port);
            }
            if (push != null) {
                buildConfig.setPushToRegistry(push);
            }
            if (deploy != null) {
                buildConfig.setDeployToK8s(deploy);
            }
            if (deployToModelStudio != null) {
                buildConfig.setDeployToModelStudio(deployToModelStudio);
            }
            if (deployToAgentRun != null) {
                buildConfig.setDeployToAgentRun(deployToAgentRun);
            }
            String resolvedBuildContextDir = buildContextDir
                    .replace("${project.build.directory}", project.getBuild().getDirectory());
            buildConfig.setBuildContextDir(resolvedBuildContextDir);

            if (k8sNamespace != null && !k8sNamespace.isEmpty()) {
                k8sConfig.setK8sNamespace(k8sNamespace);
            }
            k8sConfig.setReplicas(replicas);

            if (environment != null && !environment.isEmpty()) {
                buildConfig.setEnvironment(environment);
            }

            if (modelStudioRegion != null && !modelStudioRegion.isEmpty()) {
                modelStudioConfig.setRegion(modelStudioRegion);
            }
            if (modelStudioEndpoint != null && !modelStudioEndpoint.isEmpty()) {
                modelStudioConfig.setEndpoint(modelStudioEndpoint);
            }
            if (modelStudioWorkspace != null && !modelStudioWorkspace.isEmpty()) {
                modelStudioConfig.setWorkspaceId(modelStudioWorkspace);
            }
            if (modelStudioAccessKeyId != null && !modelStudioAccessKeyId.isEmpty()) {
                modelStudioConfig.setAccessKeyId(modelStudioAccessKeyId);
            }
            if (modelStudioAccessKeySecret != null && !modelStudioAccessKeySecret.isEmpty()) {
                modelStudioConfig.setAccessKeySecret(modelStudioAccessKeySecret);
            }
            if (modelStudioDashscopeKey != null && !modelStudioDashscopeKey.isEmpty()) {
                modelStudioConfig.setDashscopeApiKey(modelStudioDashscopeKey);
            }
            modelStudioConfig.setTelemetryEnabled(modelStudioTelemetryEnabled);
            if (modelStudioDeployName != null && !modelStudioDeployName.isEmpty()) {
                modelStudioConfig.setDeployName(modelStudioDeployName);
            }
            if (modelStudioAgentId != null && !modelStudioAgentId.isEmpty()) {
                modelStudioConfig.setAgentId(modelStudioAgentId);
            }
            if (modelStudioAgentDescription != null && !modelStudioAgentDescription.isEmpty()) {
                modelStudioConfig.setAgentDescription(modelStudioAgentDescription);
            }
            if (modelStudioService != null && !modelStudioService.isEmpty()) {
                modelStudioConfig.setServiceName(modelStudioService);
            }
            if (modelStudioFunction != null && !modelStudioFunction.isEmpty()) {
                modelStudioConfig.setFunctionName(modelStudioFunction);
            }
            if (modelStudioBucket != null && !modelStudioBucket.isEmpty()) {
                modelStudioConfig.setArtifactBucket(modelStudioBucket);
            }
            if (modelStudioMemory != null) {
                modelStudioConfig.setMemorySize(modelStudioMemory);
            }
            if (modelStudioTimeout != null) {
                modelStudioConfig.setTimeoutSeconds(modelStudioTimeout);
            }
            if (modelStudioMetadata != null && !modelStudioMetadata.isEmpty()) {
                modelStudioConfig.setMetadata(modelStudioMetadata);
            }

            if (agentRunRegion != null && !agentRunRegion.isEmpty()) {
                agentRunConfig.setRegion(agentRunRegion);
            }
            if (agentRunEndpoint != null && !agentRunEndpoint.isEmpty()) {
                agentRunConfig.setEndpoint(agentRunEndpoint);
            }
            if (agentRunAccessKeyId != null && !agentRunAccessKeyId.isEmpty()) {
                agentRunConfig.setAccessKeyId(agentRunAccessKeyId);
            }
            if (agentRunAccessKeySecret != null && !agentRunAccessKeySecret.isEmpty()) {
                agentRunConfig.setAccessKeySecret(agentRunAccessKeySecret);
            }
            if (agentRunPrefix != null && !agentRunPrefix.isEmpty()) {
                agentRunConfig.setRuntimeNamePrefix(agentRunPrefix);
            }
            if (agentRunBucket != null && !agentRunBucket.isEmpty()) {
                agentRunConfig.setArtifactBucket(agentRunBucket);
            }
            if (agentRunCpu != null) {
                agentRunConfig.setCpu(agentRunCpu);
            }
            if (agentRunMemory != null) {
                agentRunConfig.setMemorySize(agentRunMemory);
            }
            if (agentRunTimeout != null) {
                agentRunConfig.setTimeoutSeconds(agentRunTimeout);
            }
            if (agentRunExecutionRoleArn != null && !agentRunExecutionRoleArn.isEmpty()) {
                agentRunConfig.setExecutionRoleArn(agentRunExecutionRoleArn);
            }
            if (agentRunLogProject != null && !agentRunLogProject.isEmpty()) {
                agentRunConfig.setLogProject(agentRunLogProject);
            }
            if (agentRunLogStore != null && !agentRunLogStore.isEmpty()) {
                agentRunConfig.setLogStore(agentRunLogStore);
            }
            if (agentRunNetworkMode != null && !agentRunNetworkMode.isEmpty()) {
                agentRunConfig.setNetworkMode(agentRunNetworkMode);
            }
            if (agentRunVpcId != null && !agentRunVpcId.isEmpty()) {
                agentRunConfig.setVpcId(agentRunVpcId);
            }
            if (agentRunSecurityGroupId != null && !agentRunSecurityGroupId.isEmpty()) {
                agentRunConfig.setSecurityGroupId(agentRunSecurityGroupId);
            }
            if (agentRunVswitchIds != null && !agentRunVswitchIds.isEmpty()) {
                agentRunConfig.setVswitchIds(agentRunVswitchIds);
            }
            if (agentRunSessionConcurrency != null) {
                agentRunConfig.setSessionConcurrencyLimit(agentRunSessionConcurrency);
            }
            if (agentRunSessionIdleTimeout != null) {
                agentRunConfig.setSessionIdleTimeoutSeconds(agentRunSessionIdleTimeout);
            }
            if (agentRunExistingRuntimeId != null && !agentRunExistingRuntimeId.isEmpty()) {
                agentRunConfig.setExistingRuntimeId(agentRunExistingRuntimeId);
            }
            if (agentRunMetadata != null && !agentRunMetadata.isEmpty()) {
                agentRunConfig.setMetadata(agentRunMetadata);
            }
            if (agentRunLanguage != null && !agentRunLanguage.isEmpty()) {
                agentRunConfig.setCodeLanguage(agentRunLanguage);
            }

            // Use project artifact name and version as defaults
            if (buildConfig.getImageName() == null || buildConfig.getImageName().isEmpty()) {
                buildConfig.setImageName(project.getArtifactId());
            }
            if (buildConfig.getImageTag() == null || buildConfig.getImageTag().isEmpty()) {
                buildConfig.setImageTag(project.getVersion());
            }

            getLog().info("Building Docker image: " + buildConfig.getImageName() + ":" + buildConfig.getImageTag());
            getLog().info("Base image: " + buildConfig.getBaseImage());
            getLog().info("Port: " + buildConfig.getPort());

            // Find the JAR file
            File jarFile = findJarFile();
            if (jarFile == null || !jarFile.exists()) {
                throw new MojoExecutionException("JAR file not found. Please run 'mvn package' first.");
            }

            getLog().info("Using JAR file: " + jarFile.getAbsolutePath());

            // Generate Dockerfile
            DockerfileGenerator dockerfileGenerator = new DockerfileGenerator(getLog());
            File dockerfile = dockerfileGenerator.generate(
                    buildConfig.getBaseImage(),
                    jarFile.getName(),
                    buildConfig.getPort(),
                    buildConfig.getEnvironment(),
                    new File(buildConfig.getBuildContextDir())
            );

            // Build Docker image
            DockerBuildService dockerBuildService = new DockerBuildService(getLog());
            String fullImageName = dockerBuildService.buildImage(
                    buildConfig,
                    jarFile,
                    dockerfile
            );

            getLog().info("Docker image built successfully: " + fullImageName);

            // Push to registry if configured
            if (buildConfig.isPushToRegistry()) {
                getLog().info("Pushing image to registry...");
                fullImageName = dockerBuildService.pushImage(fullImageName, registryConfig);
                getLog().info("Image pushed successfully");
            }

            // Deploy to Kubernetes if configured
            if (buildConfig.isDeployToK8s()) {
                getLog().info("Deploying to Kubernetes...");
                K8sDeployService k8sDeployService = new K8sDeployService(getLog());
                String deployUrl = k8sDeployService.deploy(
                        fullImageName,
                        buildConfig,
                        k8sConfig
                );
                getLog().info("Deployed successfully. URL: " + deployUrl);
            }

            if (buildConfig.isDeployToModelStudio()) {
                getLog().info("Deploying artifact to ModelStudio (simulated)...");
                ModelStudioDeployService service = new ModelStudioDeployService(getLog());
                ModelStudioDeployService.ModelStudioDeployResult result = service.deploy(
                        jarFile,
                        buildConfig,
                        modelStudioConfig
                );
                getLog().info("ModelStudio deployment ID: " + result.getDeploymentId());
                getLog().info("ModelStudio endpoint (simulated): " + result.getEndpoint());
                getLog().info("ModelStudio artifact stored at: " + result.getUploadedArtifact().getAbsolutePath());
            }

            if (buildConfig.isDeployToAgentRun()) {
                getLog().info("Deploying artifact to AgentRun (simulated)...");
                AgentRunDeployService service = new AgentRunDeployService(getLog());
                AgentRunDeployService.AgentRunDeployResult result = service.deploy(
                        jarFile,
                        buildConfig,
                        agentRunConfig
                );
                getLog().info("AgentRun deployment ID: " + result.getDeploymentId());
                getLog().info("AgentRun endpoint (simulated): " + result.getEndpoint());
                getLog().info("AgentRun artifact stored at: " + result.getUploadedArtifact().getAbsolutePath());
            }

        } catch (Exception e) {
            getLog().error("Failed to build Docker image", e);
            throw new MojoExecutionException("Docker build failed", e);
        }
    }

    /**
     * Find the JAR file to package
     */
    private File findJarFile() {
        // Try to find Spring Boot JAR first
        File targetDir = new File(project.getBuild().getDirectory());
        String finalName = project.getBuild().getFinalName();

        // Spring Boot JAR
        File springBootJar = new File(targetDir, finalName + ".jar");
        if (springBootJar.exists()) {
            return springBootJar;
        }

        // Regular JAR
        File regularJar = new File(targetDir, finalName + ".jar");
        if (regularJar.exists()) {
            return regularJar;
        }

        // Try to find any JAR in target directory
        File[] files = targetDir.listFiles((dir, name) -> name.endsWith(".jar") && !name.endsWith("-sources.jar"));
        if (files != null && files.length > 0) {
            return files[0];
        }

        return null;
    }

    /**
     * Apply top-level OSS config to AgentRun config if not already set.
     */
    private void applyOssConfigToAgentRun(OssConfig ossConfig, AgentRunConfig agentRunConfig) {
        if (ossConfig == null) {
            return;
        }
        // Only apply if not already set in agentrun config
        if ((agentRunConfig.getAccessKeyId() == null || agentRunConfig.getAccessKeyId().isEmpty())
                && ossConfig.getAccessKeyId() != null) {
            agentRunConfig.setAccessKeyId(ossConfig.getAccessKeyId());
        }
        if ((agentRunConfig.getAccessKeySecret() == null || agentRunConfig.getAccessKeySecret().isEmpty())
                && ossConfig.getAccessKeySecret() != null) {
            agentRunConfig.setAccessKeySecret(ossConfig.getAccessKeySecret());
        }
        if ((agentRunConfig.getArtifactBucket() == null || agentRunConfig.getArtifactBucket().isEmpty()
                || "agentscope-runtime-agentrun-artifacts".equals(agentRunConfig.getArtifactBucket()))
                && ossConfig.getBucket() != null) {
            agentRunConfig.setArtifactBucket(ossConfig.getBucket());
        }
        // Use OSS region if agentrun region uses default
        if ("cn-hangzhou".equals(agentRunConfig.getRegion()) && ossConfig.getRegion() != null) {
            agentRunConfig.setRegion(ossConfig.getRegion());
        }
    }

    /**
     * Merge environment variables from multiple sources.
     * Priority: specific > top-level > build (later sources override earlier ones)
     */
    private Map<String, String> mergeEnvironments(Map<String, String> buildEnv,
                                                   Map<String, String> topLevelEnv,
                                                   Map<String, String> specificEnv) {
        Map<String, String> merged = new java.util.HashMap<>();
        if (buildEnv != null) {
            merged.putAll(buildEnv);
        }
        if (topLevelEnv != null) {
            merged.putAll(topLevelEnv);
        }
        if (specificEnv != null) {
            merged.putAll(specificEnv);
        }
        return merged;
    }
}

