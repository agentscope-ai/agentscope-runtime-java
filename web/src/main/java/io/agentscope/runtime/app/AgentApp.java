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

package io.agentscope.runtime.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import io.agentscope.runtime.LocalDeployManager;
import io.agentscope.runtime.adapters.AgentHandler;
import io.agentscope.runtime.engine.DeployManager;
import io.agentscope.runtime.engine.Runner;
import io.agentscope.runtime.protocol.ProtocolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

/**
 * AgentApp class represents an application that runs as an agent.
 * 
 * <p>This class serves as the server startup entry point for starting Spring Boot Server,
 * assembling configuration and components (Controller/Endpoint, etc.), and
 * initializing Runner (which proxies AgentAdapter).</p>
 * 
 * <p>Key responsibilities:</p>
 * <ul>
 *   <li>Initialize Runner with AgentAdapter</li>
 *   <li>Start Spring Boot Server via DeployManager</li>
 *   <li>Manage application lifecycle</li>
 *   <li>Configure endpoints and protocols</li>
 * </ul>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * AgentAdapter adapter = new AgentScopeAgentAdapter(...);
 * AgentApp app = new AgentApp(adapter);
 * app.deployManager(localDeployManager);
 * app.run("0.0.0.0", 8090);
 * }</pre>
 */
public class AgentApp {
    private static final Logger logger = LoggerFactory.getLogger(AgentApp.class);
    
    private final AgentHandler adapter;
    private volatile Runner runner;
    private DeployManager deployManager;
    
    // Configuration
    private String endpointPath;
    private String host = "0.0.0.0";
    private int port = 8090;
    private boolean stream = true;
    private String responseType = "sse";
    private Consumer<CorsRegistry> corsConfigurer;

    private List<EndpointInfo> customEndpoints = new ArrayList<>();
    private List<ProtocolConfig> protocolConfigs;

    /**
     * Constructor with AgentAdapter and Celery configuration.
     * 
     * @param adapter the AgentAdapter instance to use
     */
    public AgentApp(AgentHandler adapter) {
        if (adapter == null) {
            throw new IllegalArgumentException("AgentAdapter cannot be null");
        }
        this.adapter = adapter;
        
        // Initialize protocol adapters (simplified)
        // In real implementation, these would be actual adapter instances
        this.protocolConfigs = new ArrayList<>();
    }
    
    /**
     * Set the endpoint path for the agent service.
     * 
     * @param endpointPath the endpoint path (default: "/process")
     * @return this AgentApp instance for method chaining
     */
    public AgentApp endpointPath(String endpointPath) {
        this.endpointPath = endpointPath;
        return this;
    }
    
    /**
     * Set the host address to bind to.
     * 
     * @param host the host address (default: "0.0.0.0")
     * @return this AgentApp instance for method chaining
     */
    public AgentApp host(String host) {
        this.host = host;
        return this;
    }
    
    /**
     * Set the port number to serve on.
     * 
     * @param port the port number (default: 8090)
     * @return this AgentApp instance for method chaining
     */
    public AgentApp port(int port) {
        this.port = port;
        return this;
    }
    
    /**
     * Set whether to enable streaming responses.
     * 
     * @param stream true to enable streaming (default: true)
     * @return this AgentApp instance for method chaining
     */
    public AgentApp stream(boolean stream) {
        this.stream = stream;
        return this;
    }
    
    /**
     * Set the response type.
     * 
     * @param responseType the response type: "sse", "json", or "text" (default: "sse")
     * @return this AgentApp instance for method chaining
     */
    public AgentApp responseType(String responseType) {
        this.responseType = responseType;
        return this;
    }
    
    /**
     * Set the DeployManager to use for deployment.
     * 
     * @param deployManager the DeployManager instance
     * @return this AgentApp instance for method chaining
     */
    public AgentApp deployManager(DeployManager deployManager) {
        this.deployManager = deployManager;
        return this;
    }

    /**
     * Configure Cross-Origin Resource Sharing (CORS).
     *
     * <p>Usage example:</p>
     * <pre>{@code
     * app.cors(registry -> registry.addMapping("/**")
     *         .allowedOrigins("https://example.com")
     *         .allowedMethods("GET", "POST")
     *         .allowCredentials(true));
     * }</pre>
     *
     * @param corsConfigurer consumer to customize {@link CorsRegistry}
     * @return this AgentApp instance for method chaining
     */
    public AgentApp cors(Consumer<CorsRegistry> corsConfigurer) {
        this.corsConfigurer = corsConfigurer;
        return this;
    }
    
    /**
     * Build the Runner instance by proxying the AgentAdapter.
     * 
     * <p>This method creates a Runner instance that proxies calls to the AgentAdapter.</p>
     * 
     * @return this AgentApp instance for method chaining
     */
    public synchronized AgentApp buildRunner() {
        if (this.runner == null) {
            this.runner = new Runner(adapter);
            logger.info("[AgentApp] Runner built with adapter framework type: {}", adapter.getFrameworkType());
        }
        return this;
    }
    
    /**
     * Get the Runner instance.
     * 
     * @return the Runner instance, or null if not built yet
     */
    public Runner getRunner() {
        return runner;
    }

    
    /**
     * Register a query handler with optional framework type.
     * 
     * <p>The handler will be called to process agent queries.</p>
     * 
     * <p>Supported framework types: "agentscope", "saa"</p>
     * 
     * <p>Usage example:</p>
     * <pre>{@code
     * AgentApp app = new AgentApp(adapter);
     * app.query("agentscope", (request) -> {
     *     // Process the request and return response
     *     return processAgentRequest(request);
     * });
     * }</pre>
     * 
     * @param framework the framework type (must be one of: agentscope, saa, langchain4j)
     * @param handler the query handler function that takes a request map and returns a response
     * @return this AgentApp instance for method chaining
     * @throws IllegalArgumentException if framework type is not supported
     */
    public AgentApp query(String framework, Function<Map<String, Object>, Object> handler) {
        if (framework == null || framework.isEmpty()) {
            framework = "agentscope"; // Default framework
        }
        
        // Validate framework type
        List<String> allowedFrameworks = Arrays.asList("agentscope", "saa");
        if (!allowedFrameworks.contains(framework.toLowerCase())) {
            throw new IllegalArgumentException(
                String.format("framework must be one of %s", allowedFrameworks)
            );
        }

        return this;
    }
    
    /**
     * Register a query handler with default framework type ("agentscope").
     * 
     * @param handler the query handler function
     * @return this AgentApp instance for method chaining
     */
    public AgentApp query(java.util.function.Function<Map<String, Object>, Object> handler) {
        return query("agentscope", handler);
    }
    
    /**
     * Run the application by starting the Spring Boot Server.
     * 
     * <p>This method corresponds to AgentApp.run() in the Python version.
     * It builds the runner, initializes and starts it, then deploys via DeployManager.</p>
     * 
     * <p>If no DeployManager is set, this method will throw an IllegalStateException.
     * You should set a DeployManager (e.g., LocalDeployManager) before calling this method.</p>
     * 
     * @return a CompletableFuture that completes when the server is running
     * @throws IllegalStateException if DeployManager is not set
     */
    public void run() {
        run(host, port);
    }

    public void run(int port) {
        run("0.0.0.0", port);
    }
    
    /**
     * Run the application with specified host and port.
     * 
     * @param host the host address to bind to
     * @param port the port number to serve on
     * @return a CompletableFuture that completes when the server is running
     * @throws IllegalStateException if DeployManager is not set
     */
    public void run(String host, int port) {
        if (deployManager == null) {
            this.deployManager = LocalDeployManager.builder()
                    .host(host)
                    .port(port)
                    .endpointName(endpointPath)
                    .protocolConfigs(protocolConfigs)
                    .corsConfigurer(corsConfigurer)
                    .build();
        }
        
        buildRunner();
        logger.info("[AgentApp] Starting AgentApp with endpoint: {}, host: {}, port: {}", endpointPath, host, port);

        // Deploy via DeployManager
        deployManager.deploy(runner);
        logger.info("[AgentApp] AgentApp started successfully on {}:{}{}", host, port, endpointPath);
    }

    /**
     * Register custom endpoint.
     */
    public AgentApp endpoint(String path, List<String> methods, Function<Map<String, Object>, Object> handler) {
        if (methods == null || methods.isEmpty()) {
            methods = Arrays.asList("POST");
        }
        
        EndpointInfo endpointInfo = new EndpointInfo();
        endpointInfo.path = path;
        endpointInfo.handler = handler;
        endpointInfo.methods = methods;
        customEndpoints.add(endpointInfo);
        
        return this;
    }
    
    // Getters and setters
    public String getEndpointPath() {
        return endpointPath;
    }
    
    public void setEndpointPath(String endpointPath) {
        this.endpointPath = endpointPath;
    }
    
    public String getHost() {
        return host;
    }
    
    public int getPort() {
        return port;
    }
    
    public String getResponseType() {
        return responseType;
    }
    
    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }
    
    public boolean isStream() {
        return stream;
    }
    
    public void setStream(boolean stream) {
        this.stream = stream;
    }
    
    public List<EndpointInfo> getCustomEndpoints() {
        return customEndpoints;
    }

    /**
     * Endpoint information.
     */
    public static class EndpointInfo {
        public String path;
        public Function<Map<String, Object>, Object> handler;
        public List<String> methods;
    }
}

