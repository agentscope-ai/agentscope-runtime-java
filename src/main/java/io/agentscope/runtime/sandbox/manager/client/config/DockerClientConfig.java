package io.agentscope.runtime.sandbox.manager.client.config;

public class DockerClientConfig extends BaseClientConfig {
    private String host;
    private int port;
    private String certPath;

    public DockerClientConfig(){
        this(true, "localhost", 2375, null);
    }

    public DockerClientConfig(boolean isLocal) {
        this(isLocal, "localhost", 2375, null);
    }

    public DockerClientConfig(boolean isLocal, String host, int port, String certPath) {
        super(isLocal);
        this.host = host;
        this.port = port;
        this.certPath = certPath;
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
}
