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
package io.agentscope.runtime.engine.a2a;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Network utility class for getting current server IP address and port configuration
 */
public class NetworkUtils {

    private final int serverPort;
    private final String serverAddress;

    public NetworkUtils() {
        this(loadPortFromConfig(), "");
    }

    public NetworkUtils(int serverPort) {
        this(serverPort, "");
    }

    public NetworkUtils(int serverPort, String serverAddress) {
        this.serverPort = serverPort;
        this.serverAddress = serverAddress;
    }

    /**
     * Load port number from configuration files
     * Supports multiple configuration methods: environment variables, system properties, configuration files
     * 
     * @return port number, defaults to 8080
     */
    private static int loadPortFromConfig() {
        // 1. Check environment variables first
        String envPort = System.getenv("SERVER_PORT");
        if (envPort != null && !envPort.trim().isEmpty()) {
            try {
                return Integer.parseInt(envPort.trim());
            } catch (NumberFormatException e) {
                // Ignore format errors, continue with other methods
            }
        }

        // 2. Check system properties
        String sysPort = System.getProperty("server.port");
        if (sysPort != null && !sysPort.trim().isEmpty()) {
            try {
                return Integer.parseInt(sysPort.trim());
            } catch (NumberFormatException e) {
                // Ignore format errors, continue with other methods
            }
        }

        // 3. Check application.yml
        try {
            InputStream input = NetworkUtils.class.getClassLoader().getResourceAsStream("application.yml");
            if (input != null) {
                String content = new String(input.readAllBytes());
                String[] lines = content.split("\n");
                for (String line : lines) {
                    line = line.trim();
                    if (line.startsWith("port:")) {
                        String portStr = line.substring(5).trim();
                        return Integer.parseInt(portStr);
                    }
                }
            }
        } catch (Exception e) {
            // If reading fails, continue with other methods
        }

        // 4. Check application.properties
        try {
            InputStream input = NetworkUtils.class.getClassLoader().getResourceAsStream("application.properties");
            if (input != null) {
                String content = new String(input.readAllBytes());
                String[] lines = content.split("\n");
                for (String line : lines) {
                    line = line.trim();
                    if (line.startsWith("server.port=")) {
                        String portStr = line.substring(12).trim();
                        return Integer.parseInt(portStr);
                    }
                }
            }
        } catch (Exception e) {
            // If reading fails, use default port
        }

        return 8080; // Default port
    }

    /**
     * Get current server IP address
     * Supports multiple configuration methods: environment variables, system properties, constructor parameters, auto-detection
     * 
     * @return server IP address
     */
    public String getServerIpAddress() {
        // 1. Check environment variables first
        String envAddress = System.getenv("SERVER_ADDRESS");
        if (envAddress != null && !envAddress.trim().isEmpty()) {
            return envAddress.trim();
        }

        // 2. Check system properties
        String sysAddress = System.getProperty("server.address");
        if (sysAddress != null && !sysAddress.trim().isEmpty()) {
            return sysAddress.trim();
        }

        // 3. Use address passed in constructor
        if (serverAddress != null && !serverAddress.trim().isEmpty()) {
            return serverAddress;
        }
        
        try {
            // 4. Try to get local IP address
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
        info.append("  Server Address: ").append(serverAddress != null ? serverAddress : "auto-detect").append("\n");
        info.append("  Environment SERVER_PORT: ").append(System.getenv("SERVER_PORT")).append("\n");
        info.append("  Environment SERVER_ADDRESS: ").append(System.getenv("SERVER_ADDRESS")).append("\n");
        info.append("  System Property server.port: ").append(System.getProperty("server.port")).append("\n");
        info.append("  System Property server.address: ").append(System.getProperty("server.address")).append("\n");
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
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaces.nextElement();
            
            // Skip loopback interfaces and disabled interfaces
            if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                continue;
            }
            
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
}
