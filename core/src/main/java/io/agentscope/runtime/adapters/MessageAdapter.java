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

import io.agentscope.runtime.engine.schemas.Message;

import java.util.List;

/**
 * MessageAdapter interface provides a unified abstraction for converting messages
 * between framework-specific message formats and runtime Message objects.
 *
 * <p>This interface is designed to adapt different Agent Framework message types
 * (e.g., AgentScope Msg, AutoGen Message, etc.) to and from the runtime's
 * standardized Message format.</p>
 *
 * <p>Implementations should handle:</p>
 * <ul>
 *   <li>Converting framework-specific messages to runtime Message objects</li>
 *   <li>Converting runtime Message objects back to framework-specific format</li>
 *   <li>Handling both single messages and lists of messages</li>
 * </ul>
 */
public interface MessageAdapter {

    /**
     * Convert framework-specific message(s) into one or more runtime Message objects.
     *
     * <p>This method accepts either a single framework message or a list of messages,
     * and returns a list of runtime Message objects. The conversion should preserve
     * all relevant metadata and content from the original messages.</p>
     *
     * @param frameworkMsg Framework-specific message object or list of message objects
     * @return List of runtime Message objects
     * @throws IllegalArgumentException if the input is not of the expected type
     */
    List<Message> frameworkMsgToMessage(Object frameworkMsg);

    /**
     * Convert runtime Message(s) to framework-specific message format.
     *
     * <p>This method accepts either a single runtime Message or a list of Messages,
     * and returns the corresponding framework-specific message(s). The return type
     * is Object to allow implementations to return either a single message or a list,
     * depending on the input and framework requirements.</p>
     *
     * @param messages Runtime Message(s) - single Message or List<Message>
     * @return Framework-specific message object(s) - single message or List of messages
     * @throws IllegalArgumentException if the input is not of the expected type
     */
    Object messageToFrameworkMsg(Object messages);
}

