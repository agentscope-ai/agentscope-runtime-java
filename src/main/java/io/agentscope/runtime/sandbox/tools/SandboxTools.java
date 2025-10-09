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
package io.agentscope.runtime.sandbox.tools;

import io.agentscope.runtime.sandbox.manager.model.ContainerModel;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.SandboxType;
import io.agentscope.runtime.sandbox.tools.model.ShellCommandRequest;
import io.agentscope.runtime.sandbox.manager.util.HttpClient;
import io.agentscope.runtime.sandbox.tools.model.IpythonRequest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Sandbox tools class, providing various sandbox operation functions
 */
public class SandboxTools {
    private final Logger logger = Logger.getLogger(SandboxTools.class.getName());
    private final SandboxManager sandboxManager;
    private final HttpClient httpClient;
    
    // Use singleton pattern to ensure all instances share the same SandboxManager
    private static final SandboxManager SHARED_SANDBOX_MANAGER = new SandboxManager();

    public SandboxTools() {
        this.sandboxManager = SHARED_SANDBOX_MANAGER;
        this.httpClient = new HttpClient();
    }

    /**
     * Get the shared SandboxManager instance
     * @return SandboxManager instance
     */
    public SandboxManager getSandboxManager() {
        return sandboxManager;
    }

    /**
     * Execute IPython code
     *
     * @param code Python code to execute
     * @return execution result
     */
    public String run_ipython_cell(String code) {
        try {
            // Get sandbox
            SandboxType sandboxType = SandboxType.FILESYSTEM;
            ContainerModel sandbox = sandboxManager.getSandbox(sandboxType);

            // Ensure sandbox is running
            if (!isSandboxRunning(sandboxType)) {
                logger.info("Sandbox is not running, starting...");
                sandboxManager.startSandbox(sandboxType);
            }

            // Build request URL
            String baseUrl = sandbox.getBaseUrl();
            String authToken = sandbox.getAuthToken();
            String requestUrl = baseUrl + "/tools/run_ipython_cell";

            // Health check wait
            waitUntilHealthy(sandbox);

            // Build request headers
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + authToken);
            headers.put("Content-Type", "application/json");
            headers.put("Host", "localhost:" + sandbox.getPorts()[0]);

            // Build request body
            IpythonRequest request = new IpythonRequest(code);

            // Send request
            String response = httpClient.postJson(requestUrl, headers, request);

            return response;

        } catch (Exception e) {
            String errorMsg = "Run Python Code Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }

    public String run_shell_command(String command) {
        try {
            // Get sandbox
            SandboxType sandboxType = SandboxType.FILESYSTEM;
            ContainerModel sandbox = sandboxManager.getSandbox(sandboxType);

            // Ensure sandbox is running
            if (!isSandboxRunning(sandboxType)) {
                logger.info("Sandbox is not running, starting...");
                sandboxManager.startSandbox(sandboxType);
            }

            // Build request URL
            String baseUrl = sandbox.getBaseUrl();
            String authToken = sandbox.getAuthToken();
            String requestUrl = baseUrl + "/tools/run_shell_command";

            // Health check wait
            waitUntilHealthy(sandbox);

            // Build request headers
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + authToken);
            headers.put("Content-Type", "application/json");
            headers.put("Host", "localhost:" + sandbox.getPorts()[0]);

            // Build request body
            ShellCommandRequest request = new ShellCommandRequest(command);

            return httpClient.postJson(requestUrl, headers, request);

        } catch (Exception e) {
            String errorMsg = "Run Shell Command Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }

    /**
     * General: call sandbox MCP tool
     *
     * @param toolName tool name (such as read_file, write_file, etc.)
     * @param arguments parameter Map
     * @return execution result JSON string
     */
    public String call_mcp_tool(String toolName, Map<String, Object> arguments) {
        return call_mcp_tool(SandboxType.FILESYSTEM, toolName, arguments);
    }

    public String call_mcp_tool(SandboxType sandboxType, String toolName, Map<String, Object> arguments) {
        try {
            ContainerModel sandbox = sandboxManager.getSandbox(sandboxType);

            if (!isSandboxRunning(sandboxType)) {
                logger.info("Sandbox is not running, starting...");
                sandboxManager.startSandbox(sandboxType);
            }

            String baseUrl = sandbox.getBaseUrl();
            String authToken = sandbox.getAuthToken();
            String requestUrl = baseUrl + "/mcp/call_tool";

            // Health check wait
            waitUntilHealthy(sandbox);

            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + authToken);
            headers.put("Content-Type", "application/json");
            headers.put("Host", "localhost:" + sandbox.getPorts()[0]);

            Map<String, Object> body = new HashMap<>();
            body.put("tool_name", toolName);
            body.put("arguments", arguments == null ? new HashMap<>() : arguments);

            return httpClient.postJson(requestUrl, headers, body);
        } catch (Exception e) {
            String errorMsg = "Call MCP Tool Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }

    // Below are convenient wrappers for filesystem tools
    public String fs_read_file(String path) {
        Map<String, Object> args = new HashMap<>();
        args.put("path", path);
        return call_mcp_tool("read_file", args);
    }

    public String fs_read_multiple_files(String[] paths) {
        Map<String, Object> args = new HashMap<>();
        args.put("paths", paths);
        return call_mcp_tool("read_multiple_files", args);
    }

    public String fs_write_file(String path, String content) {
        Map<String, Object> args = new HashMap<>();
        args.put("path", path);
        args.put("content", content);
        return call_mcp_tool("write_file", args);
    }

    public String fs_edit_file(String path, Object[] edits) {
        Map<String, Object> args = new HashMap<>();
        args.put("path", path);
        args.put("edits", edits);
        return call_mcp_tool("edit_file", args);
    }

    public String fs_create_directory(String path) {
        Map<String, Object> args = new HashMap<>();
        args.put("path", path);
        return call_mcp_tool("create_directory", args);
    }

    public String fs_list_directory(String path) {
        Map<String, Object> args = new HashMap<>();
        args.put("path", path);
        return call_mcp_tool("list_directory", args);
    }

    public String fs_directory_tree(String path) {
        Map<String, Object> args = new HashMap<>();
        args.put("path", path);
        return call_mcp_tool("directory_tree", args);
    }

    public String fs_move_file(String source, String destination) {
        Map<String, Object> args = new HashMap<>();
        args.put("source", source);
        args.put("destination", destination);
        return call_mcp_tool("move_file", args);
    }

    public String fs_search_files(String path, String pattern, String[] excludePatterns) {
        Map<String, Object> args = new HashMap<>();
        args.put("path", path);
        args.put("pattern", pattern);
        if (excludePatterns != null) {
            args.put("excludePatterns", excludePatterns);
        }
        else{
            args.put("excludePatterns", new String[]{});
        }
        return call_mcp_tool("search_files", args);
    }

    public String fs_get_file_info(String path) {
        Map<String, Object> args = new HashMap<>();
        args.put("path", path);
        return call_mcp_tool("get_file_info", args);
    }

    public String fs_list_allowed_directories() {
        return call_mcp_tool("list_allowed_directories", new HashMap<>());
    }

    // browser tool wrappers
    public String browser_navigate(String url) {
        Map<String, Object> args = new HashMap<>();
        args.put("url", url);
        return call_mcp_tool(SandboxType.BROWSER, "browser_navigate", args);
    }

    public String browser_console_messages_tool(){
        return call_mcp_tool(SandboxType.BROWSER, "browser_console_messages", new HashMap<>());
    }

    public String browser_click(String element, String ref) {
        Map<String, Object> args = new HashMap<>();
        args.put("element", element);
        args.put("ref", ref);
        return call_mcp_tool(SandboxType.BROWSER, "browser_click", args);
    }

    public String browser_type(String element, String ref, String text, Boolean submit, Boolean slowly) {
        Map<String, Object> args = new HashMap<>();
        args.put("element", element);
        args.put("ref", ref);
        args.put("text", text);
        if (submit != null) args.put("submit", submit);
        if (slowly != null) args.put("slowly", slowly);
        return call_mcp_tool(SandboxType.BROWSER, "browser_type", args);
    }

    public String browser_take_screenshot(Boolean raw, String filename, String element, String ref) {
        Map<String, Object> args = new HashMap<>();
        if (raw != null) args.put("raw", raw);
        if (filename != null) args.put("filename", filename);
        if (element != null) args.put("element", element);
        if (ref != null) args.put("ref", ref);
        return call_mcp_tool(SandboxType.BROWSER, "browser_take_screenshot", args);
    }

    public String browser_snapshot() {
        return call_mcp_tool(SandboxType.BROWSER, "browser_snapshot", new HashMap<>());
    }

    public String browser_tab_new(String url) {
        Map<String, Object> args = new HashMap<>();
        if (url != null) args.put("url", url);
        return call_mcp_tool(SandboxType.BROWSER, "browser_tab_new", args);
    }

    public String browser_tab_select(Integer index) {
        Map<String, Object> args = new HashMap<>();
        if (index != null) args.put("index", index);
        return call_mcp_tool(SandboxType.BROWSER, "browser_tab_select", args);
    }

    public String browser_tab_close(Integer index) {
        Map<String, Object> args = new HashMap<>();
        if (index != null) args.put("index", index);
        return call_mcp_tool(SandboxType.BROWSER, "browser_tab_close", args);
    }

    public String browser_wait_for(Double time, String text, String textGone) {
        Map<String, Object> args = new HashMap<>();
        if (time != null) args.put("time", time);
        if (text != null) args.put("text", text);
        if (textGone != null) args.put("textGone", textGone);
        return call_mcp_tool(SandboxType.BROWSER, "browser_wait_for", args);
    }

    public String browser_resize(Double width, Double height) {
        Map<String, Object> args = new HashMap<>();
        args.put("width", width);
        args.put("height", height);
        return call_mcp_tool(SandboxType.BROWSER, "browser_resize", args);
    }

    public String browser_close() {
        return call_mcp_tool(SandboxType.BROWSER, "browser_close", new HashMap<>());
    }

    public String browser_handle_dialog(Boolean accept, String promptText) {
        Map<String, Object> args = new HashMap<>();
        args.put("accept", accept);
        if (promptText != null) {
            args.put("promptText", promptText);
        }
        return call_mcp_tool(SandboxType.BROWSER, "browser_handle_dialog", args);
    }

    public String browser_file_upload(String[] paths) {
        Map<String, Object> args = new HashMap<>();
        args.put("paths", paths);
        return call_mcp_tool(SandboxType.BROWSER, "browser_file_upload", args);
    }

    public String browser_press_key(String key) {
        Map<String, Object> args = new HashMap<>();
        args.put("key", key);
        return call_mcp_tool(SandboxType.BROWSER, "browser_press_key", args);
    }

    public String browser_navigate_back() {
        return call_mcp_tool(SandboxType.BROWSER, "browser_navigate_back", new HashMap<>());
    }

    public String browser_navigate_forward() {
        return call_mcp_tool(SandboxType.BROWSER, "browser_navigate_forward", new HashMap<>());
    }

    public String browser_network_requests() {
        return call_mcp_tool(SandboxType.BROWSER, "browser_network_requests", new HashMap<>());
    }

    public String browser_pdf_save(String filename) {
        Map<String, Object> args = new HashMap<>();
        if (filename != null) {
            args.put("filename", filename);
        }
        return call_mcp_tool(SandboxType.BROWSER, "browser_pdf_save", args);
    }

    public String browser_drag(String startElement, String startRef, String endElement, String endRef) {
        Map<String, Object> args = new HashMap<>();
        args.put("startElement", startElement);
        args.put("startRef", startRef);
        args.put("endElement", endElement);
        args.put("endRef", endRef);
        return call_mcp_tool(SandboxType.BROWSER, "browser_drag", args);
    }

    public String browser_hover(String element, String ref) {
        Map<String, Object> args = new HashMap<>();
        args.put("element", element);
        args.put("ref", ref);
        return call_mcp_tool(SandboxType.BROWSER, "browser_hover", args);
    }

    public String browser_select_option(String element, String ref, String[] values) {
        Map<String, Object> args = new HashMap<>();
        args.put("element", element);
        args.put("ref", ref);
        args.put("values", values);
        return call_mcp_tool(SandboxType.BROWSER, "browser_select_option", args);
    }

    public String browser_tab_list() {
        return call_mcp_tool(SandboxType.BROWSER, "browser_tab_list", new HashMap<>());
    }

    /**
     * Check if sandbox is running
     *
     * @param sandboxType sandbox model
     * @return whether it is running
     */
    private boolean isSandboxRunning(SandboxType sandboxType) {
        try {
            String status = sandboxManager.getSandboxStatus(sandboxType);
            return "running".equals(status);
        } catch (Exception e) {
            System.err.println("Failed to check sandbox status: " + e.getMessage());
            return false;
        }
    }

    /**
     * Wait for API service inside container to be healthy (/healthz returns 200)
     */
    private void waitUntilHealthy(ContainerModel sandbox) {
        String baseUrl = sandbox.getBaseUrl();
        String authToken = sandbox.getAuthToken();
        String healthUrl = baseUrl + "/healthz";

        Map<String, String> headers = new HashMap<>();
        if (authToken != null) {
            headers.put("Authorization", "Bearer " + authToken);
        }
        headers.put("Host", "localhost:" + sandbox.getPorts()[0]);

        long start = System.currentTimeMillis();
        long timeoutMs = 60_000;
        long sleepMs = 700;
        try {
            Thread.sleep(1500); // Container process cold start wait
        } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        while (System.currentTimeMillis() - start < timeoutMs) {
            try {
                String resp = httpClient.get(healthUrl, headers);
                if (resp != null && !resp.isEmpty()) {
                    return;
                }
            } catch (Exception ignored) {
                // ignore and retry
            }
            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Close resources
     */
    public void close() {
        try {
            if (httpClient != null) {
                httpClient.close();
            }
        } catch (IOException e) {
            System.err.println("Failed to close HTTP client: " + e.getMessage());
        }
    }
}

