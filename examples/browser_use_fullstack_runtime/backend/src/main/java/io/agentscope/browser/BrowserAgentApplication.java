package io.agentscope.browser;

import io.agentscope.browser.agent.AgentscopeBrowserUseAgent;
import io.agentscope.runtime.engine.Runner;
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

    /**
     * Runner holder to allow AgentscopeBrowserUseAgent to modify the runner instance
     */
    public static class RunnerHolder {
        private Runner runner;

        public Runner getRunner() {
            return runner;
        }

        public void setRunner(Runner runner) {
            this.runner = runner;
        }
    }

    private final RunnerHolder runnerHolder = new RunnerHolder();
    
    @Bean
    public RunnerHolder runnerHolder() {
        logger.info("Creating RunnerHolder bean");
        return runnerHolder;
    }

    @Bean
    public AgentscopeBrowserUseAgent agentscopeBrowserUseAgent(RunnerHolder runnerHolder) throws Exception {
        logger.info("Creating AgentscopeBrowserUseAgent bean...");
        agentInstance = new AgentscopeBrowserUseAgent(runnerHolder);
        agentInstance.connect();
        logger.info("AgentscopeBrowserUseAgent bean created and connected");
        return agentInstance;
    }

    @Bean
    @org.springframework.context.annotation.DependsOn("agentscopeBrowserUseAgent")
    public Runner agentRunner(RunnerHolder runnerHolder) {
        logger.info("Exposing Runner bean for A2A protocol");
        return runnerHolder.getRunner();
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

