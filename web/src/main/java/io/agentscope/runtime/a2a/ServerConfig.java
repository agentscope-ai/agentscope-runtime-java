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
package io.agentscope.runtime.a2a;

/**
 * Server configuration for deployment
 */
public class ServerConfig {
    private final int serverPort;
    private final String serverAddress;

    public ServerConfig(int serverPort){
        this(serverPort, "");
    }

    public ServerConfig(int serverPort, String serverAddress) {
        this.serverPort = serverPort;
        this.serverAddress = serverAddress != null ? serverAddress : "";
    }

    public int getServerPort() {
        return serverPort;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public static ServerConfig defaultConfig() {
        return new ServerConfig(8080, "");
    }
}


