package io.agentscope.runtime.engine.agents.agentscope;

import io.agentscope.core.ReActAgent.Builder;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.hook.ChunkMode;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.message.*;
import io.agentscope.core.tool.AgentTool;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.runtime.engine.agents.AgentCallback;
import io.agentscope.runtime.engine.agents.AgentConfig;
import io.agentscope.runtime.engine.agents.BaseAgent;
import io.agentscope.runtime.engine.agents.agentscope.tools.AgentScopeSandboxAwareTool;
import io.agentscope.runtime.engine.memory.model.MessageType;
import io.agentscope.runtime.engine.schemas.agent.Event;
import io.agentscope.runtime.engine.schemas.agent.Message;
import io.agentscope.runtime.engine.schemas.agent.RunStatus;
import io.agentscope.runtime.engine.schemas.agent.TextContent;
import io.agentscope.runtime.engine.schemas.context.Context;
import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.tools.SandboxTool;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * AgentScope Agent implementation that proxies native AgentScope Agent
 * This class wraps io.agentscope.core.agent.Agent to integrate with the runtime engine
 */
public class AgentScopeAgent extends BaseAgent {
    private static final Logger logger = Logger.getLogger(AgentScopeAgent.class.getName());

    private Builder agentScopeBuilder;
    private Function<Context, Msg> contextAdapter;
    private Function<Msg, String> responseProcessor;
    private Function<Msg, String> streamResponseProcessor;

    /**
     * Context adapter that converts runtime engine Context to AgentScope Msg
     */
    public static class AgentScopeContextAdapter {
        private final Context context;
        private final Builder agentScopeBuilder;
        private Msg newMessage;
        private List<Msg> memory;

        public AgentScopeContextAdapter(Context context, Builder agentScopeBuilder) {
            this.context = context;
            this.agentScopeBuilder = agentScopeBuilder;
        }

        public void initialize() {
            this.memory = adaptMemory();
            this.newMessage = adaptNewMessage();
            setupTools();
        }

        /**
         * Convert session messages to AgentScope Msg list (exclude the last one as memory)
         */
        private List<Msg> adaptMemory() {
            List<Msg> messages = new ArrayList<>();
            List<Message> sessionMessages = context.getSession().getMessages();

            for (int i = 0; i < sessionMessages.size() - 1; i++) {
                Message msg = sessionMessages.get(i);
                Msg agentscopeMsg = convertToAgentScopeMsg(msg);
                if (agentscopeMsg != null) {
                    messages.add(agentscopeMsg);
                }
            }

            return messages;
        }

        /**
         * Convert the current message to AgentScope Msg
         */
        private Msg adaptNewMessage() {
            if (!context.getCurrentMessages().isEmpty()) {
                return convertToAgentScopeMsg(context.getCurrentMessages().get(0));
            }

            List<Message> sessionMessages = context.getSession().getMessages();
            if (!sessionMessages.isEmpty()) {
                Message lastMessage = sessionMessages.get(sessionMessages.size() - 1);
                return convertToAgentScopeMsg(lastMessage);
            }

            return null;
        }

        /**
         * Convert runtime engine Message to AgentScope Msg
         */
        private Msg convertToAgentScopeMsg(Message message) {
            if (message == null) {
                return null;
            }

            MsgRole role = convertRole(message.getRole());
            if (role == null) {
                return null;
            }

            Msg.Builder builder = Msg.builder().role(role);

            // Convert content blocks
            if (message.getContent() != null && !message.getContent().isEmpty()) {
                List<ContentBlock> contentBlocks = new ArrayList<>();
                for (var content : message.getContent()) {
                    if (content instanceof TextContent textContent) {
                        if (textContent.getText() != null && !textContent.getText().isEmpty()) {
                            contentBlocks.add(TextBlock.builder().text(textContent.getText()).build());
                        }
                    }
                    // Add more content type conversions as needed (ImageContent, etc.)
                }
                if (!contentBlocks.isEmpty()) {
                    builder.content(contentBlocks);
                }
            }

            return builder.build();
        }

        /**
         * Convert role string to MsgRole
         */
        private MsgRole convertRole(String role) {
            if (role == null) {
                return null;
            }
            switch (role.toLowerCase()) {
                case "user":
                    return MsgRole.USER;
                case "assistant":
                    return MsgRole.ASSISTANT;
                case "system":
                    return MsgRole.SYSTEM;
                default:
                    return MsgRole.USER; // Default to USER
            }
        }

        public Msg getNewMessage() {
            return newMessage;
        }

        public List<Msg> getMemory() {
            return memory;
        }

        public Context getContext() {
            return context;
        }

        public Agent getAgent() {
            return agentScopeBuilder.build();
        }

        /**
         * Setup tools with sandbox support
         * Similar to SAA's setupTools() method, but adapted for AgentScope's Toolkit
         * Supports both SandboxTool instances and BaseSandboxToolAdapter instances
         */
        private void setupTools() {
            try {
                // Build agent to access toolkit
                Agent agentScopeAgent = agentScopeBuilder.build();
                
                // Try to get toolkit from agent using reflection
                Field toolkitField = findFieldInHierarchy(agentScopeAgent.getClass(), "toolkit");
                if (toolkitField == null) {
                    return;
                }

                toolkitField.setAccessible(true);
                Object toolkitObject = toolkitField.get(agentScopeAgent);

                if (toolkitObject == null) {
                    return;
                }

                if (toolkitObject instanceof Toolkit toolkit) {
                    Set<String> toolNames = toolkit.getToolNames();
                    List<AgentTool> tools = new ArrayList<>();
                    for (String toolName : toolNames) {
                        AgentTool tool = toolkit.getTool(toolName);
                        if (tool != null) {
                            tools.add(tool);
                        }
                    }

                    boolean enableSandbox = false;
                    for (Object tool : tools) {
                        if (tool instanceof AgentScopeSandboxAwareTool) {
                            enableSandbox = true;
                            break;
                        }
                    }

                    if (enableSandbox) {
                        if (context.getEnvironmentManager() == null) {
                            throw new IllegalStateException("EnvironmentManager cannot be null when SandboxAwareTool is present");
                        }

                        SandboxManager sandboxManager = context.getEnvironmentManager().getSandboxManager();
                        if (sandboxManager == null) {
                            throw new IllegalStateException("SandboxManager cannot be null when SandboxAwareTool is present");
                        }

                        String sessionId = context.getSession().getId();
                        String userId = context.getSession().getUserId();

                        for (Object tool : tools){
                            if(tool instanceof AgentScopeSandboxAwareTool sandboxToolAdapter){
                                SandboxTool sandboxTool = sandboxToolAdapter.getSandboxTool();
                                Class<?> sandboxClass = sandboxTool.getSandboxClass();
                                if(sandboxClass == null){
                                    throw new IllegalStateException("SandboxClass cannot be null for AgentScopeSandboxAwareTool: " + tool.getClass().getName());
                                }
                                Sandbox sandbox = (Sandbox) sandboxClass.getConstructor(
                                        SandboxManager.class,
                                        String.class,
                                        String.class
                                ).newInstance(sandboxManager, userId, sessionId);

                                sandboxTool.setSandboxManager(sandboxManager);
                                sandboxTool.setSandbox(sandbox);
                            }
                        }
                    }

                }

            } catch (Exception e) {
                // Re-throw runtime exceptions
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                throw new RuntimeException("Error setting up tools: " + e.getMessage(), e);
            }
        }

        /**
         * Recursively find a field in the class hierarchy (including parent classes)
         *
         * @param clazz     The class to start searching from
         * @param fieldName The name of the field to find
         * @return The Field object if found, null otherwise
         */
        private Field findFieldInHierarchy(Class<?> clazz, String fieldName) {
            Class<?> currentClass = clazz;
            while (currentClass != null) {
                try {
                    return currentClass.getDeclaredField(fieldName);
                } catch (NoSuchFieldException e) {
                    // Field not found in current class, try parent class
                    currentClass = currentClass.getSuperclass();
                }
            }
            // Field not found in entire hierarchy
            return null;
        }
    }

    public AgentScopeAgent() {
        super();
        this.contextAdapter = this::defaultContextAdapter;
        this.responseProcessor = this::defaultResponseProcessor;
        this.streamResponseProcessor = this::defaultStreamResponseProcessor;
    }

    public AgentScopeAgent(Builder agentScopeBuilder) {
        this(agentScopeBuilder, null, null, null);
    }

    public AgentScopeAgent(Builder agentScopeBuilder,
                           Function<Context, Msg> contextAdapter,
                           Function<Msg, String> responseProcessor,
                           Function<Msg, String> streamResponseProcessor) {
        super();
        this.agentScopeBuilder = agentScopeBuilder;
        this.contextAdapter = contextAdapter != null ? contextAdapter : this::defaultContextAdapter;
        this.responseProcessor = responseProcessor != null ? responseProcessor : this::defaultResponseProcessor;
        this.streamResponseProcessor = streamResponseProcessor != null ? streamResponseProcessor : this::defaultStreamResponseProcessor;
    }

    public AgentScopeAgent(List<AgentCallback> beforeCallbacks,
                           List<AgentCallback> afterCallbacks,
                           AgentConfig config,
                           Builder agentScopeBuilder,
                           Function<Context, Msg> contextAdapter,
                           Function<Msg, String> responseProcessor,
                           Function<Msg, String> streamResponseProcessor) {
        super(beforeCallbacks, afterCallbacks, config);
        this.agentScopeBuilder = agentScopeBuilder;
        this.contextAdapter = contextAdapter != null ? contextAdapter : this::defaultContextAdapter;
        this.responseProcessor = responseProcessor != null ? responseProcessor : this::defaultResponseProcessor;
        this.streamResponseProcessor = streamResponseProcessor != null ? streamResponseProcessor : this::defaultStreamResponseProcessor;
    }

    @Override
    protected Flux<Event> execute(Context context, boolean stream) {
        return Flux.create(sink -> {
            try {
                if (agentScopeBuilder == null) {
                    throw new IllegalStateException("AgentScope Builder is not set");
                }

                // Get the new message first (before creating agent with Hook)
                Msg userMsg = null;
                try {
                    AgentScopeContextAdapter tempAdapter = new AgentScopeContextAdapter(context, agentScopeBuilder);
                    tempAdapter.initialize();
                    userMsg = tempAdapter.getNewMessage();
                } catch (Exception e) {
                    logger.warning("Failed to initialize adapter for message extraction: " + e.getMessage());
                }

                if (userMsg == null) {
                    // Fallback: try to extract message without adapter
                    List<Message> currentMessages = context.getCurrentMessages();
                    if (!currentMessages.isEmpty()) {
                        AgentScopeContextAdapter tempAdapter = new AgentScopeContextAdapter(context, agentScopeBuilder);
                        tempAdapter.initialize();
                        userMsg = tempAdapter.getNewMessage();
                    }
                    if (userMsg == null) {
                        throw new IllegalStateException("No message to process");
                    }
                }

                // Create initial response message
                Message textMessage = new Message();
                textMessage.setType(MessageType.MESSAGE.name());
                textMessage.setRole("assistant");
                textMessage.setStatus(RunStatus.IN_PROGRESS);
                sink.next(textMessage);

                if (stream) {
                    // Create a sink for streaming chunks
                    Sinks.Many<String> chunkSink = Sinks.many().multicast().onBackpressureBuffer();

                    // Create streaming hook
                    Hook streamingHook = new Hook() {
                        @Override
                        public ChunkMode reasoningChunkMode() {
                            return ChunkMode.INCREMENTAL; // Get only new content in each chunk
                        }

                        @Override
                        public Mono<Void> onReasoningChunk(Agent agent, Msg chunk) {
                            try {
                                // Use streamResponseProcessor to extract text from chunk
                                String chunkText = streamResponseProcessor.apply(chunk);
                                if (chunkText != null && !chunkText.isEmpty()) {
                                    chunkSink.tryEmitNext(chunkText);

                                    // Emit chunk as delta message
                                    TextContent textContent = new TextContent();
                                    textContent.setText(chunkText);
                                    textContent.setDelta(true);
                                    Message deltaMessage = new Message();
                                    deltaMessage.setType(MessageType.CHUNK.name());
                                    deltaMessage.setRole("assistant");
                                    deltaMessage.setStatus(RunStatus.IN_PROGRESS);
                                    deltaMessage.setContent(List.of(textContent));
                                    sink.next(deltaMessage);
                                }
                            } catch (Exception e) {
                                logger.warning("Error processing chunk: " + e.getMessage());
                            }
                            return Mono.empty();
                        }

                        @Override
                        public Mono<Msg> postCall(Agent agent, Msg finalMsg) {
                            try {
                                String finalContent = responseProcessor.apply(finalMsg);
                                if (finalContent != null && !finalContent.isEmpty()) {
                                    TextContent textContent = new TextContent();
                                    textContent.setText(finalContent);
                                    textContent.setDelta(false);
                                    textMessage.setContent(List.of(textContent));
                                }
                                textMessage.setStatus(RunStatus.COMPLETED);
                                sink.next(textMessage);
                                sink.complete();
                            } catch (Exception e) {
                                logger.severe("Error processing final message: " + e.getMessage());
                                sink.error(e);
                            }
                            chunkSink.tryEmitComplete();
                            return Mono.just(finalMsg);
                        }

                        @Override
                        public Mono<Void> onError(Agent agent, Throwable error) {
                            logger.severe("Agent error in hook: " + error.getMessage());
                            Message errorMessage = new Message();
                            errorMessage.setType(MessageType.MESSAGE.name());
                            errorMessage.setRole("assistant");
                            errorMessage.setStatus(RunStatus.FAILED);
                            TextContent errorContent = new TextContent();
                            errorContent.setText("Error: " + error.getMessage());
                            errorMessage.setContent(List.of(errorContent));
                            sink.next(errorMessage);
                            sink.error(error);
                            chunkSink.tryEmitError(error);
                            return Mono.empty();
                        }
                    };

                    // Create a new builder with hook
                    Builder builderWithHook = agentScopeBuilder.hook(streamingHook);

                    // Create and initialize context adapter with the new builder
                    AgentScopeContextAdapter adapter = new AgentScopeContextAdapter(context, builderWithHook);
                    adapter.initialize();

                    // Get the agent from builder (now with hook)
                    Agent agentScopeAgent = adapter.getAgent();

                    // Start agent execution asynchronously on boundedElastic scheduler
                    agentScopeAgent.call(userMsg)
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe(
                                    response -> {
                                        // Success - already handled via hook
                                    },
                                    error -> {
                                        // Error handling - already handled via hook, but log here too
                                        logger.severe("Agent call error: " + error.getMessage());
                                    }
                            );

                } else {
                    // Non-streaming mode
                    // Create and initialize context adapter
                    AgentScopeContextAdapter adapter = new AgentScopeContextAdapter(context, agentScopeBuilder);
                    adapter.initialize();

                    // Get the agent from builder
                    Agent agentScopeAgent = adapter.getAgent();

                    agentScopeAgent.call(userMsg)
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe(
                                    result -> {
                                        try {
                                            String content = responseProcessor.apply(result);

                                            TextContent textContent = new TextContent();
                                            textContent.setText(content);
                                            textContent.setDelta(false);
                                            textMessage.setContent(List.of(textContent));
                                            textMessage.setStatus(RunStatus.COMPLETED);
                                            sink.next(textMessage);
                                            sink.complete();
                                        } catch (Exception e) {
                                            logger.severe("Error processing response: " + e.getMessage());
                                            sink.error(e);
                                        }
                                    },
                                    error -> {
                                        logger.severe("Agent call error: " + error.getMessage());
                                        Message errorMessage = new Message();
                                        errorMessage.setType(MessageType.MESSAGE.name());
                                        errorMessage.setRole("assistant");
                                        errorMessage.setStatus(RunStatus.FAILED);
                                        TextContent errorContent = new TextContent();
                                        errorContent.setText("Error: " + error.getMessage());
                                        errorMessage.setContent(List.of(errorContent));
                                        sink.next(errorMessage);
                                        sink.error(error);
                                    }
                            );
                }

            } catch (Exception e) {
                logger.severe("Error in execute: " + e.getMessage());
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
     * Default context adapter - extracts user message from context
     */
    private Msg defaultContextAdapter(Context context) {
        AgentScopeContextAdapter adapter = new AgentScopeContextAdapter(context, agentScopeBuilder);
        adapter.initialize();
        return adapter.getNewMessage();
    }

    /**
     * Default response processor - extracts text from AgentScope Msg
     */
    private String defaultResponseProcessor(Msg msg) {
        if (msg == null) {
            return "";
        }

        StringBuilder text = new StringBuilder();
        if (msg.getContent() != null) {
            for (Object block : msg.getContent()) {
                if (block instanceof TextBlock textBlock) {
                    if (textBlock.getText() != null) {
                        if (text.length() > 0) {
                            text.append("\n");
                        }
                        text.append(textBlock.getText());
                    }
                } else if (block instanceof ThinkingBlock thinkingBlock) {
                    // Optionally include thinking content
                    if (thinkingBlock.getThinking() != null) {
                        if (text.length() > 0) {
                            text.append("\n");
                        }
                        text.append("[Thinking: ").append(thinkingBlock.getThinking()).append("]");
                    }
                }
            }
        }

        return text.toString();
    }

    /**
     * Default stream response processor - extracts incremental text from AgentScope Msg chunk
     */
    private String defaultStreamResponseProcessor(Msg msg) {
        if (msg == null) {
            return "";
        }

        // For streaming chunks, typically only new content is in the chunk
        StringBuilder text = new StringBuilder();
        if (msg.getContent() != null) {
            for (Object block : msg.getContent()) {
                if (block instanceof TextBlock textBlock) {
                    if (textBlock.getText() != null) {
                        text.append(textBlock.getText());
                    }
                } else if (block instanceof ThinkingBlock thinkingBlock) {
                    // For thinking blocks, include the thinking text
                    if (thinkingBlock.getThinking() != null) {
                        text.append(thinkingBlock.getThinking());
                    }
                }
            }
        }

        return text.toString();
    }

    @Override
    public io.agentscope.runtime.engine.agents.Agent copy() {
        AgentScopeAgent copy = new AgentScopeAgent(
                this.beforeCallbacks,
                this.afterCallbacks,
                this.config,
                this.agentScopeBuilder,
                this.contextAdapter,
                this.responseProcessor,
                this.streamResponseProcessor
        );
        copy.setKwargs(new HashMap<>(this.kwargs));
        return copy;
    }

    // Builder pattern support
    public static AgentScopeAgentBuilder builder() {
        return new AgentScopeAgentBuilder();
    }

    public static class AgentScopeAgentBuilder {
        private Builder agentScopeBuilder;
        private Function<Context, Msg> contextAdapter;
        private Function<Msg, String> responseProcessor;
        private Function<Msg, String> streamResponseProcessor;
        private AgentConfig config = new AgentConfig();

        public AgentScopeAgentBuilder agent(Builder agentScopeBuilder) {
            this.agentScopeBuilder = agentScopeBuilder;
            return this;
        }

        public AgentScopeAgentBuilder contextAdapter(Function<Context, Msg> contextAdapter) {
            this.contextAdapter = contextAdapter;
            return this;
        }

        public AgentScopeAgentBuilder responseProcessor(Function<Msg, String> responseProcessor) {
            this.responseProcessor = responseProcessor;
            return this;
        }

        public AgentScopeAgentBuilder streamResponseProcessor(Function<Msg, String> streamResponseProcessor) {
            this.streamResponseProcessor = streamResponseProcessor;
            return this;
        }

        public AgentScopeAgentBuilder config(AgentConfig config) {
            this.config = config;
            return this;
        }

        public AgentScopeAgent build() {
            return new AgentScopeAgent(null, null, config, agentScopeBuilder, contextAdapter, responseProcessor, streamResponseProcessor);
        }
    }

    public Builder getAgentScopeBuilder() {
        return agentScopeBuilder;
    }

    public void setAgentScopeBuilder(Builder agentScopeBuilder) {
        this.agentScopeBuilder = agentScopeBuilder;
    }

    public Function<Context, Msg> getContextAdapter() {
        return contextAdapter;
    }

    public void setContextAdapter(Function<Context, Msg> contextAdapter) {
        this.contextAdapter = contextAdapter;
    }

    public Function<Msg, String> getResponseProcessor() {
        return responseProcessor;
    }

    public void setResponseProcessor(Function<Msg, String> responseProcessor) {
        this.responseProcessor = responseProcessor;
    }
}
