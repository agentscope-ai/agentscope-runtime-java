package io.agentscope.runtime.sandbox.manager.client.config;

import io.agentscope.runtime.sandbox.manager.model.container.ContainerManagerType;
import io.agentscope.runtime.sandbox.manager.model.container.PortRange;
import io.agentscope.runtime.sandbox.manager.model.container.RedisManagerConfig;

public class DockerClientConfig extends BaseClientConfig {
    private String host;
    private int port;
    private String certPath;
    private PortRange portRange;
    private boolean redisEnabled;
    private RedisManagerConfig redisConfig;

    private DockerClientConfig() {
        super(ContainerManagerType.DOCKER);
    }

    private DockerClientConfig(String host, int port, String certPath,
                             PortRange portRange, boolean redisEnabled, RedisManagerConfig redisConfig) {
        super(ContainerManagerType.DOCKER);
        this.host = host;
        this.port = port;
        this.certPath = certPath;
        this.portRange = portRange;
        this.redisEnabled = redisEnabled;
        this.redisConfig = redisConfig;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getCertPath() {
        return certPath;
    }

    public void setCertPath(String certPath) {
        this.certPath = certPath;
    }

    public PortRange getPortRange() {
        return portRange;
    }

    public void setPortRange(PortRange portRange) {
        this.portRange = portRange;
    }

    public boolean isRedisEnabled() {
        return redisEnabled;
    }

    public void setRedisEnabled(boolean redisEnabled) {
        this.redisEnabled = redisEnabled;
    }

    public RedisManagerConfig getRedisConfig() {
        return redisConfig;
    }

    public void setRedisConfig(RedisManagerConfig redisConfig) {
        this.redisConfig = redisConfig;
    }

    public static class Builder {
        private String host = "localhost";
        private int port = 2375;
        private String certPath;
        private PortRange portRange;
        private boolean redisEnabled = false;
        private RedisManagerConfig redisConfig;

        private Builder() {
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder certPath(String certPath) {
            this.certPath = certPath;
            return this;
        }

        public Builder portRange(PortRange portRange) {
            this.portRange = portRange;
            return this;
        }

        public Builder redisEnabled(boolean redisEnabled) {
            this.redisEnabled = redisEnabled;
            return this;
        }

        public Builder redisConfig(RedisManagerConfig redisConfig) {
            this.redisConfig = redisConfig;
            return this;
        }

        public DockerClientConfig build() {
            return new DockerClientConfig(host, port, certPath, portRange, redisEnabled, redisConfig);
        }
    }
}
