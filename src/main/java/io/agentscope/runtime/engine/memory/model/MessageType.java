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
package io.agentscope.runtime.engine.memory.model;

/**
 * Message type enumeration
 */
public enum MessageType {
    MESSAGE,
    SYSTEM,
    USER,
    ASSISTANT,
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
    HEARTBEAT,
    ERROR
}
