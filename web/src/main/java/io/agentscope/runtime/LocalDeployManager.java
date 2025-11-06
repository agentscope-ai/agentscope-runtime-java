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
package io.agentscope.runtime;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;

import java.util.List;
import java.util.logging.Logger;

import io.agentscope.runtime.autoconfigure.DeployProperties;
import io.agentscope.runtime.engine.DeployManager;
import io.agentscope.runtime.engine.Runner;
import io.agentscope.runtime.protocol.Protocol;

public class LocalDeployManager implements DeployManager {
    Logger logger = Logger.getLogger(LocalDeployManager.class.getName());

    private ConfigurableApplicationContext applicationContext;

    private String endpointName;
    private String host;
    private int port;
    private List<Protocol> protocols;

    public LocalDeployManager() {
        this("", 0, "", List.of(Protocol.A2A));
    }

    public LocalDeployManager(String host, int port, String endpointName, List<Protocol> protocols) {
        this.endpointName = endpointName;
        this.host = host;
        this.port = port;
        this.protocols = protocols;
    }

    @Override
    public synchronized void deploy(Runner runner) {
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
                    // Register DeployProperties instance as a bean
                    ctx.registerBean(DeployProperties.class, () -> new DeployProperties(port, host, endpointName));
                    // Scan additional packages based on protocols
                    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(ctx);
                    for (Protocol protocol : protocols) {
                        String packageName = "io.agentscope.runtime.protocol." + protocol.name().toLowerCase();
                        scanner.scan(packageName);
                    }
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
        "io.agentscope.runtime.autoconfigure"
    })
    public static class LocalDeployConfig {
    }
}
