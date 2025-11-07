package io.agentscope.maven.plugin;

import io.agentscope.maven.plugin.config.BuildConfig;
import io.agentscope.maven.plugin.config.K8sConfig;
import io.agentscope.maven.plugin.config.RegistryConfig;
import io.agentscope.maven.plugin.service.DockerBuildService;
import io.agentscope.maven.plugin.service.DockerfileGenerator;
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
public class DockerBuildMojo extends AbstractMojo {

    @Component
    private MavenProject project;

    /**
     * Configuration file path (YAML format)
     */
    @Parameter(property = "docker-build.configFile", defaultValue = "docker-build.yml")
    private String configFile;

    /**
     * Image name (overrides config file)
     */
    @Parameter(property = "docker-build.imageName")
    private String imageName;

    /**
     * Image tag (overrides config file)
     */
    @Parameter(property = "docker-build.imageTag")
    private String imageTag;

    /**
     * Base image for Dockerfile
     */
    @Parameter(property = "docker-build.baseImage", defaultValue = "eclipse-temurin:17-jre")
    private String baseImage;

    /**
     * Container port
     */
    @Parameter(property = "docker-build.port", defaultValue = "8080")
    private int port;

    /**
     * Push to registry after build
     */
    @Parameter(property = "docker-build.push", defaultValue = "false")
    private boolean push;

    /**
     * Deploy to Kubernetes after build
     */
    @Parameter(property = "docker-build.deploy", defaultValue = "false")
    private boolean deploy;

    /**
     * Kubernetes namespace
     */
    @Parameter(property = "docker-build.k8sNamespace", defaultValue = "agentscope-runtime")
    private String k8sNamespace;

    /**
     * Number of replicas for Kubernetes deployment
     */
    @Parameter(property = "docker-build.replicas", defaultValue = "1")
    private int replicas;

    /**
     * Environment variables
     */
    @Parameter
    private Map<String, String> environment;

    /**
     * Build context directory
     */
    @Parameter(property = "docker-build.buildContextDir", defaultValue = "${project.build.directory}/docker-build")
    private String buildContextDir;

    /**
     * Skip plugin execution
     */
    @Parameter(property = "docker-build.skip", defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping docker-build plugin execution");
            return;
        }

        try {
            // Load configuration from file if exists
            ConfigLoader configLoader = new ConfigLoader(project, configFile);
            BuildConfig buildConfig = configLoader.loadBuildConfig();
            RegistryConfig registryConfig = configLoader.loadRegistryConfig();
            K8sConfig k8sConfig = configLoader.loadK8sConfig();

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
            buildConfig.setPort(port);
            buildConfig.setPushToRegistry(push);
            buildConfig.setDeployToK8s(deploy);
            // Resolve Maven properties in buildContextDir
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
                dockerBuildService.pushImage(fullImageName, registryConfig);
                getLog().info("Image pushed successfully");
            }

            // Deploy to Kubernetes if configured
            if (buildConfig.isDeployToK8s()) {
                getLog().info("Deploying to Kubernetes...");
                K8sDeployService k8sDeployService = new K8sDeployService(getLog());
                // Initialize with kubeconfig path if provided
                if (k8sConfig.getKubeconfigPath() != null && !k8sConfig.getKubeconfigPath().isEmpty()) {
                    k8sDeployService.initializeK8sClient(k8sConfig.getKubeconfigPath());
                }
                String deployUrl = k8sDeployService.deploy(
                        fullImageName,
                        buildConfig,
                        k8sConfig
                );
                getLog().info("Deployed successfully. URL: " + deployUrl);
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
}

