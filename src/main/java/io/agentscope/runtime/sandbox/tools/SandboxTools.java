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

import io.agentscope.runtime.engine.Runner;
import io.agentscope.runtime.sandbox.box.BaseSandbox;
import io.agentscope.runtime.sandbox.box.BrowserSandbox;
import io.agentscope.runtime.sandbox.box.FilesystemSandbox;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.util.HttpClient;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Sandbox tools class, providing various sandbox operation functions
 */
public class SandboxTools extends BaseSandboxTools {
    private final Logger logger = Logger.getLogger(SandboxTools.class.getName());

    public SandboxTools() {
        super(Runner.getSandboxManager(), new HttpClient());
    }

    public SandboxTools(SandboxManager sandboxManager) {
        super(sandboxManager, new HttpClient());
    }

    /**
     * Get the shared SandboxManager instance
     *
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
    public String run_ipython_cell(String code, String userID, String sessionID) {
        try {
            BaseSandbox baseSandbox = new BaseSandbox(sandboxManager, userID, sessionID);
            return baseSandbox.runIpythonCell(code);

        } catch (Exception e) {
            String errorMsg = "Run Python Code Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }

    public String run_shell_command(String command, String userID, String sessionID) {
        try {
            BaseSandbox baseSandbox = new BaseSandbox(sandboxManager, userID, sessionID);
            return baseSandbox.runShellCommand(command);

        } catch (Exception e) {
            String errorMsg = "Run Shell Command Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }

    // Below are convenient wrappers for filesystem tools
    public String fs_read_file(String path, String userID, String sessionID) {
        try {
            FilesystemSandbox filesystemSandbox = new FilesystemSandbox(sandboxManager, userID, sessionID);
            return filesystemSandbox.readFile(path);
        } catch (Exception e) {
            String errorMsg = "Read File Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }

    public String fs_read_multiple_files(String[] paths, String userID, String sessionID) {
        try {
            FilesystemSandbox filesystemSandbox = new FilesystemSandbox(sandboxManager, userID, sessionID);
            return filesystemSandbox.readMultipleFiles(Arrays.asList(paths));
        } catch (Exception e) {
            String errorMsg = "Read Multiple Files Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }

    public String fs_write_file(String path, String content, String userID, String sessionID) {
        try {
            FilesystemSandbox filesystemSandbox = new FilesystemSandbox(sandboxManager, userID, sessionID);
            return filesystemSandbox.writeFile(path, content);
        } catch (Exception e) {
            String errorMsg = "Write File Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }

    public String fs_edit_file(String path, Object[] edits, String userID, String sessionID) {
        try {
            FilesystemSandbox filesystemSandbox = new FilesystemSandbox(sandboxManager, userID, sessionID);
            return filesystemSandbox.editFile(path, edits);
        } catch (Exception e) {
            String errorMsg = "Edit File Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }

    public String fs_create_directory(String path, String userID, String sessionID) {
        try {
            FilesystemSandbox filesystemSandbox = new FilesystemSandbox(sandboxManager, userID, sessionID);
            return filesystemSandbox.createDirectory(path);
        } catch (Exception e) {
            String errorMsg = "Create Directory Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }

    public String fs_list_directory(String path, String userID, String sessionID) {
        try {
            FilesystemSandbox filesystemSandbox = new FilesystemSandbox(sandboxManager, userID, sessionID);
            return filesystemSandbox.listDirectory(path);
        } catch (Exception e) {
            String errorMsg = "List Directory Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }

    public String fs_directory_tree(String path, String userID, String sessionID) {
        try {
            FilesystemSandbox filesystemSandbox = new FilesystemSandbox(sandboxManager, userID, sessionID);
            return filesystemSandbox.directoryTree(path);
        } catch (Exception e) {
            String errorMsg = "Directory Tree Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }

    public String fs_move_file(String source, String destination, String userID, String sessionID) {
        try {
            FilesystemSandbox filesystemSandbox = new FilesystemSandbox(sandboxManager, userID, sessionID);
            return filesystemSandbox.moveFile(source, destination);
        } catch (Exception e) {
            String errorMsg = "Move File Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }

    public String fs_search_files(String path, String pattern, String[] excludePatterns, String userID, String sessionID) {
        try {
            FilesystemSandbox filesystemSandbox = new FilesystemSandbox(sandboxManager, userID, sessionID);
            return filesystemSandbox.searchFiles(path, pattern, excludePatterns);
        } catch (Exception e) {
            String errorMsg = "Search Files Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }

    public String fs_get_file_info(String path, String userID, String sessionID) {
        try {
            FilesystemSandbox filesystemSandbox = new FilesystemSandbox(sandboxManager, userID, sessionID);
            return filesystemSandbox.getFileInfo(path);
        } catch (Exception e) {
            String errorMsg = "Get File Info Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }

    public String fs_list_allowed_directories(String userID, String sessionID) {
        try {
            FilesystemSandbox filesystemSandbox = new FilesystemSandbox(sandboxManager, userID, sessionID);
            return filesystemSandbox.listAllowedDirectories();
        } catch (Exception e) {
            String errorMsg = "List Allowed Directories Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }

    // browser tool wrappers
    public String browser_navigate(String url, String userID, String sessionID) {
        try {
            BrowserSandbox browserSandbox = new BrowserSandbox(sandboxManager, userID, sessionID);
            return browserSandbox.navigate(url);
        } catch (Exception e) {
            String errorMsg = "Browser Navigate Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }

    public String browser_console_messages_tool(String userID, String sessionID) {
        try {
            BrowserSandbox browserSandbox = new BrowserSandbox(sandboxManager, userID, sessionID);
            return browserSandbox.consoleMessages();
        } catch (Exception e) {
            String errorMsg = "Browser Console Messages Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }

    public String browser_click(String element, String ref, String userID, String sessionID) {
        try {
            BrowserSandbox browserSandbox = new BrowserSandbox(sandboxManager, userID, sessionID);
            return browserSandbox.click(element, ref);
        } catch (Exception e) {
            String errorMsg = "Browser Click Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }

    public String browser_type(String element, String ref, String text, Boolean submit, Boolean slowly, String userID, String sessionID) {
        try {
            BrowserSandbox browserSandbox = new BrowserSandbox(sandboxManager, userID, sessionID);
            return browserSandbox.type(element, ref, text, submit, slowly);
        } catch (Exception e) {
            String errorMsg = "Browser Type Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }

    public String browser_take_screenshot(Boolean raw, String filename, String element, String ref, String userID, String sessionID) {
        try {
            BrowserSandbox browserSandbox = new BrowserSandbox(sandboxManager, userID, sessionID);
            return browserSandbox.takeScreenshot(raw, filename, element, ref);
        } catch (Exception e) {
            String errorMsg = "Browser Take Screenshot Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }

    public String browser_snapshot(String userID, String sessionID) {
        try {
            BrowserSandbox browserSandbox = new BrowserSandbox(sandboxManager, userID, sessionID);
            return browserSandbox.snapshot();
        } catch (Exception e) {
            String errorMsg = "Browser Snapshot Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }

    public String browser_tab_new(String url, String userID, String sessionID) {
        try {
            BrowserSandbox browserSandbox = new BrowserSandbox(sandboxManager, userID, sessionID);
            return browserSandbox.tabNew(url);
        } catch (Exception e) {
            String errorMsg = "Browser Tab New Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }

    public String browser_tab_select(Integer index, String userID, String sessionID) {
        try {
            BrowserSandbox browserSandbox = new BrowserSandbox(sandboxManager, userID, sessionID);
            return browserSandbox.tabSelect(index);
        } catch (Exception e) {
            String errorMsg = "Browser Tab Select Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }

    public String browser_tab_close(Integer index, String userID, String sessionID) {
        try {
            BrowserSandbox browserSandbox = new BrowserSandbox(sandboxManager, userID, sessionID);
            return browserSandbox.tabClose(index);
        } catch (Exception e) {
            String errorMsg = "Browser Tab Close Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }

    public String browser_wait_for(Double time, String text, String textGone, String userID, String sessionID) {
        try {
            BrowserSandbox browserSandbox = new BrowserSandbox(sandboxManager, userID, sessionID);
            return browserSandbox.waitFor(time, text, textGone);
        } catch (Exception e) {
            String errorMsg = "Browser Wait For Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }

    public String browser_resize(Double width, Double height, String userID, String sessionID) {
        try {
            BrowserSandbox browserSandbox = new BrowserSandbox(sandboxManager, userID, sessionID);
            return browserSandbox.resize(width, height);
        } catch (Exception e) {
            String errorMsg = "Browser Resize Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }

    public String browser_close(String userID, String sessionID) {
        try {
            BrowserSandbox browserSandbox = new BrowserSandbox(sandboxManager, userID, sessionID);
            return browserSandbox.closeBrowser();
        } catch (Exception e) {
            String errorMsg = "Browser Close Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }

    public String browser_handle_dialog(Boolean accept, String promptText, String userID, String sessionID) {
        try {
            BrowserSandbox browserSandbox = new BrowserSandbox(sandboxManager, userID, sessionID);
            return browserSandbox.handleDialog(accept, promptText);
        } catch (Exception e) {
            String errorMsg = "Browser Handle Dialog Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }

    public String browser_file_upload(String[] paths, String userID, String sessionID) {
        try {
            BrowserSandbox browserSandbox = new BrowserSandbox(sandboxManager, userID, sessionID);
            return browserSandbox.fileUpload(paths);
        } catch (Exception e) {
            String errorMsg = "Browser File Upload Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }

    public String browser_press_key(String key, String userID, String sessionID) {
        try {
            BrowserSandbox browserSandbox = new BrowserSandbox(sandboxManager, userID, sessionID);
            return browserSandbox.pressKey(key);
        } catch (Exception e) {
            String errorMsg = "Browser Press Key Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }

    public String browser_navigate_back(String userID, String sessionID) {
        try {
            BrowserSandbox browserSandbox = new BrowserSandbox(sandboxManager, userID, sessionID);
            return browserSandbox.navigateBack();
        } catch (Exception e) {
            String errorMsg = "Browser Navigate Back Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }

    public String browser_navigate_forward(String userID, String sessionID) {
        try {
            BrowserSandbox browserSandbox = new BrowserSandbox(sandboxManager, userID, sessionID);
            return browserSandbox.navigateForward();
        } catch (Exception e) {
            String errorMsg = "Browser Navigate Forward Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }

    public String browser_network_requests(String userID, String sessionID) {
        try {
            BrowserSandbox browserSandbox = new BrowserSandbox(sandboxManager, userID, sessionID);
            return browserSandbox.networkRequests();
        } catch (Exception e) {
            String errorMsg = "Browser Network Requests Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }

    public String browser_pdf_save(String filename, String userID, String sessionID) {
        try {
            BrowserSandbox browserSandbox = new BrowserSandbox(sandboxManager, userID, sessionID);
            return browserSandbox.pdfSave(filename);
        } catch (Exception e) {
            String errorMsg = "Browser PDF Save Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }

    public String browser_drag(String startElement, String startRef, String endElement, String endRef, String userID, String sessionID) {
        try {
            BrowserSandbox browserSandbox = new BrowserSandbox(sandboxManager, userID, sessionID);
            return browserSandbox.drag(startElement, startRef, endElement, endRef);
        } catch (Exception e) {
            String errorMsg = "Browser Drag Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }

    public String browser_hover(String element, String ref, String userID, String sessionID) {
        try {
            BrowserSandbox browserSandbox = new BrowserSandbox(sandboxManager, userID, sessionID);
            return browserSandbox.hover(element, ref);
        } catch (Exception e) {
            String errorMsg = "Browser Hover Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }

    public String browser_select_option(String element, String ref, String[] values, String userID, String sessionID) {
        try {
            BrowserSandbox browserSandbox = new BrowserSandbox(sandboxManager, userID, sessionID);
            return browserSandbox.selectOption(element, ref, values);
        } catch (Exception e) {
            String errorMsg = "Browser Select Option Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }

    public String browser_tab_list(String userID, String sessionID) {
        try {
            BrowserSandbox browserSandbox = new BrowserSandbox(sandboxManager, userID, sessionID);
            return browserSandbox.tabList();
        } catch (Exception e) {
            String errorMsg = "Browser Tab List Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
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
            logger.severe("Failed to close HTTP client: " + e.getMessage());
        }
    }
}

