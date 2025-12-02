package io.agentscope.runtime.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import io.agentscope.runtime.LocalDeployManager;
import io.agentscope.runtime.adapters.AgentAdapter;
import io.agentscope.runtime.engine.DeployManager;
import io.agentscope.runtime.engine.Runner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AgentApp class represents an application that runs as an agent.
 * 
 * <p>This class corresponds to the AgentApp class in agent_app.py of the Python version.
 * It serves as the server startup entry point for starting Spring Boot Server,
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
    
    private final AgentAdapter adapter;
    private Runner runner;
    private DeployManager deployManager;
    
    // Configuration
    private String endpointPath = "/process";
    private String host = "0.0.0.0";
    private int port = 8090;
    private boolean stream = true;
    private String responseType = "sse";
    private Class<?> requestModel = io.agentscope.runtime.engine.schemas.AgentRequest.class;
    private Runnable beforeStart;
    private Runnable afterFinish;
    private boolean enableEmbeddedWorker = false;
    
    // Lifecycle handlers (corresponds to Python's @app.init(), @app.query(), @app.shutdown())
    private Runnable initHandler;
    private java.util.function.Function<Map<String, Object>, Object> queryHandler;
    private Runnable shutdownHandler;
    private String frameworkType; // Framework type for query handler (agentscope, autogen, agno, langgraph)
    
    private List<EndpointInfo> customEndpoints = new ArrayList<>();
    private List<Object> protocolAdapters = new ArrayList<>();
    
    /**
     * Constructor with AgentAdapter.
     * 
     * @param adapter the AgentAdapter instance to use
     */
    public AgentApp(AgentAdapter adapter) {
        this(adapter, null, null);
    }
    
    /**
     * Constructor with AgentAdapter and Celery configuration.
     * 
     * @param adapter the AgentAdapter instance to use
     * @param brokerUrl the Celery broker URL
     * @param backendUrl the Celery backend URL
     */
    public AgentApp(AgentAdapter adapter, String brokerUrl, String backendUrl) {
        if (adapter == null) {
            throw new IllegalArgumentException("AgentAdapter cannot be null");
        }
        this.adapter = adapter;
        
        // Initialize protocol adapters (simplified)
        // In real implementation, these would be actual adapter instances
        this.protocolAdapters = new ArrayList<>();
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
     * Build the Runner instance by proxying the AgentAdapter.
     * 
     * <p>This method corresponds to _build_runner() in the Python version.
     * It creates a Runner instance that proxies calls to the AgentAdapter.</p>
     * 
     * @return this AgentApp instance for method chaining
     */
    public AgentApp buildRunner() {
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
        buildRunner();
        return runner;
    }
    
    /**
     * Register an initialization handler.
     * 
     * <p>This method corresponds to the @app.init() decorator in Python's AgentApp.
     * The handler will be called during application startup, before the runner starts.</p>
     * 
     * <p>Usage example:</p>
     * <pre>{@code
     * AgentApp app = new AgentApp(adapter);
     * app.init(() -> {
     *     // Initialize resources, connect to services, etc.
     *     logger.info("Application initialized");
     * });
     * }</pre>
     * 
     * @param handler the initialization handler (can be null)
     * @return this AgentApp instance for method chaining
     */
    public AgentApp init(Runnable handler) {
        this.initHandler = handler;
        return this;
    }
    
    /**
     * Register a query handler with optional framework type.
     * 
     * <p>This method corresponds to the @app.query(framework="agentscope") decorator 
     * in Python's AgentApp. The handler will be called to process agent queries.</p>
     * 
     * <p>Supported framework types: "agentscope", "autogen", "agno", "langgraph"</p>
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
     * @param framework the framework type (must be one of: agentscope, autogen, agno, langgraph)
     * @param handler the query handler function that takes a request map and returns a response
     * @return this AgentApp instance for method chaining
     * @throws IllegalArgumentException if framework type is not supported
     */
    public AgentApp query(String framework, Function<Map<String, Object>, Object> handler) {
        if (framework == null || framework.isEmpty()) {
            framework = "agentscope"; // Default framework
        }
        
        // Validate framework type (corresponds to Python's allowed_frameworks check)
        List<String> allowedFrameworks = Arrays.asList("agentscope", "autogen", "agno", "langgraph");
        if (!allowedFrameworks.contains(framework.toLowerCase())) {
            throw new IllegalArgumentException(
                String.format("framework must be one of %s", allowedFrameworks)
            );
        }
        
        this.frameworkType = framework.toLowerCase();
        this.queryHandler = handler;
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
     * Register a shutdown handler.
     * 
     * <p>This method corresponds to the @app.shutdown() decorator in Python's AgentApp.
     * The handler will be called during application shutdown, after the runner stops.</p>
     * 
     * <p>Usage example:</p>
     * <pre>{@code
     * AgentApp app = new AgentApp(adapter);
     * app.shutdown(() -> {
     *     // Clean up resources, close connections, etc.
     *     logger.info("Application shutting down");
     * });
     * }</pre>
     * 
     * @param handler the shutdown handler (can be null)
     * @return this AgentApp instance for method chaining
     */
    public AgentApp shutdown(Runnable handler) {
        this.shutdownHandler = handler;
        return this;
    }
    
    /**
     * Set the before_start callback.
     * 
     * <p>This corresponds to the before_start parameter in Python's AgentApp.
     * The callback will be called before the FastAPI server starts.</p>
     * 
     * @param beforeStart the callback to execute before server starts
     * @return this AgentApp instance for method chaining
     */
    public AgentApp beforeStart(Runnable beforeStart) {
        this.beforeStart = beforeStart;
        return this;
    }
    
    /**
     * Set the after_finish callback.
     * 
     * <p>This corresponds to the after_finish parameter in Python's AgentApp.
     * The callback will be called after the FastAPI server finishes.</p>
     * 
     * @param afterFinish the callback to execute after server finishes
     * @return this AgentApp instance for method chaining
     */
    public AgentApp afterFinish(Runnable afterFinish) {
        this.afterFinish = afterFinish;
        return this;
    }
    
    /**
     * Initialize the application by initializing the adapter.
     * 
     * <p>This method corresponds to calling init_handler in the Python version.
     * It will call the registered init handler if one exists.</p>
     * 
     * @return a CompletableFuture that completes when initialization is done
     */
    public CompletableFuture<Void> init() {
        buildRunner();
        
        // Call registered init handler if exists (corresponds to Python's init_handler call)
        if (initHandler != null) {
            try {
                initHandler.run();
            } catch (Exception e) {
                logger.warn("[AgentApp] Exception in init handler: {}", e.getMessage(), e);
            }
        }
        
        return runner.init();
    }
    
    /**
     * Start the application by starting the adapter.
     * 
     * <p>This method corresponds to calling start() in the Python version.</p>
     * 
     * @return a CompletableFuture that completes when the application is started
     */
    public CompletableFuture<Void> start() {
        buildRunner();
        return runner.start();
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
    public CompletableFuture<Void> run() {
        return run(host, port);
    }
    
    /**
     * Run the application with specified host and port.
     * 
     * @param host the host address to bind to
     * @param port the port number to serve on
     * @return a CompletableFuture that completes when the server is running
     * @throws IllegalStateException if DeployManager is not set
     */
    public CompletableFuture<Void> run(String host, int port) {
        this.host = host;
        this.port = port;
        
        if (deployManager == null) {
            this.deployManager = LocalDeployManager.builder().port(10001).build();
        }
        
        buildRunner();
        
        logger.info("[AgentApp] Starting AgentApp with endpoint: {}, host: {}, port: {}", 
            endpointPath, host, port);
        
        // Initialize and start the runner
        return init()
            .thenCompose(v -> start())
            .thenRun(() -> {
                // Deploy via DeployManager
                deployManager.deploy(runner);
                logger.info("[AgentApp] AgentApp started successfully on {}:{}{}", 
                    host, port, endpointPath);
            });
    }
    
    /**
     * Stop the application by stopping the runner.
     * 
     * @return a CompletableFuture that completes when the application is stopped
     */
    public CompletableFuture<Void> stop() {
        if (runner != null) {
            return runner.stop();
        }
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Shutdown the application by shutting down the runner.
     * 
     * <p>This method will call the registered shutdown handler if one exists,
     * corresponding to Python's shutdown_handler call.</p>
     * 
     * @return a CompletableFuture that completes when shutdown is done
     */
    public CompletableFuture<Void> shutdown() {
        CompletableFuture<Void> shutdownFuture = CompletableFuture.completedFuture(null);
        
        if (runner != null) {
            shutdownFuture = runner.shutdown();
        }
        
        // Call registered shutdown handler if exists (corresponds to Python's shutdown_handler call)
        if (shutdownHandler != null) {
            try {
                shutdownHandler.run();
            } catch (Exception e) {
                logger.warn("[AgentApp] Exception in shutdown handler: {}", e.getMessage(), e);
            }
        }
        
        return shutdownFuture;
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
     * Get the registered init handler.
     * 
     * @return the init handler, or null if not set
     */
    public Runnable getInitHandler() {
        return initHandler;
    }
    
    /**
     * Get the registered query handler.
     * 
     * @return the query handler, or null if not set
     */
    public java.util.function.Function<Map<String, Object>, Object> getQueryHandler() {
        return queryHandler;
    }
    
    /**
     * Get the registered shutdown handler.
     * 
     * @return the shutdown handler, or null if not set
     */
    public Runnable getShutdownHandler() {
        return shutdownHandler;
    }
    
    /**
     * Get the framework type for the query handler.
     * 
     * @return the framework type, or null if not set
     */
    public String getFrameworkType() {
        return frameworkType;
    }
    
    /**
     * Get the before_start callback.
     * 
     * @return the before_start callback, or null if not set
     */
    public Runnable getBeforeStart() {
        return beforeStart;
    }
    
    /**
     * Get the after_finish callback.
     * 
     * @return the after_finish callback, or null if not set
     */
    public Runnable getAfterFinish() {
        return afterFinish;
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

