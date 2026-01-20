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

import io.agentscope.runtime.sandbox.manager.SandboxService;
import io.agentscope.runtime.sandbox.manager.fs.FileSystemConfig;
import io.agentscope.runtime.sandbox.manager.fs.local.LocalFileSystemConfig;
import io.agentscope.runtime.sandbox.manager.registry.RegisterSandbox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GUI Sandbox for desktop GUI interactions.
 * Provides VNC desktop access and computer use functionality.
 */
@RegisterSandbox(
        imageName = "agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-gui:latest",
        sandboxType = "gui",
        securityLevel = "high",
        timeout = 60,
        description = "GUI Sandbox"
)
public class GuiSandbox extends Sandbox {

    private final String baseUrl;

    public GuiSandbox(SandboxService managerApi, String userId, String sessionId) {
        this(managerApi, userId, sessionId, null, LocalFileSystemConfig.builder().build(), Map.of());
    }

    public GuiSandbox(
            SandboxService managerApi,
            String userId,
            String sessionId,
            String baseUrl) {
        this(managerApi, userId, sessionId, baseUrl, LocalFileSystemConfig.builder().build(), Map.of());
    }

    public GuiSandbox(
            SandboxService managerApi,
            String userId,
            String sessionId,
            FileSystemConfig fileSystemConfig) {
        this(managerApi, userId, sessionId, null, fileSystemConfig, Map.of());
    }

    public GuiSandbox(
            SandboxService managerApi,
            String userId,
            String sessionId,
            Map<String, String> environment) {
        this(managerApi, userId, sessionId, null, LocalFileSystemConfig.builder().build(), environment);
    }

    public GuiSandbox(
            SandboxService managerApi,
            String userId,
            String sessionId,
            String baseUrl,
            FileSystemConfig fileSystemConfig,
            Map<String, String> environment) {
        super(managerApi, userId, sessionId, "gui", fileSystemConfig, environment);
        this.baseUrl = baseUrl;
    }

    /**
     * Get the desktop URL for VNC access.
     * This method provides GUI mixin functionality.
     *
     * @return The desktop URL for VNC access
     * @throws RuntimeException if sandbox is not healthy
     */
    public String getDesktopUrl() {
        return GuiMixin.getDesktopUrl(managerApi, this, baseUrl);
    }

    /**
     * Use computer mouse and keyboard to interact with the desktop.
     *
     * @param action The action to perform (key, type, mouse_move, left_click, etc.)
     * @return The result of the action
     */
    public String computerUse(String action) {
        return computerUse(action, null, null);
    }

    /**
     * Use computer mouse and keyboard to interact with the desktop.
     *
     * @param action     The action to perform (key, type, mouse_move, left_click, etc.)
     * @param coordinate The (x, y) coordinate for mouse actions
     * @return The result of the action
     */
    public String computerUse(String action, List<Double> coordinate) {
        return computerUse(action, coordinate, null);
    }

    /**
     * Use computer mouse and keyboard to interact with the desktop.
     *
     * @param action     The action to perform (key, type, mouse_move, left_click, etc.)
     * @param coordinate The (x, y) coordinate for mouse actions
     * @param text       Text to type for type action, or key command for key action
     * @return The result of the action
     */
    public String computerUse(String action, List<Double> coordinate, String text) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("action", action);
        if (coordinate != null && !coordinate.isEmpty()) {
            arguments.put("coordinate", coordinate);
        }
        if (text != null && !text.isEmpty()) {
            arguments.put("text", text);
        }
        return callTool("computer", arguments);
    }
}
