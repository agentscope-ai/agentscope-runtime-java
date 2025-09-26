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
package io.agentscope.runtime.autoconfig.deployer.a2a;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.events.InMemoryQueueManager;
import io.a2a.server.events.QueueManager;
import io.a2a.server.requesthandlers.DefaultRequestHandler;
import io.a2a.server.requesthandlers.RequestHandler;
import io.a2a.server.tasks.BasePushNotificationSender;
import io.a2a.server.tasks.InMemoryPushNotificationConfigStore;
import io.a2a.server.tasks.InMemoryTaskStore;
import io.a2a.server.tasks.PushNotificationConfigStore;
import io.a2a.server.tasks.PushNotificationSender;
import io.a2a.server.tasks.TaskStore;
import io.a2a.spec.AgentCard;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Configuration
public class AgentHandlerConfiguration {

	private ExecutorService executor;

	@PostConstruct
	public void init() {
		executor = Executors.newCachedThreadPool();
	}

	@PreDestroy
	public void close() {
		executor.shutdown();
	}

	@Bean
	public Executor executor() {
		return executor;
	}

	@Bean
	public JSONRPCHandler jsonrpcHandler(AgentCard agentCard, RequestHandler requestHandler) {
		return new JSONRPCHandler(agentCard, requestHandler);
	}

	@Bean
	public RequestHandler requestHandler(AgentExecutor agentExecutor, TaskStore taskStore, QueueManager queueManager,
			PushNotificationConfigStore pushConfigStore, PushNotificationSender pushSender) {
		return new DefaultRequestHandler(agentExecutor, taskStore, queueManager, pushConfigStore, pushSender, executor);
	}

	@Bean
	public TaskStore taskStore() {
		return new InMemoryTaskStore();
	}

	@Bean
	public QueueManager queueManager() {
		return new InMemoryQueueManager();
	}

	@Bean
	public PushNotificationConfigStore pushConfigStore() {
		return new InMemoryPushNotificationConfigStore();
	}

	@Bean
	public PushNotificationSender pushSender(PushNotificationConfigStore pushConfigStore) {
		return new BasePushNotificationSender(pushConfigStore);
	}

}
