package io.agentscope.runtime.protocol.a2a.configuration;

import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.TransportProtocol;
import io.agentscope.runtime.autoconfigure.DeployProperties;
import io.agentscope.runtime.engine.Runner;
import io.agentscope.runtime.engine.agents.Agent;
import io.agentscope.runtime.protocol.ProtocolConfig;
import io.agentscope.runtime.protocol.a2a.A2aProtocolConfig;
import io.agentscope.runtime.protocol.a2a.NetworkUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * AgentCard Configuration.
 *
 * @author xiweng.yy
 */
@Configuration
public class AgentCardConfiguration {

    @Bean
    @ConditionalOnBean(value = Runner.class)
    public AgentCard agentCard(DeployProperties deployProperties, ObjectProvider<ProtocolConfig> protocolConfigs,
                               Runner runner) {
        A2aProtocolConfig a2aConfig = protocolConfigs.stream()
                .filter(protocolConfig -> A2aProtocolConfig.class.isAssignableFrom(protocolConfig.getClass()))
                .map(protocolConfig -> (A2aProtocolConfig) protocolConfig).findFirst()
                .orElse(new A2aProtocolConfig.Builder().build());
        return createAgentCard(new NetworkUtils(deployProperties), runner.getAgent(), a2aConfig);
    }

    private AgentCard createAgentCard(NetworkUtils networkUtils, Agent agent, A2aProtocolConfig a2aConfig) {
        AgentCapabilities capabilities = createDefaultCapabilities();
        String dynamicUrl = StringUtils.isEmpty(a2aConfig.getUrl()) ? networkUtils.getServerUrl("/a2a/") : a2aConfig.getUrl();
        return new AgentCard.Builder().name(StringUtils.isEmpty(a2aConfig.getName()) ? agent.getName() : a2aConfig.getName())
                .description(StringUtils.isEmpty(a2aConfig.getDescription()) ? agent.getDescription() : a2aConfig.getDescription())
                .url(dynamicUrl)
                .provider(a2aConfig.getProvider())
                .version(StringUtils.isEmpty(a2aConfig.getVersion()) ? "1.0.0" : a2aConfig.getVersion())
                .documentationUrl(a2aConfig.getDocumentationUrl())
                .capabilities(capabilities)
                .defaultInputModes(null != a2aConfig.getDefaultInputModes() ? a2aConfig.getDefaultInputModes() : List.of("text"))
                .defaultOutputModes(null != a2aConfig.getDefaultOutputModes() ? a2aConfig.getDefaultOutputModes() : List.of("text"))
                .skills(null != a2aConfig.getSkills() ? a2aConfig.getSkills() : List.of())
                .supportsAuthenticatedExtendedCard(a2aConfig.isSupportsAuthenticatedExtendedCard())
                .securitySchemes(a2aConfig.getSecuritySchemes())
                .security(a2aConfig.getSecurity())
                .iconUrl(a2aConfig.getIconUrl())
                .additionalInterfaces(a2aConfig.getAdditionalInterfaces())
                .preferredTransport(TransportProtocol.JSONRPC.name())
                .protocolVersion("0.3.0")
                .build();
    }

    private static AgentCapabilities createDefaultCapabilities() {
        return new AgentCapabilities.Builder().streaming(true).pushNotifications(false).stateTransitionHistory(false)
                .build();
    }
}
