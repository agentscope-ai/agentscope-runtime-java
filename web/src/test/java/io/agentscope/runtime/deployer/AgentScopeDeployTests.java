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

package io.agentscope.runtime.deployer;

import java.io.IOException;
import java.net.URI;
import java.net.URL;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import io.agentscope.runtime.adapters.AgentHandler;
import io.agentscope.runtime.adapters.agentscope.MyAgentScopeAgentHandler;
import io.agentscope.runtime.app.AgentApp;
import io.agentscope.runtime.hook.AbstractAppLifecycleHook;
import io.agentscope.runtime.hook.HookContext;
import io.agentscope.runtime.sandbox.manager.ManagerConfig;
import io.agentscope.runtime.sandbox.manager.SandboxService;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.web.servlet.function.ServerResponse;

import static io.agentscope.runtime.hook.AppLifecycleHook.AFTER_RUN;
import static io.agentscope.runtime.hook.AppLifecycleHook.BEFORE_RUN;

public class AgentScopeDeployTests {

	private static AgentApp app;

	private static AtomicInteger flag;

	private static UUID uuid;

	@BeforeAll
	static void setUp() {
		URL resource = Thread.currentThread().getContextClassLoader().getResource(".env");
		AgentApp app;
		if (Objects.nonNull(resource)) {
			String[] commandLine = new String[2];
			commandLine[0] = "-f";
			commandLine[1] = Objects.requireNonNull(resource).getPath();
			app = new AgentApp(commandLine);
		}
		else {
			// ci can not get the .env file, here it is injected through the system properties
			System.setProperty("agent.app.port", "7779");
			System.setProperty("agent.app.handler.provider.class", "io.agentscope.runtime.deployer.AgentScopeDeployWithCommandLineTests$MyAgentHandlerProvider");
			System.setProperty("agent.app.sandbox.service.provider.class", "io.agentscope.runtime.deployer.AgentScopeDeployWithCommandLineTests$MySandboxServiceProvider");
			System.setProperty("AI_DASHSCOPE_API_KEY", "your key");
			app = new AgentApp(new String[0]);
		}
		AgentScopeDeployTests.app = app;
		flag = new AtomicInteger(0);
		app.hooks(new AbstractAppLifecycleHook() {
			@Override
			public int operation() {
				return BEFORE_RUN | AFTER_RUN | JVM_EXIT;
			}

			@Override
			public void beforeRun(HookContext context) {
				flag.set(flag.get() | BEFORE_RUN);
			}

			@Override
			public void afterRun(HookContext context) {
				flag.set(flag.get() | AFTER_RUN);
			}
		});
		uuid = UUID.randomUUID();
		AgentScopeDeployTests.app.endpoint("/test/get", List.of("GET"), serverRequest -> ServerResponse.ok()
				.body(uuid.toString()));
		AgentScopeDeployTests.app.endpoint("/test/post", List.of("POST"), serverRequest -> ServerResponse.ok()
				.body("OK"));
		FilterRegistrationBean<Filter> filterRegistrationBean = authFilter();
		AgentScopeDeployTests.app.middleware(filterRegistrationBean);
		AgentScopeDeployTests.app.run();

	}

	@NotNull
	private static FilterRegistrationBean<Filter> authFilter() {
		FilterRegistrationBean<Filter> filterRegistrationBean = new FilterRegistrationBean<>();
		filterRegistrationBean.setFilter((servletRequest, servletResponse, filterChain) -> {
			HttpServletRequest request = (HttpServletRequest) servletRequest;
			String token = request.getHeader("token");
			if (Objects.isNull(token) || !token.equals("token")) {
				servletResponse.getWriter().write("Unauthorized");
				return;
			}
			filterChain.doFilter(servletRequest, servletResponse);
		});
		filterRegistrationBean.setBeanName("authFilter");
		filterRegistrationBean.setOrder(-1);
		filterRegistrationBean.setUrlPatterns(List.of("/test/post"));
		return filterRegistrationBean;
	}

	@Test
	void testHooks() {
		Assertions.assertTrue((flag.get() & BEFORE_RUN) > 0);
		Assertions.assertTrue((flag.get() & AFTER_RUN) > 0);
	}

	@Test
	void testCustomizeEndpoint() throws IOException, InterruptedException {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:7779/test/get"))
				.GET()
				.build();
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		Assertions.assertEquals(response.body(), uuid.toString());


	}

	@Test
	void testMiddleware() throws IOException, InterruptedException {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest requestWithoutToken = HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:7779/test/post"))
				.POST(HttpRequest.BodyPublishers.ofString(""))
				.build();
		HttpResponse<String> response1 = client.send(requestWithoutToken, HttpResponse.BodyHandlers.ofString());
		Assertions.assertEquals("Unauthorized", response1.body());

		HttpRequest requestWithToken = HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:7779/test/post"))
				.header("token", "token")  // 添加token请求头
				.POST(HttpRequest.BodyPublishers.ofString(""))
				.build();

		HttpResponse<String> response2 = client.send(requestWithToken, HttpResponse.BodyHandlers.ofString());
		Assertions.assertEquals("OK", response2.body());

	}

	public static class MyAgentHandlerProvider implements AgentApp.AgentHandlerProvider {

		@Override
		public AgentHandler get(Properties properties, AgentApp.ServiceComponentManager serviceComponentManager) {
			MyAgentScopeAgentHandler handler = new MyAgentScopeAgentHandler();
			handler.setStateService(serviceComponentManager.getStateService());
			handler.setSandboxService(serviceComponentManager.getSandboxService());
			handler.setMemoryService(serviceComponentManager.getMemoryService());
			handler.setSessionHistoryService(serviceComponentManager.getSessionHistoryService());
			return handler;
		}
	}

	public static class MySandboxServiceProvider implements AgentApp.SandboxServiceProvider {
		@Override
		public SandboxService get(Properties properties) {
			ManagerConfig managerConfig = ManagerConfig.builder()
					.build();
			return new SandboxService(
					managerConfig);
		}
	}
}
