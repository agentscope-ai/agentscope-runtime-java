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

/**
 * 抽象基类，定义所有工具的通用接口
 * 类似于 Python 的 Tool 基类
 * 
 * 主要职责：
 * - 定义标准的工具接口
 * - 确保不同工具类型之间的一致性行为
 * - 提供通用功能
 */
public abstract class Tool {
    
    /**
     * 工具名称
     */
    protected String name;
    
    /**
     * 工具类型标识符
     */
    protected String toolType;
    
    /**
     * 工具描述
     */
    protected String description;
    
    /**
     * 构造函数
     * 
     * @param name 工具名称
     * @param toolType 工具类型
     * @param description 工具描述
     */
    protected Tool(String name, String toolType, String description) {
        this.name = name;
        this.toolType = toolType;
        this.description = description;
    }
    
    /**
     * 获取工具名称
     * 
     * @return 工具名称
     */
    public String getName() {
        return name;
    }
    
    /**
     * 获取工具类型
     * 
     * @return 工具类型
     */
    public String getToolType() {
        return toolType;
    }
    
    /**
     * 获取工具描述
     * 
     * @return 工具描述
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 构建 ToolCallback
     * 每个具体工具必须实现此方法来创建其对应的 ToolCallback
     * 
     * @return ToolCallback 实例
     */
    public abstract ToolCallback buildTool();
    
    @Override
    public String toString() {
        return String.format("%s(name='%s', type='%s')", 
            this.getClass().getSimpleName(), name, toolType);
    }
}

