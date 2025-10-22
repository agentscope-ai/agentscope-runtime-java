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
import java.util.Map;

/**
 * 浏览器沙箱实现
 * 对应Python版本的BrowserSandbox
 * 提供浏览器自动化能力（导航、点击、截图等）
 */
public class BrowserSandbox extends Sandbox {
    
    /**
     * 构造函数
     * 
     * @param managerApi SandboxManager实例
     * @param userId 用户ID
     * @param sessionId 会话ID
     */
    public BrowserSandbox(SandboxManager managerApi, String userId, String sessionId) {
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
    public BrowserSandbox(
            SandboxManager managerApi,
            String userId,
            String sessionId,
            int timeout) {
        super(managerApi, userId, sessionId, SandboxType.BROWSER, timeout);
    }
    
    /**
     * 导航到URL
     * 
     * @param url 目标URL
     * @return 执行结果
     */
    public String navigate(String url) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("url", url);
        return callTool("browser_navigate", arguments);
    }
    
    /**
     * 点击元素
     * 
     * @param element 元素选择器
     * @param ref 引用
     * @return 执行结果
     */
    public String click(String element, String ref) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("element", element);
        arguments.put("ref", ref);
        return callTool("browser_click", arguments);
    }
    
    /**
     * 输入文本
     * 
     * @param element 元素选择器
     * @param ref 引用
     * @param text 输入文本
     * @return 执行结果
     */
    public String type(String element, String ref, String text) {
        return type(element, ref, text, null, null);
    }
    
    /**
     * 输入文本
     * 
     * @param element 元素选择器
     * @param ref 引用
     * @param text 输入文本
     * @param submit 是否提交
     * @param slowly 是否慢速输入
     * @return 执行结果
     */
    public String type(String element, String ref, String text, Boolean submit, Boolean slowly) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("element", element);
        arguments.put("ref", ref);
        arguments.put("text", text);
        if (submit != null) arguments.put("submit", submit);
        if (slowly != null) arguments.put("slowly", slowly);
        return callTool("browser_type", arguments);
    }
    
    /**
     * 截图
     * 
     * @return 执行结果
     */
    public String takeScreenshot() {
        return takeScreenshot(null, null, null, null);
    }
    
    /**
     * 截图
     * 
     * @param raw 是否返回原始数据
     * @param filename 文件名
     * @param element 元素选择器
     * @param ref 引用
     * @return 执行结果
     */
    public String takeScreenshot(Boolean raw, String filename, String element, String ref) {
        Map<String, Object> arguments = new HashMap<>();
        if (raw != null) arguments.put("raw", raw);
        if (filename != null) arguments.put("filename", filename);
        if (element != null) arguments.put("element", element);
        if (ref != null) arguments.put("ref", ref);
        return callTool("browser_take_screenshot", arguments);
    }
    
    /**
     * 获取页面快照
     * 
     * @return 执行结果
     */
    public String snapshot() {
        return callTool("browser_snapshot", new HashMap<>());
    }
    
    /**
     * 新建标签页
     * 
     * @param url 可选URL
     * @return 执行结果
     */
    public String tabNew(String url) {
        Map<String, Object> arguments = new HashMap<>();
        if (url != null) arguments.put("url", url);
        return callTool("browser_tab_new", arguments);
    }
    
    /**
     * 选择标签页
     * 
     * @param index 标签页索引
     * @return 执行结果
     */
    public String tabSelect(Integer index) {
        Map<String, Object> arguments = new HashMap<>();
        if (index != null) arguments.put("index", index);
        return callTool("browser_tab_select", arguments);
    }
    
    /**
     * 关闭标签页
     * 
     * @param index 标签页索引
     * @return 执行结果
     */
    public String tabClose(Integer index) {
        Map<String, Object> arguments = new HashMap<>();
        if (index != null) arguments.put("index", index);
        return callTool("browser_tab_close", arguments);
    }
    
    /**
     * 列出所有标签页
     * 
     * @return 标签页列表
     */
    public String tabList() {
        return callTool("browser_tab_list", new HashMap<>());
    }
    
    /**
     * 等待
     * 
     * @param time 等待时间（秒）
     * @return 执行结果
     */
    public String waitFor(Double time) {
        return waitFor(time, null, null);
    }
    
    /**
     * 等待
     * 
     * @param time 等待时间（秒）
     * @param text 等待文本出现
     * @param textGone 等待文本消失
     * @return 执行结果
     */
    public String waitFor(Double time, String text, String textGone) {
        Map<String, Object> arguments = new HashMap<>();
        if (time != null) arguments.put("time", time);
        if (text != null) arguments.put("text", text);
        if (textGone != null) arguments.put("textGone", textGone);
        return callTool("browser_wait_for", arguments);
    }
    
    /**
     * 调整浏览器窗口大小
     * 
     * @param width 宽度
     * @param height 高度
     * @return 执行结果
     */
    public String resize(Double width, Double height) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("width", width);
        arguments.put("height", height);
        return callTool("browser_resize", arguments);
    }
    
    /**
     * 关闭浏览器
     * 
     * @return 执行结果
     */
    public String closeBrowser() {
        return callTool("browser_close", new HashMap<>());
    }
    
    /**
     * 获取控制台消息
     * 
     * @return 控制台消息
     */
    public String consoleMessages() {
        return callTool("browser_console_messages", new HashMap<>());
    }
    
    /**
     * 获取网络请求
     * 
     * @return 网络请求列表
     */
    public String networkRequests() {
        return callTool("browser_network_requests", new HashMap<>());
    }
    
    /**
     * 后退
     * 
     * @return 执行结果
     */
    public String navigateBack() {
        return callTool("browser_navigate_back", new HashMap<>());
    }
    
    /**
     * 前进
     * 
     * @return 执行结果
     */
    public String navigateForward() {
        return callTool("browser_navigate_forward", new HashMap<>());
    }
    
    /**
     * 悬停在元素上
     * 
     * @param element 元素选择器
     * @param ref 引用
     * @return 执行结果
     */
    public String hover(String element, String ref) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("element", element);
        arguments.put("ref", ref);
        return callTool("browser_hover", arguments);
    }
    
    /**
     * 按键
     * 
     * @param key 按键名称
     * @return 执行结果
     */
    public String pressKey(String key) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("key", key);
        return callTool("browser_press_key", arguments);
    }
    
    /**
     * 处理对话框
     * 
     * @param accept 是否接受
     * @param promptText 提示文本
     * @return 执行结果
     */
    public String handleDialog(Boolean accept, String promptText) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("accept", accept);
        if (promptText != null) {
            arguments.put("promptText", promptText);
        }
        return callTool("browser_handle_dialog", arguments);
    }
    
    /**
     * 文件上传
     * 
     * @param paths 文件路径数组
     * @return 执行结果
     */
    public String fileUpload(String[] paths) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("paths", paths);
        return callTool("browser_file_upload", arguments);
    }
    
    /**
     * 保存PDF
     * 
     * @param filename 文件名
     * @return 执行结果
     */
    public String pdfSave(String filename) {
        Map<String, Object> arguments = new HashMap<>();
        if (filename != null) {
            arguments.put("filename", filename);
        }
        return callTool("browser_pdf_save", arguments);
    }
    
    /**
     * 拖拽元素
     * 
     * @param startElement 起始元素选择器
     * @param startRef 起始引用
     * @param endElement 结束元素选择器
     * @param endRef 结束引用
     * @return 执行结果
     */
    public String drag(String startElement, String startRef, String endElement, String endRef) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("startElement", startElement);
        arguments.put("startRef", startRef);
        arguments.put("endElement", endElement);
        arguments.put("endRef", endRef);
        return callTool("browser_drag", arguments);
    }
    
    /**
     * 选择选项
     * 
     * @param element 元素选择器
     * @param ref 引用
     * @param values 选项值数组
     * @return 执行结果
     */
    public String selectOption(String element, String ref, String[] values) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("element", element);
        arguments.put("ref", ref);
        arguments.put("values", values);
        return callTool("browser_select_option", arguments);
    }
}

