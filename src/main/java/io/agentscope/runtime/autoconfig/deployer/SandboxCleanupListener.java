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
package io.agentscope.runtime.autoconfig.deployer;

import io.agentscope.runtime.engine.Runner;
import io.agentscope.runtime.sandbox.tools.SandboxTool;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;

import java.util.logging.Logger;

/**
 * Spring Boot shutdown listener for cleaning up all sandbox containers when the application shuts down
 */
@Component
public class SandboxCleanupListener implements ApplicationListener<ContextClosedEvent> {
    Logger logger = Logger.getLogger(SandboxCleanupListener.class.getName());

    @Override
    public void onApplicationEvent(@NonNull ContextClosedEvent event) {
        logger.info("Application is shutting down, starting sandbox cleanup...");

        try {
            SandboxManager sandboxManager = Runner.getSandboxManager();
            
            if (sandboxManager != null) {
                logger.info("Found shared SandboxManager instance, starting cleanup...");
                sandboxManager.cleanupAllSandboxes();
                logger.info("Sandbox cleanup completed successfully");
            } else {
                logger.info("No SandboxManager instance found, nothing to cleanup");
            }
        } catch (Exception e) {
            logger.severe("Error during sandbox cleanup: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
