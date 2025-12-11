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

package io.agentscope.browser;

import io.agentscope.browser.agent.AgentscopeBrowserUseAgent;
import io.agentscope.runtime.engine.services.agent_state.InMemoryStateService;
import io.agentscope.runtime.engine.services.memory.persistence.memory.service.InMemoryMemoryService;
import io.agentscope.runtime.engine.services.memory.persistence.session.InMemorySessionHistoryService;
import io.agentscope.runtime.engine.services.sandbox.SandboxService;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.client.config.BaseClientConfig;
import io.agentscope.runtime.sandbox.manager.client.config.KubernetesClientConfig;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;

import jakarta.annotation.PreDestroy;

/**
 * Spring Boot application for browser agent service
 */
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
public class BrowserAgentApplication {

    private static final Logger logger = LoggerFactory.getLogger(BrowserAgentApplication.class);

    private static AgentscopeBrowserUseAgent agentInstance;

    @Bean
    public InMemoryStateService stateService() {
        logger.info("Creating InMemoryStateService bean");
        return new InMemoryStateService();
    }

    @Bean
    public InMemorySessionHistoryService sessionHistoryService() {
        logger.info("Creating InMemorySessionHistoryService bean");
        return new InMemorySessionHistoryService();
    }

    @Bean
    public InMemoryMemoryService memoryService() {
        logger.info("Creating InMemoryMemoryService bean");
        return new InMemoryMemoryService();
    }

    @Bean
    public SandboxService sandboxService() {
        logger.info("Creating SandboxService bean");
        BaseClientConfig clientConfig = KubernetesClientConfig.builder().build();
        ManagerConfig managerConfig = ManagerConfig.builder()
                .containerDeployment(clientConfig)
                .build();
        SandboxManager sandboxManager = new SandboxManager(managerConfig);
        return new SandboxService(sandboxManager);
    }

    @Bean
    public AgentscopeBrowserUseAgent agentscopeBrowserUseAgent(
            InMemoryStateService stateService,
            InMemorySessionHistoryService sessionHistoryService,
            InMemoryMemoryService memoryService,
            SandboxService sandboxService) {
        logger.info("Creating AgentscopeBrowserUseAgent bean...");
        agentInstance = new AgentscopeBrowserUseAgent();
        
        // Set services as in AgentScopeDeployExample
        agentInstance.setStateService(stateService);
        agentInstance.setSessionHistoryService(sessionHistoryService);
        agentInstance.setMemoryService(memoryService);
        agentInstance.setSandboxService(sandboxService);
        
        // Note: start() will be called by Runner.start(), not here
        logger.info("AgentscopeBrowserUseAgent bean created");
        return agentInstance;
    }

    @PreDestroy
    public void cleanup() {
        logger.info("Application shutting down, cleaning up resources...");
        if (agentInstance != null) {
            try {
                agentInstance.close();
            } catch (Exception e) {
                logger.error("Error during cleanup", e);
            }
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(BrowserAgentApplication.class, args);
    }
}

