package io.agentscope.runtime.engine.agents;

import io.agentscope.runtime.engine.schemas.context.Context;

/**
 * Agent callback interface
 * Corresponds to before_agent_callback and after_agent_callback in the Python version
 */
@FunctionalInterface
public interface AgentCallback {
    
    /**
     * Execute callback
     *
     * @param context execution context
     */
    void execute(Context context);
}
