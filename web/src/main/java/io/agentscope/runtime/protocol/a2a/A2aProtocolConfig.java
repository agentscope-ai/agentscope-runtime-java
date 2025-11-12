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
public record A2aProtocolConfig(String name, String description, String url, AgentProvider provider, String version,
                                String documentationUrl, List<String> defaultInputModes,
                                List<String> defaultOutputModes, List<AgentSkill> skills,
                                boolean supportsAuthenticatedExtendedCard, Map<String, SecurityScheme> securitySchemes,
                                List<Map<String, List<String>>> security, String iconUrl,
                                List<AgentInterface> additionalInterfaces) implements ProtocolConfig {
    
    @Override
    public Protocol type() {
        return Protocol.A2A;
    }
    
    public static class Builder {
        
        private String name;
        
        private String description;
        
        private String url;
        
        private AgentProvider provider;
        
        private String version;
        
        private String documentationUrl;
        
        private List<String> defaultInputModes;
        
        private List<String> defaultOutputModes;
        
        private List<AgentSkill> skills;
        
        private boolean supportsAuthenticatedExtendedCard = false;
        
        private Map<String, SecurityScheme> securitySchemes;
        
        private List<Map<String, List<String>>> security;
        
        private String iconUrl;
        
        private List<AgentInterface> additionalInterfaces;
        
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
        
        public A2aProtocolConfig build() {
            return new A2aProtocolConfig(name, description, url, provider, version, documentationUrl, defaultInputModes,
                    defaultOutputModes, skills, supportsAuthenticatedExtendedCard, securitySchemes, security, iconUrl,
                    additionalInterfaces);
        }
    }
}
