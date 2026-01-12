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
