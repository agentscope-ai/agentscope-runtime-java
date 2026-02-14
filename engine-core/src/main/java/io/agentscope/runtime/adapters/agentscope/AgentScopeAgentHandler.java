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
package io.agentscope.runtime.adapters.agentscope;

import io.agentscope.runtime.adapters.AgentHandler;
import io.agentscope.runtime.adapters.MessageAdapter;
import io.agentscope.runtime.adapters.StreamAdapter;
import io.agentscope.runtime.engine.schemas.AgentRequest;
import io.agentscope.runtime.engine.services.agent_state.StateService;
import io.agentscope.runtime.engine.services.memory.service.MemoryService;
import io.agentscope.runtime.engine.services.memory.service.SessionHistoryService;
import io.agentscope.runtime.sandbox.manager.SandboxService;
import reactor.core.publisher.Flux;

/**
 * Abstract base class for AgentScope framework adapter implementations.
 *
 * <p>This class provides common functionality for AgentScope adapters, including:</p>
 * <ul>
 *   <li>Framework type identification ("agentscope")</li>
 *   <li>Stream adapter for converting AgentScope streaming events</li>
 * </ul>
 *
 * <p>Subclasses must implement the lifecycle methods:</p>
 * <ul>
 *   <li>{@link #start()} - Start the adapter</li>
 *   <li>{@link #streamQuery(AgentRequest, Object)} - Process queries</li>
 *   <li>{@link #stop()} - Stop the adapter</li>
 *   <li>{@link #isHealthy()} - Check health status</li>
 * </ul>
 */
public abstract class AgentScopeAgentHandler implements AgentHandler {

    protected final StreamAdapter streamAdapter;
    protected final MessageAdapter messageAdapter;
    protected SandboxService sandboxService;

    // short-term from runtime, specifically for agentscope
    protected StateService stateService;
    // short-term from runtime
    protected SessionHistoryService sessionHistoryService;
    // long-term from runtime
    protected MemoryService memoryService;

    AgentScopeAgentHandler(StateService stateService, SessionHistoryService sessionHistoryService, MemoryService memoryService, SandboxService sandboxService) {
        this();
        this.stateService = stateService;
        this.sessionHistoryService = sessionHistoryService;
        this.memoryService = memoryService;
        this.sandboxService = sandboxService;
    }

    /**
     * Creates a new AgentScopeAgentAdapter instance.
     * Initializes the stream adapter and message adapter for AgentScope framework.
     */
    protected AgentScopeAgentHandler() {
        this.streamAdapter = new AgentScopeStreamAdapter();
        this.messageAdapter = new AgentScopeMessageAdapter();
    }

    public void setSessionHistoryService(SessionHistoryService sessionHistoryService) {
        this.sessionHistoryService = sessionHistoryService;
    }

    public void setStateService(StateService stateService) {
        this.stateService = stateService;
    }

    public void setMemoryService(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    /**
     * Get the framework type this adapter supports.
     *
     * @return "agentscope" as the framework type
     */
    @Override
    public String getFrameworkType() {
        return "agentscope";
    }
    
    /**
     * Get the StreamAdapter implementation for AgentScope framework.
     *
     * @return the AgentScopeStreamAdapter instance
     */
    @Override
    public StreamAdapter getStreamAdapter() {
        return streamAdapter;
    }
    
    /**
     * Get the MessageAdapter implementation for AgentScope framework.
     *
     * @return the AgentScopeMessageAdapter instance
     */
    @Override
    public MessageAdapter getMessageAdapter() {
        return messageAdapter;
    }
    
    // Lifecycle methods - must be implemented by subclasses
    
    /**
     * Start the adapter and mark it as ready to process queries.
     * Must be implemented by subclasses.
     */
    @Override
    public void start() {
        if (stateService != null) {
            stateService.start();
        }
        if (sessionHistoryService != null) {
            sessionHistoryService.start();
        }
    }

    /**
     * Stop the adapter and prevent it from accepting new queries.
     * Must be implemented by subclasses.
     */
    @Override
    public void stop() {
        if (stateService != null) {
            stateService.stop();
        }
        if (sessionHistoryService != null) {
            sessionHistoryService.stop();
        }
        if(sandboxService != null){
            sandboxService.stop();
        }
    }

    /**
     * Check if the adapter is in a healthy/ready state.
     * Must be implemented by subclasses.
     *
     * @return true if the adapter is healthy and ready, false otherwise
     */
    @Override
    public abstract boolean isHealthy();

    /**
     * Process an agent query and return a stream of framework-specific events.
     * Must be implemented by subclasses.
     *
     * @param request the agent request containing input messages, session info, etc.
     * @param messages the converted framework-specific messages (e.g., List&lt;Msg&gt; for agentscope),
     *                 or null if no conversion is needed
     * @return a Flux of AgentScope Event objects (Flux&lt;io.agentscope.core.agent.Event&gt;)
     */
    @Override
    public abstract Flux<?> streamQuery(AgentRequest request, Object messages);

    public SandboxService getSandboxService(){
        return sandboxService;
    }

    public void setSandboxService(SandboxService sandboxService){
        this.sandboxService = sandboxService;
    }

}

