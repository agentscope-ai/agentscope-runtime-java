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
package io.agentscope.maven.plugin.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Result object for container creation operations.
 * This aligns with Python version's return tuple: (_id, ports, ip, *rest)
 */
public class ContainerCreateResult {
    private String containerId;
    private List<String> ports;
    private String ip;
    private List<String> rest; // Additional values like protocol ("https")

    public ContainerCreateResult(String containerId, List<String> ports, String ip) {
        this.containerId = containerId;
        this.ports = ports != null ? ports : new ArrayList<>();
        this.ip = ip;
        this.rest = new ArrayList<>();
    }

    public ContainerCreateResult(String containerId, List<String> ports, String ip, String... rest) {
        this.containerId = containerId;
        this.ports = ports != null ? ports : new ArrayList<>();
        this.ip = ip;
        this.rest = rest != null ? Arrays.asList(rest) : new ArrayList<>();
    }

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public List<String> getPorts() {
        return ports;
    }

    public void setPorts(List<String> ports) {
        this.ports = ports;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public List<String> getRest() {
        return rest;
    }

    public void setRest(List<String> rest) {
        this.rest = rest;
    }

    /**
     * Get the first port, or null if no ports available
     */
    public String getFirstPort() {
        return ports != null && !ports.isEmpty() ? ports.get(0) : null;
    }

    /**
     * Get the protocol from rest values, defaulting to "http" if not found
     */
    public String getProtocol() {
        if (rest != null && !rest.isEmpty() && "https".equals(rest.get(0))) {
            return "https";
        }
        return "http";
    }
}


