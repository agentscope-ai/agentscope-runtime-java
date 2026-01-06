package io.agentscope.runtime.sandbox.manager.client.container;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Result object for container creation operations.
 */
public class ContainerCreateResult {
    private String containerId;
    private List<String> ports;
    private String ip;
    private List<String> rest; // Additional values like protocol ("https")

    public ContainerCreateResult(String containerId) {
        this(containerId, null, null, "http");
    }

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

