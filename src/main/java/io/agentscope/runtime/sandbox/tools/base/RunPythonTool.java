package io.agentscope.runtime.sandbox.tools.base;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.runtime.sandbox.box.BaseSandbox;
import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.tools.SandboxTool;
import io.agentscope.runtime.sandbox.tools.utils.ContextUtils;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.logging.Logger;

public class RunPythonTool extends SandboxTool {
    public RunPythonTool() {
        super("run_ipython_cell", "generic", "Execute Python code snippets and return the output or errors.");
        schema = Map.of();
        Map<String, Object> codeProperty = new HashMap<>();
        codeProperty.put("type", "string");
        codeProperty.put("description", "Python code to be executed");

        Map<String, Object> properties = new HashMap<>();
        properties.put("code", codeProperty);

        List<String> required = Arrays.asList("code");

        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("description", "Request object to perform Python code execution");
    }

    @Override
    public SandboxTool bind(Sandbox sandbox) {
        this.sandbox = sandbox;
        return this;
    }

    @Override
    public ToolCallback buildTool() {
        ObjectMapper mapper = new ObjectMapper();
        String inputSchema = "";
        try {
            inputSchema = mapper.writeValueAsString(schema);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return FunctionToolCallback
                .builder(
                        name,
                        new PythonExecutor()
                ).description(description)
                .inputSchema(
                        inputSchema
                ).inputType(PythonExecutor.RunPythonToolRequest.class)
                .toolMetadata(ToolMetadata.builder().returnDirect(false).build())
                .build();
    }

    /**
     * 内部类：处理Python代码执行的工具
     */
    class PythonExecutor implements BiFunction<PythonExecutor.RunPythonToolRequest, ToolContext, PythonExecutor.RunPythonToolResponse> {

        Logger logger = Logger.getLogger(PythonExecutor.class.getName());

        @Override
        public RunPythonToolResponse apply(RunPythonToolRequest request, ToolContext toolContext) {
            String[] userAndSession = ContextUtils.extractUserAndSessionID(toolContext);
            String userID = userAndSession[0];
            String sessionID = userAndSession[1];

            try {
                String result = performPythonExecute(
                        request.code,
                        userID,
                        sessionID
                );

                return new RunPythonToolResponse(
                        new Response(result, "Code execution completed")
                );
            } catch (Exception e) {
                return new RunPythonToolResponse(
                        new Response("Error", "Code execution error : " + e.getMessage())
                );
            }
        }


        private String performPythonExecute(String code, String userID, String sessionID) {
            logger.info("Run Code: " + code);
            String result = run_ipython_cell(code, userID, sessionID);
            logger.info("Execute Result: " + result);
            return result;
        }

        // Request type definition
        public record RunPythonToolRequest(
                @JsonProperty(required = true, value = "code")
                @JsonPropertyDescription("Python code to be executed")
                String code
        ) {
            public RunPythonToolRequest(String code) {
                this.code = code;
            }
        }

        // Response type definition
        public record RunPythonToolResponse(@JsonProperty("Response") Response output) {
            public RunPythonToolResponse(Response output) {
                this.output = output;
            }
        }


        @JsonClassDescription("The result contains the code output and the execute result")
        public record Response(String result, String message) {
            public Response(String result, String message) {
                this.result = result;
                this.message = message;
            }

            @JsonProperty(required = true, value = "result")
            @JsonPropertyDescription("code output")
            public String result() {
                return this.result;
            }

            @JsonProperty(required = true, value = "message")
            @JsonPropertyDescription("execute result")
            public String message() {
                return this.message;
            }
        }

        public String run_ipython_cell(String code, String userID, String sessionID) {
            try {
                if (sandbox != null && sandbox instanceof BaseSandbox baseSandbox) {
                    return baseSandbox.runIpythonCell(code);
                }
                BaseSandbox baseSandbox = new BaseSandbox(sandboxManager, userID, sessionID);
                return baseSandbox.runIpythonCell(code);

            } catch (Exception e) {
                String errorMsg = "Run Python Code Error: " + e.getMessage();
                logger.severe(errorMsg);
                e.printStackTrace();
                return errorMsg;
            }
        }
    }
}

