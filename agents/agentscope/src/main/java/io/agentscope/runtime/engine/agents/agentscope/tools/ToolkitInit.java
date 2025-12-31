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

package io.agentscope.runtime.engine.agents.agentscope.tools;

import io.agentscope.core.tool.AgentTool;
import io.agentscope.runtime.engine.agents.agentscope.tools.base.AsBasePythonRunner;
import io.agentscope.runtime.engine.agents.agentscope.tools.base.AsBaseShellRunner;
import io.agentscope.runtime.engine.agents.agentscope.tools.browser.*;
import io.agentscope.runtime.engine.agents.agentscope.tools.fs.*;
import io.agentscope.runtime.engine.agents.agentscope.tools.mcp.AsMCPTool;
import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxType;
import io.agentscope.runtime.sandbox.tools.MCPTool;
import io.agentscope.runtime.sandbox.tools.McpConfigConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ToolkitInit {
    private static final Logger logger = LoggerFactory.getLogger(ToolkitInit.class);

    public static List<AgentTool> getAllTools(Sandbox sandbox) {
        return List.of(
                RunPythonCodeTool(sandbox),
                RunShellCommandTool(sandbox),
                ReadFileTool(sandbox),
                ReadMultipleFilesTool(sandbox),
                WriteFileTool(sandbox),
                EditFileTool(sandbox),
                CreateDirectoryTool(sandbox),
                ListDirectoryTool(sandbox),
                DirectoryTreeTool(sandbox),
                MoveFileTool(sandbox),
                SearchFilesTool(sandbox),
                GetFileInfoTool(sandbox),
                ListAllowedDirectoriesTool(sandbox),
                BrowserNavigateTool(sandbox),
                BrowserClickTool(sandbox),
                BrowserTypeTool(sandbox),
                BrowserTakeScreenshotTool(sandbox),
                BrowserSnapshotTool(sandbox),
                BrowserTabNewTool(sandbox),
                BrowserTabSelectTool(sandbox),
                BrowserTabCloseTool(sandbox),
                BrowserWaitForTool(sandbox),
                BrowserResizeTool(sandbox),
                BrowserCloseTool(sandbox),
                BrowserConsoleMessagesTool(sandbox),
                BrowserHandleDialogTool(sandbox),
                BrowserFileUploadTool(sandbox),
                BrowserPressKeyTool(sandbox),
                BrowserNavigateBackTool(sandbox),
                BrowserNavigateForwardTool(sandbox),
                BrowserNetworkRequestsTool(sandbox),
                BrowserPdfSaveTool(sandbox),
                BrowserDragTool(sandbox),
                BrowserHoverTool(sandbox),
                BrowserSelectOptionTool(sandbox),
                BrowserTabListTool(sandbox)
        );
    }

    // Base tools
    public static AgentTool RunPythonCodeTool(Sandbox sandbox) {
        AsBasePythonRunner asBasePythonRunner = new AsBasePythonRunner();
        asBasePythonRunner.setSandbox(sandbox);
        return asBasePythonRunner;
    }

    public static AgentTool RunShellCommandTool(Sandbox sandbox) {
        AsBaseShellRunner asBaseShellRunner = new AsBaseShellRunner();
        asBaseShellRunner.setSandbox(sandbox);
        return asBaseShellRunner;
    }

    // Browser tools
    public static AgentTool BrowserNavigateTool(Sandbox sandbox) {
        AsBrowserNavigator asBrowserNavigator = new AsBrowserNavigator();
        asBrowserNavigator.setSandbox(sandbox);
        return asBrowserNavigator;
    }

    public static AgentTool BrowserClickTool(Sandbox sandbox) {
        AsBrowserClicker asBrowserClicker = new AsBrowserClicker();
        asBrowserClicker.setSandbox(sandbox);
        return asBrowserClicker;
    }

    public static AgentTool BrowserTypeTool(Sandbox sandbox) {
        AsBrowserTyper asBrowserTyper = new AsBrowserTyper();
        asBrowserTyper.setSandbox(sandbox);
        return asBrowserTyper;
    }

    public static AgentTool BrowserSnapshotTool(Sandbox sandbox) {
        AsBrowserSnapshotTaker asBrowserSnapshotTaker = new AsBrowserSnapshotTaker();
        asBrowserSnapshotTaker.setSandbox(sandbox);
        return asBrowserSnapshotTaker;
    }

    public static AgentTool BrowserTakeScreenshotTool(Sandbox sandbox) {
        AsBrowserScreenshotTaker asBrowserScreenshotTaker = new AsBrowserScreenshotTaker();
        asBrowserScreenshotTaker.setSandbox(sandbox);
        return asBrowserScreenshotTaker;
    }

    public static AgentTool BrowserCloseTool(Sandbox sandbox) {
        AsBrowserCloser asBrowserCloser = new AsBrowserCloser();
        asBrowserCloser.setSandbox(sandbox);
        return asBrowserCloser;
    }

    public static AgentTool BrowserHoverTool(Sandbox sandbox) {
        AsBrowserHoverer asBrowserHoverer = new AsBrowserHoverer();
        asBrowserHoverer.setSandbox(sandbox);
        return asBrowserHoverer;
    }

    public static AgentTool BrowserDragTool(Sandbox sandbox) {
        AsBrowserDragger asBrowserDragger = new AsBrowserDragger();
        asBrowserDragger.setSandbox(sandbox);
        return asBrowserDragger;
    }

    public static AgentTool BrowserConsoleMessagesTool(Sandbox sandbox) {
        AsBrowserConsoleMessagesRetriever asBrowserConsoleMessagesRetriever = new AsBrowserConsoleMessagesRetriever();
        asBrowserConsoleMessagesRetriever.setSandbox(sandbox);
        return asBrowserConsoleMessagesRetriever;
    }

    public static AgentTool BrowserFileUploadTool(Sandbox sandbox) {
        AsBrowserFileUploader asBrowserFileUploader = new AsBrowserFileUploader();
        asBrowserFileUploader.setSandbox(sandbox);
        return asBrowserFileUploader;
    }

    public static AgentTool BrowserHandleDialogTool(Sandbox sandbox) {
        AsBrowserDialogHandler asBrowserDialogHandler = new AsBrowserDialogHandler();
        asBrowserDialogHandler.setSandbox(sandbox);
        return asBrowserDialogHandler;
    }

    public static AgentTool BrowserNavigateBackTool(Sandbox sandbox) {
        AsBrowserBackNavigator asBrowserBackNavigator = new AsBrowserBackNavigator();
        asBrowserBackNavigator.setSandbox(sandbox);
        return asBrowserBackNavigator;
    }

    public static AgentTool BrowserNavigateForwardTool(Sandbox sandbox) {
        AsBrowserForwardNavigator asBrowserForwardNavigator = new AsBrowserForwardNavigator();
        asBrowserForwardNavigator.setSandbox(sandbox);
        return asBrowserForwardNavigator;
    }

    public static AgentTool BrowserNetworkRequestsTool(Sandbox sandbox) {
        AsBrowserNetworkRequestsRetriever asBrowserNetworkRequestsRetriever = new AsBrowserNetworkRequestsRetriever();
        asBrowserNetworkRequestsRetriever.setSandbox(sandbox);
        return asBrowserNetworkRequestsRetriever;
    }

    public static AgentTool BrowserPdfSaveTool(Sandbox sandbox) {
        AsBrowserPdfSaver asBrowserPdfSaver = new AsBrowserPdfSaver();
        asBrowserPdfSaver.setSandbox(sandbox);
        return asBrowserPdfSaver;
    }

    public static AgentTool BrowserPressKeyTool(Sandbox sandbox) {
        AsBrowserKeyPresser asBrowserKeyPresser = new AsBrowserKeyPresser();
        asBrowserKeyPresser.setSandbox(sandbox);
        return asBrowserKeyPresser;
    }

    public static AgentTool BrowserResizeTool(Sandbox sandbox) {
        AsBrowserResizer asBrowserResizer = new AsBrowserResizer();
        asBrowserResizer.setSandbox(sandbox);
        return asBrowserResizer;
    }

    public static AgentTool BrowserSelectOptionTool(Sandbox sandbox) {
        AsBrowserOptionSelector asBrowserOptionSelector = new AsBrowserOptionSelector();
        asBrowserOptionSelector.setSandbox(sandbox);
        return asBrowserOptionSelector;
    }

    public static AgentTool BrowserTabCloseTool(Sandbox sandbox) {
        AsBrowserTabCloser asBrowserTabCloser = new AsBrowserTabCloser();
        asBrowserTabCloser.setSandbox(sandbox);
        return asBrowserTabCloser;
    }

    public static AgentTool BrowserTabListTool(Sandbox sandbox) {
        AsBrowserTabLister asBrowserTabLister = new AsBrowserTabLister();
        asBrowserTabLister.setSandbox(sandbox);
        return asBrowserTabLister;
    }

    public static AgentTool BrowserTabNewTool(Sandbox sandbox) {
        AsBrowserTabCreator asBrowserTabCreator = new AsBrowserTabCreator();
        asBrowserTabCreator.setSandbox(sandbox);
        return asBrowserTabCreator;
    }

    public static AgentTool BrowserTabSelectTool(Sandbox sandbox) {
        AsBrowserTabSelector asBrowserTabSelector = new AsBrowserTabSelector();
        asBrowserTabSelector.setSandbox(sandbox);
        return asBrowserTabSelector;
    }

    public static AgentTool BrowserWaitForTool(Sandbox sandbox) {
        AsBrowserWaiter asBrowserWaiter = new AsBrowserWaiter();
        asBrowserWaiter.setSandbox(sandbox);
        return asBrowserWaiter;
    }

    // Filesystem tools
    public static AgentTool ReadFileTool(Sandbox sandbox) {
        AsFsReadFile asFsReadFile = new AsFsReadFile();
        asFsReadFile.setSandbox(sandbox);
        return asFsReadFile;
    }

    public static AgentTool WriteFileTool(Sandbox sandbox) {
        AsFsWriteFile asFsWriteFile = new AsFsWriteFile();
        asFsWriteFile.setSandbox(sandbox);
        return asFsWriteFile;
    }

    public static AgentTool ListDirectoryTool(Sandbox sandbox) {
        AsFsListDirectory asFsListDirectory = new AsFsListDirectory();
        asFsListDirectory.setSandbox(sandbox);
        return asFsListDirectory;
    }

    public static AgentTool CreateDirectoryTool(Sandbox sandbox) {
        AsFsCreateDirectory asFsCreateDirectory = new AsFsCreateDirectory();
        asFsCreateDirectory.setSandbox(sandbox);
        return asFsCreateDirectory;
    }

    public static AgentTool DirectoryTreeTool(Sandbox sandbox) {
        AsFsDirectoryTree asFsDirectoryTree = new AsFsDirectoryTree();
        asFsDirectoryTree.setSandbox(sandbox);
        return asFsDirectoryTree;
    }

    public static AgentTool EditFileTool(Sandbox sandbox) {
        AsFsEditFile asFsEditFile = new AsFsEditFile();
        asFsEditFile.setSandbox(sandbox);
        return asFsEditFile;
    }

    public static AgentTool GetFileInfoTool(Sandbox sandbox) {
        AsFsGetFileInfo asFsGetFileInfo = new AsFsGetFileInfo();
        asFsGetFileInfo.setSandbox(sandbox);
        return asFsGetFileInfo;
    }

    public static AgentTool ListAllowedDirectoriesTool(Sandbox sandbox) {
        AsFsListAllowedDirectories asFsListAllowedDirectories = new AsFsListAllowedDirectories();
        asFsListAllowedDirectories.setSandbox(sandbox);
        return asFsListAllowedDirectories;
    }

    public static AgentTool MoveFileTool(Sandbox sandbox) {
        AsFsMoveFile asFsMoveFile = new AsFsMoveFile();
        asFsMoveFile.setSandbox(sandbox);
        return asFsMoveFile;
    }

    public static AgentTool ReadMultipleFilesTool(Sandbox sandbox) {
        AsFsReadMultipleFiles asFsReadMultipleFiles = new AsFsReadMultipleFiles();
        asFsReadMultipleFiles.setSandbox(sandbox);
        return asFsReadMultipleFiles;
    }

    public static AgentTool SearchFilesTool(Sandbox sandbox) {
        AsFsSearchFiles asFsSearchFiles = new AsFsSearchFiles();
        asFsSearchFiles.setSandbox(sandbox);
        return asFsSearchFiles;
    }

    public static List<AgentTool> getMcpTools(String serverConfigs,
                                              SandboxType sandboxType,
                                              SandboxManager sandboxManager) {
        return getMcpTools(serverConfigs, sandboxType, sandboxManager, null, null);
    }

    public static List<AgentTool> getMcpTools(Map<String, Object> serverConfigs,
                                              SandboxType sandboxType,
                                              SandboxManager sandboxManager) {
        return getMcpTools(serverConfigs, sandboxType, sandboxManager, null, null);
    }

    public static List<AgentTool> getMcpTools(String serverConfigs,
                                              SandboxType sandboxType,
                                              SandboxManager sandboxManager,
                                              Set<String> whitelist,
                                              Set<String> blacklist) {
        McpConfigConverter converter = McpConfigConverter.builder()
                .serverConfigs(serverConfigs)
                .sandboxType(sandboxType)
                .sandboxManager(sandboxManager)
                .whitelist(whitelist)
                .blacklist(blacklist)
                .build();

        return buildMcpAgentTools(converter);
    }

    public static List<AgentTool> getMcpTools(Map<String, Object> serverConfigs,
                                              SandboxType sandboxType,
                                              SandboxManager sandboxManager,
                                              Set<String> whitelist,
                                              Set<String> blacklist) {
        McpConfigConverter converter = McpConfigConverter.builder()
                .serverConfigs(serverConfigs)
                .sandboxType(sandboxType)
                .sandboxManager(sandboxManager)
                .whitelist(whitelist)
                .blacklist(blacklist)
                .build();

        return buildMcpAgentTools(converter);
    }

    public static List<AgentTool> getMcpTools(String serverConfigs,
                                              SandboxManager sandboxManager) {
        return getMcpTools(serverConfigs, null, sandboxManager, null, null);
    }

    public static List<AgentTool> getMcpTools(Map<String, Object> serverConfigs,
                                              SandboxManager sandboxManager) {
        return getMcpTools(serverConfigs, null, sandboxManager, null, null);
    }

//    public static List<AgentTool> getAllToolsWithMcp(String mcpServerConfigs,
//                                                     SandboxType sandboxType,
//                                                     SandboxManager sandboxManager) {
//        List<AgentTool> allTools = new ArrayList<>(getAllTools());
//
//        if (mcpServerConfigs != null && !mcpServerConfigs.trim().isEmpty()) {
//            try {
//                List<AgentTool> mcpTools = getMcpTools(mcpServerConfigs, sandboxType, sandboxManager);
//                allTools.addAll(mcpTools);
//                logger.info(String.format("Added %d MCP tools to the toolkit", mcpTools.size()));
//            } catch (Exception e) {
//                logger.warning("Failed to add MCP tools: " + e.getMessage());
//            }
//        }
//
//        return allTools;
//    }

//    public static List<AgentTool> getAllToolsWithMcp(Map<String, Object> mcpServerConfigs,
//                                                     SandboxType sandboxType,
//                                                     SandboxManager sandboxManager) {
//        List<AgentTool> allTools = new ArrayList<>(getAllTools());
//
//        if (mcpServerConfigs != null && !mcpServerConfigs.isEmpty()) {
//            try {
//                List<AgentTool> mcpTools = getMcpTools(mcpServerConfigs, sandboxType, sandboxManager);
//                allTools.addAll(mcpTools);
//                logger.info(String.format("Added %d MCP tools to the toolkit", mcpTools.size()));
//            } catch (Exception e) {
//                logger.warning("Failed to add MCP tools: " + e.getMessage());
//            }
//        }
//
//        return allTools;
//    }

    public static List<MCPTool> createMcpToolInstances(String serverConfigs,
                                                       SandboxType sandboxType,
                                                       SandboxManager sandboxManager) {
        McpConfigConverter converter = McpConfigConverter.builder()
                .serverConfigs(serverConfigs)
                .sandboxType(sandboxType)
                .sandboxManager(sandboxManager)
                .build();

        return converter.toBuiltinTools();
    }

    public static List<MCPTool> createMcpToolInstances(Map<String, Object> serverConfigs,
                                                       SandboxType sandboxType,
                                                       SandboxManager sandboxManager) {
        McpConfigConverter converter = McpConfigConverter.builder()
                .serverConfigs(serverConfigs)
                .sandboxType(sandboxType)
                .sandboxManager(sandboxManager)
                .build();

        return converter.toBuiltinTools();
    }

    private static List<AgentTool> buildMcpAgentTools(McpConfigConverter converter) {
        try {
            logger.info("Creating MCP tools from server configuration");

            List<MCPTool> mcpTools = converter.toBuiltinTools();
            List<AgentTool> agentTools = new ArrayList<>(mcpTools.size());
            for (MCPTool mcpTool : mcpTools) {
                agentTools.add(new AsMCPTool(mcpTool));
            }

            logger.info("Created {} MCP tools", agentTools.size());
            return agentTools;
        } catch (Exception e) {
            logger.error("Failed to create MCP tools: {}", e.getMessage());
            throw new RuntimeException("Failed to create MCP tools", e);
        }
    }
}
