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

package io.agentscope.runtime.engine.services.agent_state;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.agentscope.runtime.engine.services.ServiceWithLifecycleManager;

/**
 * Abstract base class for agent state management services.
 * 
 * <p>Stores and manages agent states organized by user_id, session_id,
 * and round_id. Supports saving, retrieving, listing, and deleting states.
 * 
 * <p><b>Usage Example:</b>
 * <pre>{@code
 * StateService stateService = new InMemoryStateService();
 * stateService.start().join();
 * 
 * // Save state
 * int roundId = stateService.saveState("user_123", stateMap, "session_456", null).join();
 * 
 * // Export state
 * Map<String, Object> state = stateService.exportState("user_123", "session_456", null).join();
 * }</pre>
 */
public abstract class StateService extends ServiceWithLifecycleManager {
    
    /**
     * Save serialized state data for a specific user/session.
     * 
     * <p>If round_id is provided, store the state in that round.
     * If round_id is null, append as a new round with automatically
     * assigned round_id.
     * 
     * @param userId The unique ID of the user
     * @param state A map representing serialized agent state
     * @param sessionId Optional session/conversation ID. Defaults to "default"
     * @param roundId Optional conversation round number
     * @return A CompletableFuture that completes with the round_id in which the state was saved
     */
    public abstract CompletableFuture<Integer> saveState(
        String userId,
        Map<String, Object> state,
        String sessionId,
        Integer roundId
    );
    
    /**
     * Retrieve serialized state data for a user/session.
     * 
     * <p>If round_id is provided, return that round's state.
     * If round_id is null, return the latest round's state.
     * 
     * @param userId The unique ID of the user
     * @param sessionId Optional session/conversation ID
     * @param roundId Optional round number
     * @return A CompletableFuture that completes with a map representing the agent state,
     *         or null if not found
     */
    public abstract CompletableFuture<Map<String, Object>> exportState(
        String userId,
        String sessionId,
        Integer roundId
    );
}

