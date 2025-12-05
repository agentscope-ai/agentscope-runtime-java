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

package io.agentscope.runtime.engine.schemas;

import java.util.Arrays;
import java.util.List;

/**
 * Message type constants.
 */
public class MessageType {
    public static final String MESSAGE = "message";
    public static final String FUNCTION_CALL = "function_call";
    public static final String FUNCTION_CALL_OUTPUT = "function_call_output";
    public static final String PLUGIN_CALL = "plugin_call";
    public static final String PLUGIN_CALL_OUTPUT = "plugin_call_output";
    public static final String COMPONENT_CALL = "component_call";
    public static final String COMPONENT_CALL_OUTPUT = "component_call_output";
    public static final String MCP_LIST_TOOLS = "mcp_list_tools";
    public static final String MCP_APPROVAL_REQUEST = "mcp_approval_request";
    public static final String MCP_TOOL_CALL = "mcp_call";
    public static final String MCP_APPROVAL_RESPONSE = "mcp_approval_response";
    public static final String REASONING = "reasoning";
    public static final String HEARTBEAT = "heartbeat";
    public static final String ERROR = "error";
    
    /**
     * Returns all message type values.
     */
    public static List<String> allValues() {
        return Arrays.asList(
            MESSAGE,
            FUNCTION_CALL,
            FUNCTION_CALL_OUTPUT,
            PLUGIN_CALL,
            PLUGIN_CALL_OUTPUT,
            COMPONENT_CALL,
            COMPONENT_CALL_OUTPUT,
            MCP_LIST_TOOLS,
            MCP_APPROVAL_REQUEST,
            MCP_TOOL_CALL,
            MCP_APPROVAL_RESPONSE,
            REASONING,
            HEARTBEAT,
            ERROR
        );
    }
    
    private MessageType() {
        // Utility class
    }
}

