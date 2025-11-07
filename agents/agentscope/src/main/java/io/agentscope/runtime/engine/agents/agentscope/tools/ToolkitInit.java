package io.agentscope.runtime.engine.agents.agentscope.tools;

import io.agentscope.core.tool.AgentTool;
import io.agentscope.runtime.engine.agents.agentscope.tools.base.AsBasePythonRunner;
import io.agentscope.runtime.engine.agents.agentscope.tools.base.AsBaseShellRunner;
import io.agentscope.runtime.engine.agents.agentscope.tools.browser.*;
import io.agentscope.runtime.engine.agents.agentscope.tools.fs.*;
import io.agentscope.runtime.engine.agents.agentscope.tools.mcp.AsMCPTool;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxType;
import io.agentscope.runtime.sandbox.tools.MCPTool;
import io.agentscope.runtime.sandbox.tools.McpConfigConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class ToolkitInit {
    private static final Logger logger = Logger.getLogger(ToolkitInit.class.getName());

    public static List<AgentTool> getAllTools() {
        return List.of(
                RunPythonCodeTool(),
                RunShellCommandTool(),
                ReadFileTool(),
                ReadMultipleFilesTool(),
                WriteFileTool(),
                EditFileTool(),
                CreateDirectoryTool(),
                ListDirectoryTool(),
                DirectoryTreeTool(),
                MoveFileTool(),
                SearchFilesTool(),
                GetFileInfoTool(),
                ListAllowedDirectoriesTool(),
                BrowserNavigateTool(),
                BrowserClickTool(),
                BrowserTypeTool(),
                BrowserTakeScreenshotTool(),
                BrowserSnapshotTool(),
                BrowserTabNewTool(),
                BrowserTabSelectTool(),
                BrowserTabCloseTool(),
                BrowserWaitForTool(),
                BrowserResizeTool(),
                BrowserCloseTool(),
                BrowserConsoleMessagesTool(),
                BrowserHandleDialogTool(),
                BrowserFileUploadTool(),
                BrowserPressKeyTool(),
                BrowserNavigateBackTool(),
                BrowserNavigateForwardTool(),
                BrowserNetworkRequestsTool(),
                BrowserPdfSaveTool(),
                BrowserDragTool(),
                BrowserHoverTool(),
                BrowserSelectOptionTool(),
                BrowserTabListTool()
        );
    }

    // Base tools
    public static AgentTool RunPythonCodeTool() {
        return new AsBasePythonRunner();
    }

    public static AgentTool RunShellCommandTool() {
        return new AsBaseShellRunner();
    }

    // Browser tools
    public static AgentTool BrowserNavigateTool() {
        return new AsBrowserNavigator();
    }

    public static AgentTool BrowserClickTool() {
        return new AsBrowserClicker();
    }

    public static AgentTool BrowserTypeTool() {
        return new AsBrowserTyper();
    }

    public static AgentTool BrowserSnapshotTool() {
        return new AsBrowserSnapshotTaker();
    }

    public static AgentTool BrowserTakeScreenshotTool() {
        return new AsBrowserScreenshotTaker();
    }

    public static AgentTool BrowserCloseTool() {
        return new AsBrowserCloser();
    }

    public static AgentTool BrowserHoverTool() {
        return new AsBrowserHoverer();
    }

    public static AgentTool BrowserDragTool() {
        return new AsBrowserDragger();
    }

    public static AgentTool BrowserConsoleMessagesTool() {
        return new AsBrowserConsoleMessagesRetriever();
    }

    public static AgentTool BrowserFileUploadTool() {
        return new AsBrowserFileUploader();
    }

    public static AgentTool BrowserHandleDialogTool() {
        return new AsBrowserDialogHandler();
    }

    public static AgentTool BrowserNavigateBackTool() {
        return new AsBrowserBackNavigator();
    }

    public static AgentTool BrowserNavigateForwardTool() {
        return new AsBrowserForwardNavigator();
    }

    public static AgentTool BrowserNetworkRequestsTool() {
        return new AsBrowserNetworkRequestsRetriever();
    }

    public static AgentTool BrowserPdfSaveTool() {
        return new AsBrowserPdfSaver();
    }

    public static AgentTool BrowserPressKeyTool() {
        return new AsBrowserKeyPresser();
    }

    public static AgentTool BrowserResizeTool() {
        return new AsBrowserResizer();
    }

    public static AgentTool BrowserSelectOptionTool() {
        return new AsBrowserOptionSelector();
    }

    public static AgentTool BrowserTabCloseTool() {
        return new AsBrowserTabCloser();
    }

    public static AgentTool BrowserTabListTool() {
        return new AsBrowserTabLister();
    }

    public static AgentTool BrowserTabNewTool() {
        return new AsBrowserTabCreator();
    }

    public static AgentTool BrowserTabSelectTool() {
        return new AsBrowserTabSelector();
    }

    public static AgentTool BrowserWaitForTool() {
        return new AsBrowserWaiter();
    }

    // Filesystem tools
    public static AgentTool ReadFileTool() {
        return new AsFsReadFile();
    }

    public static AgentTool WriteFileTool() {
        return new AsFsWriteFile();
    }

    public static AgentTool ListDirectoryTool() {
        return new AsFsListDirectory();
    }

    public static AgentTool CreateDirectoryTool() {
        return new AsFsCreateDirectory();
    }

    public static AgentTool DirectoryTreeTool() {
        return new AsFsDirectoryTree();
    }

    public static AgentTool EditFileTool() {
        return new AsFsEditFile();
    }

    public static AgentTool GetFileInfoTool() {
        return new AsFsGetFileInfo();
    }

    public static AgentTool ListAllowedDirectoriesTool() {
        return new AsFsListAllowedDirectories();
    }

    public static AgentTool MoveFileTool() {
        return new AsFsMoveFile();
    }

    public static AgentTool ReadMultipleFilesTool() {
        return new AsFsReadMultipleFiles();
    }

    public static AgentTool SearchFilesTool() {
        return new AsFsSearchFiles();
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

    public static List<AgentTool> getAllToolsWithMcp(String mcpServerConfigs,
                                                     SandboxType sandboxType,
                                                     SandboxManager sandboxManager) {
        List<AgentTool> allTools = new ArrayList<>(getAllTools());

        if (mcpServerConfigs != null && !mcpServerConfigs.trim().isEmpty()) {
            try {
                List<AgentTool> mcpTools = getMcpTools(mcpServerConfigs, sandboxType, sandboxManager);
                allTools.addAll(mcpTools);
                logger.info(String.format("Added %d MCP tools to the toolkit", mcpTools.size()));
            } catch (Exception e) {
                logger.warning("Failed to add MCP tools: " + e.getMessage());
            }
        }

        return allTools;
    }

    public static List<AgentTool> getAllToolsWithMcp(Map<String, Object> mcpServerConfigs,
                                                     SandboxType sandboxType,
                                                     SandboxManager sandboxManager) {
        List<AgentTool> allTools = new ArrayList<>(getAllTools());

        if (mcpServerConfigs != null && !mcpServerConfigs.isEmpty()) {
            try {
                List<AgentTool> mcpTools = getMcpTools(mcpServerConfigs, sandboxType, sandboxManager);
                allTools.addAll(mcpTools);
                logger.info(String.format("Added %d MCP tools to the toolkit", mcpTools.size()));
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

    private static List<AgentTool> buildMcpAgentTools(McpConfigConverter converter) {
        try {
            logger.info("Creating MCP tools from server configuration");

            List<MCPTool> mcpTools = converter.toBuiltinTools();
            List<AgentTool> agentTools = new ArrayList<>(mcpTools.size());
            for (MCPTool mcpTool : mcpTools) {
                agentTools.add(new AsMCPTool(mcpTool));
            }

            logger.info(String.format("Created %d MCP tools", agentTools.size()));
            return agentTools;
        } catch (Exception e) {
            logger.severe("Failed to create MCP tools: " + e.getMessage());
            throw new RuntimeException("Failed to create MCP tools", e);
        }
    }
}
