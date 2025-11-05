package io.agentscope.runtime.engine.agents.agentscope.tools;

import io.agentscope.core.tool.AgentTool;
import io.agentscope.runtime.engine.agents.agentscope.tools.base.AsBasePythonRunner;
import io.agentscope.runtime.engine.agents.agentscope.tools.base.AsBaseShellRunner;
import io.agentscope.runtime.engine.agents.agentscope.tools.browser.*;
import io.agentscope.runtime.engine.agents.agentscope.tools.fs.*;

public class ToolkitInit {
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
}
