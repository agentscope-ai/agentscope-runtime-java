package io.agentscope.browser;

import io.agentscope.browser.agent.AgentscopeBrowseruseAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import jakarta.annotation.PreDestroy;

/**
 * Spring Boot application for browser agent service
 */
@SpringBootApplication
public class BrowserAgentApplication {

    private static final Logger logger = LoggerFactory.getLogger(BrowserAgentApplication.class);

    private static AgentscopeBrowseruseAgent agentInstance;

    @Bean
    public AgentscopeBrowseruseAgent agentscopeBrowseruseAgent() throws Exception {
        logger.info("Creating AgentscopeBrowseruseAgent bean...");
        agentInstance = new AgentscopeBrowseruseAgent();
        agentInstance.connect();
        logger.info("AgentscopeBrowseruseAgent bean created and connected");
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

