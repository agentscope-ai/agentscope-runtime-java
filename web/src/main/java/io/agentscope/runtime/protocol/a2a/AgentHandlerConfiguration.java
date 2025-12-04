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
import io.agentscope.runtime.protocol.ProtocolConfig;
import org.springframework.beans.factory.ObjectProvider;

import java.lang.reflect.Field;
import java.util.concurrent.Executors;

public class AgentHandlerConfiguration {

    private static volatile AgentHandlerConfiguration INSTANCE;

    private final JSONRPCHandler jsonrpcHandler;

    public AgentHandlerConfiguration(Runner runner, AgentCard agentCard, A2aProtocolConfig a2aProtocolConfig) {
        this(new GraphAgentExecutor(runner::streamQuery), agentCard, a2aProtocolConfig);
    }

    protected AgentHandlerConfiguration(AgentExecutor agentExecutor, AgentCard agentCard, A2aProtocolConfig a2aProtocolConfig) {
        this.jsonrpcHandler = new JSONRPCHandler(agentCard, requestHandler(agentExecutor, a2aProtocolConfig));
    }

    public static AgentHandlerConfiguration getInstance(Runner runner, AgentCard agentCard,
                                                        ObjectProvider<ProtocolConfig> protocolConfigs) {
        AgentHandlerConfiguration inst = INSTANCE;
        if (inst == null) {
            synchronized (AgentHandlerConfiguration.class) {
                if (INSTANCE == null) {
                    A2aProtocolConfig a2aProtocolConfig = A2aProtocolConfigUtils.getConfigIfAbsent(protocolConfigs);
                    INSTANCE = new AgentHandlerConfiguration(runner, agentCard, a2aProtocolConfig);
                }
                inst = INSTANCE;
            }
        }
        return inst;
    }

    public JSONRPCHandler jsonrpcHandler() {
        return this.jsonrpcHandler;
    }

    public static RequestHandler requestHandler(AgentExecutor agentExecutor, A2aProtocolConfig a2aProtocolConfig) {
        PushNotificationConfigStore pushConfigStore = new InMemoryPushNotificationConfigStore();
        InMemoryTaskStore inMemoryTaskStore = new InMemoryTaskStore();
        DefaultRequestHandler requestHandler = DefaultRequestHandler.create(agentExecutor, inMemoryTaskStore,
                new InMemoryQueueManager(inMemoryTaskStore), pushConfigStore,
                new BasePushNotificationSender(pushConfigStore), Executors.newCachedThreadPool());
        setTimeoutProperties(requestHandler, a2aProtocolConfig);
        return requestHandler;
    }

    /**
     * A2A Server Request Handler don't provider configurable way to set timeout. So temp use reflection to do.
     *
     * <p>
     * If no timeout property setting, the blocking A2A request will return innerError immediately.
     * </p>
     */
    private static void setTimeoutProperties(DefaultRequestHandler requestHandler, A2aProtocolConfig a2aProtocolConfig) {
        try {
            Field field = DefaultRequestHandler.class.getDeclaredField("agentCompletionTimeoutSeconds");
            field.setAccessible(true);
            field.set(requestHandler, a2aProtocolConfig.getAgentCompletionTimeoutSeconds());
            field = DefaultRequestHandler.class.getDeclaredField("consumptionCompletionTimeoutSeconds");
            field.setAccessible(true);
            field.set(requestHandler, a2aProtocolConfig.getConsumptionCompletionTimeoutSeconds());
        } catch (Exception ignored) {
        }
    }
}
