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

package io.agentscope.runtime.sandbox.manager.util;

import java.util.Map;

public abstract class SandboxClient implements AutoCloseable {

    public abstract boolean checkHealth();

    public abstract void waitUntilHealthy();

    public abstract String callTool(String toolName, Map<String, Object> arguments);

    public abstract Map<String, Object> listTools(String toolType, Map<String, Object> arguments);

    public abstract Map<String, Object> addMcpServers(Map<String, Object> serverConfigs, boolean overwrite);

    @Override
    public void close() throws Exception {

    }
}
