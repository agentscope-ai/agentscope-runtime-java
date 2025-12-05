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
package io.agentscope.runtime.autoconfigure;

import io.agentscope.runtime.protocol.ProtocolConfig;
import io.agentscope.runtime.protocol.a2a.A2aProtocolConfig;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(value = {"io.agentscope.runtime.autoconfigure",
        "io.agentscope.runtime.protocol.a2a"})
@EnableConfigurationProperties({A2aCommonProperties.class, A2aAgentCardProperties.class})
public class A2aAutoConfiguration {

    @Bean
    public DeployProperties deployProperties(A2aCommonProperties properties, ObjectProvider<ServerProperties> serverPropertiesProvider) {
        DeployProperties result = new DeployProperties();
        result.setEndpointName(properties.getEndpointName());
        if (serverPropertiesProvider.getIfAvailable() != null) {
            ServerProperties serverProperties = serverPropertiesProvider.getIfAvailable();
            if (null != serverProperties.getPort()) {
                result.setServerPort(serverProperties.getPort());
            }
            if (null != serverProperties.getAddress()) {
                result.setServerAddress(serverProperties.getAddress().getHostAddress());
            }
        }
        return result;
    }

    @Bean
    public ProtocolConfig a2aProtocolConfig(A2aAgentCardProperties properties) {
        A2aProtocolConfig.Builder builder = new A2aProtocolConfig.Builder();
        return builder.name(properties.getName())
                .description(properties.getDescription())
                .url(properties.getUrl())
                .provider(properties.getProvider())
                .version(properties.getVersion())
                .documentationUrl(properties.getDocumentationUrl())
                .defaultInputModes(properties.getDefaultInputModes())
                .defaultOutputModes(properties.getDefaultOutputModes())
                .skills(properties.getSkills())
                .supportsAuthenticatedExtendedCard(properties.isSupportsAuthenticatedExtendedCard())
                .securitySchemes(properties.getSecuritySchemes())
                .security(properties.getSecurity())
                .iconUrl(properties.getIconUrl())
                .additionalInterfaces(properties.getAdditionalInterfaces())
                .build();
    }
}
