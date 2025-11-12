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
package io.agentscope.runtime.protocol.a2a;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.events.InMemoryQueueManager;
import io.a2a.server.requesthandlers.DefaultRequestHandler;
import io.a2a.server.requesthandlers.RequestHandler;
import io.a2a.server.tasks.BasePushNotificationSender;
import io.a2a.server.tasks.InMemoryPushNotificationConfigStore;
import io.a2a.server.tasks.InMemoryTaskStore;
import io.a2a.server.tasks.PushNotificationConfigStore;
import io.a2a.spec.AgentCard;
import io.agentscope.runtime.engine.Runner;

import java.util.concurrent.Executors;

public class AgentHandlerConfiguration {

    private static volatile AgentHandlerConfiguration INSTANCE;

    private final JSONRPCHandler jsonrpcHandler;

    public AgentHandlerConfiguration(Runner runner, AgentCard agentCard) {
        this(new GraphAgentExecutor(runner::streamQuery), agentCard);
    }

    protected AgentHandlerConfiguration(AgentExecutor agentExecutor, AgentCard agentCard) {
        this.jsonrpcHandler = new JSONRPCHandler(agentCard, requestHandler(agentExecutor));
    }

    public static AgentHandlerConfiguration getInstance(Runner runner, AgentCard agentCard) {
        AgentHandlerConfiguration inst = INSTANCE;
        if (inst == null) {
            synchronized (AgentHandlerConfiguration.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AgentHandlerConfiguration(runner, agentCard);
                }
                inst = INSTANCE;
            }
        }
        return inst;
    }

    public JSONRPCHandler jsonrpcHandler() {
        return this.jsonrpcHandler;
    }

    public static RequestHandler requestHandler(AgentExecutor agentExecutor) {
        PushNotificationConfigStore pushConfigStore = new InMemoryPushNotificationConfigStore();
        InMemoryTaskStore inMemoryTaskStore = new InMemoryTaskStore();
        return new DefaultRequestHandler(agentExecutor, inMemoryTaskStore, new InMemoryQueueManager(inMemoryTaskStore),
                pushConfigStore, new BasePushNotificationSender(pushConfigStore), Executors.newCachedThreadPool());
    }
}
