package io.agentscope.runtime.sandbox.tools.base;

import io.agentscope.runtime.sandbox.box.BaseSandbox;
import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.tools.SandboxTool;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class RunPythonTool extends BaseSandboxTool {

    Logger logger = Logger.getLogger(RunPythonTool.class.getName());

    public RunPythonTool() {
        super("run_ipython_cell", "generic", "Execute Python code snippets and return the output or errors.");
        schema = new HashMap<>();
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
