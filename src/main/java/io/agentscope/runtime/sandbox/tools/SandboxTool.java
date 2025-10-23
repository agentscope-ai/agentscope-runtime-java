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
import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.manager.SandboxManager;

import java.util.Map;

/**
 * Sandbox工具抽象类
 * 类似于 Python 的 SandboxTool 类
 * <p>
 * 继承自 Tool，添加了 sandbox 相关的功能
 */
public abstract class SandboxTool extends Tool {

    /**
     * Sandbox管理器
     */
    protected SandboxManager sandboxManager;

    /**
     * Sandbox实例
     */
    protected Sandbox sandbox;

    /**
     * 工具的 JSON Schema（OpenAI function calling 格式）
     */
    protected Map<String, Object> schema;

    /**
     * 构造函数
     *
     * @param name        工具名称
     * @param toolType    工具类型
     * @param description 工具描述
     */
    protected SandboxTool(String name, String toolType, String description) {
        super(name, toolType, description);
        this.sandboxManager = Runner.getSandboxManager();
    }

    /**
     * 构造函数，允许指定 SandboxManager
     *
     * @param name           工具名称
     * @param toolType       工具类型
     * @param description    工具描述
     * @param sandboxManager Sandbox管理器
     */
    protected SandboxTool(String name, String toolType, String description,
                          SandboxManager sandboxManager) {
        super(name, toolType, description);
        this.sandboxManager = sandboxManager;
    }

    /**
     * 获取 Sandbox管理器
     *
     * @return SandboxManager 实例
     */
    public SandboxManager getSandboxManager() {
        return sandboxManager;
    }

    /**
     * 获取 Sandbox实例
     *
     * @return Sandbox 实例（可能为 null）
     */
    public Sandbox getSandbox() {
        return sandbox;
    }

    /**
     * 设置 Sandbox实例
     *
     * @param sandbox Sandbox 实例
     */
    public void setSandbox(Sandbox sandbox) {
        this.sandbox = sandbox;
    }

    /**
     * 获取工具的 JSON Schema
     *
     * @return JSON Schema 字符串
     */
    public Map<String, Object> getSchema() {
        return schema;
    }

    /**
     * 设置工具的 JSON Schema
     *
     * @param schema JSON Schema 字符串
     */
    protected void setSchema(Map<String, Object> schema) {
        this.schema = schema;
    }


    /**
     * 绑定 Sandbox
     * 返回一个新的绑定了特定 sandbox 的工具实例
     *
     * @param sandbox 要绑定的 Sandbox
     * @return 绑定后的新工具实例
     */
    public abstract SandboxTool bind(Sandbox sandbox);
}
