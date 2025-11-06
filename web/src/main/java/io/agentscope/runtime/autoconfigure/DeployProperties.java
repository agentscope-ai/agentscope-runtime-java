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

import java.util.Objects;

/**
 * Server configuration for deployment
 */
public class DeployProperties {
    private static final String DEFAULT_ENDPOINT_NAME = "process";
    private static final String DEFAULT_HOST = "";
    private static final int DEFAULT_PORT = 9090;

    private int serverPort;
    private String serverAddress;
    private String endpointName;

    public DeployProperties() {}

    public DeployProperties(int port){
        this(port, "", "");
    }

    public DeployProperties(int port, String host, String endpointName) {
        this.serverAddress = Objects.requireNonNullElse(host, DEFAULT_HOST);
        this.serverPort = (port > 0) ? port : DEFAULT_PORT;
        this.endpointName = Objects.requireNonNullElse(endpointName, DEFAULT_ENDPOINT_NAME);
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    String getEndpointName() {
        return endpointName;
    }

    void setEndpointName(String endpointName) {
        this.endpointName = endpointName;
    }
}
