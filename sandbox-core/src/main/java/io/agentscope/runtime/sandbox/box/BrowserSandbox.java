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
import java.util.Map;

@RegisterSandbox(
        imageName = "agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-browser:latest",
        sandboxType = "browser",
        securityLevel = "medium",
        timeout = 60,
        description = "Browser sandbox"
)
public class BrowserSandbox extends Sandbox {

    private final String baseUrl;

    public BrowserSandbox(SandboxService managerApi, String userId, String sessionId) {
        this(managerApi, userId, sessionId, null, LocalFileSystemConfig.builder().build(), Map.of());
    }

    public BrowserSandbox(
            SandboxService managerApi,
            String userId,
            String sessionId,
            String baseUrl) {
        this(managerApi, userId, sessionId, baseUrl, LocalFileSystemConfig.builder().build(), Map.of());
    }

    public BrowserSandbox(
            SandboxService managerApi,
            String userId,
            String sessionId,
            FileSystemConfig fileSystemConfig) {
        this(managerApi, userId, sessionId, null, fileSystemConfig, Map.of());
    }

    public BrowserSandbox(
            SandboxService managerApi,
            String userId,
            String sessionId,
            Map<String, String> environment) {
        this(managerApi, userId, sessionId, null, LocalFileSystemConfig.builder().build(), environment);
    }

    public BrowserSandbox(
            SandboxService managerApi,
            String userId,
            String sessionId,
            String baseUrl,
            FileSystemConfig fileSystemConfig,
            Map<String, String> environment) {
        super(managerApi, userId, sessionId, "browser", fileSystemConfig, environment);
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
        return GuiMixin.getDesktopUrl(managerApi, sandboxId, baseUrl);
    }

    public String navigate(String url) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("url", url);
        return callTool("browser_navigate", arguments);
    }

    public String click(String element, String ref) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("element", element);
        arguments.put("ref", ref);
        return callTool("browser_click", arguments);
    }

    public String type(String element, String ref, String text) {
        return type(element, ref, text, null, null);
    }

    public String type(String element, String ref, String text, Boolean submit, Boolean slowly) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("element", element);
        arguments.put("ref", ref);
        arguments.put("text", text);
        if (submit != null) arguments.put("submit", submit);
        if (slowly != null) arguments.put("slowly", slowly);
        return callTool("browser_type", arguments);
    }

    public String takeScreenshot() {
        return takeScreenshot(null, null, null, null);
    }

    public String takeScreenshot(Boolean raw, String filename, String element, String ref) {
        Map<String, Object> arguments = new HashMap<>();
        if (raw != null) arguments.put("raw", raw);
        if (filename != null) arguments.put("filename", filename);
        if (element != null) arguments.put("element", element);
        if (ref != null) arguments.put("ref", ref);
        return callTool("browser_take_screenshot", arguments);
    }

    public String snapshot() {
        return callTool("browser_snapshot", new HashMap<>());
    }

    public String tabNew(String url) {
        Map<String, Object> arguments = new HashMap<>();
        if (url != null) arguments.put("url", url);
        return callTool("browser_tab_new", arguments);
    }

    public String tabSelect(Integer index) {
        Map<String, Object> arguments = new HashMap<>();
        if (index != null) arguments.put("index", index);
        return callTool("browser_tab_select", arguments);
    }

    public String tabClose(Integer index) {
        Map<String, Object> arguments = new HashMap<>();
        if (index != null) arguments.put("index", index);
        return callTool("browser_tab_close", arguments);
    }

    public String tabList() {
        return callTool("browser_tab_list", new HashMap<>());
    }

    public String waitFor(Double time) {
        return waitFor(time, null, null);
    }

    public String waitFor(Double time, String text, String textGone) {
        Map<String, Object> arguments = new HashMap<>();
        if (time != null) arguments.put("time", time);
        if (text != null) arguments.put("text", text);
        if (textGone != null) arguments.put("textGone", textGone);
        return callTool("browser_wait_for", arguments);
    }

    public String resize(Double width, Double height) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("width", width);
        arguments.put("height", height);
        return callTool("browser_resize", arguments);
    }

    public String closeBrowser() {
        return callTool("browser_close", new HashMap<>());
    }

    public String consoleMessages() {
        return callTool("browser_console_messages", new HashMap<>());
    }

    public String networkRequests() {
        return callTool("browser_network_requests", new HashMap<>());
    }

    public String navigateBack() {
        return callTool("browser_navigate_back", new HashMap<>());
    }

    public String navigateForward() {
        return callTool("browser_navigate_forward", new HashMap<>());
    }

    public String hover(String element, String ref) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("element", element);
        arguments.put("ref", ref);
        return callTool("browser_hover", arguments);
    }

    public String pressKey(String key) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("key", key);
        return callTool("browser_press_key", arguments);
    }

    public String handleDialog(Boolean accept, String promptText) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("accept", accept);
        if (promptText != null) {
            arguments.put("promptText", promptText);
        }
        return callTool("browser_handle_dialog", arguments);
    }

    public String fileUpload(String[] paths) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("paths", paths);
        return callTool("browser_file_upload", arguments);
    }

    public String pdfSave(String filename) {
        Map<String, Object> arguments = new HashMap<>();
        if (filename != null) {
            arguments.put("filename", filename);
        }
        return callTool("browser_pdf_save", arguments);
    }

    public String drag(String startElement, String startRef, String endElement, String endRef) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("startElement", startElement);
        arguments.put("startRef", startRef);
        arguments.put("endElement", endElement);
        arguments.put("endRef", endRef);
        return callTool("browser_drag", arguments);
    }

    public String selectOption(String element, String ref, String[] values) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("element", element);
        arguments.put("ref", ref);
        arguments.put("values", values);
        return callTool("browser_select_option", arguments);
    }
}
