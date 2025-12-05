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

package io.agentscope.runtime.engine;

import io.agentscope.runtime.adapters.AgentHandler;
import io.agentscope.runtime.adapters.MessageAdapter;
import io.agentscope.runtime.adapters.StreamAdapter;
import io.agentscope.runtime.engine.schemas.AgentRequest;
import io.agentscope.runtime.engine.schemas.AgentResponse;
import io.agentscope.runtime.engine.schemas.Error;
import io.agentscope.runtime.engine.schemas.Event;
import io.agentscope.runtime.engine.schemas.Message;
import io.agentscope.runtime.engine.schemas.RunStatus;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Runner class that proxies calls to AgentAdapter.
 * 
 * It acts as a proxy to AgentAdapter, managing the lifecycle and query processing.</p>
 * 
 * <p>Key responsibilities:</p>
 * <ul>
 *   <li>Delegate lifecycle methods (init, start, stop, shutdown) to AgentAdapter</li>
 *   <li>Process stream queries by delegating to AgentAdapter</li>
 *   <li>Manage health status</li>
 *   <li>Handle sequence numbers for events</li>
 * </ul>
 */
public class Runner {
    private static final Logger logger = LoggerFactory.getLogger(Runner.class);
    
    private final AgentHandler adapter;
    private volatile boolean health = false;
    private final AtomicInteger sequenceGenerator = new AtomicInteger(0);
    
    /**
     * Constructor with AgentAdapter.
     * 
     * @param adapter the AgentAdapter instance to proxy
     */
    public Runner(AgentHandler adapter) {
        if (adapter == null) {
            throw new IllegalArgumentException("AgentAdapter cannot be null");
        }
        this.adapter = adapter;
    }
    
    /**
     * Initialize the runner by calling the adapter's init method.
     * 
     * @return a CompletableFuture that completes when initialization is done
     */
    public void init() {

    }
    
    /**
     * Start the runner by calling the adapter's start method.
     * 
     * <p>After this method completes, the runner is ready to process queries.</p>
     * 
     * @return a CompletableFuture that completes when the runner is started
     */
    public void start() {
        adapter.start();
        this.health = true;
        logger.info("[Runner] Runner started successfully");
    }
    
    /**
     * Stop the runner by calling the adapter's stop method.
     */
    public void stop() {
        adapter.stop();
        this.health = false;
        logger.info("[Runner] Runner stopped");
    }
    
    /**
     * Shutdown the runner by calling the adapter's shutdown method.
     *
     * @return a CompletableFuture that completes when shutdown is done
     */
    public void shutdown() {
        this.health = false;
        logger.info("[Runner] Runner shutdown completed");
    }
    
    /**
     * Stream query by delegating to the adapter.
     * 
     * <p>This method processes the agent request and returns a reactive stream of events with sequence numbers.</p>
     * 
     * <p>The implementation follows the following logic:</p>
     * <ol>
     *   <li>Check framework type is allowed</li>
     *   <li>Check runner health status</li>
     *   <li>Create initial response and yield it</li>
     *   <li>Set in-progress status and yield it</li>
     *   <li>Assign session ID and user ID</li>
     *   <li>Delegate to adapter's streamQuery (which handles query_handler and stream_adapter)</li>
     *   <li>Collect completed messages into response</li>
     *   <li>Handle errors</li>
     *   <li>Extract token usage</li>
     *   <li>Yield completed response</li>
     * </ol>
     * 
     * @param request the agent request
     * @return a Flux of Event objects with sequence numbers
     * @throws IllegalStateException if the runner is not healthy or framework type is invalid
     * @throws RuntimeException if framework type is invalid or not set
     */
    public Flux<Event> streamQuery(AgentRequest request) {
        String frameworkType = adapter.getFrameworkType();

        if (!health) {
            throw new IllegalStateException(
                "Runner has not been started. Please call 'runner.start()' or use 'async with Runner()' before calling 'streamQuery'."
            );
        }
        
        // Ensure session ID and user ID are set 
        if (request.getSessionId() == null || request.getSessionId().isEmpty()) {
            request.setSessionId(UUID.randomUUID().toString());
        }
        if (request.getUserId() == null || request.getUserId().isEmpty()) {
            request.setUserId(request.getSessionId());
        }
        
        // Create initial response 
        AgentResponse response = new AgentResponse(request.getId());
        response.setSessionId(request.getSessionId());
        
        // Initial response with created status 
        Flux<Event> initialResponse = Flux.just(
            withSequenceNumber(response.created())
        );
        
        // Set to in-progress status 
        Flux<Event> inProgressResponse = Flux.just(
            withSequenceNumber(response.inProgress())
        );
        
        // Track if an error occurred
        AtomicBoolean hasError = new AtomicBoolean(false);
        
        // Get MessageAdapter and StreamAdapter from adapter
        // Adapters are selected based on framework_type
        MessageAdapter messageAdapter = adapter.getMessageAdapter();
        StreamAdapter streamAdapter = adapter.getStreamAdapter();
        
        if (streamAdapter == null) {
            throw new RuntimeException(
                String.format("StreamAdapter is not available for framework type '%s'", frameworkType)
            );
        }
        
        // Convert request messages using MessageAdapter 
        Object frameworkMessages = messageAdapter.messageToFrameworkMsg(request.getInput());
        
        // Call adapter.streamQuery() to get raw framework stream
        // Note: adapter.streamQuery() returns raw framework stream, not converted Event stream
        // The adapter should internally use the converted framework messages (messages parameter)
        Flux<Object> rawFrameworkStream = adapter.streamQuery(request, frameworkMessages)
            .cast(Object.class); // Cast to Object to handle different framework stream types
        
        // Apply StreamAdapter to convert framework stream to runtime Event stream
        // Note: StreamAdapter returns Flux<Event> which can contain both Message and Content
        Flux<Event> eventStream = streamAdapter.adaptFrameworkStream(rawFrameworkStream);
        
        // Add sequence numbers to events
        Flux<Event> adapterStream = eventStream
            .map(this::withSequenceNumber)
            .doOnNext(event -> {
                // Collect completed messages into response
                if (event instanceof Message message) {
                    if (RunStatus.COMPLETED.equals(message.getStatus())
                        && "message".equals(message.getObject())) {
                        response.addNewMessage(message);
                    }
                }
            })
            .onErrorResume(throwable -> {
                hasError.set(true);
                logger.error("Error happens in `query_handler`: {}", throwable.getMessage(), throwable);
                Error error = new Error();
                error.setCode("500");
                error.setMessage("Error happens in `query_handler`: " + throwable.getMessage());
                // Return only the failed response
                return Flux.just(withSequenceNumber(response.failed(error)));
            });

        // Combine initial events with adapter stream, then complete with final response
        // This matches flow: initial -> in_progress -> stream -> completed
        return Flux.concat(
            initialResponse,
            inProgressResponse,
            adapterStream
        ).concatWith(
            // Only execute if stream completed successfully (no error)
            // This matches the logic: if error occurred, return immediately (no further processing)
            Flux.defer(() -> {
                // If error occurred, don't process further
                if (hasError.get() || RunStatus.FAILED.equals(response.getStatus())) {
                    return Flux.empty();
                }

                // Extract token usage from last message
                try {
                    if (response.getOutput() != null && !response.getOutput().isEmpty()) {
                        Message lastMessage = response.getOutput().get(response.getOutput().size() - 1);
                        if (lastMessage.getUsage() != null) {
                            response.setUsage(lastMessage.getUsage());
                        }
                    }
                } catch (IndexOutOfBoundsException e) {
                    // Avoid empty message error
                    logger.debug("Could not extract usage: {}", e.getMessage());
                }

                // Yield completed response
                return Flux.just(withSequenceNumber(response.completed()));
            })
        );
    }
    
    /**
     * Add sequence number to an event.
     * 
     * @param event the event to add sequence number to
     * @return the event with sequence number set
     */
    private Event withSequenceNumber(Event event) {
        event.setSequenceNumber(sequenceGenerator.incrementAndGet());
        return event;
    }
    
    /**
     * Check if the runner is healthy.
     * 
     * @return true if healthy, false otherwise
     */
    public boolean isHealthy() {
        return health && adapter.isHealthy();
    }
    
    /**
     * Get the underlying adapter.
     * 
     * @return the AgentAdapter instance
     */
    public AgentHandler getAgent() {
        return adapter;
    }

    /** FIXME
     */
    public SandboxManager getSandboxManager() {
        return null;
    }
    
}

