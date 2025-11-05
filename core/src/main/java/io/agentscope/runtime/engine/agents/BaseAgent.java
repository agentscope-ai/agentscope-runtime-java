package io.agentscope.runtime.engine.agents;

import reactor.core.publisher.Flux;
import io.agentscope.runtime.engine.schemas.context.Context;
import io.agentscope.runtime.engine.schemas.agent.Event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Agent abstract base class
 */
public abstract class BaseAgent implements Agent {
    
//    protected String name;
//    protected String description;
    protected List<AgentCallback> beforeCallbacks;
    protected List<AgentCallback> afterCallbacks;
    protected AgentConfig config;
    protected Map<String, Object> kwargs;
    
    public BaseAgent() {
        this(null, null, new AgentConfig(), new HashMap<>());
    }
    
    public BaseAgent(AgentConfig config) {
        this(null, null, config, new HashMap<>());
    }
    
    public BaseAgent(List<AgentCallback> beforeCallbacks,
                    List<AgentCallback> afterCallbacks, 
                    AgentConfig config) {
        this(beforeCallbacks, afterCallbacks, config, new HashMap<>());
    }
    
    public BaseAgent(List<AgentCallback> beforeCallbacks,
                    List<AgentCallback> afterCallbacks, 
                    AgentConfig config, Map<String, Object> kwargs) {
        this.beforeCallbacks = beforeCallbacks != null ? beforeCallbacks : new ArrayList<>();
        this.afterCallbacks = afterCallbacks != null ? afterCallbacks : new ArrayList<>();
        this.config = config != null ? config : new AgentConfig();
        this.kwargs = kwargs != null ? kwargs : new HashMap<>();
    }
    
    @Override
    public CompletableFuture<Flux<Event>> runAsync(Context context, boolean stream) {
        return CompletableFuture.supplyAsync(() -> {
            executeBeforeCallbacks(context);
            
            try {
                Flux<Event> eventFlux = execute(context, stream);
                
                executeAfterCallbacks(context);
                
                return eventFlux;
            } catch (Exception e) {
                return Flux.error(e);
            }
        });
    }
    
    protected abstract Flux<Event> execute(Context context, boolean stream);
    
    @Override
    public void setBeforeCallback(AgentCallback callback) {
        if (callback != null) {
            this.beforeCallbacks.add(callback);
        }
    }
    
    @Override
    public void setAfterCallback(AgentCallback callback) {
        if (callback != null) {
            this.afterCallbacks.add(callback);
        }
    }
    
    @Override
    public AgentConfig getConfig() {
        return config;
    }
    
    public Map<String, Object> getKwargs() {
        return kwargs;
    }
    
    public void setKwargs(Map<String, Object> kwargs) {
        this.kwargs = kwargs != null ? kwargs : new HashMap<>();
    }
    
    private void executeBeforeCallbacks(Context context) {
        for (AgentCallback callback : beforeCallbacks) {
            try {
                callback.execute(context);
            } catch (Exception e) {
                // Ignore callback errors
                e.printStackTrace();
            }
        }
    }
    
    private void executeAfterCallbacks(Context context) {
        for (AgentCallback callback : afterCallbacks) {
            try {
                callback.execute(context);
            } catch (Exception e) {
                // Ignore callback errors
                e.printStackTrace();
            }
        }
    }
}
