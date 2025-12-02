package io.agentscope.runtime.engine.schemas;

/**
 * Run status constants for agent events.
 */
public class RunStatus {
    public static final String CREATED = "created";
    public static final String IN_PROGRESS = "in_progress";
    public static final String COMPLETED = "completed";
    public static final String CANCELED = "canceled";
    public static final String FAILED = "failed";
    public static final String REJECTED = "rejected";
    public static final String UNKNOWN = "unknown";
    public static final String QUEUED = "queued";
    public static final String INCOMPLETE = "incomplete";
    
    private RunStatus() {
        // Utility class
    }
}

