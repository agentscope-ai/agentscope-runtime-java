package io.agentscope.runtime.engine.agents.saa;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.agentscope.runtime.engine.agents.Agent;
import io.agentscope.runtime.engine.agents.AgentCallback;
import io.agentscope.runtime.engine.agents.AgentConfig;
import io.agentscope.runtime.engine.agents.BaseAgent;
import io.agentscope.runtime.engine.memory.model.MessageType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import reactor.core.publisher.Flux;
import io.agentscope.runtime.engine.schemas.context.Context;
import io.agentscope.runtime.engine.schemas.agent.Event;
import io.agentscope.runtime.engine.schemas.agent.Message;
import io.agentscope.runtime.engine.schemas.agent.TextContent;
import io.agentscope.runtime.engine.schemas.agent.RunStatus;

import com.alibaba.cloud.ai.graph.OverAllState;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * AgentScope Agent implementation that proxies Spring AI Alibaba Agent
 */
public class SaaAgent extends BaseAgent {
    Logger logger = Logger.getLogger(SaaAgent.class.getName());
    private com.alibaba.cloud.ai.graph.agent.Agent agentBuilder;
    private Function<Context, Object> contextAdapter;
    private Function<Object, String> responseProcessor;
    private final Function<Object, String> streamResponseProcessor;

    public static class SaaContextAdapter {
        private final Context context;
        private final com.alibaba.cloud.ai.graph.agent.Agent agent;

        private List<org.springframework.ai.chat.messages.Message> memory;
        private org.springframework.ai.chat.messages.Message newMessage;

        public SaaContextAdapter(Context context, com.alibaba.cloud.ai.graph.agent.Agent agent) {
            this.context = context;
            this.agent = agent;
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
//            List<Message> sessionMessages = context.getSession().getMessages();
//            if (!sessionMessages.isEmpty()) {
//                Message lastMessage = sessionMessages.get(sessionMessages.size() - 1);
//                return convertToSpringMessage(lastMessage);
//            }
//            return null;
            return convertToSpringMessage(context.getCurrentMessages().get(0));
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
                if (message.getContent().get(0) instanceof TextContent textContent) {
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

        public com.alibaba.cloud.ai.graph.agent.Agent getAgent() {
            return agent;
        }
    }

    public SaaAgent() {
        super();
        this.contextAdapter = this::defaultContextAdapter;
        this.responseProcessor = this::defaultResponseProcessor;
        this.streamResponseProcessor = this::defaultStreamResponseProcessor;
    }

    public SaaAgent(com.alibaba.cloud.ai.graph.agent.Agent agentBuilder) {
        this(agentBuilder, null, null, null);
    }

    public SaaAgent(com.alibaba.cloud.ai.graph.agent.Agent agentBuilder,
                    Function<Context, Object> contextAdapter,
                    Function<Object, String> responseProcessor,
                    Function<Object, String> streamResponseProcessor) {
        super();
        this.agentBuilder = agentBuilder;
        this.contextAdapter = contextAdapter != null ? contextAdapter : this::defaultContextAdapter;
        this.responseProcessor = responseProcessor != null ? responseProcessor : this::defaultResponseProcessor;
        this.streamResponseProcessor = streamResponseProcessor != null ? streamResponseProcessor : this::defaultStreamResponseProcessor;
    }

    public SaaAgent(List<AgentCallback> beforeCallbacks,
                    List<AgentCallback> afterCallbacks,
                    io.agentscope.runtime.engine.agents.AgentConfig config,
                    com.alibaba.cloud.ai.graph.agent.Agent agentBuilder,
                    Function<Context, Object> contextAdapter,
                    Function<Object, String> responseProcessor,
                    Function<Object, String> streamResponseProcessor) {
        super(beforeCallbacks, afterCallbacks, config);
        this.agentBuilder = agentBuilder;
        this.contextAdapter = contextAdapter != null ? contextAdapter : this::defaultContextAdapter;
        this.responseProcessor = responseProcessor != null ? responseProcessor : this::defaultResponseProcessor;
        this.streamResponseProcessor = streamResponseProcessor != null ? streamResponseProcessor : this::defaultStreamResponseProcessor;
    }

    @Override
    protected Flux<Event> execute(Context context, boolean stream) {
        return Flux.create(sink -> {
            try {
                // Create and initialize context adapter
                SaaContextAdapter adapter = new SaaContextAdapter(context, agentBuilder);
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

                // Invoke Agent with adapted context
                Object output = invokeAgentWithContext(adapter, processedInput, stream);
                StringBuilder contentBuilder = new StringBuilder();

                if (stream && output instanceof Flux<?> outputFlux) {
                    outputFlux.subscribe(part -> {
                        String contentPart = this.streamResponseProcessor.apply(part);
                        TextContent textContent = new TextContent();
                        textContent.setText(contentPart);
                        textContent.setDelta(true);
                        Message deltaMessage = new Message();
                        if (part instanceof StreamingOutput) {
                            deltaMessage.setType(MessageType.CHUNK.name());
                            deltaMessage.setRole("assistant");
                            deltaMessage.setStatus(RunStatus.IN_PROGRESS);
                            deltaMessage.setContent(List.of(textContent));
                            sink.next(deltaMessage);
                        } else {
//                            Todo: Currently skipping everything except chunks
//                            deltaMessage.setType(MessageType.MESSAGE.name());
//                            contentBuilder.append(contentPart);
                        }
                    }, error -> {
                        // Handle error during streaming
                        Message errorMessage = new Message();
                        errorMessage.setType(MessageType.MESSAGE.name());
                        errorMessage.setRole("assistant");
                        errorMessage.setStatus(RunStatus.FAILED);
                        TextContent errorContent = new TextContent();
                        errorContent.setText("Error: " + error.getMessage());
                        errorMessage.setContent(List.of(errorContent));
                        sink.next(errorMessage);
                        sink.error(error);
                    }, () -> {
                        TextContent textContent = new TextContent();
                        textContent.setText(contentBuilder.toString());
                        textContent.setDelta(false);
                        textMessage.setContent(List.of(textContent));

                        textMessage.setStatus(RunStatus.COMPLETED);
                        sink.next(textMessage);
                        sink.complete();
                    });
                } else {
                    // Apply response processor to process output
                    String content = responseProcessor.apply(output);

                    // Create text content with delta=false
                    TextContent textContent = new TextContent();
                    textContent.setText(content);
                    textContent.setDelta(false);
                    textMessage.setContent(List.of(textContent));

                    textMessage.setStatus(RunStatus.COMPLETED);
                    sink.next(textMessage);
                    sink.complete();
                }

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

    private String defaultStreamResponseProcessor(Object output) {
        if (output == null) {
            return "";
        }
        if (output instanceof NodeOutput nodeOutput) {
            String nodeName = nodeOutput.node();
            String content;

            if (output instanceof StreamingOutput streamingOutput) {
//                Todo: Simply return chunk content for now, can adjust return format as needed later
//                return JSON.toJSONString(Map.of(nodeName, streamingOutput.chunk()));
                return streamingOutput.chunk();
            } else {
                JSONObject result = new JSONObject();
                result.put("data", nodeOutput.state().data());
                result.put("node", nodeName);
                content = JSON.toJSONString(result);
                Optional<OverAllState> state = nodeOutput.state().snapShot();
                if (state.isPresent()) {
                    Optional<Object> messages = state.get().value("messages");
                    if (messages.isPresent() && messages.get() instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Object> messagesList = (List<Object>) messages.get();
                        if (!messagesList.isEmpty()) {
                            Object message = messagesList.get(messagesList.size() - 1);
                            if (message instanceof AssistantMessage) {
                                return this.responseProcessor.apply(message);
                            }
                            return "";
                        }
                    }
                }
            }
            return content;
        }

        return "";
    }

    /**
     * Default response processor implementation
     */
    private String defaultResponseProcessor(Object output) {
        if (output == null) {
            return "";
        }

        // If the output is AssistantMessage from Agent
        if (output instanceof AssistantMessage assistantMessage) {
            return assistantMessage.getText();
        }

        // Otherwise convert to string
        return output.toString();
    }

    /**
     * Invoke Agent with context - enhanced version that uses context information
     */
    private Object invokeAgentWithContext(SaaContextAdapter adapter, Object input, boolean stream) {
        if (agentBuilder == null) {
            throw new IllegalStateException("Agent Builder is not set");
        }

        try {
            // Prepare input for Agent using context information
            Map<String, Object> reactInput = new HashMap<>();

//            // Add the new message
//            if (adapter.getNewMessage() != null) {
//                reactInput.put("messages", adapter.getNewMessage());
//            } else {
//                // Fallback to input string
//                reactInput.put("messages", new UserMessage(input.toString()));
//            }
//
//            // Add memory/context if available
//            if (!adapter.getMemory().isEmpty()) {
//                reactInput.put("history", adapter.getMemory());
//            }
//
//            // Add session information from context
//            if (adapter.getContext().getSession() != null) {
//                reactInput.put("session_id", adapter.getContext().getSession().getId());
//                reactInput.put("user_id", adapter.getContext().getSession().getUserId());
//            }
//
//
//            // Use the Agent from adapter (which was built from builder)
            com.alibaba.cloud.ai.graph.agent.Agent agent = adapter.getAgent();

            System.out.println(adapter.getNewMessage());

            RunnableConfig runnableConfig = RunnableConfig.builder()
                    .addMetadata("session_id", adapter.getContext().getSession().getId())
                    .addMetadata("user_id", adapter.getContext().getSession().getUserId())
                    .build();

            if (stream) {
                return agent.stream((UserMessage) adapter.getNewMessage(), runnableConfig);
            } else {
                Optional<OverAllState> state = agent.invoke((UserMessage) adapter.getNewMessage(), runnableConfig);

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
            }

            return new AssistantMessage("No response generated");
        } catch (Exception e) {
            logger.severe("Error invoking Agent: " + e.getMessage());
            throw new RuntimeException("Failed to invoke Agent with context", e);
        }
    }

    private String extractTextContent(Message message) {
        if (message.getContent() != null && !message.getContent().isEmpty()) {
            if (message.getContent().get(0) instanceof TextContent textContent) {
                return textContent.getText() != null ? textContent.getText() : "";
            }
        }
        return "";
    }

    /**
     * Convenience method for direct invocation with Context
     */
    public Flux<Event> runWithContext(Context context) {
        return execute(context, false);
    }

    @Override
    public Agent copy() {
        SaaAgent copy = new SaaAgent(
                this.beforeCallbacks,
                this.afterCallbacks,
                this.config,
                this.agentBuilder,
                this.contextAdapter,
                this.responseProcessor,
                this.streamResponseProcessor
        );
        copy.setKwargs(new HashMap<>(this.kwargs));
        return copy;
    }

    // Builder pattern support
    public static SaaAgentBuilder builder() {
        return new SaaAgentBuilder();
    }

    public static class SaaAgentBuilder {
        private com.alibaba.cloud.ai.graph.agent.Agent agentBuilder;
        private Function<Context, Object> contextAdapter;
        private Function<Object, String> responseProcessor;
        private Function<Object, String> streamResponseProcessor;
        private io.agentscope.runtime.engine.agents.AgentConfig config = new io.agentscope.runtime.engine.agents.AgentConfig();

        public SaaAgentBuilder agentBuilder(com.alibaba.cloud.ai.graph.agent.Agent agentBuilder) {
            this.agentBuilder = agentBuilder;
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

        public SaaAgentBuilder streamResponseProcessor(Function<Object, String> streamResponseProcessor) {
            this.streamResponseProcessor = streamResponseProcessor;
            return this;
        }

        public SaaAgentBuilder config(AgentConfig config) {
            this.config = config;
            return this;
        }

        public SaaAgent build() {
            return new SaaAgent(null, null, config, agentBuilder, contextAdapter, responseProcessor, streamResponseProcessor);
        }
    }

    // Getters and setters
    public com.alibaba.cloud.ai.graph.agent.Agent getAgentBuilder() {
        return agentBuilder;
    }

    public void setAgentBuilder(com.alibaba.cloud.ai.graph.agent.Agent agentBuilder) {
        this.agentBuilder = agentBuilder;
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
}
