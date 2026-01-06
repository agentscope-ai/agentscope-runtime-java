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


import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.manager.SandboxService;

import java.util.Map;

public abstract class SandboxTool extends Tool {

    protected SandboxService sandboxService;

    protected Sandbox sandbox;

    protected Map<String, Object> schema;

    protected SandboxTool(String name, String toolType, String description) {
        super(name, toolType, description);
    }

    protected SandboxTool(String name, String toolType, String description,
                          SandboxService sandboxService) {
        super(name, toolType, description);
        this.sandboxService = sandboxService;
    }

    public SandboxService getSandboxService() {
        return sandboxService;
    }

    public void setSandboxService(SandboxService sandboxService) {
        this.sandboxService = sandboxService;
    }

    public Sandbox getSandbox() {
        return sandbox;
    }

    public void setSandbox(Sandbox sandbox) {
        this.sandbox = sandbox;
    }

    public Map<String, Object> getSchema() {
        return schema;
    }

    protected void setSchema(Map<String, Object> schema) {
        this.schema = schema;
    }

    public abstract Class<? extends Sandbox> getSandboxClass();

    public abstract SandboxTool bind(Sandbox sandbox);
}
