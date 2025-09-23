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
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;
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

@Component
public class ToolsInit {

    public static List<ToolCallback> getAllTools() {
        return List.of(
                RunPythonCodeTools(),
                RunShellCommandTools(),
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

        // Tool name mapping table
        return switch (toolName.toLowerCase().trim()) {
            // Base tools
            case "runpython", "run_python", "python" -> RunPythonCodeTools();
            case "runshell", "run_shell", "shell" -> RunShellCommandTools();
            
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
                System.err.println("Unknown tool name: " + toolName);
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
                RunPythonCodeTools(),
                RunShellCommandTools()
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

    private static ToolCallback RunPythonCodeTools() {
        return FunctionToolCallback
                .builder(
                        "PythonExecuteService",
                        new RunPythonTool()
                ).description("Execute Python code snippets and return the output or errors.")
                .inputSchema(
                        """
                                {
                                    "type": "object",
                                    "properties": {
                                        "code": {
                                            "type": "string",
                                            "description": "Python code to be executed"
                                        }
                                    },
                                    "required": ["code"],
                                    "description": "Request object to perform Python code execution"
                                }
                                """
                ).inputType(RunPythonTool.RunPythonToolRequest.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    private static ToolCallback RunShellCommandTools() {
        return FunctionToolCallback
                .builder(
                        "ShellExecuteService",
                        new RunShellCommandTool()
                ).description("Execute shell commands and return the output or errors.")
                .inputSchema(
                        """
                                {
                                    "type": "object",
                                    "properties": {
                                        "command": {
                                            "type": "string",
                                            "description": "Shell command to be executed"
                                        }
                                    },
                                    "required": ["command"],
                                    "description": "Request object to perform shell execution"
                                }
                                """
                ).inputType(RunShellCommandTool.RunShellCommandToolRequest.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    private static ToolCallback ReadFileTool() {
        return FunctionToolCallback
                .builder(
                        "FSReadFileService",
                        new ReadFileTool()
                ).description("Read the complete contents of a file.")
                .inputSchema(
                        """
                                {
                                    "type": "object",
                                    "properties": {
                                        "path": {"type": "string", "description": "Path to the file to read"}
                                    },
                                    "required": ["path"],
                                    "description": "Filesystem read file request"
                                }
                                """
                ).inputType(ReadFileTool.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    private static ToolCallback ReadMultipleFilesTool() {
        return FunctionToolCallback
                .builder(
                        "FSReadMultipleFilesService",
                        new ReadMultipleFilesTool()
                ).description("Read the contents of multiple files simultaneously.")
                .inputSchema(
                        """
                                {
                                    "type": "object",
                                    "properties": {
                                        "paths": {"type": "array", "items": {"type": "string"}, "description": "Paths to the files to read"}
                                    },
                                    "required": ["paths"],
                                    "description": "Filesystem read multiple files request"
                                }
                                """
                ).inputType(ReadMultipleFilesTool.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    private static ToolCallback WriteFileTool() {
        return FunctionToolCallback
                .builder(
                        "FSWriteFileService",
                        new WriteFileTool()
                ).description("Create a new file or overwrite an existing file with new content.")
                .inputSchema(
                        """
                                {
                                    "type": "object",
                                    "properties": {
                                        "path": {"type": "string", "description": "Path to the file to write to"},
                                        "content": {"type": "string", "description": "Content to write into the file"}
                                    },
                                    "required": ["path", "content"],
                                    "description": "Filesystem write file request"
                                }
                                """
                ).inputType(WriteFileTool.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    private static ToolCallback EditFileTool() {
        return FunctionToolCallback
                .builder(
                        "FSEditFileService",
                        new EditFileTool()
                ).description("Make line-based edits to a text file.")
                .inputSchema(
                        """
                                {
                                    "type": "object",
                                    "properties": {
                                        "path": {"type": "string", "description": "Path to the file to edit"},
                                        "edits": {
                                            "type": "array",
                                            "items": {
                                                "type": "object",
                                                "properties": {
                                                    "oldText": {"type": "string", "description": "Text to search for - must match exactly"},
                                                    "newText": {"type": "string", "description": "Text to replace with"}
                                                },
                                                "required": ["oldText", "newText"],
                                                "additionalProperties": false
                                            },
                                            "description": "Array of edit objects with oldText and newText properties"
                                        }
                                    },
                                    "required": ["path", "edits"],
                                    "description": "Filesystem edit file request"
                                }
                                """
                ).inputType(EditFileTool.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    private static ToolCallback CreateDirectoryTool() {
        return FunctionToolCallback
                .builder(
                        "FSCreateDirectoryService",
                        new CreateDirectoryTool()
                ).description("Create a new directory or ensure a directory exists.")
                .inputSchema(
                        """
                                {
                                    "type": "object",
                                    "properties": {
                                        "path": {"type": "string", "description": "Path to the directory to create"}
                                    },
                                    "required": ["path"],
                                    "description": "Filesystem create directory request"
                                }
                                """
                ).inputType(CreateDirectoryTool.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    private static ToolCallback ListDirectoryTool() {
        return FunctionToolCallback
                .builder(
                        "FSListDirectoryService",
                        new ListDirectoryTool()
                ).description("Get a detailed listing of all files and directories in a specified path.")
                .inputSchema(
                        """
                                {
                                    "type": "object",
                                    "properties": {
                                        "path": {"type": "string", "description": "Path to list contents"}
                                    },
                                    "required": ["path"],
                                    "description": "Filesystem list directory request"
                                }
                                """
                ).inputType(ListDirectoryTool.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    private static ToolCallback DirectoryTreeTool() {
        return FunctionToolCallback
                .builder(
                        "FSDirectoryTreeService",
                        new DirectoryTreeTool()
                ).description("Get a recursive tree view of files and directories as a JSON structure.")
                .inputSchema(
                        """
                                {
                                    "type": "object",
                                    "properties": {
                                        "path": {"type": "string", "description": "Path to get tree structure"}
                                    },
                                    "required": ["path"],
                                    "description": "Filesystem directory tree request"
                                }
                                """
                ).inputType(DirectoryTreeTool.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    private static ToolCallback MoveFileTool() {
        return FunctionToolCallback
                .builder(
                        "FSMoveFileService",
                        new MoveFileTool()
                ).description("Move or rename files and directories.")
                .inputSchema(
                        """
                                {
                                    "type": "object",
                                    "properties": {
                                        "source": {"type": "string", "description": "Source path to move from"},
                                        "destination": {"type": "string", "description": "Destination path to move to"}
                                    },
                                    "required": ["source", "destination"],
                                    "description": "Filesystem move file request"
                                }
                                """
                ).inputType(MoveFileTool.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    private static ToolCallback SearchFilesTool() {
        return FunctionToolCallback
                .builder(
                        "FSSearchFilesService",
                        new SearchFilesTool()
                ).description("Recursively search for files and directories matching a pattern.")
                .inputSchema(
                        """
                                {
                                    "type": "object",
                                    "properties": {
                                        "path": {"type": "string", "description": "Starting path for the search"},
                                        "pattern": {"type": "string", "description": "Pattern to match files/directories"},
                                        "excludePatterns": {"type": "array", "items": {"type": "string"}, "description": "Patterns to exclude from search"}
                                    },
                                    "required": ["path", "pattern"],
                                    "description": "Filesystem search files request"
                                }
                                """
                ).inputType(SearchFilesTool.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    private static ToolCallback GetFileInfoTool() {
        return FunctionToolCallback
                .builder(
                        "FSGetFileInfoService",
                        new GetFileInfoTool()
                ).description("Retrieve detailed metadata about a file or directory.")
                .inputSchema(
                        """
                                {
                                    "type": "object",
                                    "properties": {
                                        "path": {"type": "string", "description": "Path to the file or directory"}
                                    },
                                    "required": ["path"],
                                    "description": "Filesystem get file info request"
                                }
                                """
                ).inputType(GetFileInfoTool.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    private static ToolCallback ListAllowedDirectoriesTool() {
        return FunctionToolCallback
                .builder(
                        "FSListAllowedDirectoriesService",
                        new ListAllowedDirectoriesTool()
                ).description("Returns the list of directories that this server is allowed to access.")
                .inputSchema(
                        """
                                {
                                    "type": "object",
                                    "properties": {},
                                    "required": [],
                                    "description": "Filesystem list allowed directories request"
                                }
                                """
                ).inputType(ListAllowedDirectoriesTool.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    // Browser tools
    private static ToolCallback BrowserNavigateTool() {
        return FunctionToolCallback.builder(
                        "BrowserNavigateService",
                        new NavigateTool()
                ).description("Navigate to a URL")
                .inputSchema("""
                        {
                            "type": "object",
                            "properties": {"url": {"type": "string"}},
                            "required": ["url"],
                            "description": "Browser navigate request"
                        }
                        """)
                .inputType(NavigateTool.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    private static ToolCallback BrowserClickTool() {
        return FunctionToolCallback.builder(
                        "BrowserClickService",
                        new ClickTool()
                ).description("Perform click on a web page")
                .inputSchema("""
                        {
                            "type": "object",
                            "properties": {"element": {"type": "string"}, "ref": {"type": "string"}},
                            "required": ["element", "ref"],
                            "description": "Browser click request"
                        }
                        """)
                .inputType(ClickTool.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    private static ToolCallback BrowserTypeTool() {
        return FunctionToolCallback.builder(
                        "BrowserTypeService",
                        new TypeTool()
                ).description("Type text into an element")
                .inputSchema("""
                        {
                            "type": "object",
                            "properties": {
                                "element": {"type": "string"},
                                "ref": {"type": "string"},
                                "text": {"type": "string"},
                                "submit": {"type": "boolean"},
                                "slowly": {"type": "boolean"}
                            },
                            "required": ["element", "ref", "text"],
                            "description": "Browser type request"
                        }
                        """)
                .inputType(TypeTool.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    private static ToolCallback BrowserTakeScreenshotTool() {
        return FunctionToolCallback.builder(
                        "BrowserTakeScreenshotService",
                        new TakeScreenshotTool()
                ).description("Take a screenshot of the current page")
                .inputSchema("""
                        {
                            "type": "object",
                            "properties": {
                                "raw": {"type": "boolean"},
                                "filename": {"type": "string"},
                                "element": {"type": "string"},
                                "ref": {"type": "string"}
                            },
                            "required": [],
                            "description": "Browser take screenshot request"
                        }
                        """)
                .inputType(TakeScreenshotTool.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    private static ToolCallback BrowserSnapshotTool() {
        return FunctionToolCallback.builder(
                        "BrowserSnapshotService",
                        new SnapshotTool()
                ).description("Capture accessibility snapshot of the current page")
                .inputSchema("""
                        {"type": "object", "properties": {}, "required": [], "description": "Browser snapshot request"}
                        """)
                .inputType(SnapshotTool.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    private static ToolCallback BrowserTabNewTool() {
        return FunctionToolCallback.builder(
                        "BrowserTabNewService",
                        new TabNewTool()
                ).description("Open a new tab")
                .inputSchema("""
                        {"type": "object", "properties": {"url": {"type": "string"}}, "required": [], "description": "Browser new tab request"}
                        """)
                .inputType(TabNewTool.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    private static ToolCallback BrowserTabSelectTool() {
        return FunctionToolCallback.builder(
                        "BrowserTabSelectService",
                        new TabSelectTool()
                ).description("Select a tab by index")
                .inputSchema("""
                        {"type": "object", "properties": {"index": {"type": "number"}}, "required": ["index"], "description": "Browser select tab request"}
                        """)
                .inputType(TabSelectTool.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    private static ToolCallback BrowserTabCloseTool() {
        return FunctionToolCallback.builder(
                        "BrowserTabCloseService",
                        new TabCloseTool()
                ).description("Close a tab")
                .inputSchema("""
                        {"type": "object", "properties": {"index": {"type": "number"}}, "required": [], "description": "Browser close tab request"}
                        """)
                .inputType(TabCloseTool.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    private static ToolCallback BrowserWaitForTool() {
        return FunctionToolCallback.builder(
                        "BrowserWaitForService",
                        new WaitForTool()
                ).description("Wait for conditions")
                .inputSchema("""
                        {
                            "type": "object",
                            "properties": {"time": {"type": "number"}, "text": {"type": "string"}, "textGone": {"type": "string"}},
                            "required": [],
                            "description": "Browser wait for request"
                        }
                        """)
                .inputType(WaitForTool.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    private static ToolCallback BrowserResizeTool() {
        return FunctionToolCallback.builder(
                        "BrowserResizeService",
                        new ResizeTool()
                ).description("Resize browser window")
                .inputSchema("""
                        {"type": "object", "properties": {"width": {"type": "number"}, "height": {"type": "number"}}, "required": ["width", "height"], "description": "Browser resize request"}
                        """)
                .inputType(ResizeTool.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    private static ToolCallback BrowserCloseTool() {
        return FunctionToolCallback.builder(
                        "BrowserCloseService",
                        new CloseTool()
                ).description("Close current page")
                .inputSchema("""
                        {"type": "object", "properties": {}, "required": [], "description": "Browser close request"}
                        """)
                .inputType(CloseTool.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    private static ToolCallback BrowserConsoleMessagesTool() {
        return FunctionToolCallback.builder(
                        "BrowserConsoleMessagesService",
                        new ConsoleMessagesTool()
                ).description("Returns all console messages")
                .inputSchema("""
                        {"type": "object", "properties": {}, "required": [], "description": "Browser console messages request"}
                        """)
                .inputType(ConsoleMessagesTool.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    private static ToolCallback BrowserHandleDialogTool() {
        return FunctionToolCallback.builder(
                        "BrowserHandleDialogService",
                        new HandleDialogTool()
                ).description("Handle a dialog")
                .inputSchema("""
                        {
                            "type": "object",
                            "properties": {
                                "accept": {"type": "boolean", "description": "Whether to accept the dialog"},
                                "promptText": {"type": "string", "description": "The text of the prompt in case of a prompt dialog"}
                            },
                            "required": ["accept"],
                            "description": "Browser handle dialog request"
                        }
                        """)
                .inputType(HandleDialogTool.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    private static ToolCallback BrowserFileUploadTool() {
        return FunctionToolCallback.builder(
                        "BrowserFileUploadService",
                        new FileUploadTool()
                ).description("Upload one or multiple files")
                .inputSchema("""
                        {
                            "type": "object",
                            "properties": {
                                "paths": {"type": "array", "items": {"type": "string"}, "description": "The absolute paths to the files to upload"}
                            },
                            "required": ["paths"],
                            "description": "Browser file upload request"
                        }
                        """)
                .inputType(FileUploadTool.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    private static ToolCallback BrowserPressKeyTool() {
        return FunctionToolCallback.builder(
                        "BrowserPressKeyService",
                        new PressKeyTool()
                ).description("Press a key on the keyboard")
                .inputSchema("""
                        {
                            "type": "object",
                            "properties": {
                                "key": {"type": "string", "description": "Name of the key to press or a character to generate"}
                            },
                            "required": ["key"],
                            "description": "Browser press key request"
                        }
                        """)
                .inputType(PressKeyTool.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    private static ToolCallback BrowserNavigateBackTool() {
        return FunctionToolCallback.builder(
                        "BrowserNavigateBackService",
                        new NavigateBackTool()
                ).description("Go back to the previous page")
                .inputSchema("""
                        {"type": "object", "properties": {}, "required": [], "description": "Browser navigate back request"}
                        """)
                .inputType(NavigateBackTool.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    private static ToolCallback BrowserNavigateForwardTool() {
        return FunctionToolCallback.builder(
                        "BrowserNavigateForwardService",
                        new NavigateForwardTool()
                ).description("Go forward to the next page")
                .inputSchema("""
                        {"type": "object", "properties": {}, "required": [], "description": "Browser navigate forward request"}
                        """)
                .inputType(NavigateForwardTool.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    private static ToolCallback BrowserNetworkRequestsTool() {
        return FunctionToolCallback.builder(
                        "BrowserNetworkRequestsService",
                        new NetworkRequestsTool()
                ).description("Returns all network requests since loading the page")
                .inputSchema("""
                        {"type": "object", "properties": {}, "required": [], "description": "Returns all network requests since loading the page"}
                        """)
                .inputType(NetworkRequestsTool.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    private static ToolCallback BrowserPdfSaveTool() {
        return FunctionToolCallback.builder(
                        "BrowserPdfSaveService",
                        new PdfSaveTool()
                ).description("Save page as PDF")
                .inputSchema("""
                        {
                            "type": "object",
                            "properties": {
                                "filename": {"type": "string", "description": "File name to save the pdf to"}
                            },
                            "required": [],
                            "description": "Browser PDF save request"
                        }
                        """)
                .inputType(PdfSaveTool.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    private static ToolCallback BrowserDragTool() {
        return FunctionToolCallback.builder(
                        "BrowserDragService",
                        new DragTool()
                ).description("Perform drag and drop between two elements")
                .inputSchema("""
                        {
                            "type": "object",
                            "properties": {
                                "startElement": {"type": "string", "description": "Human-readable source element description"},
                                "startRef": {"type": "string", "description": "Exact source element reference from the page snapshot"},
                                "endElement": {"type": "string", "description": "Human-readable target element description"},
                                "endRef": {"type": "string", "description": "Exact target element reference from the page snapshot"}
                            },
                            "required": ["startElement", "startRef", "endElement", "endRef"],
                            "description": "Browser drag request"
                        }
                        """)
                .inputType(DragTool.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    private static ToolCallback BrowserHoverTool() {
        return FunctionToolCallback.builder(
                        "BrowserHoverService",
                        new HoverTool()
                ).description("Hover over an element on a page")
                .inputSchema("""
                        {
                            "type": "object",
                            "properties": {
                                "element": {"type": "string", "description": "Human-readable element description"},
                                "ref": {"type": "string", "description": "Exact target element reference from the page snapshot"}
                            },
                            "required": ["element", "ref"],
                            "description": "Browser hover request"
                        }
                        """)
                .inputType(HoverTool.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    private static ToolCallback BrowserSelectOptionTool() {
        return FunctionToolCallback.builder(
                        "BrowserSelectOptionService",
                        new SelectOptionTool()
                ).description("Select an option in a dropdown")
                .inputSchema("""
                        {
                            "type": "object",
                            "properties": {
                                "element": {"type": "string", "description": "Human-readable element description"},
                                "ref": {"type": "string", "description": "Exact target element reference from the page snapshot"},
                                "values": {"type": "array", "items": {"type": "string"}, "description": "Array of values to select in the dropdown"}
                            },
                            "required": ["element", "ref", "values"],
                            "description": "Browser select option request"
                        }
                        """)
                .inputType(SelectOptionTool.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    private static ToolCallback BrowserTabListTool() {
        return FunctionToolCallback.builder(
                        "BrowserTabListService",
                        new TabListTool()
                ).description("List browser tabs")
                .inputSchema("""
                        {"type": "object", "properties": {}, "required": [], "description": "Browser tab list request"}
                        """)
                .inputType(TabListTool.Request.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

}
