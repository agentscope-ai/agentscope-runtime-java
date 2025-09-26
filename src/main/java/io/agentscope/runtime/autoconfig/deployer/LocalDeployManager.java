package io.agentscope.runtime.autoconfig.deployer;

import io.agentscope.runtime.engine.schemas.agent.AgentRequest;
import io.agentscope.runtime.engine.schemas.agent.Event;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;
import java.util.function.Function;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentCapabilities;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Constructor;
import java.util.List;

public class LocalDeployManager extends DeployManager{
    private ConfigurableApplicationContext applicationContext;

    @Override
    public synchronized void deployStreaming(Function<AgentRequest, Flux<Event>> queryFunction, String endpointName) {
        if (this.applicationContext != null && this.applicationContext.isActive()) {
            return;
        }

        System.setProperty("org.springframework.boot.logging.LoggingSystem", "none");

        this.applicationContext = new SpringApplicationBuilder(LocalDeployer.class)
            .initializers((GenericApplicationContext ctx) -> {
                ctx.registerBean("agentRequestStreamQueryFunction", Function.class, () -> queryFunction);
                ctx.registerBean("endpointName", String.class, () -> endpointName);
                ctx.registerBean(AgentCard.class, LocalDeployManager::createDefaultAgentCard);
            })
            .properties("spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration")
            .run();
    }

    @Bean
    private static AgentCard createDefaultAgentCard() {
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
            if (capsClass.isInterface()) {
                InvocationHandler ih = (proxy, method, args) -> {
                    String n = method.getName();
                    if ("streaming".equals(n)) return Boolean.TRUE;
                    if ("pushNotifications".equals(n)) return Boolean.FALSE;
                    Class<?> rt = method.getReturnType();
                    if (rt.equals(boolean.class) || rt.equals(Boolean.class)) return Boolean.FALSE;
                    return null;
                };
                return (AgentCapabilities) Proxy.newProxyInstance(capsClass.getClassLoader(), new Class<?>[]{capsClass}, ih);
            }

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
                    if (inst != null) return (AgentCapabilities) inst;
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable e) {
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
}
