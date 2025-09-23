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
     * Get Agent name
     */
    String getName();
    
    /**
     * Get Agent description
     */
    String getDescription();
    
    /**
     * Execute Agent asynchronously
     * Corresponds to the run_async method of the Python version
     *
     * @param context execution context
     * @return event stream
     */
    CompletableFuture<Flux<Event>> runAsync(Context context);
    
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
