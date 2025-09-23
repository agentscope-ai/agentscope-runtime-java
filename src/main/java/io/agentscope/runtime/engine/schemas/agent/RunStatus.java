package io.agentscope.runtime.engine.schemas.agent;

/**
 * Run status enumeration
 * Corresponds to the RunStatus class in agent_schemas.py of the Python version
 */
public class RunStatus {
    
    public static final String CREATED = "created";
    public static final String IN_PROGRESS = "in_progress";
    public static final String COMPLETED = "completed";
    public static final String CANCELED = "canceled";
    public static final String FAILED = "failed";
    public static final String REJECTED = "rejected";
    public static final String UNKNOWN = "unknown";
    
    private RunStatus() {
        // Utility class, instantiation not allowed
    }
}
