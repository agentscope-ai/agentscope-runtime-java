package io.agentscope.runtime.engine.agents.saa;

import io.agentscope.runtime.engine.agents.Agent;
import io.agentscope.runtime.engine.agents.AgentCallback;
import io.agentscope.runtime.engine.agents.AgentConfig;
import io.agentscope.runtime.engine.agents.BaseAgent;
import io.agentscope.runtime.engine.memory.model.MessageType;
import io.agentscope.runtime.sandbox.tools.ToolsInit;
import reactor.core.publisher.Flux;
import io.agentscope.runtime.engine.schemas.context.Context;
import io.agentscope.runtime.engine.schemas.agent.Event;
import io.agentscope.runtime.engine.schemas.agent.Message;
import io.agentscope.runtime.engine.schemas.agent.TextContent;
import io.agentscope.runtime.engine.schemas.agent.RunStatus;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.Builder;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Function;

/**
 * AgentScope Agent implementation that proxies Spring AI Alibaba ReactAgent
 */
public class SaaAgent extends BaseAgent {

    private Builder reactAgentBuilder;
    private Function<Context, Object> contextAdapter;
    private Function<Object, String> responseProcessor;
    private List<String> tools;

    public static class SaaContextAdapter {
        private final Context context;
        private final ReactAgent reactAgent;

        private List<org.springframework.ai.chat.messages.Message> memory;
        private org.springframework.ai.chat.messages.Message newMessage;

        public SaaContextAdapter(Context context, ReactAgent reactAgent) {
            this.context = context;
            this.reactAgent = reactAgent;
        }

        public void initialize() {
            this.memory = adaptMemory();
            this.newMessage = adaptNewMessage();
        }

        private List<org.springframework.ai.chat.messages.Message> adaptMemory() {
            List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();

            // Build context from session messages (exclude the last one)
            List<Message> sessionMessages = context.getSession().getMessages();
            for (int i = 0; i < sessionMessages.size() - 1; i++) {
                Message msg = sessionMessages.get(i);
                org.springframework.ai.chat.messages.Message springMessage = convertToSpringMessage(msg);
                if (springMessage != null) {
                    messages.add(springMessage);
                }
            }

            return messages;
        }

        private org.springframework.ai.chat.messages.Message adaptNewMessage() {
            List<Message> sessionMessages = context.getSession().getMessages();
            if (!sessionMessages.isEmpty()) {
                Message lastMessage = sessionMessages.get(sessionMessages.size() - 1);
                return convertToSpringMessage(lastMessage);
            }
            return null;
        }

        private org.springframework.ai.chat.messages.Message convertToSpringMessage(Message message) {
            String content = extractTextContent(message);

            if ("user".equals(message.getRole())) {
                return new UserMessage(content);
            } else if ("assistant".equals(message.getRole())) {
                return new AssistantMessage(content);
            }

            // Default to UserMessage if role is unclear
            return new UserMessage(content);
        }

        private String extractTextContent(Message message) {
            if (message.getContent() != null && !message.getContent().isEmpty()) {
                if (message.getContent().get(0) instanceof TextContent) {
                    TextContent textContent = (TextContent) message.getContent().get(0);
                    return textContent.getText() != null ? textContent.getText() : "";
                }
            }
            return "";
        }

        public List<org.springframework.ai.chat.messages.Message> getMemory() {
            return memory;
        }

        public org.springframework.ai.chat.messages.Message getNewMessage() {
            return newMessage;
        }

        public Context getContext() {
            return context;
        }

        public ReactAgent getReactAgent() {
            return reactAgent;
        }
    }

    public SaaAgent() {
        super();
        this.contextAdapter = this::defaultContextAdapter;
        this.responseProcessor = this::defaultResponseProcessor;
        this.tools = new ArrayList<>();
    }

    public SaaAgent(Builder reactAgentBuilder) {
        this(reactAgentBuilder, null, null, null);
    }

    public SaaAgent(Builder reactAgentBuilder,
                   Function<Context, Object> contextAdapter,
                   Function<Object, String> responseProcessor) {
        this(reactAgentBuilder, contextAdapter, responseProcessor, null);
    }

    public SaaAgent(Builder reactAgentBuilder,
                   Function<Context, Object> contextAdapter,
                   Function<Object, String> responseProcessor,
                   List<String> tools) {
        super();
        this.reactAgentBuilder = reactAgentBuilder;
        this.contextAdapter = contextAdapter != null ? contextAdapter : this::defaultContextAdapter;
        this.responseProcessor = responseProcessor != null ? responseProcessor : this::defaultResponseProcessor;
        this.tools = tools != null ? new ArrayList<>(tools) : new ArrayList<>();
    }

    public SaaAgent(String name, String description,
                   List<AgentCallback> beforeCallbacks,
                   List<AgentCallback> afterCallbacks,
                   io.agentscope.runtime.engine.agents.AgentConfig config,
                   Builder reactAgentBuilder,
                   Function<Context, Object> contextAdapter,
                   Function<Object, String> responseProcessor,
                   List<String> tools) {
        super(name, description, beforeCallbacks, afterCallbacks, config);
        this.reactAgentBuilder = reactAgentBuilder;
        this.contextAdapter = contextAdapter != null ? contextAdapter : this::defaultContextAdapter;
        this.responseProcessor = responseProcessor != null ? responseProcessor : this::defaultResponseProcessor;
        this.tools = tools != null ? new ArrayList<>(tools) : new ArrayList<>();
    }

    @Override
    protected Flux<Event> execute(Context context) {
        return Flux.create(sink -> {
            try {

                // Build ReactAgent from Builder with specified tools
                ReactAgent reactAgent = reactAgentBuilder.tools(ToolsInit.getToolsByName(tools)).build();

                // Create and initialize context adapter
                SaaContextAdapter adapter = new SaaContextAdapter(context, reactAgent);
                adapter.initialize();

                // Create initial response message
                Message textMessage = new Message();
                textMessage.setType(MessageType.MESSAGE.name());
                textMessage.setRole("assistant");
                textMessage.setStatus(RunStatus.IN_PROGRESS);

                // Emit the initial message as an event
                sink.next(textMessage);

                // Apply context adapter to process input
                Object processedInput = contextAdapter.apply(context);

                // Invoke ReactAgent with adapted context
                Object output = invokeReactAgentWithContext(adapter, processedInput);

                // Apply response processor to process output
                String content = responseProcessor.apply(output);

                // Create text content with delta=false
                TextContent textContent = new TextContent();
                textContent.setText(content);
                textContent.setDelta(false);
                textMessage.setContent(List.of(textContent));

                // Mark message as completed and emit final event
                textMessage.setStatus(RunStatus.COMPLETED);
                sink.next(textMessage);
                sink.complete();

            } catch (Exception e) {
                // Create error message
                Message errorMessage = new Message();
                errorMessage.setType(MessageType.MESSAGE.name());
                errorMessage.setRole("assistant");
                errorMessage.setStatus(RunStatus.FAILED);
                TextContent errorContent = new TextContent();
                errorContent.setText("Error: " + e.getMessage());
                errorMessage.setContent(List.of(errorContent));
                sink.next(errorMessage);
                sink.error(e);
            }
        });
    }

    /**
     * Enhanced context adapter that fully leverages Context information
     */
    private Object enhancedContextAdapter(Context context) {
        // Extract comprehensive context information
        Map<String, Object> contextData = new HashMap<>();

        if (context.getSession() != null) {
            contextData.put("session_id", context.getSession().getId());
            contextData.put("user_id", context.getSession().getUserId());

            // Add message history
            List<Message> messages = context.getSession().getMessages();
            if (!messages.isEmpty()) {
                contextData.put("current_message", messages.get(messages.size() - 1));
                if (messages.size() > 1) {
                    contextData.put("message_history", messages.subList(0, messages.size() - 1));
                }
            }
        }

        // Add environment and tools information if available
        if (context.getEnvironmentManager() != null) {
            contextData.put("environment_manager", context.getEnvironmentManager());
        }

        if (context.getActivateTools() != null && !context.getActivateTools().isEmpty()) {
            contextData.put("activated_tools", context.getActivateTools());
        }

        return contextData;
    }

    /**
     * Default context adapter implementation - processes Context to extract input
     */
    private Object defaultContextAdapter(Context context) {
        List<Message> sessionMessages = context.getSession().getMessages();
        if (!sessionMessages.isEmpty()) {
            // Get the last message content as input
            Message lastMessage = sessionMessages.get(sessionMessages.size() - 1);
            return extractTextContent(lastMessage);
        }
        return extractTextContent(context.getCurrentMessages().get(0));
    }

    /**
     * Default response processor implementation
     */
    private String defaultResponseProcessor(Object output) {
        if (output == null) {
            return "";
        }

        // If the output is AssistantMessage from ReactAgent
        if (output instanceof AssistantMessage) {
            AssistantMessage assistantMessage = (AssistantMessage) output;
            return assistantMessage.getText();
        }

        // Otherwise convert to string
        return output.toString();
    }

    /**
     * Invoke ReactAgent with context - enhanced version that uses context information
     */
    private Object invokeReactAgentWithContext(SaaContextAdapter adapter, Object input) {
        if (reactAgentBuilder == null) {
            throw new IllegalStateException("ReactAgent Builder is not set");
        }

        try {
            // Prepare input for ReactAgent using context information
            Map<String, Object> reactInput = new HashMap<>();

            // Add the new message
            if (adapter.getNewMessage() != null) {
                reactInput.put("messages", adapter.getNewMessage());
            } else {
                // Fallback to input string
                reactInput.put("messages", new UserMessage(input.toString()));
            }

            // Add memory/context if available
            if (!adapter.getMemory().isEmpty()) {
                reactInput.put("history", adapter.getMemory());
            }

            // Add session information from context
            if (adapter.getContext().getSession() != null) {
                reactInput.put("session_id", adapter.getContext().getSession().getId());
                reactInput.put("user_id", adapter.getContext().getSession().getUserId());
            }

            // Use the ReactAgent from adapter (which was built from builder)
            ReactAgent reactAgent = adapter.getReactAgent();
            Optional<OverAllState> state = reactAgent.invoke(reactInput);

            if (state.isPresent()) {
                Optional<Object> messages = state.get().value("messages");
                if (messages.isPresent() && messages.get() instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> messagesList = (List<Object>) messages.get();
                    if (!messagesList.isEmpty()) {
                        return messagesList.get(messagesList.size() - 1);
                    }
                }
            }

            return new AssistantMessage("No response generated");
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke ReactAgent with context", e);
        }
    }

    private String extractTextContent(Message message) {
        if (message.getContent() != null && !message.getContent().isEmpty()) {
            if (message.getContent().get(0) instanceof TextContent) {
                TextContent textContent = (TextContent) message.getContent().get(0);
                return textContent.getText() != null ? textContent.getText() : "";
            }
        }
        return "";
    }

    /**
     * Convenience method for direct invocation with Context
     */
    public Flux<Event> runWithContext(Context context) {
        return execute(context);
    }

    @Override
    public Agent copy() {
        SaaAgent copy = new SaaAgent(
            this.name,
            this.description,
            this.beforeCallbacks,
            this.afterCallbacks,
            this.config,
            this.reactAgentBuilder,
            this.contextAdapter,
            this.responseProcessor,
            this.tools
        );
        copy.setKwargs(new HashMap<>(this.kwargs));
        return copy;
    }

    // Builder pattern support
    public static SaaAgentBuilder builder() {
        return new SaaAgentBuilder();
    }

    public static class SaaAgentBuilder {
        private String name;
        private String description;
        private com.alibaba.cloud.ai.graph.agent.Builder reactAgentBuilder;
        private Function<Context, Object> contextAdapter;
        private Function<Object, String> responseProcessor;
        private List<String> tools;
        private io.agentscope.runtime.engine.agents.AgentConfig config = new io.agentscope.runtime.engine.agents.AgentConfig();

        public SaaAgentBuilder name(String name) {
            this.name = name;
            return this;
        }

        public SaaAgentBuilder description(String description) {
            this.description = description;
            return this;
        }

        public SaaAgentBuilder reactAgentBuilder(com.alibaba.cloud.ai.graph.agent.Builder reactAgentBuilder) {
            this.reactAgentBuilder = reactAgentBuilder;
            return this;
        }

        public SaaAgentBuilder contextAdapter(Function<Context, Object> contextAdapter) {
            this.contextAdapter = contextAdapter;
            return this;
        }

        public SaaAgentBuilder responseProcessor(Function<Object, String> responseProcessor) {
            this.responseProcessor = responseProcessor;
            return this;
        }

        public SaaAgentBuilder tools(List<String> tools) {
            this.tools = tools;
            return this;
        }

        public SaaAgentBuilder config(AgentConfig config) {
            this.config = config;
            return this;
        }

        public SaaAgent build() {
            return new SaaAgent(name, description, null, null, config, reactAgentBuilder, contextAdapter, responseProcessor, tools);
        }
    }

    // Getters and setters
    public com.alibaba.cloud.ai.graph.agent.Builder getReactAgentBuilder() {
        return reactAgentBuilder;
    }

    public void setReactAgentBuilder(com.alibaba.cloud.ai.graph.agent.Builder reactAgentBuilder) {
        this.reactAgentBuilder = reactAgentBuilder;
    }

    public Function<Context, Object> getContextAdapter() {
        return contextAdapter;
    }

    public void setContextAdapter(Function<Context, Object> contextAdapter) {
        this.contextAdapter = contextAdapter;
    }

    public Function<Object, String> getResponseProcessor() {
        return responseProcessor;
    }

    public void setResponseProcessor(Function<Object, String> responseProcessor) {
        this.responseProcessor = responseProcessor;
    }

    public List<String> getTools() {
        return tools;
    }

    public void setTools(List<String> tools) {
        this.tools = tools;
    }
}
