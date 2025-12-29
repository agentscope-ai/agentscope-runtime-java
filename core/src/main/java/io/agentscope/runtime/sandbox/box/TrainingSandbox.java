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

import java.util.HashMap;
import java.util.Map;

public class TrainingSandbox extends Sandbox {
    
    public TrainingSandbox(
            SandboxManager managerApi,
            String userId,
            String sessionId,
            String sandboxType) {
        this(managerApi, userId, sessionId, sandboxType, 3000);
    }
    
    public TrainingSandbox(
            SandboxManager managerApi,
            String userId,
            String sessionId,
            String sandboxType,
            int timeout) {
        super(managerApi, userId, sessionId, sandboxType, timeout);
    }
    
    /**
     * Create a new instance of a training environment
     * 
     * @param envType Type of environment to create
     * @param taskId Identifier for the specific task
     * @return The created instance details
     */
    public String createInstance(String envType, String taskId) {
        return createInstance(envType, taskId, null, null);
    }
    
    /**
     * Create a new instance of a training environment
     * 
     * @param envType Type of environment to create
     * @param taskId Identifier for the specific task
     * @param instanceId Custom instance identifier (optional)
     * @param params Additional parameters for instance creation (optional)
     * @return The created instance details
     */
    public String createInstance(
            String envType, 
            String taskId, 
            String instanceId, 
            Map<String, Object> params) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("env_type", envType);
        arguments.put("task_id", taskId);
        if (instanceId != null) {
            arguments.put("instance_id", instanceId);
        }
        if (params != null) {
            arguments.put("params", params);
        }
        return callTool("create_instance", arguments);
    }
    
    /**
     * Retrieve task identifiers for a specific environment
     * 
     * @param envType Type of environment
     * @return List of task identifiers
     */
    public String getTaskIds(String envType) {
        return getTaskIds(envType, "train", null);
    }
    
    /**
     * Retrieve task identifiers for a specific environment
     * 
     * @param envType Type of environment
     * @param split Data split to retrieve tasks from (default: "train")
     * @param params Additional filtering parameters (optional)
     * @return List of task identifiers
     */
    public String getTaskIds(String envType, String split, Map<String, Object> params) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("env_type", envType);
        arguments.put("split", split);
        if (params != null) {
            arguments.put("params", params);
        }
        return callTool("get_task_ids", arguments);
    }
    
    /**
     * Retrieve the environment profile
     * 
     * @param envType Type of environment
     * @return Environment profile details
     */
    public String getEnvProfile(String envType) {
        return getEnvProfile(envType, "train", null);
    }
    
    /**
     * Retrieve the environment profile
     * 
     * @param envType Type of environment
     * @param split Data split to retrieve profile from (default: "train")
     * @param params Additional profile retrieval parameters (optional)
     * @return Environment profile details
     */
    public String getEnvProfile(String envType, String split, Map<String, Object> params) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("env_type", envType);
        arguments.put("split", split);
        if (params != null) {
            arguments.put("params", params);
        }
        return callTool("get_env_profile", arguments);
    }
    
    /**
     * Execute a step in the training environment
     * 
     * @param instanceId Identifier of the environment instance
     * @return Result of the step execution
     */
    public String step(String instanceId) {
        return step(instanceId, null, null);
    }
    
    /**
     * Execute a step in the training environment
     * 
     * @param instanceId Identifier of the environment instance
     * @param action Action to be performed in the environment (optional)
     * @param params Additional step parameters (optional)
     * @return Result of the step execution
     */
    public String step(String instanceId, Map<String, Object> action, Map<String, Object> params) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("instance_id", instanceId);
        arguments.put("action", action != null ? action : new HashMap<>());
        arguments.put("params", params != null ? params : new HashMap<>());
        return callTool("step", arguments);
    }
    
    /**
     * Evaluate the performance of a training environment instance
     * 
     * @param instanceId Identifier of the environment instance
     * @return Evaluation results
     */
    public String evaluate(String instanceId) {
        return evaluate(instanceId, null, null);
    }
    
    /**
     * Evaluate the performance of a training environment instance
     * 
     * @param instanceId Identifier of the environment instance
     * @param messages Evaluation-related messages (optional)
     * @param params Additional evaluation parameters (optional)
     * @return Evaluation results
     */
    public String evaluate(
            String instanceId, 
            Map<String, Object> messages, 
            Map<String, Object> params) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("instance_id", instanceId);
        arguments.put("messages", messages != null ? messages : new HashMap<>());
        arguments.put("params", params != null ? params : new HashMap<>());
        return callTool("evaluate", arguments);
    }
    
    /**
     * Release a training environment instance
     * 
     * @param instanceId Identifier of the instance to be released
     * @return Result of the instance release operation
     */
    public String releaseInstance(String instanceId) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("instance_id", instanceId);
        return callTool("release_instance", arguments);
    }
}

