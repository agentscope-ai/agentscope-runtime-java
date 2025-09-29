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
package io.agentscope.runtime.engine.a2a;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.Executors;

import io.a2a.spec.AgentCapabilities;
import io.agentscope.runtime.engine.Runner;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.events.InMemoryQueueManager;
import io.a2a.server.requesthandlers.DefaultRequestHandler;
import io.a2a.server.requesthandlers.RequestHandler;
import io.a2a.server.tasks.BasePushNotificationSender;
import io.a2a.server.tasks.InMemoryPushNotificationConfigStore;
import io.a2a.server.tasks.InMemoryTaskStore;
import io.a2a.server.tasks.PushNotificationConfigStore;
import io.a2a.spec.AgentCard;

public class AgentHandlerConfiguration {

    private static volatile AgentHandlerConfiguration INSTANCE;

    private final JSONRPCHandler jsonrpcHandler;

    public AgentHandlerConfiguration() {
        this(new GraphAgentExecutor(Runner::streamQuery));
    }

    public AgentHandlerConfiguration(AgentExecutor agentExecutor) {
        this.jsonrpcHandler = new JSONRPCHandler(
                createDefaultAgentCard(),
                requestHandler(agentExecutor)
        );
    }

    public static synchronized AgentHandlerConfiguration initialize(AgentExecutor agentExecutor) {
        if (INSTANCE == null) {
            INSTANCE = new AgentHandlerConfiguration(agentExecutor);
        }
        return INSTANCE;
    }

    public static AgentHandlerConfiguration getInstance() {
        AgentHandlerConfiguration inst = INSTANCE;
        if (inst == null) {
            synchronized (AgentHandlerConfiguration.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AgentHandlerConfiguration();
                }
                inst = INSTANCE;
            }
        }
        return inst;
    }

    public static AgentCard createDefaultAgentCard() {
        AgentCapabilities capabilities = createDefaultCapabilities();
        return new AgentCard.Builder()
                .name("agentscope-runtime")
                .description("AgentScope Runtime")
                .url("http://localhost:10001/a2a/")
                .version("1.0.0")
                .protocolVersion("1.0")
                .capabilities(capabilities)
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of())
                .build();
    }

    private static AgentCapabilities createDefaultCapabilities() {
        try {
            Class<?> capsClass = AgentCapabilities.class;

            try {
                Class<?> builderClass = Class.forName(capsClass.getName() + "$Builder");
                Object builder = builderClass.getConstructor().newInstance();
                Method mStreaming = findMethod(builderClass, "streaming", boolean.class);
                if (mStreaming != null) mStreaming.invoke(builder, true);
                Method mPush = findMethod(builderClass, "pushNotifications", boolean.class);
                if (mPush != null) mPush.invoke(builder, false);
                Method build = findMethod(builderClass, "build");
                Object caps = build != null ? build.invoke(builder) : null;
                if (caps != null) return (AgentCapabilities) caps;
            } catch (Throwable ignore) {
            }

            for (Constructor<?> c : capsClass.getDeclaredConstructors()) {
                c.setAccessible(true);
                Class<?>[] pts = c.getParameterTypes();
                Object[] args = new Object[pts.length];
                for (int i = 0; i < pts.length; i++) {
                    if (pts[i].equals(boolean.class) || pts[i].equals(Boolean.class)) args[i] = false;
                    else args[i] = null;
                }
                try {
                    Object inst = c.newInstance(args);
                    return (AgentCapabilities) inst;
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... paramTypes) {
        try {
            return type.getMethod(name, paramTypes);
        } catch (Throwable e) {
            return null;
        }
    }

    public JSONRPCHandler jsonrpcHandler() {
        return this.jsonrpcHandler;
    }

    public static RequestHandler requestHandler(AgentExecutor agentExecutor) {
        PushNotificationConfigStore pushConfigStore = new InMemoryPushNotificationConfigStore();
        return new DefaultRequestHandler(agentExecutor, new InMemoryTaskStore(), new InMemoryQueueManager(),
                pushConfigStore, new BasePushNotificationSender(pushConfigStore), Executors.newCachedThreadPool());
    }
}
