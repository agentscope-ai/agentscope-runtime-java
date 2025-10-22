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

import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件系统沙箱实现
 * 对应Python版本的FilesystemSandbox
 * 提供文件系统操作能力（读写文件、目录操作等）
 */
public class FilesystemSandbox extends Sandbox {
    
    /**
     * 构造函数
     * 
     * @param managerApi SandboxManager实例
     * @param userId 用户ID
     * @param sessionId 会话ID
     */
    public FilesystemSandbox(SandboxManager managerApi, String userId, String sessionId) {
        this(managerApi, userId, sessionId, 3000);
    }
    
    /**
     * 构造函数
     * 
     * @param managerApi SandboxManager实例
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param timeout 超时时间（秒）
     */
    public FilesystemSandbox(
            SandboxManager managerApi,
            String userId,
            String sessionId,
            int timeout) {
        super(managerApi, userId, sessionId, SandboxType.FILESYSTEM, timeout);
    }
    
    /**
     * 读取文件
     * 
     * @param path 文件路径
     * @return 文件内容
     */
    public String readFile(String path) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("path", path);
        return callTool("read_file", arguments);
    }
    
    /**
     * 读取多个文件
     * 
     * @param paths 文件路径列表
     * @return 文件内容
     */
    public String readMultipleFiles(List<String> paths) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("paths", paths);
        return callTool("read_multiple_files", arguments);
    }
    
    /**
     * 写入文件
     * 
     * @param path 文件路径
     * @param content 文件内容
     * @return 执行结果
     */
    public String writeFile(String path, String content) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("path", path);
        arguments.put("content", content);
        return callTool("write_file", arguments);
    }
    
    /**
     * 编辑文件
     * 
     * @param path 文件路径
     * @param edits 编辑操作列表
     * @return 执行结果
     */
    public String editFile(String path, Object[] edits) {
        return editFile(path, edits, false);
    }
    
    /**
     * 编辑文件
     * 
     * @param path 文件路径
     * @param edits 编辑操作列表
     * @param dryRun 是否为试运行
     * @return 执行结果
     */
    public String editFile(String path, Object[] edits, boolean dryRun) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("path", path);
        arguments.put("edits", edits);
        arguments.put("dryRun", dryRun);
        return callTool("edit_file", arguments);
    }
    
    /**
     * 创建目录
     * 
     * @param path 目录路径
     * @return 执行结果
     */
    public String createDirectory(String path) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("path", path);
        return callTool("create_directory", arguments);
    }
    
    /**
     * 列出目录内容
     * 
     * @param path 目录路径
     * @return 目录内容列表
     */
    public String listDirectory(String path) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("path", path);
        return callTool("list_directory", arguments);
    }
    
    /**
     * 获取目录树
     * 
     * @param path 目录路径
     * @return 目录树结构
     */
    public String directoryTree(String path) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("path", path);
        return callTool("directory_tree", arguments);
    }
    
    /**
     * 移动文件
     * 
     * @param source 源路径
     * @param destination 目标路径
     * @return 执行结果
     */
    public String moveFile(String source, String destination) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("source", source);
        arguments.put("destination", destination);
        return callTool("move_file", arguments);
    }
    
    /**
     * 搜索文件
     * 
     * @param path 搜索路径
     * @param pattern 搜索模式
     * @return 匹配的文件列表
     */
    public String searchFiles(String path, String pattern) {
        return searchFiles(path, pattern, new String[0]);
    }
    
    /**
     * 搜索文件
     * 
     * @param path 搜索路径
     * @param pattern 搜索模式
     * @param excludePatterns 排除模式
     * @return 匹配的文件列表
     */
    public String searchFiles(String path, String pattern, String[] excludePatterns) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("path", path);
        arguments.put("pattern", pattern);
        arguments.put("excludePatterns", excludePatterns != null ? excludePatterns : new String[0]);
        return callTool("search_files", arguments);
    }
    
    /**
     * 获取文件信息
     * 
     * @param path 文件路径
     * @return 文件信息
     */
    public String getFileInfo(String path) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("path", path);
        return callTool("get_file_info", arguments);
    }
    
    /**
     * 列出允许访问的目录
     * 
     * @return 允许访问的目录列表
     */
    public String listAllowedDirectories() {
        return callTool("list_allowed_directories", new HashMap<>());
    }
}

