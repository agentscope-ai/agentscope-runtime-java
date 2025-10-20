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

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import java.util.logging.Logger;

public class LocalDeployManager extends DeployManager{
    private ConfigurableApplicationContext applicationContext;
    Logger logger = Logger.getLogger(LocalDeployManager.class.getName());

    @Override
    public synchronized void deployStreaming(String endpointName) {
        if (this.applicationContext != null && this.applicationContext.isActive()) {
            return;
        }

        // Todo: Currently, only the A2A protocol is supported for calls. The protocol format for regular calls needs to be determined in the future
        this.applicationContext = new SpringApplicationBuilder(LocalDeployer.class)
            .initializers((GenericApplicationContext ctx) -> ctx.registerBean("endpointName", String.class, () -> endpointName))
            .properties("spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration")
            .run();
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
}
