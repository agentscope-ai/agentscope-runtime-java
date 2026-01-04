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

import com.aliyun.agentbay.browser.ActResult;
import com.aliyun.agentbay.browser.BrowserOption;
import com.aliyun.agentbay.exception.BrowserException;
import com.aliyun.agentbay.model.*;
import com.aliyun.agentbay.session.Session;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.client.AgentBayClient;
import io.agentscope.runtime.sandbox.manager.model.container.ContainerModel;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxType;
import io.agentscope.runtime.sandbox.manager.registry.RegisterSandbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@RegisterSandbox(
        imageName = "agentbay-cloud",
        sandboxType = SandboxType.AGENTBAY,
        securityLevel = "high",
        timeout = 300,
        description = "AgentBay Cloud Sandbox Environment"
)
public class AgentBaySandbox extends CloudSandbox {

    private static final Logger logger = LoggerFactory.getLogger(AgentBaySandbox.class);
    private String imageId;
    private Map<String, String> labels;

    public AgentBaySandbox(SandboxManager managerApi, String userId, String sessionId) {
        this(managerApi, userId, sessionId, "linux_latest");
    }

    public AgentBaySandbox(SandboxManager managerApi, String userId, String sessionId, String imageId) {
        this(managerApi, userId, sessionId, 3000, imageId, null);
    }

    public AgentBaySandbox(SandboxManager managerApi, String userId, String sessionId,
                           int timeout, String imageId, Map<String, String> labels) {
        super(managerApi, userId, sessionId, timeout, SandboxType.AGENTBAY);
        if(imageId == null || imageId.isEmpty()){
            this.imageId = "linux_latest";
        } else {
            this.imageId = imageId;
        }
        try {
            ContainerModel containerModel = managerApi.createFromPool(sandboxType, userId, sessionId, imageId, labels);
            if (containerModel == null) {
                throw new RuntimeException(
                        "No sandbox available. Please check if sandbox images exist."
                );
            }
            this.sandboxId = containerModel.getContainerId();
            logger.info("Sandbox initialized: {} (type={}, user={}, session={}, autoRelease={})", this.sandboxId, sandboxType, userId, sessionId, autoRelease);
        } catch (Exception e) {
            logger.error("Failed to initialize sandbox: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize sandbox", e);
        }
    }

    public Map<String, Object> getSessionInfo() {
        AgentBayClient agentBay = this.managerApi.getAgentBayClient();
        if (agentBay == null) {
            throw new RuntimeException("AgentBay client is not initialized.");
        }
        return agentBay.getSessionInfo(this.sandboxId);
    }

    public String getCloudProviderName() {
        return "AgentBay";
    }

    public Map<String, Object> listTools() {
        return listTools(null);
    }

    public Map<String, Object> listTools(String toolType) {
        Map<String, List<String>> toolsByType = Map.of(
                "file", List.of("read_file", "write_file", "list_directory", "create_directory", "move_file", "delete_file"),
                "command", List.of("run_shell_command", "run_ipython_cell"),
                "browser", List.of("browser_navigate", "browser_click", "browser_input"),
                "system", List.of("screenshot")
        );

        if (toolType != null) {
            List<String> tools = toolsByType.getOrDefault(toolType, Collections.emptyList());
            return Map.of(
                    "tools", tools,
                    "tool_type", toolType,
                    "sandbox_id", this.sandboxId,
                    "total_count", tools.size()
            );
        }

        List<String> allTools = toolsByType.values().stream().flatMap(List::stream).toList();
        return Map.of(
                "tools", allTools,
                "tools_by_type", toolsByType,
                "tool_type", "all",
                "sandbox_id", this.sandboxId,
                "total_count", allTools.size()
        );
    }

    public String callCloudTool(String toolName, Map<String, Object> parameters) {
        Session session = getSession();
        if (session == null) {
            logger.error("AgentBay session not found: {}", this.sandboxId);
            return "AgentBay session not found: " + this.sandboxId;
        }
        if(Objects.equals(toolName, "run_shell_command")){
            String command = (String) parameters.getOrDefault("command", "");
            return runShellCommand(command);
        } else if(Objects.equals(toolName, "run_ipython_cell")){
            String code = (String) parameters.getOrDefault("code", "");
            return runIpythonCell(code);
        } else if(Objects.equals(toolName, "read_file")){
            String path = (String) parameters.getOrDefault("path", "");
            return readFile(path);
        } else if(Objects.equals(toolName, "write_file")){
            String path = (String) parameters.getOrDefault("path", "");
            String content = (String) parameters.getOrDefault("content", "");
            return writeFile(path, content);
        } else if(Objects.equals(toolName, "list_directory")){
            String path = (String) parameters.getOrDefault("path", "");
            return listDirectory(path);
        } else if(Objects.equals(toolName, "create_directory")){
            String path = (String) parameters.getOrDefault("path", "");
            return createDirectory(path);
        } else if(Objects.equals(toolName, "move_file")){
            String source = (String) parameters.getOrDefault("source", "");
            String destination = (String) parameters.getOrDefault("destination", "");
            return moveFile(source, destination);
        } else if(Objects.equals(toolName, "delete_file")){
            String path = (String) parameters.getOrDefault("path", "");
            return deleteFile(path);
        } else if(Objects.equals(toolName, "screenshot")){
            return takeScreenShot();
        } else if(Objects.equals(toolName, "browser_navigate")){
            String url = (String) parameters.getOrDefault("url", "");
            return browserNavigate(url);
        } else if(Objects.equals(toolName, "browser_click")){
            String selector = (String) parameters.getOrDefault("selector", "");
            return browserClick(selector);
        } else {
            return "Tool not supported: " + toolName;
        }
    }

    public Session getSession() {
        AgentBayClient agentBay = this.managerApi.getAgentBayClient();
        if (agentBay == null) {
            throw new RuntimeException("AgentBay client is not initialized.");
        }
        return agentBay.getSession(this.sandboxId);
    }

    /**
     * Execute IPython code
     */
    public String runIpythonCell(String code) {
        Session session = getSession();
        code = code == null ? "" : code;
        if (session == null) {
            logger.error("AgentBay session not found: {}", this.sandboxId);
            return "AgentBay session not found: " + this.sandboxId;
        }
        CodeExecutionResult result = session.getCode().runCode(code, "python");
        Map<String, Object> response = Map.of(
                "success", result.isSuccess(),
                "output", result.getResult(),
                "error", result.getErrorMessage() != null ? result.getErrorMessage() : ""
        );
        return response.toString();
    }

    /**
     * Execute shell command
     */
    public String runShellCommand(String command) {
        Session session = getSession();
        command = command == null ? "" : command;
        if (session == null) {
            logger.error("AgentBay session not found: {}", this.sandboxId);
            return "AgentBay session not found: " + this.sandboxId;
        }
        CommandResult result = session.getCommand().executeCommand(command, 1000);
        Map<String, Object> response = Map.of(
                "success", result.isSuccess(),
                "output", result.getOutput(),
                "error", result.getErrorMessage() != null ? result.getErrorMessage() : "",
                "exitCode", result.getExitCode()
        );
        return response.toString();
    }

    public String readFile(String path) {
        Session session = getSession();
        path = path == null ? "" : path;
        if (session == null) {
            logger.error("AgentBay session not found: {}", this.sandboxId);
            return "AgentBay session not found: " + this.sandboxId;
        }
        FileContentResult result = session.getFileSystem().readFile(path);
        Map<String, Object> response = Map.of(
                "success", result.isSuccess(),
                "content", result.getContent() != null ? result.getContent() : "",
                "error", result.getErrorMessage() != null ? result.getErrorMessage() : ""
        );
        return response.toString();
    }

    public String writeFile(String path, String content) {
        Session session = getSession();
        path = path == null ? "" : path;
        content = content == null ? "" : content;
        if (session == null) {
            logger.error("AgentBay session not found: {}", this.sandboxId);
            return "AgentBay session not found: " + this.sandboxId;
        }
        BoolResult result = session.getFileSystem().writeFile(path, content);
        Map<String, Object> response = Map.of(
                "success", result.isSuccess(),
                "error", result.getErrorMessage() != null ? result.getErrorMessage() : ""
        );
        return response.toString();
    }

    public String listDirectory(String path) {
        Session session = getSession();
        path = path == null ? "" : path;
        if (session == null) {
            logger.error("AgentBay session not found: {}", this.sandboxId);
            return "AgentBay session not found: " + this.sandboxId;
        }
        DirectoryListResult result = session.getFileSystem().listDirectory(path);
        Map<String, Object> response = Map.of(
                "success", result.isSuccess(),
                "files", result.getFiles() != null ? result.getFiles() : List.of(),
                "error", result.getErrorMessage() != null ? result.getErrorMessage() : ""
        );
        return response.toString();
    }

    public String moveFile(String source, String destination) {
        Session session = getSession();
        source = source == null ? "" : source;
        destination = destination == null ? "" : destination;
        if (session == null) {
            logger.error("AgentBay session not found: {}", this.sandboxId);
            return "AgentBay session not found: " + this.sandboxId;
        }
        BoolResult result = session.getFileSystem().moveFile(source, destination);
        Map<String, Object> response = Map.of(
                "success", result.isSuccess(),
                "error", result.getErrorMessage() != null ? result.getErrorMessage() : ""
        );
        return response.toString();
    }

    public String deleteFile(String path) {
        Session session = getSession();
        path = path == null ? "" : path;
        if (session == null) {
            logger.error("AgentBay session not found: {}", this.sandboxId);
            return "AgentBay session not found: " + this.sandboxId;
        }
        DeleteResult result = session.getFileSystem().deleteFile(path);
        Map<String, Object> response = Map.of(
                "success", result.isSuccess(),
                "error", result.getErrorMessage() != null ? result.getErrorMessage() : ""
        );
        return response.toString();
    }

    public String createDirectory(String path) {
        Session session = getSession();
        path = path == null ? "" : path;
        if (session == null) {
            logger.error("AgentBay session not found: {}", this.sandboxId);
            return "AgentBay session not found: " + this.sandboxId;
        }
        BoolResult result = session.getFileSystem().createDirectory(path);
        Map<String, Object> response = Map.of(
                "success", result.isSuccess(),
                "error", result.getErrorMessage() != null ? result.getErrorMessage() : ""
        );
        return response.toString();
    }

    public String takeScreenShot() {
        Session session = getSession();
        if (session == null) {
            logger.error("AgentBay session not found: {}", this.sandboxId);
            return "AgentBay session not found: " + this.sandboxId;
        }
        OperationResult result = session.getComputer().screenshot();
        Map<String, Object> response = Map.of(
                "success", result.isSuccess(),
                "screenshotUrl", result.getData() != null ? result.getData() : "",
                "error", result.getErrorMessage() != null ? result.getErrorMessage() : ""
        );
        return response.toString();
    }

    public String browserNavigate(String url) {
        Session session = getSession();
        url = url == null ? "" : url;
        if (session == null) {
            logger.error("AgentBay session not found: {}", this.sandboxId);
            return "AgentBay session not found: " + this.sandboxId;
        }
        Map<String, Object> response = new HashMap<>();
        String result = "";
        try {
            String endpointUrl = session.getBrowser().getEndpointUrl();

            try (Playwright playwright = Playwright.create()) {
                Browser browser = playwright.chromium().connectOverCDP(endpointUrl);

                Page page = browser.newPage();

                // Navigate to a website
                page.navigate(url);

                // Wait for page load
                page.waitForTimeout(2000);

                result = page.content();
                browser.close();
            } catch (Exception e) {
                logger.error("Playwright integration failed: {}", e.getMessage());
            }
            response = Map.of(
                    "success", true,
                    "content", result,
                    "error", ""
            );
        } catch (Exception e) {
            logger.error("Browser navigate failed: {}", e.getMessage());
            response = Map.of(
                    "success", false,
                    "error", e.getMessage() != null ? e.getMessage() : ""
            );
        }
        return response.toString();
    }

    public String browserClick(String selector) {
        Session session = getSession();
        selector = selector == null ? "" : selector;
        if (session == null) {
            logger.error("AgentBay session not found: {}", this.sandboxId);
            return "AgentBay session not found: " + this.sandboxId;
        }
        Map<String, Object> response = new HashMap<>();
        ActResult result = null;
        try {
            session.getBrowser().initialize(new BrowserOption());
            result = session.getBrowser().getAgent().click(null, selector);
            response = Map.of(
                    "success", result.isSuccess(),
                    "error", result.getMessage() != null ? result.getMessage() : ""
            );
        } catch (BrowserException e) {
            logger.error("Browser click failed: {}", e.getMessage());
            response = Map.of(
                    "success", false,
                    "error", e.getMessage() != null ? e.getMessage() : ""
            );
        }
        return response.toString();
    }

    public String genericToolCall(String toolName, Map<String, Object> arguments) {
        // Todo: sdk not support yet
        return "";
    }

    public String browserInput(String selector, String inputText) {
        // Todo: sdk not support yet
        return "";
    }
}
