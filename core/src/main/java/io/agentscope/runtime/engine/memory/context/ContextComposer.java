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
package io.agentscope.runtime.engine.memory.context;

import io.agentscope.runtime.engine.memory.model.Message;
import io.agentscope.runtime.engine.schemas.context.Session;
import io.agentscope.runtime.engine.memory.service.MemoryService;
import io.agentscope.runtime.engine.memory.service.SessionHistoryService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Context composer
 * Responsible for composing session history and memory information
 */
public class ContextComposer {
    
    /**
     * Compose context information
     *
     * @param requestInput current input messages
     * @param session session object
     * @param memoryService optional memory service
     * @param sessionHistoryService optional session history service
     * @return CompletableFuture<Void> asynchronous composition result
     */
    public static CompletableFuture<Void> compose(
            List<Message> requestInput,
            Session session,
            Optional<MemoryService> memoryService,
            Optional<SessionHistoryService> sessionHistoryService) {
        
        return CompletableFuture.runAsync(() -> {
            // Process session history
            if (sessionHistoryService.isPresent()) {
                try {
                    sessionHistoryService.get().appendMessage(session, requestInput).get();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to append message to session history", e);
                }
            } else {
                session.getMessages().addAll(requestInput);
            }
            
            // Process memory
            if (memoryService.isPresent()) {
                try {
                    // Search relevant memories
                    List<Message> memories = memoryService.get()
                            .searchMemory(session.getUserId(), requestInput, Optional.of(Map.of("top_k", 5)))
                            .get();
                    
                    // Add current messages to memory
                    memoryService.get()
                            .addMemory(session.getUserId(), requestInput, Optional.of(session.getId()))
                            .get();
                    
                    // Add memories to the beginning of session messages
                    session.getMessages().addAll(0, memories);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to process memory", e);
                }
            }
        });
    }
}
