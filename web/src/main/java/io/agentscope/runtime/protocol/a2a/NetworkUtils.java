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
package io.agentscope.runtime.protocol.a2a;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Comparator;

import io.agentscope.runtime.autoconfigure.DeployProperties;

/**
 * Network utility class for getting current server IP address and port configuration
 */
public class NetworkUtils {

    private final int serverPort;
    private final String serverAddress;

    public NetworkUtils(DeployProperties serverConfig) {
        this.serverPort = serverConfig.getServerPort();
        this.serverAddress = serverConfig.getServerAddress();
    }

    public NetworkUtils(int serverPort, String serverAddress) {
        this.serverPort = serverPort;
        this.serverAddress = serverAddress != null ? serverAddress : "";
    }

    /**
     * Get current server IP address
     * Uses address from configuration, or auto-detects local IP if not configured
     * 
     * @return server IP address
     */
    public String getServerIpAddress() {
        // Use address from configuration
        if (serverAddress != null && !serverAddress.trim().isEmpty()) {
            return serverAddress;
        }
        
        try {
            // Try to get local IP address
            return getLocalIpAddress();
        } catch (Exception e) {
            // If getting fails, use localhost
            return "localhost";
        }
    }

    /**
     * Get server port
     * 
     * @return server port
     */
    public int getServerPort() {
        return serverPort;
    }

    /**
     * Get complete server URL
     * 
     * @param path path (optional)
     * @return complete server URL
     */
    public String getServerUrl(String path) {
        String ip = getServerIpAddress();
        String baseUrl = "http://" + ip + ":" + serverPort;
        if (path != null && !path.trim().isEmpty()) {
            if (!path.startsWith("/")) {
                baseUrl += "/";
            }
            baseUrl += path;
        }
        return baseUrl;
    }

    /**
     * Get configuration information (for debugging)
     * 
     * @return configuration information string
     */
    public String getConfigInfo() {
        StringBuilder info = new StringBuilder();
        info.append("NetworkUtils Configuration:\n");
        info.append("  Server Port: ").append(serverPort).append("\n");
        info.append("  Server Address: ").append(serverAddress != null && !serverAddress.isEmpty() ? serverAddress : "auto-detect").append("\n");
        info.append("  Final IP: ").append(getServerIpAddress()).append("\n");
        info.append("  Final URL: ").append(getServerUrl(""));
        return info.toString();
    }

    /**
     * Get local IP address
     * Prefer non-loopback addresses
     * 
     * @return local IP address
     * @throws SocketException network exception
     */
    private String getLocalIpAddress() throws SocketException {
        Stream<NetworkInterface> networkInterfaces = NetworkInterface.networkInterfaces();
        
        List<NetworkInterface> nis = networkInterfaces
                                        .filter(this::isValidIpAddress)
                                        .sorted(Comparator.comparing((n) -> n.getIndex()))
                                        .collect(Collectors.toList());

        for (NetworkInterface networkInterface : nis) {
            Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();
                
                // Skip loopback addresses and IPv6 addresses
                if (address.isLoopbackAddress() || address.isLinkLocalAddress()) {
                    continue;
                }
                
                // Prefer IPv4 addresses
                if (address.getAddress().length == 4) {
                    return address.getHostAddress();
                }
            }
        }
        
        // If no suitable IP is found, return localhost
        return "localhost";
    }

    private boolean isValidIpAddress(NetworkInterface n) {
        try {
            return n != null && !n.isLoopback() && n.isUp();
        } catch (SocketException e) {
            return false;
        }
    }
}
