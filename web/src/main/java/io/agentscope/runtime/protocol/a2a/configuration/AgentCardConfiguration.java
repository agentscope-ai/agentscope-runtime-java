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

package io.agentscope.runtime.protocol.a2a.configuration;

import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.TransportProtocol;

import io.agentscope.runtime.adapters.AgentHandler;
import io.agentscope.runtime.autoconfigure.DeployProperties;
import io.agentscope.runtime.engine.Runner;
import io.agentscope.runtime.protocol.ProtocolConfig;
import io.agentscope.runtime.protocol.a2a.A2aProtocolConfig;
import io.agentscope.runtime.protocol.a2a.A2aProtocolConfigUtils;
import io.agentscope.runtime.protocol.a2a.ConfigurableAgentCard;
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
    public AgentCard agentCard(DeployProperties deployProperties, ObjectProvider<ProtocolConfig> protocolConfigs,
                               AgentHandler agentHandler) {
        A2aProtocolConfig a2aConfig = A2aProtocolConfigUtils.getConfigIfAbsent(protocolConfigs);
        return createAgentCard(new NetworkUtils(deployProperties), agentHandler, a2aConfig.getAgentCard());
    }

    private AgentCard createAgentCard(NetworkUtils networkUtils, AgentHandler agent, ConfigurableAgentCard agentCard) {
        AgentCapabilities capabilities = createDefaultCapabilities();
        String dynamicUrl = StringUtils.isEmpty(agentCard.getUrl()) ? networkUtils.getServerUrl("/a2a/") : agentCard.getUrl();
        return new AgentCard.Builder().name(StringUtils.isEmpty(agentCard.getName()) ? agent.getName() : agentCard.getName())
                .description(StringUtils.isEmpty(agentCard.getDescription()) ? agent.getDescription() : agentCard.getDescription())
                .url(dynamicUrl)
                .provider(agentCard.getProvider())
                .version(StringUtils.isEmpty(agentCard.getVersion()) ? "1.0.0" : agentCard.getVersion())
                .documentationUrl(agentCard.getDocumentationUrl())
                .capabilities(capabilities)
                .defaultInputModes(null != agentCard.getDefaultInputModes() ? agentCard.getDefaultInputModes() : List.of("text"))
                .defaultOutputModes(null != agentCard.getDefaultOutputModes() ? agentCard.getDefaultOutputModes() : List.of("text"))
                .skills(null != agentCard.getSkills() ? agentCard.getSkills() : List.of())
                .supportsAuthenticatedExtendedCard(agentCard.isSupportsAuthenticatedExtendedCard())
                .securitySchemes(agentCard.getSecuritySchemes())
                .security(agentCard.getSecurity())
                .iconUrl(agentCard.getIconUrl())
                .additionalInterfaces(agentCard.getAdditionalInterfaces())
                .preferredTransport(StringUtils.isEmpty(agentCard.getPreferredTransport()) ? TransportProtocol.JSONRPC.name() : agentCard.getPreferredTransport())
                .protocolVersion("0.3.0")
                .build();
    }

    private static AgentCapabilities createDefaultCapabilities() {
        return new AgentCapabilities.Builder().streaming(true).pushNotifications(false).stateTransitionHistory(false)
                .build();
    }
}
