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

import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;
import io.agentscope.runtime.sandbox.tools.base.RunPythonTool;
import io.agentscope.runtime.sandbox.tools.base.RunShellCommandTool;
import io.agentscope.runtime.sandbox.tools.browser.ClickTool;
import io.agentscope.runtime.sandbox.tools.browser.CloseTool;
import io.agentscope.runtime.sandbox.tools.browser.ConsoleMessagesTool;
import io.agentscope.runtime.sandbox.tools.browser.DragTool;
import io.agentscope.runtime.sandbox.tools.browser.FileUploadTool;
import io.agentscope.runtime.sandbox.tools.browser.HandleDialogTool;
import io.agentscope.runtime.sandbox.tools.browser.HoverTool;
import io.agentscope.runtime.sandbox.tools.browser.NavigateBackTool;
import io.agentscope.runtime.sandbox.tools.browser.NavigateForwardTool;
import io.agentscope.runtime.sandbox.tools.browser.NavigateTool;
import io.agentscope.runtime.sandbox.tools.browser.NetworkRequestsTool;
import io.agentscope.runtime.sandbox.tools.browser.PdfSaveTool;
import io.agentscope.runtime.sandbox.tools.browser.PressKeyTool;
import io.agentscope.runtime.sandbox.tools.browser.ResizeTool;
import io.agentscope.runtime.sandbox.tools.browser.SelectOptionTool;
import io.agentscope.runtime.sandbox.tools.browser.SnapshotTool;
import io.agentscope.runtime.sandbox.tools.browser.TabCloseTool;
import io.agentscope.runtime.sandbox.tools.browser.TabListTool;
import io.agentscope.runtime.sandbox.tools.browser.TabNewTool;
import io.agentscope.runtime.sandbox.tools.browser.TabSelectTool;
import io.agentscope.runtime.sandbox.tools.browser.TakeScreenshotTool;
import io.agentscope.runtime.sandbox.tools.browser.TypeTool;
import io.agentscope.runtime.sandbox.tools.browser.WaitForTool;
import io.agentscope.runtime.sandbox.tools.fs.CreateDirectoryTool;
import io.agentscope.runtime.sandbox.tools.fs.DirectoryTreeTool;
import io.agentscope.runtime.sandbox.tools.fs.EditFileTool;
import io.agentscope.runtime.sandbox.tools.fs.GetFileInfoTool;
import io.agentscope.runtime.sandbox.tools.fs.ListAllowedDirectoriesTool;
import io.agentscope.runtime.sandbox.tools.fs.ListDirectoryTool;
import io.agentscope.runtime.sandbox.tools.fs.MoveFileTool;
import io.agentscope.runtime.sandbox.tools.fs.ReadFileTool;
import io.agentscope.runtime.sandbox.tools.fs.ReadMultipleFilesTool;
import io.agentscope.runtime.sandbox.tools.fs.SearchFilesTool;
import io.agentscope.runtime.sandbox.tools.fs.WriteFileTool;

import java.util.List;
import java.util.logging.Logger;

@Component
public class ToolsInit {
    public static Logger logger =  Logger.getLogger(ToolsInit.class.getName());

    public static List<ToolCallback> getAllTools() {
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

    /**
     * Get corresponding ToolCallback based on tool name
     * @param toolName tool name
     * @return ToolCallback instance, returns null if not found
     */
    public static ToolCallback getToolByName(String toolName) {
        if (toolName == null || toolName.trim().isEmpty()) {
            return null;
        }

        return switch (toolName.toLowerCase().trim()) {
            // Base tools
            case "runpython", "run_python", "python" -> RunPythonCodeTool();
            case "runshell", "run_shell", "shell" -> RunShellCommandTool();
            
            // File system tools
            case "readfile", "read_file", "fs_read" -> ReadFileTool();
            case "readmultiplefiles", "read_multiple_files", "fs_read_multiple" -> ReadMultipleFilesTool();
            case "writefile", "write_file", "fs_write" -> WriteFileTool();
            case "editfile", "edit_file", "fs_edit" -> EditFileTool();
            case "createdirectory", "create_directory", "fs_create_dir" -> CreateDirectoryTool();
            case "listdirectory", "list_directory", "fs_list" -> ListDirectoryTool();
            case "directorytree", "directory_tree", "fs_tree" -> DirectoryTreeTool();
            case "movefile", "move_file", "fs_move" -> MoveFileTool();
            case "searchfiles", "search_files", "fs_search" -> SearchFilesTool();
            case "getfileinfo", "get_file_info", "fs_info" -> GetFileInfoTool();
            case "listalloweddirectories", "list_allowed_directories", "fs_allowed" -> ListAllowedDirectoriesTool();
            
            // Browser tools
            case "browsernavigate", "browser_navigate", "browser_nav" -> BrowserNavigateTool();
            case "browserclick", "browser_click" -> BrowserClickTool();
            case "browsertype", "browser_type" -> BrowserTypeTool();
            case "browsertakescreenshot", "browser_take_screenshot", "browser_screenshot" -> BrowserTakeScreenshotTool();
            case "browsersnapshot", "browser_snapshot" -> BrowserSnapshotTool();
            case "browsertabnew", "browser_tab_new" -> BrowserTabNewTool();
            case "browsertabselect", "browser_tab_select" -> BrowserTabSelectTool();
            case "browsertabclose", "browser_tab_close" -> BrowserTabCloseTool();
            case "browserwaitfor", "browser_wait_for" -> BrowserWaitForTool();
            case "browserresize", "browser_resize" -> BrowserResizeTool();
            case "browserclose", "browser_close" -> BrowserCloseTool();
            case "browserconsolemessages", "browser_console_messages" -> BrowserConsoleMessagesTool();
            case "browserhandledialog", "browser_handle_dialog" -> BrowserHandleDialogTool();
            case "browserfileupload", "browser_file_upload" -> BrowserFileUploadTool();
            case "browserpresskey", "browser_press_key" -> BrowserPressKeyTool();
            case "browsernavigateback", "browser_navigate_back" -> BrowserNavigateBackTool();
            case "browsernavigateforward", "browser_navigate_forward" -> BrowserNavigateForwardTool();
            case "browsernetworkrequests", "browser_network_requests" -> BrowserNetworkRequestsTool();
            case "browserpdfsave", "browser_pdf_save" -> BrowserPdfSaveTool();
            case "browserdrag", "browser_drag" -> BrowserDragTool();
            case "browserhover", "browser_hover" -> BrowserHoverTool();
            case "browserselectoption", "browser_select_option" -> BrowserSelectOptionTool();
            case "browsertablist", "browser_tab_list" -> BrowserTabListTool();
            
            default -> {
                logger.severe("Unknown tool name: " + toolName);
                yield null;
            }
        };
    }

    public static List<ToolCallback> getToolsByName(List<String> toolNames) {
        if (toolNames == null || toolNames.isEmpty()) {
            return List.of();
        }

        return toolNames.stream()
                .map(ToolsInit::getToolByName)
                .filter(tool -> tool != null)
                .toList();
    }

    public static List<ToolCallback> getBaseTools() {
        return List.of(
                RunPythonCodeTool(),
                RunShellCommandTool()
        );
    }

    public static List<ToolCallback> getFileSystemTools() {
        return List.of(
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
                ListAllowedDirectoriesTool()
        );
    }

    public static List<ToolCallback> getBrowserTools() {
        return List.of(
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

    public static ToolCallback RunPythonCodeTool() {
        return new RunPythonTool().buildTool();
    }

    public static ToolCallback RunShellCommandTool() {
        return new RunShellCommandTool().buildTool();
    }

    public static ToolCallback ReadFileTool() {
        return new ReadFileTool().buildTool();
    }

    public static ToolCallback ReadMultipleFilesTool() {
        return new ReadMultipleFilesTool().buildTool();
    }

    public static ToolCallback WriteFileTool() {
        return new WriteFileTool().buildTool();
    }

    public static ToolCallback EditFileTool() {
        return new EditFileTool().buildTool();
    }

    public static ToolCallback CreateDirectoryTool() {
        return new CreateDirectoryTool().buildTool();
    }

    public static ToolCallback ListDirectoryTool() {
        return new ListDirectoryTool().buildTool();
    }

    public static ToolCallback DirectoryTreeTool() {
        return new DirectoryTreeTool().buildTool();
    }

    public static ToolCallback MoveFileTool() {
        return new MoveFileTool().buildTool();
    }

    public static ToolCallback SearchFilesTool() {
        return new SearchFilesTool().buildTool();
    }

    public static ToolCallback GetFileInfoTool() {
        return new GetFileInfoTool().buildTool();
    }

    public static ToolCallback ListAllowedDirectoriesTool() {
        return new ListAllowedDirectoriesTool().buildTool();
    }

    // Browser tools
    public static ToolCallback BrowserNavigateTool() {
        return new NavigateTool().buildTool();
    }

    public static ToolCallback BrowserClickTool() {
        return new ClickTool().buildTool();
    }

    public static ToolCallback BrowserTypeTool() {
        return new TypeTool().buildTool();
    }

    public static ToolCallback BrowserTakeScreenshotTool() {
        return new TakeScreenshotTool().buildTool();
    }

    public static ToolCallback BrowserSnapshotTool() {
        return new SnapshotTool().buildTool();
    }

    public static ToolCallback BrowserTabNewTool() {
        return new TabNewTool().buildTool();
    }

    public static ToolCallback BrowserTabSelectTool() {
        return new TabSelectTool().buildTool();
    }

    public static ToolCallback BrowserTabCloseTool() {
        return new TabCloseTool().buildTool();
    }

    public static ToolCallback BrowserWaitForTool() {
        return new WaitForTool().buildTool();
    }

    public static ToolCallback BrowserResizeTool() {
        return new ResizeTool().buildTool();
    }

    public static ToolCallback BrowserCloseTool() {
        return new CloseTool().buildTool();
    }

    public static ToolCallback BrowserConsoleMessagesTool() {
        return new ConsoleMessagesTool().buildTool();
    }

    public static ToolCallback BrowserHandleDialogTool() {
        return new HandleDialogTool().buildTool();
    }

    public static ToolCallback BrowserFileUploadTool() {
        return new FileUploadTool().buildTool();
    }

    public static ToolCallback BrowserPressKeyTool() {
        return new PressKeyTool().buildTool();
    }

    public static ToolCallback BrowserNavigateBackTool() {
        return new NavigateBackTool().buildTool();
    }

    public static ToolCallback BrowserNavigateForwardTool() {
        return new NavigateForwardTool().buildTool();
    }

    public static ToolCallback BrowserNetworkRequestsTool() {
        return new NetworkRequestsTool().buildTool();
    }

    public static ToolCallback BrowserPdfSaveTool() {
        return new PdfSaveTool().buildTool();
    }

    public static ToolCallback BrowserDragTool() {
        return new DragTool().buildTool();
    }

    public static ToolCallback BrowserHoverTool() {
        return new HoverTool().buildTool();
    }

    public static ToolCallback BrowserSelectOptionTool() {
        return new SelectOptionTool().buildTool();
    }

    public static ToolCallback BrowserTabListTool() {
        return new TabListTool().buildTool();
    }

}
