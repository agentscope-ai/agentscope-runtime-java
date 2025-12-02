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

import io.agentscope.runtime.engine.schemas.Event;
import reactor.core.publisher.Flux;

/**
 * StreamAdapter interface provides a unified abstraction for converting streaming
 * events between framework-specific formats and runtime Event streams.
 *
 * <p>This interface is designed to adapt different Agent Framework streaming event
 * types (e.g., AgentScope Event, AutoGen streaming events, etc.) to runtime Event
 * streams. The returned stream can contain both Message and Content objects, as
 * both inherit from Event.</p>
 *
 * <p>This matches the Python implementation where adapt_agentscope_message_stream()
 * has a return type of AsyncIterator[Message] but actually yields both Message
 * and Content objects (both inherit from Event in Python).</p>
 *
 * <p>Implementations should handle:</p>
 * <ul>
 *   <li>Converting framework-specific event streams to runtime Event streams</li>
 *   <li>Handling reactive streaming models using Flux</li>
 *   <li>Preserving streaming state (in-progress, completed) in the converted events</li>
 *   <li>Yielding both Message and Content objects as needed for incremental updates</li>
 * </ul>
 */
public interface StreamAdapter {

    /**
     * Adapts framework-specific event stream to runtime Event stream.
     *
     * <p>This method converts a framework-specific event stream to a Reactor Flux
     * of runtime Event objects (which can be Message or Content). This provides
     * reactive, non-blocking streaming support.</p>
     *
     * @param sourceStream Framework-specific event stream (type depends on framework)
     * @return Flux of runtime Event objects (Message or Content)
     */
    Flux<Event> adaptFrameworkStream(Object sourceStream);

}

