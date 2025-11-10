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

package io.agentscope.runtime.engine.agents;

import io.agentscope.runtime.engine.schemas.context.Context;

/**
 * Agent callback interface
 * Corresponds to before_agent_callback and after_agent_callback in the Python version
 */
@FunctionalInterface
public interface AgentCallback {
    
    /**
     * Execute callback
     *
     * @param context execution context
     */
    void execute(Context context);
}
