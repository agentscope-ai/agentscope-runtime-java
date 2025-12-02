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
package io.agentscope.runtime.engine.agents.saa.tools;

import io.agentscope.runtime.engine.agents.saa.tools.mcp.MCPToolExecutor;
import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.tools.McpConfigConverter;
import org.springframework.ai.tool.ToolCallback;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.agentscope.runtime.engine.agents.saa.tools.base.BasePythonRunner;
import io.agentscope.runtime.engine.agents.saa.tools.base.BaseShellRunner;
import io.agentscope.runtime.engine.agents.saa.tools.browser.BrowserBackNavigator;
import io.agentscope.runtime.engine.agents.saa.tools.browser.BrowserClicker;
import io.agentscope.runtime.engine.agents.saa.tools.browser.BrowserCloser;
import io.agentscope.runtime.engine.agents.saa.tools.browser.BrowserConsoleMessagesRetriever;
import io.agentscope.runtime.engine.agents.saa.tools.browser.BrowserDialogHandler;
import io.agentscope.runtime.engine.agents.saa.tools.browser.BrowserDragger;
import io.agentscope.runtime.engine.agents.saa.tools.browser.BrowserFileUploader;
import io.agentscope.runtime.engine.agents.saa.tools.browser.BrowserForwardNavigator;
import io.agentscope.runtime.engine.agents.saa.tools.browser.BrowserHoverer;
import io.agentscope.runtime.engine.agents.saa.tools.browser.BrowserKeyPresser;
import io.agentscope.runtime.engine.agents.saa.tools.browser.BrowserNavigator;
import io.agentscope.runtime.engine.agents.saa.tools.browser.BrowserNetworkRequestsRetriever;
import io.agentscope.runtime.engine.agents.saa.tools.browser.BrowserOptionSelector;
import io.agentscope.runtime.engine.agents.saa.tools.browser.BrowserPdfSaver;
import io.agentscope.runtime.engine.agents.saa.tools.browser.BrowserScreenshotTaker;
import io.agentscope.runtime.engine.agents.saa.tools.browser.BrowserSnapshotTaker;
import io.agentscope.runtime.engine.agents.saa.tools.browser.BrowserTabCloser;
import io.agentscope.runtime.engine.agents.saa.tools.browser.BrowserTabCreator;
import io.agentscope.runtime.engine.agents.saa.tools.browser.BrowserTabLister;
import io.agentscope.runtime.engine.agents.saa.tools.browser.BrowserTabSelector;
import io.agentscope.runtime.engine.agents.saa.tools.browser.BrowserTyper;
import io.agentscope.runtime.engine.agents.saa.tools.browser.BrowserWaiter;
import io.agentscope.runtime.engine.agents.saa.tools.browser.BrowserWindowResizer;
import io.agentscope.runtime.engine.agents.saa.tools.fs.FsAllowedDirectoriesLister;
import io.agentscope.runtime.engine.agents.saa.tools.fs.FsDirectoryCreator;
import io.agentscope.runtime.engine.agents.saa.tools.fs.FsDirectoryLister;
import io.agentscope.runtime.engine.agents.saa.tools.fs.FsFileEditor;
import io.agentscope.runtime.engine.agents.saa.tools.fs.FsFileInfoRetriever;
import io.agentscope.runtime.engine.agents.saa.tools.fs.FsFileMover;
import io.agentscope.runtime.engine.agents.saa.tools.fs.FsFileReader;
import io.agentscope.runtime.engine.agents.saa.tools.fs.FsFileSearcher;
import io.agentscope.runtime.engine.agents.saa.tools.fs.FsFileWriter;
import io.agentscope.runtime.engine.agents.saa.tools.fs.FsMultiFileReader;
import io.agentscope.runtime.engine.agents.saa.tools.fs.FsTreeBuilder;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxType;
import io.agentscope.runtime.sandbox.tools.MCPTool;

@Component
public class ToolcallsInit {
    public static Logger logger =  Logger.getLogger(ToolcallsInit.class.getName());

    public static List<ToolCallback> getAllTools(Sandbox sandbox) {
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

    /**
     * Get corresponding ToolCallback based on tool name
     * @param toolName tool name
     * @param sandbox sandbox instance
     * @return ToolCallback instance, returns null if not found
     */
    public static ToolCallback getToolByName(String toolName, Sandbox sandbox) {
        if (toolName == null || toolName.trim().isEmpty()) {
            return null;
        }

        return switch (toolName.toLowerCase().trim()) {
            // Base tools
            case "runpython", "run_python", "python" -> RunPythonCodeTool(sandbox);
            case "runshell", "run_shell", "shell" -> RunShellCommandTool(sandbox);
            
            // File system tools
            case "readfile", "read_file", "fs_read" -> ReadFileTool(sandbox);
            case "readmultiplefiles", "read_multiple_files", "fs_read_multiple" -> ReadMultipleFilesTool(sandbox);
            case "writefile", "write_file", "fs_write" -> WriteFileTool(sandbox);
            case "editfile", "edit_file", "fs_edit" -> EditFileTool(sandbox);
            case "createdirectory", "create_directory", "fs_create_dir" -> CreateDirectoryTool(sandbox);
            case "listdirectory", "list_directory", "fs_list" -> ListDirectoryTool(sandbox);
            case "directorytree", "directory_tree", "fs_tree" -> DirectoryTreeTool(sandbox);
            case "movefile", "move_file", "fs_move" -> MoveFileTool(sandbox);
            case "searchfiles", "search_files", "fs_search" -> SearchFilesTool(sandbox);
            case "getfileinfo", "get_file_info", "fs_info" -> GetFileInfoTool(sandbox);
            case "listalloweddirectories", "list_allowed_directories", "fs_allowed" -> ListAllowedDirectoriesTool(sandbox);
            
            // Browser tools
            case "browsernavigate", "browser_navigate", "browser_nav" -> BrowserNavigateTool(sandbox);
            case "browserclick", "browser_click" -> BrowserClickTool(sandbox);
            case "browsertype", "browser_type" -> BrowserTypeTool(sandbox);
            case "browsertakescreenshot", "browser_take_screenshot", "browser_screenshot" -> BrowserTakeScreenshotTool(sandbox);
            case "browsersnapshot", "browser_snapshot" -> BrowserSnapshotTool(sandbox);
            case "browsertabnew", "browser_tab_new" -> BrowserTabNewTool(sandbox);
            case "browsertabselect", "browser_tab_select" -> BrowserTabSelectTool(sandbox);
            case "browsertabclose", "browser_tab_close" -> BrowserTabCloseTool(sandbox);
            case "browserwaitfor", "browser_wait_for" -> BrowserWaitForTool(sandbox);
            case "browserresize", "browser_resize" -> BrowserResizeTool(sandbox);
            case "browserclose", "browser_close" -> BrowserCloseTool(sandbox);
            case "browserconsolemessages", "browser_console_messages" -> BrowserConsoleMessagesTool(sandbox);
            case "browserhandledialog", "browser_handle_dialog" -> BrowserHandleDialogTool(sandbox);
            case "browserfileupload", "browser_file_upload" -> BrowserFileUploadTool(sandbox);
            case "browserpresskey", "browser_press_key" -> BrowserPressKeyTool(sandbox);
            case "browsernavigateback", "browser_navigate_back" -> BrowserNavigateBackTool(sandbox);
            case "browsernavigateforward", "browser_navigate_forward" -> BrowserNavigateForwardTool(sandbox);
            case "browsernetworkrequests", "browser_network_requests" -> BrowserNetworkRequestsTool(sandbox);
            case "browserpdfsave", "browser_pdf_save" -> BrowserPdfSaveTool(sandbox);
            case "browserdrag", "browser_drag" -> BrowserDragTool(sandbox);
            case "browserhover", "browser_hover" -> BrowserHoverTool(sandbox);
            case "browserselectoption", "browser_select_option" -> BrowserSelectOptionTool(sandbox);
            case "browsertablist", "browser_tab_list" -> BrowserTabListTool(sandbox);
            
            default -> {
                logger.severe("Unknown tool name: " + toolName);
                yield null;
            }
        };
    }

    public static List<ToolCallback> getToolsByName(List<String> toolNames, Sandbox sandbox) {
        if (toolNames == null || toolNames.isEmpty()) {
            return List.of();
        }

        return toolNames.stream()
                .map(toolName -> getToolByName(toolName, sandbox))
                .filter(tool -> tool != null)
                .toList();
    }

    public static List<ToolCallback> getBaseTools(Sandbox sandbox) {
        return List.of(
                RunPythonCodeTool(sandbox),
                RunShellCommandTool(sandbox)
        );
    }

    public static List<ToolCallback> getFileSystemTools(Sandbox sandbox) {
        return List.of(
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
                ListAllowedDirectoriesTool(sandbox)
        );
    }

    public static List<ToolCallback> getBrowserTools(Sandbox sandbox) {
        return List.of(
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

    public static ToolCallback RunPythonCodeTool(Sandbox sandbox) {
        BasePythonRunner basePythonRunner = new BasePythonRunner();
        basePythonRunner.setSandbox(sandbox);
        return basePythonRunner.buildTool();
    }

    public static ToolCallback RunShellCommandTool(Sandbox sandbox) {
        BaseShellRunner baseShellRunner = new BaseShellRunner();
        baseShellRunner.setSandbox(sandbox);
        return baseShellRunner.buildTool();
    }

    public static ToolCallback ReadFileTool(Sandbox sandbox) {
        FsFileReader fsFileReader = new FsFileReader();
        fsFileReader.setSandbox(sandbox);
        return fsFileReader.buildTool();
    }

    public static ToolCallback ReadMultipleFilesTool(Sandbox sandbox) {
        FsMultiFileReader fsMultiFileReader = new FsMultiFileReader();
        fsMultiFileReader.setSandbox(sandbox);
        return fsMultiFileReader.buildTool();
    }

    public static ToolCallback WriteFileTool(Sandbox sandbox) {
        FsFileWriter fsFileWriter = new FsFileWriter();
        fsFileWriter.setSandbox(sandbox);
        return fsFileWriter.buildTool();
    }

    public static ToolCallback EditFileTool(Sandbox sandbox) {
        FsFileEditor fsFileEditor = new FsFileEditor();
        fsFileEditor.setSandbox(sandbox);
        return fsFileEditor.buildTool();
    }

    public static ToolCallback CreateDirectoryTool(Sandbox sandbox) {
        FsDirectoryCreator fsDirectoryCreator = new FsDirectoryCreator();
        fsDirectoryCreator.setSandbox(sandbox);
        return fsDirectoryCreator.buildTool();
    }

    public static ToolCallback ListDirectoryTool(Sandbox sandbox) {
        FsDirectoryLister fsDirectoryLister = new FsDirectoryLister();
        fsDirectoryLister.setSandbox(sandbox);
        return fsDirectoryLister.buildTool();
    }

    public static ToolCallback DirectoryTreeTool(Sandbox sandbox) {
        FsTreeBuilder fsTreeBuilder = new FsTreeBuilder();
        fsTreeBuilder.setSandbox(sandbox);
        return fsTreeBuilder.buildTool();
    }

    public static ToolCallback MoveFileTool(Sandbox sandbox) {
        FsFileMover fsFileMover = new FsFileMover();
        fsFileMover.setSandbox(sandbox);
        return fsFileMover.buildTool();
    }

    public static ToolCallback SearchFilesTool(Sandbox sandbox) {
        FsFileSearcher fsFileSearcher = new FsFileSearcher();
        fsFileSearcher.setSandbox(sandbox);
        return fsFileSearcher.buildTool();
    }

    public static ToolCallback GetFileInfoTool(Sandbox sandbox) {
        FsFileInfoRetriever fsFileInfoRetriever = new FsFileInfoRetriever();
        fsFileInfoRetriever.setSandbox(sandbox);
        return fsFileInfoRetriever.buildTool();
    }

    public static ToolCallback ListAllowedDirectoriesTool(Sandbox sandbox) {
        FsAllowedDirectoriesLister fsAllowedDirectoriesLister = new FsAllowedDirectoriesLister();
        fsAllowedDirectoriesLister.setSandbox(sandbox);
        return fsAllowedDirectoriesLister.buildTool();
    }

    // Browser tools
    public static ToolCallback BrowserNavigateTool(Sandbox sandbox) {
        BrowserNavigator browserNavigator = new BrowserNavigator();
        browserNavigator.setSandbox(sandbox);
        return browserNavigator.buildTool();
    }

    public static ToolCallback BrowserClickTool(Sandbox sandbox) {
        BrowserClicker browserClicker = new BrowserClicker();
        browserClicker.setSandbox(sandbox);
        return browserClicker.buildTool();
    }

    public static ToolCallback BrowserTypeTool(Sandbox sandbox) {
        BrowserTyper browserTyper = new BrowserTyper();
        browserTyper.setSandbox(sandbox);
        return browserTyper.buildTool();
    }

    public static ToolCallback BrowserTakeScreenshotTool(Sandbox sandbox) {
        BrowserScreenshotTaker browserScreenshotTaker = new BrowserScreenshotTaker();
        browserScreenshotTaker.setSandbox(sandbox);
        return browserScreenshotTaker.buildTool();
    }

    public static ToolCallback BrowserSnapshotTool(Sandbox sandbox) {
        BrowserSnapshotTaker browserSnapshotTaker = new BrowserSnapshotTaker();
        browserSnapshotTaker.setSandbox(sandbox);
        return browserSnapshotTaker.buildTool();
    }

    public static ToolCallback BrowserTabNewTool(Sandbox sandbox) {
        BrowserTabCreator browserTabCreator = new BrowserTabCreator();
        browserTabCreator.setSandbox(sandbox);
        return browserTabCreator.buildTool();
    }

    public static ToolCallback BrowserTabSelectTool(Sandbox sandbox) {
        BrowserTabSelector browserTabSelector = new BrowserTabSelector();
        browserTabSelector.setSandbox(sandbox);
        return browserTabSelector.buildTool();
    }

    public static ToolCallback BrowserTabCloseTool(Sandbox sandbox) {
        BrowserTabCloser browserTabCloser = new BrowserTabCloser();
        browserTabCloser.setSandbox(sandbox);
        return browserTabCloser.buildTool();
    }

    public static ToolCallback BrowserWaitForTool(Sandbox sandbox) {
        BrowserWaiter browserWaiter = new BrowserWaiter();
        browserWaiter.setSandbox(sandbox);
        return browserWaiter.buildTool();
    }

    public static ToolCallback BrowserResizeTool(Sandbox sandbox) {
        BrowserWindowResizer browserWindowResizer = new BrowserWindowResizer();
        browserWindowResizer.setSandbox(sandbox);
        return browserWindowResizer.buildTool();
    }

    public static ToolCallback BrowserCloseTool(Sandbox sandbox) {
        BrowserCloser browserCloser = new BrowserCloser();
        browserCloser.setSandbox(sandbox);
        return browserCloser.buildTool();
    }

    public static ToolCallback BrowserConsoleMessagesTool(Sandbox sandbox) {
        BrowserConsoleMessagesRetriever browserConsoleMessagesRetriever = new BrowserConsoleMessagesRetriever();
        browserConsoleMessagesRetriever.setSandbox(sandbox);
        return browserConsoleMessagesRetriever.buildTool();
    }

    public static ToolCallback BrowserHandleDialogTool(Sandbox sandbox) {
        BrowserDialogHandler browserDialogHandler = new BrowserDialogHandler();
        browserDialogHandler.setSandbox(sandbox);
        return browserDialogHandler.buildTool();
    }

    public static ToolCallback BrowserFileUploadTool(Sandbox sandbox) {
        BrowserFileUploader browserFileUploader = new BrowserFileUploader();
        browserFileUploader.setSandbox(sandbox);
        return browserFileUploader.buildTool();
    }

    public static ToolCallback BrowserPressKeyTool(Sandbox sandbox) {
        BrowserKeyPresser browserKeyPresser = new BrowserKeyPresser();
        browserKeyPresser.setSandbox(sandbox);
        return browserKeyPresser.buildTool();
    }

    public static ToolCallback BrowserNavigateBackTool(Sandbox sandbox) {
        BrowserBackNavigator browserBackNavigator = new BrowserBackNavigator();
        browserBackNavigator.setSandbox(sandbox);
        return browserBackNavigator.buildTool();
    }

    public static ToolCallback BrowserNavigateForwardTool(Sandbox sandbox) {
        BrowserForwardNavigator browserForwardNavigator = new BrowserForwardNavigator();
        browserForwardNavigator.setSandbox(sandbox);
        return browserForwardNavigator.buildTool();
    }

    public static ToolCallback BrowserNetworkRequestsTool(Sandbox sandbox) {
        BrowserNetworkRequestsRetriever browserNetworkRequestsRetriever = new BrowserNetworkRequestsRetriever();
        browserNetworkRequestsRetriever.setSandbox(sandbox);
        return browserNetworkRequestsRetriever.buildTool();
    }

    public static ToolCallback BrowserPdfSaveTool(Sandbox sandbox) {
        BrowserPdfSaver browserPdfSaver = new BrowserPdfSaver();
        browserPdfSaver.setSandbox(sandbox);
        return browserPdfSaver.buildTool();
    }

    public static ToolCallback BrowserDragTool(Sandbox sandbox) {
        BrowserDragger browserDragger = new BrowserDragger();
        browserDragger.setSandbox(sandbox);
        return browserDragger.buildTool();
    }

    public static ToolCallback BrowserHoverTool(Sandbox sandbox) {
        BrowserHoverer browserHoverer = new BrowserHoverer();
        browserHoverer.setSandbox(sandbox);
        return browserHoverer.buildTool();
    }

    public static ToolCallback BrowserSelectOptionTool(Sandbox sandbox) {
        BrowserOptionSelector browserOptionSelector = new BrowserOptionSelector();
        browserOptionSelector.setSandbox(sandbox);
        return browserOptionSelector.buildTool();
    }

    public static ToolCallback BrowserTabListTool(Sandbox sandbox) {
        BrowserTabLister browserTabLister = new BrowserTabLister();
        browserTabLister.setSandbox(sandbox);
        return browserTabLister.buildTool();
    }

    public static List<ToolCallback> getMcpTools(String serverConfigs,
                                                SandboxType sandboxType,
                                                SandboxManager sandboxManager) {
        return getMcpTools(serverConfigs, sandboxType, sandboxManager, null, null);
    }

    public static List<ToolCallback> getMcpTools(Map<String, Object> serverConfigs, 
                                                SandboxType sandboxType,
                                                SandboxManager sandboxManager) {
        return getMcpTools(serverConfigs, sandboxType, sandboxManager, null, null);
    }

    public static List<ToolCallback> getMcpTools(String serverConfigs,
                                                SandboxType sandboxType,
                                                SandboxManager sandboxManager,
                                                Set<String> whitelist,
                                                Set<String> blacklist) {
        try {
            logger.info("Creating MCP tools from server configuration");
            
            McpConfigConverter converter = McpConfigConverter.builder()
                    .serverConfigs(serverConfigs)
                    .sandboxType(sandboxType)
                    .sandboxManager(sandboxManager)
                    .whitelist(whitelist)
                    .blacklist(blacklist)
                    .build();
            
            List<MCPTool> mcpTools = converter.toBuiltinTools();
            
            List<ToolCallback> toolCallbacks = mcpTools.stream()
                    .map(mcpTool -> new MCPToolExecutor(mcpTool).buildTool())
                    .collect(Collectors.toList());
            
            logger.info(String.format("Created %d MCP tools", toolCallbacks.size()));
            return toolCallbacks;
            
        } catch (Exception e) {
            logger.severe("Failed to create MCP tools: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create MCP tools", e);
        }
    }

    public static List<ToolCallback> getMcpTools(Map<String, Object> serverConfigs,
                                                SandboxType sandboxType,
                                                SandboxManager sandboxManager,
                                                Set<String> whitelist,
                                                Set<String> blacklist) {
        try {
            logger.info("Creating MCP tools from server configuration");
            
            McpConfigConverter converter = McpConfigConverter.builder()
                    .serverConfigs(serverConfigs)
                    .sandboxType(sandboxType)
                    .sandboxManager(sandboxManager)
                    .whitelist(whitelist)
                    .blacklist(blacklist)
                    .build();
            
            List<MCPTool> mcpTools = converter.toBuiltinTools();
            
            List<ToolCallback> toolCallbacks = mcpTools.stream()
                    .map(mcpTool -> new MCPToolExecutor(mcpTool).buildTool())
                    .collect(Collectors.toList());
            
            logger.info(String.format("Created %d MCP tools", toolCallbacks.size()));
            return toolCallbacks;
            
        } catch (Exception e) {
            logger.severe("Failed to create MCP tools: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create MCP tools", e);
        }
    }

    public static List<ToolCallback> getMcpTools(String serverConfigs,
                                                SandboxManager sandboxManager) {
        return getMcpTools(serverConfigs, null, sandboxManager, null, null);
    }

    public static List<ToolCallback> getMcpTools(Map<String, Object> serverConfigs,
                                                SandboxManager sandboxManager) {
        return getMcpTools(serverConfigs, null, sandboxManager, null, null);
    }

    public static List<ToolCallback> getAllToolsWithMcp(String mcpServerConfigs,
                                                       SandboxType sandboxType,
                                                       SandboxManager sandboxManager,
                                                       Sandbox sandbox) {
        List<ToolCallback> allTools = new ArrayList<>(getAllTools(sandbox));
        
        if (mcpServerConfigs != null && !mcpServerConfigs.trim().isEmpty()) {
            try {
                List<ToolCallback> mcpTools = getMcpTools(mcpServerConfigs, sandboxType, sandboxManager);
                allTools.addAll(mcpTools);
                logger.info(String.format("Added %d MCP tools to the tool list", mcpTools.size()));
            } catch (Exception e) {
                logger.warning("Failed to add MCP tools: " + e.getMessage());
            }
        }
        
        return allTools;
    }

    public static List<ToolCallback> getAllToolsWithMcp(Map<String, Object> mcpServerConfigs,
                                                       SandboxType sandboxType,
                                                       SandboxManager sandboxManager,
                                                       Sandbox sandbox) {
        List<ToolCallback> allTools = new ArrayList<>(getAllTools(sandbox));
        
        if (mcpServerConfigs != null && !mcpServerConfigs.isEmpty()) {
            try {
                List<ToolCallback> mcpTools = getMcpTools(mcpServerConfigs, sandboxType, sandboxManager);
                allTools.addAll(mcpTools);
                logger.info(String.format("Added %d MCP tools to the tool list", mcpTools.size()));
            } catch (Exception e) {
                logger.warning("Failed to add MCP tools: " + e.getMessage());
            }
        }
        
        return allTools;
    }

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

}
