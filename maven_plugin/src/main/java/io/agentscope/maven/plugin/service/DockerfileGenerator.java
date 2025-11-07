package io.agentscope.maven.plugin.service;

import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

/**
 * Generates Dockerfile for Spring Boot applications
 */
public class DockerfileGenerator {

    private final Log log;

    public DockerfileGenerator(Log log) {
        this.log = log;
    }

    /**
     * Generate Dockerfile
     */
    public File generate(String baseImage, String jarFileName, int port, 
                        Map<String, String> environment, File buildContextDir) throws IOException {
        // Create build context directory
        if (!buildContextDir.exists()) {
            buildContextDir.mkdirs();
        }

        File dockerfile = new File(buildContextDir, "Dockerfile");
        
        StringBuilder dockerfileContent = new StringBuilder();
        dockerfileContent.append("FROM ").append(baseImage).append("\n");
        dockerfileContent.append("\n");
        dockerfileContent.append("WORKDIR /app\n");
        dockerfileContent.append("\n");
        dockerfileContent.append("# Copy JAR file\n");
        dockerfileContent.append("COPY ").append(jarFileName).append(" app.jar\n");
        dockerfileContent.append("\n");
        
        // Add environment variables
        if (environment != null && !environment.isEmpty()) {
            dockerfileContent.append("# Environment variables\n");
            for (Map.Entry<String, String> entry : environment.entrySet()) {
                dockerfileContent.append("ENV ").append(entry.getKey())
                    .append("=").append(entry.getValue()).append("\n");
            }
            dockerfileContent.append("\n");
        }
        
        dockerfileContent.append("# Expose port\n");
        dockerfileContent.append("EXPOSE ").append(port).append("\n");
        dockerfileContent.append("\n");
        dockerfileContent.append("# Run application\n");
        dockerfileContent.append("ENTRYPOINT [\"java\", \"-jar\", \"app.jar\"]\n");

        Files.write(dockerfile.toPath(), dockerfileContent.toString().getBytes(StandardCharsets.UTF_8));
        log.info("Generated Dockerfile: " + dockerfile.getAbsolutePath());
        
        return dockerfile;
    }
}

