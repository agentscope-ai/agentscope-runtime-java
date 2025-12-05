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
package io.agentscope.runtime.adapters;

import io.agentscope.runtime.engine.schemas.AgentRequest;
import reactor.core.publisher.Flux;

/**
 * AgentAdapter interface provides a unified abstraction for AgentApp core capabilities.
 *
 * <p>This interface is designed to adapt different Agent Framework Types (e.g., AgentScope,
 * Spring AI Alibaba, Langchain4j) and provides the core lifecycle management and query processing
 * capabilities.</p>
 *
 *
 * <p>Lifecycle flow:</p>
 * <ol>
 *   <li>{@link #start()} - Start the adapter and mark it as ready</li>
 *   <li>{@link #streamQuery(AgentRequest, Object)} - Process queries (can be called multiple times)</li>
 *   <li>{@link #stop()} - Stop accepting new queries</li>
 * </ol>
 *
 */
public interface AgentHandler {

    String getName();

    String getDescription();

    /**
     * Get the framework type this adapter supports.
     *
     * <p>Supported framework types: "AgentScope", "Spring Ai Alibaba", "Langchain4j"</p>
     *
     * @return the framework type string
     */
    String getFrameworkType();

    /**
     * Start the adapter and mark it as ready to process queries.
     *
     * After this method completes, the adapter should be ready to accept queries
     * via {@link #streamQuery(AgentRequest, Object)}.</p>
     *
     * <p>This method typically calls the init_handler if one was registered</p>
     */
    void start();

    /**
     * Process an agent query and return a stream of framework-specific events.
     *
     * It processes the agent request and returns a reactive stream of framework-specific events.</p>
     *
     * <p>The implementation should:</p>
     * <ul>
     *   <li>Handle the framework-specific message format conversion</li>
     *   <li>Invoke the registered query handler</li>
     *   <li>Return the raw framework-specific event stream (e.g., Flux&lt;io.agentscope.core.agent.Event&gt; for agentscope)</li>
     * </ul>
     *
     * <p>This method can be called multiple times concurrently. The adapter should
     * be in a started state (after {@link #start()} completes) before this method
     * is called.</p>
     *
     * <p>The messages parameter contains framework-specific converted messages
     * (e.g., List&lt;Msg&gt; for agentscope).
     * Converted messages are passed via kwargs: kwargs.update({"msgs": message_to_agentscope_msg(request.input)}).
     * If messages is null, the adapter should use request.getInput() directly.</p>
     *
     * <p>Note: The returned stream should contain framework-specific Event objects.
     * The Runner will use StreamAdapter to convert this raw framework stream to runtime Event stream.</p>
     *
     * @param request the agent request containing input messages, session info, etc.
     * @param messages the converted framework-specific messages (e.g., List&lt;Msg&gt; for agentscope),
     *                 or null if no conversion is needed
     * @return a Flux of framework-specific Event objects (e.g., Flux&lt;io.agentscope.core.agent.Event&gt; for agentscope)
     * @throws IllegalStateException if the adapter is not started
     * @throws RuntimeException if the query handler is not set or encounters an error
     */
    Flux<?> streamQuery(AgentRequest request, Object messages);

    /**
     * Stop the adapter and prevent it from accepting new queries.
     *
     * <p>This method should gracefully stop processing new queries while allowing
     * ongoing queries to complete. After this method completes, {@link #streamQuery(AgentRequest, Object)}
     * should not be called.</p>
     */
    void stop();


    /**
     * Check if the adapter is in a healthy/ready state.
     *
     * <p>An adapter is considered healthy if it has been started and is ready
     * to process queries.</p>
     *
     * @return true if the adapter is healthy and ready, false otherwise
     */
    boolean isHealthy();

    /**
     * Get the StreamAdapter implementation for this framework adapter.
     *
     * <p>This method returns a StreamAdapter instance that can convert framework-specific
     * streaming events to runtime Message streams. Not all adapters may support streaming,
     * in which case this method should return null.</p>
     *
     * <p>The StreamAdapter provides:</p>
     * <ul>
     *   <li>Stream conversion via {@link StreamAdapter#adaptFrameworkStream(Object)}</li>
     * </ul>
     * <p>Both methods return Flux&lt;Message&gt; for reactive, non-blocking streaming support.</p>
     *
     * @return the StreamAdapter implementation for this framework, or null if streaming is not supported
     */
    StreamAdapter getStreamAdapter();

    /**
     * Get the MessageAdapter implementation for this framework adapter.
     *
     * <p>This method returns a MessageAdapter instance that can convert between framework-specific
     * message formats and runtime Message objects.</p>
     *
     * <p>The MessageAdapter provides:</p>
     * <ul>
     *   <li>Framework to runtime conversion via {@link MessageAdapter#frameworkMsgToMessage(Object)}</li>
     *   <li>Runtime to framework conversion via {@link MessageAdapter#messageToFrameworkMsg(Object)}</li>
     * </ul>
     *
     * @return the MessageAdapter implementation for this framework
     */
    MessageAdapter getMessageAdapter();
}
