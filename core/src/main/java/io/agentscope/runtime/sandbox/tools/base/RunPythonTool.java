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

    public String run_ipython_cell(String code) {
        try {
            if(sandbox instanceof BaseSandbox baseSandbox){
                return baseSandbox.runIpythonCell(code);
            }
            throw new RuntimeException("Only BaseSandbox supported in run python tool");
        } catch (Exception e) {
            String errorMsg = "Run Python Code Error: " + e.getMessage();
            logger.severe(errorMsg);
            e.printStackTrace();
            return errorMsg;
        }
    }
}
