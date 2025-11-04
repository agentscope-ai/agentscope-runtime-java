package io.agentscope.runtime.engine.schemas.context;

import io.agentscope.runtime.engine.agents.Agent;
import io.agentscope.runtime.engine.schemas.agent.AgentRequest;
import io.agentscope.runtime.engine.schemas.agent.Message;
import io.agentscope.runtime.engine.service.EnvironmentManager;
import io.agentscope.runtime.engine.memory.context.ContextManager;

import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.Map;

public class Context {
    
    private String userId;
    private Session session;
    private List<ToolCallback> activateTools;
    private Message newMessage;
    private List<Message> currentMessages;
    private AgentRequest request;
    private Map<String, Object> newMessageDict;
    private List<Map<String, Object>> messagesList;
    
    private EnvironmentManager environmentManager;
    private ContextManager contextManager;
    
    private Agent agent;
    private Map<String, Object> agentConfig;
    
    public Context() {
        this.session = new Session();
        this.activateTools = List.of();
        this.currentMessages = List.of();
        this.messagesList = List.of();
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public Session getSession() {
        return session;
    }
    
    public void setSession(Session session) {
        this.session = session;
    }
    
    public List<ToolCallback> getActivateTools() {
        return activateTools;
    }
    
    public void setActivateTools(List<ToolCallback> activateTools) {
        this.activateTools = activateTools;
    }
    
    public Message getNewMessage() {
        return newMessage;
    }
    
    public void setNewMessage(Message newMessage) {
        this.newMessage = newMessage;
    }
    
    public List<Message> getCurrentMessages() {
        return currentMessages;
    }
    
    public void setCurrentMessages(List<Message> currentMessages) {
        this.currentMessages = currentMessages;
    }
    
    public AgentRequest getRequest() {
        return request;
    }
    
    public void setRequest(AgentRequest request) {
        this.request = request;
    }
    
    public Map<String, Object> getNewMessageDict() {
        return newMessageDict;
    }
    
    public void setNewMessageDict(Map<String, Object> newMessageDict) {
        this.newMessageDict = newMessageDict;
    }
    
    public List<Map<String, Object>> getMessagesList() {
        return messagesList;
    }
    
    public void setMessagesList(List<Map<String, Object>> messagesList) {
        this.messagesList = messagesList;
    }
    
    public EnvironmentManager getEnvironmentManager() {
        return environmentManager;
    }
    
    public void setEnvironmentManager(EnvironmentManager environmentManager) {
        this.environmentManager = environmentManager;
    }
    
    public ContextManager getContextManager() {
        return contextManager;
    }
    
    public void setContextManager(ContextManager contextManager) {
        this.contextManager = contextManager;
    }
    
    public Agent getAgent() {
        return agent;
    }
    
    public void setAgent(Agent agent) {
        this.agent = agent;
    }
    
    public Map<String, Object> getAgentConfig() {
        return agentConfig;
    }
    
    public void setAgentConfig(Map<String, Object> agentConfig) {
        this.agentConfig = agentConfig;
    }
    
    public List<Map<String, Object>> getMessages() {
        if (newMessageDict != null) {
            List<Map<String, Object>> result = List.copyOf(messagesList);
            result.add(newMessageDict);
            return result;
        } else {
            return messagesList;
        }
    }
}
