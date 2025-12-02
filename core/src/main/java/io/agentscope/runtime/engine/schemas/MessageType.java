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

