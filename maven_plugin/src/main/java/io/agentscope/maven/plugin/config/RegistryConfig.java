package io.agentscope.maven.plugin.config;

/**
 * Docker registry configuration
 */
public class RegistryConfig {
    private String registryUrl;
    private String username;
    private String password;
    private String namespace;

    public String getRegistryUrl() {
        return registryUrl;
    }

    public void setRegistryUrl(String registryUrl) {
        this.registryUrl = registryUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * Get full registry URL with namespace
     */
    public String getFullUrl() {
        if (registryUrl == null || registryUrl.isEmpty()) {
            return null;
        }
        if (namespace != null && !namespace.isEmpty()) {
            return registryUrl + "/" + namespace;
        }
        return registryUrl;
    }
}



