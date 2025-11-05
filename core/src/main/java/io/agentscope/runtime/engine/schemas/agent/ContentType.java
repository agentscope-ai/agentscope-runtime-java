package io.agentscope.runtime.engine.schemas.agent;

/**
 * Content type constant class
 * Corresponds to the ContentType class in agent_schemas.py of the Python version
 */
public class ContentType {
    
    public static final String TEXT = "text";
    public static final String DATA = "data";
    public static final String IMAGE = "image";
    public static final String AUDIO = "audio";
    
    private ContentType() {
        // Utility class, instantiation not allowed
    }
}
