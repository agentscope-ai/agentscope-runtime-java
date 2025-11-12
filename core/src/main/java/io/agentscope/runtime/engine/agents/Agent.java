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

import reactor.core.publisher.Flux;
import io.agentscope.runtime.engine.schemas.context.Context;
import io.agentscope.runtime.engine.schemas.agent.Event;

import java.util.concurrent.CompletableFuture;

/**
 * Agent interface definition
 * Corresponds to the Agent class in base_agent.py of the Python version
 */
public interface Agent {

    /**
     * Get name of Agent.
     *
     * @return agent name
     */
    String getName();

    /**
     * Get description of Agent.
     *
     * @return agent description
     */
    String getDescription();

    /**
     * Execute Agent asynchronously
     * Corresponds to the run_async method of the Python version
     *
     * @param context execution context
     * @return event stream
     */
    CompletableFuture<Flux<Event>> runAsync(Context context, boolean stream);
    
    /**
     * Set pre-execution callback
     * Corresponds to the before_agent_callback of the Python version
     */
    void setBeforeCallback(AgentCallback callback);
    
    /**
     * Set post-execution callback
     * Corresponds to the after_agent_callback of the Python version
     */
    void setAfterCallback(AgentCallback callback);
    
    /**
     * Get Agent configuration
     * Corresponds to the agent_config of the Python version
     */
    AgentConfig getConfig();
    
    /**
     * Copy Agent instance
     * Corresponds to the copy method of the Python version
     */
    Agent copy();
}
