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
import io.agentscope.runtime.sandbox.manager.fs.FileSystemStarter;
import io.agentscope.runtime.sandbox.manager.fs.LocalFileSystemStarter;
import io.agentscope.runtime.sandbox.manager.registry.RegisterSandbox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RegisterSandbox(
        imageName = "agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-filesystem:latest",
        sandboxType = "filesystem",
        securityLevel = "medium",
        timeout = 60,
        description = "Filesystem sandbox"
)
public class FilesystemSandbox extends Sandbox {

    private final String baseUrl;

    public FilesystemSandbox(SandboxService managerApi, String userId, String sessionId) {
        this(managerApi, userId, sessionId, null, LocalFileSystemStarter.builder().build(), Map.of());
    }

    public FilesystemSandbox(
            SandboxService managerApi,
            String userId,
            String sessionId,
            String baseUrl) {
        this(managerApi, userId, sessionId, baseUrl, LocalFileSystemStarter.builder().build(), Map.of());
    }

    public FilesystemSandbox(
            SandboxService managerApi,
            String userId,
            String sessionId,
            FileSystemStarter fileSystemStarter) {
        this(managerApi, userId, sessionId, null, fileSystemStarter, Map.of());
    }

    public FilesystemSandbox(
            SandboxService managerApi,
            String userId,
            String sessionId,
            Map<String, String> environment) {
        this(managerApi, userId, sessionId, null, LocalFileSystemStarter.builder().build(), environment);
    }

    public FilesystemSandbox(
            SandboxService managerApi,
            String userId,
            String sessionId,
            String baseUrl,
            FileSystemStarter fileSystemStarter,
            Map<String, String> environment) {
        super(managerApi, userId, sessionId, "filesystem", fileSystemStarter, environment);
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

    public String readFile(String path) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("path", path);
        return callTool("read_file", arguments);
    }

    public String readMultipleFiles(List<String> paths) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("paths", paths);
        return callTool("read_multiple_files", arguments);
    }

    public String writeFile(String path, String content) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("path", path);
        arguments.put("content", content);
        return callTool("write_file", arguments);
    }

    public String editFile(String path, Object[] edits) {
        return editFile(path, edits, false);
    }

    public String editFile(String path, Object[] edits, boolean dryRun) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("path", path);
        arguments.put("edits", edits);
        arguments.put("dryRun", dryRun);
        return callTool("edit_file", arguments);
    }

    public String createDirectory(String path) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("path", path);
        return callTool("create_directory", arguments);
    }

    public String listDirectory(String path) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("path", path);
        return callTool("list_directory", arguments);
    }

    public String directoryTree(String path) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("path", path);
        return callTool("directory_tree", arguments);
    }

    public String moveFile(String source, String destination) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("source", source);
        arguments.put("destination", destination);
        return callTool("move_file", arguments);
    }

    public String searchFiles(String path, String pattern) {
        return searchFiles(path, pattern, new String[0]);
    }

    public String searchFiles(String path, String pattern, String[] excludePatterns) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("path", path);
        arguments.put("pattern", pattern);
        arguments.put("excludePatterns", excludePatterns != null ? excludePatterns : new String[0]);
        return callTool("search_files", arguments);
    }

    public String getFileInfo(String path) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("path", path);
        return callTool("get_file_info", arguments);
    }

    public String listAllowedDirectories() {
        return callTool("list_allowed_directories", new HashMap<>());
    }
}
