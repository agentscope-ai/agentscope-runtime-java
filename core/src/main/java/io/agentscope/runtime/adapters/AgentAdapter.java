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
package io.agentscope.runtime.adapters;

import io.agentscope.runtime.engine.schemas.AgentRequest;
import io.agentscope.runtime.engine.schemas.Event;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;

/**
 * AgentAdapter interface provides a unified abstraction for AgentApp core capabilities.
 *
 * <p>This interface is designed to adapt different Agent Framework Types (e.g., agentscope, 
 * autogen, agno, langgraph) and provides the core lifecycle management and query processing 
 * capabilities similar to Python's AgentApp.</p>
 *
 * <p>The interface follows the pattern from Python's AgentApp where:</p>
 * <ul>
 *   <li>{@link #init()} corresponds to the @app.init() decorator</li>
 *   <li>{@link #streamQuery(AgentRequest, Object)} corresponds to the @app.query() decorator</li>
 *   <li>{@link #shutdown()} corresponds to the @app.shutdown() decorator</li>
 * </ul>
 *
 * <p>Lifecycle flow:</p>
 * <ol>
 *   <li>{@link #init()} - Initialize the adapter and prepare resources</li>
 *   <li>{@link #start()} - Start the adapter and mark it as ready</li>
 *   <li>{@link #streamQuery(AgentRequest, Object)} - Process queries (can be called multiple times)</li>
 *   <li>{@link #stop()} - Stop accepting new queries</li>
 *   <li>{@link #shutdown()} - Clean up resources</li>
 * </ol>
 *
 */
public interface AgentAdapter {

    /**
     * Get the framework type this adapter supports.
     *
     * <p>Supported framework types: "agentscope", "autogen", "agno", "langgraph"</p>
     *
     * @return the framework type string
     */
    String getFrameworkType();

    /**
     * Initialize the adapter and prepare resources.
     *
     * <p>This method corresponds to the @app.init() decorator in Python's AgentApp.
     * It should be called before {@link #start()} to set up any required resources,
     * services, or dependencies.</p>
     *
     * <p>This method can be synchronous or asynchronous. If it returns a CompletableFuture,
     * the caller should wait for it to complete before calling {@link #start()}.</p>
     *
     * @return a CompletableFuture that completes when initialization is done.
     *         If initialization is synchronous, return CompletableFuture.completedFuture(null)
     */
    CompletableFuture<Void> init();

    /**
     * Start the adapter and mark it as ready to process queries.
     *
     * <p>This method should be called after {@link #init()} completes successfully.
     * After this method completes, the adapter should be ready to accept queries
     * via {@link #streamQuery(AgentRequest, Object)}.</p>
     *
     * <p>This method typically calls the init_handler if one was registered,
     * similar to Python's Runner.start() method.</p>
     *
     * @return a CompletableFuture that completes when the adapter is started
     */
    CompletableFuture<Void> start();

    /**
     * Process an agent query and return a stream of events.
     *
     * <p>This method corresponds to the @app.query() decorator in Python's AgentApp.
     * It processes the agent request and returns a reactive stream of events.</p>
     *
     * <p>The implementation should:</p>
     * <ul>
     *   <li>Handle the framework-specific message format conversion</li>
     *   <li>Invoke the registered query handler</li>
     *   <li>Adapt the result to a stream of Event objects</li>
     * </ul>
     *
     * <p>This method can be called multiple times concurrently. The adapter should
     * be in a started state (after {@link #start()} completes) before this method
     * is called.</p>
     *
     * <p>The messages parameter contains framework-specific converted messages
     * (e.g., List&lt;Msg&gt; for agentscope). This matches Python's behavior where
     * converted messages are passed via kwargs: kwargs.update({"msgs": message_to_agentscope_msg(request.input)}).
     * If messages is null, the adapter should use request.getInput() directly.</p>
     *
     * @param request the agent request containing input messages, session info, etc.
     * @param messages the converted framework-specific messages (e.g., List&lt;Msg&gt; for agentscope),
     *                 or null if no conversion is needed
     * @return a Flux of Event objects representing the streaming response
     * @throws IllegalStateException if the adapter is not started
     * @throws RuntimeException if the query handler is not set or encounters an error
     */
    Flux<Event> streamQuery(AgentRequest request, Object messages);

    /**
     * Stop the adapter and prevent it from accepting new queries.
     *
     * <p>This method should gracefully stop processing new queries while allowing
     * ongoing queries to complete. After this method completes, {@link #streamQuery(AgentRequest, Object)}
     * should not be called.</p>
     *
     * @return a CompletableFuture that completes when the adapter is stopped
     */
    CompletableFuture<Void> stop();

    /**
     * Shutdown the adapter and clean up resources.
     *
     * <p>This method corresponds to the @app.shutdown() decorator in Python's AgentApp.
     * It should be called after {@link #stop()} to perform final cleanup, such as:</p>
     * <ul>
     *   <li>Closing connections</li>
     *   <li>Releasing resources</li>
     *   <li>Stopping background services</li>
     * </ul>
     *
     * <p>This method typically calls the shutdown_handler if one was registered,
     * similar to Python's Runner.stop() method.</p>
     *
     * @return a CompletableFuture that completes when shutdown is done
     */
    CompletableFuture<Void> shutdown();

    /**
     * Check if the adapter is in a healthy/ready state.
     *
     * <p>An adapter is considered healthy if it has been started and is ready
     * to process queries. This is similar to Python's Runner._health flag.</p>
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
     * message formats and runtime Message objects. This corresponds to Python version's message
     * adapter functions (e.g., message_to_agentscope_msg for agentscope).</p>
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
