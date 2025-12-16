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

package io.agentscope.runtime.engine.services.sandbox;

import io.agentscope.runtime.engine.services.ServiceWithLifecycleManager;
import io.agentscope.runtime.sandbox.box.AgentBaySandbox;
import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Service for managing sandbox environments.
 *
 * <p>This service provides functionality to connect to sandbox environments,
 * create new environments, and release them. It manages the lifecycle of
 * sandbox instances associated with user sessions.
 *
 * <p><b>Usage Example:</b>
 * <pre>{@code
 * SandboxService sandboxService = new SandboxService("http://localhost:8000", "token");
 * sandboxService.start();
 *
 * // Connect to sandbox for a session
 * Sandbox sandbox = sandboxService.connect("session_123", "user_456", BaseSandbox.class);
 *
 * // Release sandbox
 * sandboxService.release("session_123", "user_456");
 * }</pre>
 */
public class SandboxService extends ServiceWithLifecycleManager {
    private static final Logger logger = LoggerFactory.getLogger(SandboxService.class);

    private SandboxManager managerApi;
    private boolean health = false;

    public SandboxService(SandboxManager sandboxManager) {
       this.managerApi = sandboxManager;
    }

    public SandboxManager getManagerApi() {
        return managerApi;
    }

    @Override
    public void start() {
		if (managerApi != null) {
			managerApi.start();
		}
        health = true;
    }

    @Override
    public void stop() {
        health = false;
        if (managerApi != null) {
            managerApi.close();
			managerApi = null;
		}
    }

    @Override
    public boolean health() {
        return health;
    }

    /**
     * Connect to sandbox environment for a session.
     *
     * @param sessionId The session ID
     * @param userId Optional user ID
     * @param sandboxType sandbox type to connect to
     * @return List of sandbox instances
     */
    public Sandbox connect(
            String sessionId,
            String userId,
            Class<? extends Sandbox> sandboxType) {
		try {
			return sandboxType.getConstructor(
					SandboxManager.class,
					String.class,
					String.class
			).newInstance(managerApi, userId, sessionId);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

    public Sandbox connect(
            String sessionId,
            String userId,
            Class<? extends Sandbox> sandboxType,
            String imageId) {
        try {
            if(sandboxType!= AgentBaySandbox.class){
                logger.warn("The imageId parameter is only applicable to AgentBaySandbox. Ignoring imageId for other sandbox types.");
                return sandboxType.getConstructor(
                        SandboxManager.class,
                        String.class,
                        String.class
                ).newInstance(managerApi, userId, sessionId);
            }
            return sandboxType.getConstructor(
                    SandboxManager.class,
                    String.class,
                    String.class,
                    String.class
            ).newInstance(managerApi, userId, sessionId, imageId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Sandbox connect(
            String sessionId,
            String userId,
            Class<? extends Sandbox> sandboxType,
            String imageId,
            Map<String, String> labels) {
        try {
            if(sandboxType!= AgentBaySandbox.class){
                logger.warn("The imageId parameter is only applicable to AgentBaySandbox. Ignoring imageId for other sandbox types.");
                return sandboxType.getConstructor(
                        SandboxManager.class,
                        String.class,
                        String.class
                ).newInstance(managerApi, userId, sessionId);
            }
            return sandboxType.getConstructor(
                    SandboxManager.class,
                    String.class,
                    String.class,
                    String.class,
                    Map.class
            ).newInstance(managerApi, userId, sessionId, imageId, labels);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}

