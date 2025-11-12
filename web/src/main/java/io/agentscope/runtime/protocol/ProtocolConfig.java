package io.agentscope.runtime.protocol;

/**
 * Protocol configuration for each type {@link Protocol}.
 *
 * @author xiweng.yy
 */
public interface ProtocolConfig {
    
    Protocol type();
    
    default String name() {
        return type().name().toLowerCase() + ProtocolConfig.class.getSimpleName();
    }
}
