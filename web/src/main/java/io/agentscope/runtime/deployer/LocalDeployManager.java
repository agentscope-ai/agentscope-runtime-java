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

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;

import java.util.logging.Logger;

import io.agentscope.runtime.engine.DeployManager;
import io.agentscope.runtime.engine.Runner;

public class LocalDeployManager implements DeployManager {
    private ConfigurableApplicationContext applicationContext;
    Logger logger = Logger.getLogger(LocalDeployManager.class.getName());

    @Override
    public synchronized void deployStreaming(String endpointName, Runner runner) {
        if (this.applicationContext != null && this.applicationContext.isActive()) {
            logger.info("Application context is already active, skipping deployment");
            return;
        }

        logger.info("Starting streaming deployment for endpoint: " + endpointName);

        this.applicationContext = new SpringApplicationBuilder()
                .sources(LocalDeployConfig.class)
                .web(WebApplicationType.SERVLET)
                .initializers((GenericApplicationContext ctx) -> {
                    // Register Runner instance as a bean
                    ctx.registerBean(Runner.class, () -> runner);
                    // Register endpoint name as a bean
                    ctx.registerBean("endpointName", String.class, () -> endpointName);
                })
                .run();

        logger.info("Streaming deployment completed for endpoint: " + endpointName);
    }

    /**
     * Shutdown the application and clean up resources
     */
    public synchronized void shutdown() {
        if (this.applicationContext != null && this.applicationContext.isActive()) {
            logger.info("Shutting down LocalDeployManager...");
            this.applicationContext.close();
            this.applicationContext = null;
            logger.info("LocalDeployManager shutdown completed");
        }
    }

    /**
     * Configuration class for local deployment of streaming services.
     * This class enables component scanning for A2A controllers and other Spring components.
     */
    @Configuration
    @EnableAutoConfiguration
    @ComponentScan(basePackages = {
        "io.agentscope.runtime"
    })
    public static class LocalDeployConfig {
    }
}
