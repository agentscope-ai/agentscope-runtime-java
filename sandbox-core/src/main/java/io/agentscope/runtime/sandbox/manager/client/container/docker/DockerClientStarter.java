package io.agentscope.runtime.sandbox.manager.client.container.docker;

import io.agentscope.runtime.sandbox.manager.client.container.BaseClientStarter;
import io.agentscope.runtime.sandbox.manager.model.container.ContainerClientType;
import io.agentscope.runtime.sandbox.manager.utils.PortManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DockerClientStarter extends BaseClientStarter {
    Logger logger = LoggerFactory.getLogger(DockerClientStarter.class);
    private String host;
    private int port;
    private String certPath;

    private DockerClientStarter() {
        super(ContainerClientType.DOCKER);
    }

    private DockerClientStarter(String host, int port, String certPath) {
        super(ContainerClientType.DOCKER);
        this.host = host;
        this.port = port;
        this.certPath = certPath;
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

    @Override
    public DockerClient startClient(PortManager portManager) {
        DockerClient dockerClient = new DockerClient(this, portManager);
        dockerClient.connect();
        logger.info("Docker client created");
        return dockerClient;
    }

    public static class Builder {
        private String host = "localhost";
        private int port = 2375;
        private String certPath;

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

        public DockerClientStarter build() {
            return new DockerClientStarter(host, port, certPath);
        }
    }
}
