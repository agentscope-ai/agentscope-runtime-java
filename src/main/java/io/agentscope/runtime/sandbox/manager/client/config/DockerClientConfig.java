package io.agentscope.runtime.sandbox.manager.client.config;

import io.agentscope.runtime.sandbox.manager.model.container.PortRange;
import io.agentscope.runtime.sandbox.manager.model.container.RedisManagerConfig;

public class DockerClientConfig extends BaseClientConfig {
    private String host;
    private int port;
    private String certPath;
    private PortRange portRange;
    private boolean redisEnabled;
    private RedisManagerConfig redisConfig;

    public DockerClientConfig(){
        this(true, "localhost", 2375, null, null, false, null);
    }

    public DockerClientConfig(boolean isLocal) {
        this(isLocal, "localhost", 2375, null, null, false, null);
    }

    public DockerClientConfig(boolean isLocal, String host, int port, String certPath) {
        this(isLocal, host, port, certPath, null, false, null);
    }

    public DockerClientConfig(boolean isLocal, String host, int port, String certPath,
                             PortRange portRange, boolean redisEnabled, RedisManagerConfig redisConfig) {
        super(isLocal);
        this.host = host;
        this.port = port;
        this.certPath = certPath;
        this.portRange = portRange;
        this.redisEnabled = redisEnabled;
        this.redisConfig = redisConfig;
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
}
