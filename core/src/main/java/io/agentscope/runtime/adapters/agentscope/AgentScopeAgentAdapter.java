/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agentscope.runtime.adapters.agentscope;

import io.agentscope.runtime.adapters.AgentAdapter;
import io.agentscope.runtime.adapters.MessageAdapter;
import io.agentscope.runtime.adapters.StreamAdapter;
import io.agentscope.runtime.engine.schemas.AgentRequest;
import io.agentscope.runtime.engine.schemas.Event;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;

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
 *   <li>{@link #init()} - Initialize the adapter</li>
 *   <li>{@link #start()} - Start the adapter</li>
 *   <li>{@link #streamQuery(AgentRequest, Object)} - Process queries</li>
 *   <li>{@link #stop()} - Stop the adapter</li>
 *   <li>{@link #shutdown()} - Shutdown the adapter</li>
 *   <li>{@link #isHealthy()} - Check health status</li>
 * </ul>
 */
public abstract class AgentScopeAgentAdapter implements AgentAdapter {
    
    private final StreamAdapter streamAdapter;
    private final MessageAdapter messageAdapter;
    
    /**
     * Creates a new AgentScopeAgentAdapter instance.
     * Initializes the stream adapter and message adapter for AgentScope framework.
     */
    protected AgentScopeAgentAdapter() {
        this.streamAdapter = new AgentScopeStreamAdapter();
        this.messageAdapter = new AgentScopeMessageAdapter();
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
     * Initialize the adapter and prepare resources.
     * Must be implemented by subclasses.
     *
     * @return a CompletableFuture that completes when initialization is done
     */
    @Override
    public abstract CompletableFuture<Void> init();
    
    /**
     * Start the adapter and mark it as ready to process queries.
     * Must be implemented by subclasses.
     *
     * @return a CompletableFuture that completes when the adapter is started
     */
    @Override
    public abstract CompletableFuture<Void> start();
    
    /**
     * Process an agent query and return a stream of events.
     * Must be implemented by subclasses.
     *
     * @param request the agent request containing input messages, session info, etc.
     * @param messages the converted framework-specific messages (e.g., List&lt;Msg&gt; for agentscope),
     *                 or null if no conversion is needed
     * @return a Flux of Event objects representing the streaming response
     */
    @Override
    public abstract Flux<Event> streamQuery(AgentRequest request, Object messages);
    
    /**
     * Stop the adapter and prevent it from accepting new queries.
     * Must be implemented by subclasses.
     *
     * @return a CompletableFuture that completes when the adapter is stopped
     */
    @Override
    public abstract CompletableFuture<Void> stop();
    
    /**
     * Shutdown the adapter and clean up resources.
     * Must be implemented by subclasses.
     *
     * @return a CompletableFuture that completes when shutdown is done
     */
    @Override
    public abstract CompletableFuture<Void> shutdown();
    
    /**
     * Check if the adapter is in a healthy/ready state.
     * Must be implemented by subclasses.
     *
     * @return true if the adapter is healthy and ready, false otherwise
     */
    @Override
    public abstract boolean isHealthy();
}

