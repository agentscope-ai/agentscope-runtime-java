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
package io.agentscope.runtime.sandbox.box;

import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.container.ContainerModel;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * GUI Mixin providing desktop URL functionality for sandboxes.
 * This class provides helper methods that can be used by sandbox classes
 * to enable GUI/VNC desktop access.
 */
public class GuiMixin {
    
    /**
     * Get the desktop URL for VNC access.
     * This method should be called from a Sandbox instance that has access to
     * SandboxManager and sandboxId.
     *
     * @param managerApi The SandboxManager instance
     * @param sandboxId The sandbox ID
     * @param baseUrl Optional base URL (can be null)
     * @return The desktop URL for VNC access
     * @throws RuntimeException if sandbox is not healthy or info cannot be retrieved
     */
    public static String getDesktopUrl(SandboxManager managerApi, String sandboxId, String baseUrl) {
        // Check if sandbox is healthy by attempting to get info
        ContainerModel info;
        try {
            info = managerApi.getInfo(sandboxId);
        } catch (Exception e) {
            throw new RuntimeException("Sandbox " + sandboxId + " is not healthy: " + e.getMessage(), e);
        }
        
        if (info == null) {
            throw new RuntimeException("Sandbox " + sandboxId + " is not healthy: cannot retrieve info");
        }
        
        String runtimeToken = info.getRuntimeToken();
        if (runtimeToken == null || runtimeToken.isEmpty()) {
            throw new RuntimeException("Sandbox " + sandboxId + " does not have a runtime token");
        }
        
        String path = "/vnc/vnc_lite.html";
        String remotePath = "/vnc/vnc_relay.html";
        
        try {
            String encodedPassword = URLEncoder.encode(runtimeToken, StandardCharsets.UTF_8.toString());
            String params = "password=" + encodedPassword;
            
            if (baseUrl == null || baseUrl.isEmpty()) {
                // Use direct URL from container info
                String containerUrl = info.getBaseUrl();
                if (containerUrl == null || containerUrl.isEmpty()) {
                    throw new RuntimeException("Sandbox " + sandboxId + " does not have a base URL");
                }
                // Ensure URL ends with / if not present
                if (!containerUrl.endsWith("/")) {
                    containerUrl += "/";
                }
                return containerUrl + path.substring(1) + "?" + params;
            } else {
                // Use base_url with sandbox ID
                String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
                return base + "/desktop/" + sandboxId + remotePath + "?" + params;
            }
        } catch (UnsupportedEncodingException e) {
            // Should never happen with UTF-8
            throw new RuntimeException("Failed to encode password", e);
        }
    }
}

