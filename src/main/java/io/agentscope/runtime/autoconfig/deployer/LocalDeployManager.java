package io.agentscope.runtime.autoconfig.deployer;

import io.agentscope.runtime.engine.schemas.agent.AgentRequest;
import io.agentscope.runtime.engine.schemas.agent.Event;
import reactor.core.publisher.Flux;
import java.util.function.Function;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

public class LocalDeployManager extends DeployManager{
    private ConfigurableApplicationContext applicationContext;

    @Override
    public synchronized void deployStreaming(String endpointName) {
        if (this.applicationContext != null && this.applicationContext.isActive()) {
            return;
        }

        this.applicationContext = new SpringApplicationBuilder(LocalDeployer.class)
            .initializers((GenericApplicationContext ctx) -> {
                ctx.registerBean("endpointName", String.class, () -> endpointName);
            })
            .properties("spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration")
            .run();
    }
}
