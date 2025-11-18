/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.agentscope.runtime.protocol.a2a;

import io.a2a.spec.AgentInterface;
import io.a2a.spec.AgentProvider;
import io.a2a.spec.AgentSkill;
import io.a2a.spec.SecurityScheme;
import io.agentscope.runtime.protocol.Protocol;
import io.agentscope.runtime.protocol.ProtocolConfig;

import java.util.List;
import java.util.Map;

/**
 * {@link io.agentscope.runtime.protocol.ProtocolConfig} implementation for A2A protocol.
 *
 * @author xiweng.yy
 */
public class A2aProtocolConfig implements ProtocolConfig {

    private final String name;

    private final String description;

    private final String url;

    private final AgentProvider provider;

    private final String version;

    private final String documentationUrl;

    private final List<String> defaultInputModes;

    private final List<String> defaultOutputModes;

    private final List<AgentSkill> skills;

    private final boolean supportsAuthenticatedExtendedCard;

    private final Map<String, SecurityScheme> securitySchemes;

    private final List<Map<String, List<String>>> security;

    private final String iconUrl;

    private final List<AgentInterface> additionalInterfaces;

    private final String preferredTransport;

    public A2aProtocolConfig(String name, String description, String url, AgentProvider provider, String version,
                             String documentationUrl, List<String> defaultInputModes,
                             List<String> defaultOutputModes, List<AgentSkill> skills,
                             boolean supportsAuthenticatedExtendedCard, Map<String, SecurityScheme> securitySchemes,
                             List<Map<String, List<String>>> security, String iconUrl,
                             List<AgentInterface> additionalInterfaces, String preferredTransport) {
        this.name = name;
        this.description = description;
        this.url = url;
        this.provider = provider;
        this.version = version;
        this.documentationUrl = documentationUrl;
        this.defaultInputModes = defaultInputModes;
        this.defaultOutputModes = defaultOutputModes;
        this.skills = skills;
        this.supportsAuthenticatedExtendedCard = supportsAuthenticatedExtendedCard;
        this.securitySchemes = securitySchemes;
        this.security = security;
        this.iconUrl = iconUrl;
        this.additionalInterfaces = additionalInterfaces;
        this.preferredTransport = preferredTransport;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getUrl() {
        return url;
    }

    public AgentProvider getProvider() {
        return provider;
    }

    public String getVersion() {
        return version;
    }

    public String getDocumentationUrl() {
        return documentationUrl;
    }

    public List<String> getDefaultInputModes() {
        return defaultInputModes;
    }

    public List<String> getDefaultOutputModes() {
        return defaultOutputModes;
    }

    public List<AgentSkill> getSkills() {
        return skills;
    }

    public boolean isSupportsAuthenticatedExtendedCard() {
        return supportsAuthenticatedExtendedCard;
    }

    public Map<String, SecurityScheme> getSecuritySchemes() {
        return securitySchemes;
    }

    public List<Map<String, List<String>>> getSecurity() {
        return security;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public List<AgentInterface> getAdditionalInterfaces() {
        return additionalInterfaces;
    }

    public String getPreferredTransport() {
        return preferredTransport;
    }

    @Override
    public Protocol type() {
        return Protocol.A2A;
    }

    public static class Builder {

        protected String name;

        protected String description;

        protected String url;

        protected AgentProvider provider;

        protected String version;

        protected String documentationUrl;

        protected List<String> defaultInputModes;

        protected List<String> defaultOutputModes;

        protected List<AgentSkill> skills;

        protected boolean supportsAuthenticatedExtendedCard = false;

        protected Map<String, SecurityScheme> securitySchemes;

        protected List<Map<String, List<String>>> security;

        protected String iconUrl;

        protected List<AgentInterface> additionalInterfaces;

        protected String preferredTransport;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder provider(AgentProvider provider) {
            this.provider = provider;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder documentationUrl(String documentationUrl) {
            this.documentationUrl = documentationUrl;
            return this;
        }

        public Builder defaultInputModes(List<String> defaultInputModes) {
            this.defaultInputModes = defaultInputModes;
            return this;
        }

        public Builder defaultOutputModes(List<String> defaultOutputModes) {
            this.defaultOutputModes = defaultOutputModes;
            return this;
        }

        public Builder skills(List<AgentSkill> skills) {
            this.skills = skills;
            return this;
        }

        public Builder supportsAuthenticatedExtendedCard(boolean supportsAuthenticatedExtendedCard) {
            this.supportsAuthenticatedExtendedCard = supportsAuthenticatedExtendedCard;
            return this;
        }

        public Builder securitySchemes(Map<String, SecurityScheme> securitySchemes) {
            this.securitySchemes = securitySchemes;
            return this;
        }

        public Builder security(List<Map<String, List<String>>> security) {
            this.security = security;
            return this;
        }

        public Builder iconUrl(String iconUrl) {
            this.iconUrl = iconUrl;
            return this;
        }

        public Builder additionalInterfaces(List<AgentInterface> additionalInterfaces) {
            this.additionalInterfaces = additionalInterfaces;
            return this;
        }

        public Builder preferredTransport(String preferredTransport) {
            this.preferredTransport = preferredTransport;
            return this;
        }

        public A2aProtocolConfig build() {
            return new A2aProtocolConfig(name, description, url, provider, version, documentationUrl, defaultInputModes,
                    defaultOutputModes, skills, supportsAuthenticatedExtendedCard, securitySchemes, security, iconUrl,
                    additionalInterfaces, preferredTransport);
        }
    }
}
