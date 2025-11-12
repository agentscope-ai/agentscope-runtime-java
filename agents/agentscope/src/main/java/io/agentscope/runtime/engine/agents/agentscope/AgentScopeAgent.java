/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agentscope.runtime.engine.agents.agentscope;

import io.agentscope.core.ReActAgent.Builder;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.EventType;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.message.*;
import io.agentscope.core.tool.AgentTool;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.runtime.engine.agents.AgentCallback;
import io.agentscope.runtime.engine.agents.AgentConfig;
import io.agentscope.runtime.engine.agents.BaseAgent;
import io.agentscope.runtime.engine.agents.agentscope.tools.AgentScopeSandboxAwareTool;
import io.agentscope.runtime.engine.memory.model.MessageType;
import io.agentscope.runtime.engine.schemas.agent.*;
import io.agentscope.runtime.engine.schemas.context.Context;
import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.tools.SandboxTool;
import reactor.core.publisher.Flux;
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
    private Function<ContentBlock, StreamResponse> streamResponseProcessor;

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

            Msg.Builder builder = Msg.builder().role(MsgRole.USER);

            if (message.getContent() != null && !message.getContent().isEmpty()) {
                List<ContentBlock> contentBlocks = new ArrayList<>();
                for (var content : message.getContent()) {
                    if (content instanceof TextContent textContent) {
                        if (textContent.getText() != null && !textContent.getText().isEmpty()) {
                            contentBlocks.add(TextBlock.builder().text(textContent.getText()).build());
                        }
                    }
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

                        for (Object tool : tools) {
                            if (tool instanceof AgentScopeSandboxAwareTool sandboxToolAdapter) {
                                SandboxTool sandboxTool = sandboxToolAdapter.getSandboxTool();
                                Class<?> sandboxClass = sandboxTool.getSandboxClass();
                                if (sandboxClass == null) {
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
            return null;
        }
    }

    public AgentScopeAgent() {
        super();
        this.contextAdapter = this::defaultContextAdapter;
        this.streamResponseProcessor = this::defaultStreamResponseProcessor;
    }

    public AgentScopeAgent(Builder agentScopeBuilder) {
        this(agentScopeBuilder, null, null);
    }

    public AgentScopeAgent(Builder agentScopeBuilder,
                           Function<Context, Msg> contextAdapter,
                           Function<ContentBlock, StreamResponse> streamResponseProcessor) {
        super();
        this.agentScopeBuilder = agentScopeBuilder;
        this.contextAdapter = contextAdapter != null ? contextAdapter : this::defaultContextAdapter;
        this.streamResponseProcessor = streamResponseProcessor != null ? streamResponseProcessor : this::defaultStreamResponseProcessor;
    }

    public AgentScopeAgent(List<AgentCallback> beforeCallbacks,
                           List<AgentCallback> afterCallbacks,
                           AgentConfig config,
                           Builder agentScopeBuilder,
                           Function<Context, Msg> contextAdapter,
                           Function<ContentBlock, StreamResponse> streamResponseProcessor) {
        super(beforeCallbacks, afterCallbacks, config);
        this.agentScopeBuilder = agentScopeBuilder;
        this.contextAdapter = contextAdapter != null ? contextAdapter : this::defaultContextAdapter;
        this.streamResponseProcessor = streamResponseProcessor != null ? streamResponseProcessor : this::defaultStreamResponseProcessor;
    }

    @Override
    protected Flux<Event> execute(Context context) {
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
                textMessage.setType(MessageType.ASSISTANT);
                textMessage.setStatus(RunStatus.IN_PROGRESS);
                sink.next(textMessage);
                AgentScopeContextAdapter adapter = new AgentScopeContextAdapter(context, agentScopeBuilder);
                adapter.initialize();

                Agent agentScopeAgent = adapter.getAgent();

                StreamOptions streamOptions = StreamOptions.builder()
                        .eventTypes(EventType.REASONING, EventType.TOOL_RESULT)
                        .incremental(true)
                        .build();

                agentScopeAgent.stream(userMsg, streamOptions)
                        .subscribeOn(Schedulers.boundedElastic())
                        .subscribe(
                                event -> {
                                    try {
                                        for (ContentBlock contentBlock : event.getMessage().getContent()) {
                                            Message deltaMessage = new Message();
                                            StreamResponse delta = streamResponseProcessor.apply(contentBlock);
                                            TextContent deltaContent = new TextContent();
                                            deltaContent.setText(delta.toString());
                                            deltaContent.setDelta(true);
                                            deltaMessage.setType(delta.type);
                                            deltaMessage.setStatus(RunStatus.IN_PROGRESS);
                                            deltaMessage.setContent(List.of(deltaContent));
                                            sink.next(deltaMessage);
                                        }
                                    } catch (Exception e) {
                                        logger.severe("Error processing stream event: " + e.getMessage());
                                    }
                                },
                                error -> {
                                    logger.severe("Agent stream error: " + error.getMessage());
                                    Message errorMessage = new Message();
                                    errorMessage.setType(MessageType.ASSISTANT);
                                    errorMessage.setStatus(RunStatus.FAILED);
                                    TextContent errorContent = new TextContent();
                                    errorContent.setText("Error: " + error.getMessage());
                                    errorMessage.setContent(List.of(errorContent));
                                    sink.next(errorMessage);
                                    sink.error(error);
                                },
                                () -> {
                                    Message completedMessage = new Message();
                                    completedMessage.setType(MessageType.ASSISTANT);
                                    completedMessage.setStatus(RunStatus.COMPLETED);
                                    sink.next(completedMessage);
                                    sink.complete();
                                }
                        );

            } catch (Exception e) {
                logger.severe("Error in execute: " + e.getMessage());
                Message errorMessage = new Message();
                errorMessage.setType(MessageType.ASSISTANT);
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
    private StreamResponse defaultStreamResponseProcessor(ContentBlock contentBlock) {
        if (contentBlock == null) {
            return new StreamResponse();
        }

        if (contentBlock instanceof TextBlock textBlock) {
            return new StreamResponse(textBlock.getText());
        } else if (contentBlock instanceof ThinkingBlock thinkingBlock) {
            return new StreamResponse("",thinkingBlock.getThinking(), MessageType.THINKING);
        }
        else if (contentBlock instanceof ToolUseBlock toolUseBlock){
            return new StreamResponse(toolUseBlock.getName(), toolUseBlock.getInput().toString(), MessageType.TOOL_CALL, toolUseBlock.getId());
        }
        else if (contentBlock instanceof ToolResultBlock toolResultBlock){
            String result = "";
            if(!toolResultBlock.getOutput().isEmpty()){
                result = toolResultBlock.getOutput().get(0).toString();
            }
            return new StreamResponse(toolResultBlock.getName(), result, MessageType.TOOL_RESPONSE, toolResultBlock.getId());
        }
        return new StreamResponse();
    }

    @Override
    public io.agentscope.runtime.engine.agents.Agent copy() {
        AgentScopeAgent copy = new AgentScopeAgent(
                this.beforeCallbacks,
                this.afterCallbacks,
                this.config,
                this.agentScopeBuilder,
                this.contextAdapter,
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
        private Function<ContentBlock, StreamResponse> streamResponseProcessor;
        private AgentConfig config = new AgentConfig();

        public AgentScopeAgentBuilder agent(Builder agentScopeBuilder) {
            this.agentScopeBuilder = agentScopeBuilder;
            return this;
        }

        public AgentScopeAgentBuilder contextAdapter(Function<Context, Msg> contextAdapter) {
            this.contextAdapter = contextAdapter;
            return this;
        }

        public AgentScopeAgentBuilder streamResponseProcessor(Function<ContentBlock, StreamResponse> streamResponseProcessor) {
            this.streamResponseProcessor = streamResponseProcessor;
            return this;
        }

        public AgentScopeAgentBuilder config(AgentConfig config) {
            this.config = config;
            return this;
        }

        public AgentScopeAgent build() {
            return new AgentScopeAgent(null, null, config, agentScopeBuilder, contextAdapter, streamResponseProcessor);
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
}