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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

import io.agentscope.runtime.LocalDeployManager;
import io.agentscope.runtime.adapters.AgentHandler;
import io.agentscope.runtime.engine.DeployManager;
import io.agentscope.runtime.engine.Runner;
import io.agentscope.runtime.engine.services.agent_state.InMemoryStateService;
import io.agentscope.runtime.engine.services.agent_state.StateService;
import io.agentscope.runtime.engine.services.memory.persistence.memory.service.InMemoryMemoryService;
import io.agentscope.runtime.engine.services.memory.persistence.session.InMemorySessionHistoryService;
import io.agentscope.runtime.engine.services.memory.service.MemoryService;
import io.agentscope.runtime.engine.services.memory.service.SessionHistoryService;

import io.agentscope.runtime.lifecycle.AppLifecycleHook;

import io.agentscope.runtime.protocol.ProtocolConfig;

import io.agentscope.runtime.sandbox.manager.ManagerConfig;
import io.agentscope.runtime.sandbox.manager.SandboxService;
import okio.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import picocli.CommandLine;
import picocli.CommandLine.Option;

import static io.agentscope.runtime.lifecycle.AppLifecycleHook.AFTER_RUN;
import static io.agentscope.runtime.lifecycle.AppLifecycleHook.AFTER_STOP;
import static io.agentscope.runtime.lifecycle.AppLifecycleHook.BEFORE_RUN;
import static io.agentscope.runtime.lifecycle.AppLifecycleHook.BEFORE_STOP;
import static io.agentscope.runtime.lifecycle.AppLifecycleHook.JVM_EXIT;


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

	private AgentHandler adapter;
	private volatile Runner runner;
	private DeployManager deployManager;

	// Configuration
	private String endpointPath;
	private String host = "0.0.0.0";
	private int port = 8090;
	private boolean stream = true;
	private String responseType = "sse";
	private Consumer<CorsRegistry> corsConfigurer;

	private final List<EndpointInfo> customEndpoints = new ArrayList<>();
	private final List<AppLifecycleHook> hooks = new ArrayList<>();
	private final AtomicBoolean stopped = new AtomicBoolean(false);
	private List<ProtocolConfig> protocolConfigs;
	private static final String DEFAULT_ENV_FILE_PATH = ".env";
	private static final String CLASS_SUFFIX = ".class";
	private static final String PROVIDER_SUFFIX = ".provider";
	private static final String PROVIDER_CLASS_SUFFIX = PROVIDER_SUFFIX + CLASS_SUFFIX;
	private static final String SERVICE_SUFFIX = ".service";
	private static final String DEFAULT_PREFIX = "agent.app.";
	private static final String AGENT_APP_PORT = DEFAULT_PREFIX + "port";
	private static final String AGENT_HANDLER_PROVIDER = DEFAULT_PREFIX + "handler" + PROVIDER_CLASS_SUFFIX;
	private static final String STATE_SERVICE_PROVIDER = DEFAULT_PREFIX + "state" + SERVICE_SUFFIX + PROVIDER_CLASS_SUFFIX;
	private static final String SESSION_HISTORY_SERVICE_PROVIDER = DEFAULT_PREFIX + "sessionHistory" + SERVICE_SUFFIX + PROVIDER_CLASS_SUFFIX;
	private static final String MEMORY_SERVICE_PROVIDER = DEFAULT_PREFIX + "memory" + SERVICE_SUFFIX + PROVIDER_CLASS_SUFFIX;
	private static final String SANDBOX_SERVICE_PROVIDER = DEFAULT_PREFIX + "sandbox" + SERVICE_SUFFIX + PROVIDER_CLASS_SUFFIX;

	/**
	 * Constructor with command line arguments
	 * @param args Command line arguments
	 */
	public AgentApp(String[] args) {
		String path;
		if (Objects.isNull(args) || args.length == 0) {
			path = DEFAULT_ENV_FILE_PATH;
		}
		else {
			Argument argument = new Argument();
			CommandLine cmd = new CommandLine(argument);
			CommandLine.ParseResult parseResult = cmd.parseArgs(args);
			List<Exception> errors = parseResult.errors();
			if (Objects.nonNull(errors) && !errors.isEmpty()) {
				StringBuilder errMsg = new StringBuilder();
				for (Exception exception : errors) {
					errMsg.append(exception.getMessage()).append(System.lineSeparator());
				}
				logger.warn("Parse config error,cause:{},will be use default configuration file:{}", errMsg, DEFAULT_ENV_FILE_PATH);
				path = DEFAULT_ENV_FILE_PATH;
			}
			else {
				path = argument.configFilePath;
			}
		}

		Path filePath = Path.get(path);
		File file = filePath.toFile();
		Properties properties = new Properties();
		//Inject environment variables, and if a configuration file exists, the corresponding variables will be overwritten
		properties.putAll(System.getenv());
		properties.putAll(System.getProperties());
		if (!file.exists() || file.isDirectory()) {
			logger.warn("File [{}] is not exits or is not a file,please check it", file.getAbsolutePath());
		}
		else {
			try (FileInputStream input = new FileInputStream(file)) {
				properties.load(input);
			}
			catch (IOException e) {
				logger.error("Load config file [{}] error,please check it", file.getAbsolutePath());
				return;
			}
		}
		this.port = port(properties.getOrDefault(AGENT_APP_PORT, this.port), this.port);
		try {
			StateService stateService = component(properties, STATE_SERVICE_PROVIDER, StateServiceProvider.class, new InMemoryStateService());
			SessionHistoryService sessionHistoryService = component(properties, SESSION_HISTORY_SERVICE_PROVIDER, SessionHistoryServiceProvider.class, new InMemorySessionHistoryService());
			MemoryService memoryService = component(properties, MEMORY_SERVICE_PROVIDER, MemoryServiceProvider.class, new InMemoryMemoryService());
			SandboxService sandboxService = component(properties, SANDBOX_SERVICE_PROVIDER, SandboxServiceProvider.class, new SandboxService(ManagerConfig.builder().build()));
			ServiceComponentManager serviceComponentManager = new ServiceComponentManager();
			serviceComponentManager.setStateService(stateService);
			serviceComponentManager.setSessionHistoryService(sessionHistoryService);
			serviceComponentManager.setMemoryService(memoryService);
			serviceComponentManager.setSandboxService(sandboxService);
			this.adapter = agentHandler(properties, serviceComponentManager);
		}
		catch (Exception e) {
			logger.error("Can not init handler,please check it", e);
			return;
		}
		this.protocolConfigs = new ArrayList<>();
	}

	/**
	 *
	 * @param properties Full configuration information
	 * @param key  Service component provider class property
	 * @param superClass Service component provider interface
	 * @param defaultInstance If process error,will be fall back
	 * @return A Service component instance
	 * @param <T> Service component real type
	 * @throws ClassNotFoundException If classLoader can not load service component provider class instance
	 * @throws InstantiationException If reflect process error
	 * @throws IllegalAccessException If reflect process error
	 * @throws InvocationTargetException If reflect process error
	 */
	private <T> T component(Properties properties, String key, Class<? extends ComponentProvider<T>> superClass, T defaultInstance) throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException {
		Object providerName = properties.get(key);
		if (Objects.nonNull(providerName)) {
			Class<?> providerClass = Class.forName(providerName.toString());
			if (!superClass.isAssignableFrom(providerClass)) {
				throw new RuntimeException(String.format("Except class[%s] is a %s implementation,but it is not corresponding implementation", providerName, superClass.getName()));
			}
			Optional<Constructor<?>> optionalConstructor = Arrays.stream(providerClass.getConstructors())
					.filter(e -> e.getParameterCount() == 0).findFirst();
			if (optionalConstructor.isEmpty()) {
				throw new RuntimeException(String.format("Except class[%s] has a none parameter constructor,but it is not exists", providerClass.getName()));
			}
			@SuppressWarnings("unchecked")
			ComponentProvider<T> serviceProvider = (ComponentProvider<T>) optionalConstructor.get().newInstance();
			return serviceProvider.get(properties);
		}
		else {
			return defaultInstance;
		}
	}

	/***
	 *
	 * @param properties Full configuration information
	 * @param serviceComponentManager Service component management, including state, memory, session,sandbox and the like
	 * @return An io.agentscope.runtime.adapters.AgentHandler instance
	 * @throws ClassNotFoundException  If classLoader can not load AgentHandlerProvider instance
	 * @throws InvocationTargetException If reflect process error
	 * @throws InstantiationException If reflect process error
	 * @throws IllegalAccessException If reflect process error
	 */

	private AgentHandler agentHandler(Properties properties, ServiceComponentManager serviceComponentManager) throws ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException {
		Object providerName = properties.get(AgentApp.AGENT_HANDLER_PROVIDER);
		if (Objects.isNull(providerName)) {
			throw new RuntimeException(String.format("Can not init agentHandler because property[%s] is not exists", AgentApp.AGENT_HANDLER_PROVIDER));
		}
		Class<?> providerClass = Class.forName(providerName.toString());
		if (!AgentHandlerProvider.class.isAssignableFrom(providerClass)) {
			throw new RuntimeException(String.format("Except class[%s] is a  implementation", AgentHandlerProvider.class.getName()));
		}
		Optional<Constructor<?>> optionalConstructor = Arrays.stream(providerClass.getConstructors())
				.filter(e -> e.getParameterCount() == 0).findFirst();
		if (optionalConstructor.isEmpty()) {
			throw new RuntimeException(String.format("Except class[%s] has a none parameter constructor,but it is not exists", providerClass.getName()));
		}
		AgentHandlerProvider provider = (AgentHandlerProvider) optionalConstructor.get().newInstance();
		return provider.get(properties, serviceComponentManager);
	}

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
		AgentApp app = this;
		List<AppLifecycleHook> lifecycleHooks = hooks.stream()
				.sorted(Comparator.comparingInt(AppLifecycleHook::priority)).toList();

		Runtime.getRuntime().addShutdownHook(new Thread(() -> lifecycleHooks.stream().filter(e -> ((JVM_EXIT & e.operation()) != 0))
				.forEach(lifecycle -> lifecycle.onJvmExit(app, deployManager))));
		lifecycleHooks.stream()
				.filter(e -> ((BEFORE_RUN & e.operation()) != 0))
				.forEach(lifecycle -> lifecycle.beforeRun(app, deployManager));
		// Deploy via DeployManager
		deployManager.deploy(runner);
		logger.info("[AgentApp] AgentApp started successfully on {}:{}{}", host, port, endpointPath);
		lifecycleHooks.stream()
				.filter(e -> ((AFTER_RUN & e.operation()) != 0))
				.forEach(lifecycle -> lifecycle.afterRun(app, deployManager));
	}

	/**
	 * Register custom endpoint.
	 */
	public AgentApp endpoint(String path, List<String> methods, Function<ServerRequest, ServerResponse> handler) {
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
		public Function<ServerRequest, ServerResponse> handler;
		public List<String> methods;
	}

	private static class Argument {
		@Option(names = {"-f", "--file"})
		public String configFilePath;
	}

	protected int port(Object value, int defaultValue) {
		int validated = validatePort(value);
		if (validated != -1 && validated <= 65535) {
			return validated;
		}
		return defaultValue;
	}


	private int validatePort(Object value) {
		if (value instanceof Integer val) {
			return val;
		}
		try {
			return Integer.parseInt(value.toString());
		}
		catch (Exception e) {
			//ignore
			return -1;
		}
	}

	public interface ComponentProvider<T> {
		T get(Properties properties);
	}

	@FunctionalInterface
	public interface StateServiceProvider extends ComponentProvider<StateService> {
		StateService get(Properties properties);
	}

	@FunctionalInterface
	public interface SessionHistoryServiceProvider extends ComponentProvider<SessionHistoryService> {
		SessionHistoryService get(Properties properties);
	}

	@FunctionalInterface
	public interface MemoryServiceProvider extends ComponentProvider<MemoryService> {
		MemoryService get(Properties properties);
	}

	@FunctionalInterface
	public interface SandboxServiceProvider extends ComponentProvider<SandboxService> {
		SandboxService get(Properties properties);
	}

	@FunctionalInterface
	public interface AgentHandlerProvider {
		AgentHandler get(Properties properties, ServiceComponentManager serviceComponentManager);
	}

	public static class ServiceComponentManager {
		private StateService stateService;
		private SessionHistoryService sessionHistoryService;
		private MemoryService memoryService;
		private SandboxService sandboxService;

		public StateService getStateService() {
			return stateService;
		}

		public void setStateService(StateService stateService) {
			this.stateService = stateService;
		}

		public SessionHistoryService getSessionHistoryService() {
			return sessionHistoryService;
		}

		public void setSessionHistoryService(SessionHistoryService sessionHistoryService) {
			this.sessionHistoryService = sessionHistoryService;
		}

		public MemoryService getMemoryService() {
			return memoryService;
		}

		public void setMemoryService(MemoryService memoryService) {
			this.memoryService = memoryService;
		}

		public SandboxService getSandboxService() {
			return sandboxService;
		}

		public void setSandboxService(SandboxService sandboxService) {
			this.sandboxService = sandboxService;
		}
	}

	public void stop() {
		if (stopped.get()) {
			return;
		}
		synchronized (stopped) {
			if (stopped.get()) {
				return;
			}
			if (Objects.nonNull(deployManager)) {
				AgentApp app = this;
				List<AppLifecycleHook> lifecycleHooks = hooks.stream()
						.sorted(Comparator.comparingInt(AppLifecycleHook::priority)).toList();
				lifecycleHooks.stream()
						.filter(e -> ((BEFORE_STOP & e.operation()) != 0))
						.forEach(lifecycle -> lifecycle.beforeStop(app, deployManager));
				deployManager.undeploy();
				stopped.set(true);
				lifecycleHooks.stream()
						.filter(e -> ((AFTER_STOP & e.operation()) != 0))
						.forEach(lifecycle -> lifecycle.afterStop(app, deployManager));
			}
		}
	}

	public AgentApp hooks(AppLifecycleHook... hooks) {
		if (Objects.nonNull(hooks)) {
			for (AppLifecycleHook hook : hooks) {
				if (!this.hooks.contains(hook)) {
					this.hooks.add(hook);
				}
			}
		}
		return this;
	}
}

