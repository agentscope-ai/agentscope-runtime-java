/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.agentscope.runtime.adapters.agentscope;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.message.*;
import io.agentscope.runtime.adapters.StreamAdapter;
import io.agentscope.runtime.engine.schemas.Event;
import io.agentscope.runtime.engine.schemas.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Adapter for converting AgentScope Java streaming events to runtime messages.
 * but uses Reactor Flux<Event> instead of AsyncIterator.
 * 
 * <p>This implementation strictly follows the following logic:
 * <ul>
 *   <li>Maintains state across streaming events (msg_id, truncate memory, tool dict, etc.)</li>
 *   <li>Handles incremental text updates with deduplication (removeprefix logic)</li>
 *   <li>Manages tool_use messages in stages (create empty, then fill on last)</li>
 *   <li>Processes all content block types: text, thinking, tool_use, tool_result, image, audio</li>
 * </ul>
 */
public class AgentScopeStreamAdapter implements StreamAdapter {
    private static final Logger logger = LoggerFactory.getLogger(AgentScopeStreamAdapter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Adapts AgentScope Java Flux<Event> stream to runtime Event stream.
     * 
     * @param sourceStream Flux of AgentScope Event objects (EventType, Msg, isLast)
     * @return Flux of runtime Event objects (Message or Content)
     */
    @Override
    public Flux<Event> adaptFrameworkStream(Object sourceStream) {
        if (!(sourceStream instanceof Flux)) {
            throw new IllegalArgumentException(
                "Expected Flux<Event>, got " + 
                (sourceStream != null ? sourceStream.getClass().getName() : "null")
            );
        }
        @SuppressWarnings("unchecked")
        Flux<io.agentscope.core.agent.Event> eventFlux = (Flux<io.agentscope.core.agent.Event>) sourceStream;
        return adaptAgentScopeMessageStreamReactive(eventFlux);
    }
    
    /**
     * Async version using Flux for better reactive support.
     * 
     * <p>Key features:
     * <ul>
     *   <li>State management across events (msg_id tracking, truncate memory, tool dict)</li>
     *   <li>Incremental text processing with deduplication</li>
     *   <li>Staged tool_use message building</li>
     *   <li>Support for all content block types</li>
     *   <li>Yields both Message and Content objects</li>
     * </ul>
     * 
     * @param sourceStream Flux of AgentScope framework Event objects (io.agentscope.core.agent.Event)
     * @return Flux of runtime Event objects (io.agentscope.runtime.engine.schemas.Event - Message or Content)
     */
    public Flux<Event> adaptAgentScopeMessageStreamReactive(Flux<io.agentscope.core.agent.Event> sourceStream) {
        // Use stateful operator to maintain state across events
        // We need to use a mutable state object that persists across events
        return sourceStream
            .transformDeferred(flux -> {
                StreamState state = new StreamState();
                return flux
            .flatMap(event -> {
                        Msg msg = event.getMessage();
                        boolean last = event.isLast();
                        
                        // If a new message, reset state
                        if (state.msgId == null || !msg.getId().equals(state.msgId)) {
                            state.reset(msg.getId());
                        }
                        
                        // Process the message content and emit all resulting events
                        List<Event> newEvents = processMessageContent(msg, last, state);
                        return Flux.fromIterable(newEvents);
                    })
                    .concatWith(Flux.defer(() -> {
                        // Handle last_content if any (final processing)
                        if (state.lastContent != null && !state.lastContent.isEmpty()) {
                            List<Event> finalEvents = new ArrayList<>();
                            
                            if (state.shouldStartMessage) {
                                state.index = null;
                                updateMessageAttrs(state.message, state.metadata, state.usage);
                                finalEvents.add(state.message.inProgress());
                            }
                            
                            TextContent textDeltaContent = new TextContent(true, state.index, state.lastContent);
                            textDeltaContent = (TextContent) state.message.addDeltaContent(textDeltaContent);
                            finalEvents.add(textDeltaContent);
                            
                            updateMessageAttrs(state.message, state.metadata, state.usage);
                            finalEvents.add(state.message.completed());
                            
                            return Flux.fromIterable(finalEvents);
                        }
                        return Flux.empty();
                    }));
            });
    }
    
    /**
     * Process message content blocks.
     * Returns both Message and Content objects.
     */
    private List<Event> processMessageContent(Msg msg, boolean last, StreamState state) {
        List<Event> results = new ArrayList<>();
        List<ContentBlock> content = msg.getContent();
        
        // Handle string content (shouldn't happen with AgentScope Java API, but handle for compatibility)
        // Content is always List<ContentBlock>, but we check for empty
        if (content == null || content.isEmpty()) {
            return results;
        }
        
        // Separate tool_use blocks from other blocks
        List<ContentBlock> newBlocks = new ArrayList<>();
        List<ContentBlock> newToolBlocks = new ArrayList<>();
        
        for (ContentBlock block : content) {
            if (block instanceof ToolUseBlock) {
                newToolBlocks.add(block);
            } else {
                newBlocks.add(block);
            }
        }
        
        // Update content based on tool_start flag
        List<ContentBlock> blocksToProcess;
        if (!newToolBlocks.isEmpty()) {
            if (state.toolStart) {
                blocksToProcess = newToolBlocks;
            } else {
                blocksToProcess = newBlocks;
                state.toolStart = true;
            }
        } else {
            blocksToProcess = newBlocks;
        }
        
        // Update metadata and usage
        state.metadata = msg.getMetadata();
        // Note: AgentScope Java Msg doesn't have usage field directly
        // It might be in metadata or we skip it
        state.usage = null; // Usage not available in AgentScope Java Msg
        
        // Process each block
        // Content is always List<ContentBlock>, so we process blocks directly
        for (ContentBlock element : blocksToProcess) {
            if (element instanceof TextBlock) {
                processTextBlock((TextBlock) element, last, state, results);
            } else if (element instanceof ThinkingBlock) {
                processThinkingBlock((ThinkingBlock) element, last, state, results);
            } else if (element instanceof ToolUseBlock) {
                processToolUseBlock((ToolUseBlock) element, last, state, results);
            } else if (element instanceof ToolResultBlock) {
                processToolResultBlock((ToolResultBlock) element, state, results);
            } else if (element instanceof ImageBlock) {
                processImageBlock((ImageBlock) element, state, results);
            } else if (element instanceof AudioBlock) {
                processAudioBlock((AudioBlock) element, state, results);
            } else {
                // Fallback: convert to text
                processUnknownBlock(element, state, results);
            }
        }
        
        return results;
    }
    
    /**
     * Process text block with incremental updates and deduplication.
     */
    private void processTextBlock(TextBlock block, boolean last, StreamState state, List<Event> results) {
        String text = block.getText();
        if (text == null || text.isEmpty()) {
            return;
        }
        
        // Start message if needed
        if (state.shouldStartMessage) {
            state.index = null;
            updateMessageAttrs(state.message, state.metadata, state.usage);
            results.add(state.message.inProgress());
            state.shouldStartMessage = false;
        }
        
        // Remove prefix (deduplication logic)
        String newText = removePrefix(text, state.localTruncateMemory);
        state.localTruncateMemory = text;
        
        // Create delta content
        TextContent textDeltaContent = new TextContent(true, state.index, newText);
        textDeltaContent = (TextContent) state.message.addDeltaContent(textDeltaContent);
        state.index = textDeltaContent.getIndex();
        
        // Only yield valid text
        if (textDeltaContent.getText() != null && !textDeltaContent.getText().isEmpty()) {
            results.add(textDeltaContent);
        }
        
        // Complete message if last or tool_start
        if (last || state.toolStart) {
            if (state.index != null && state.index < state.message.getContent().size()) {
                Content completedContent = state.message.getContent().get(state.index);
                if (completedContent instanceof TextContent) {
                    TextContent textContent = (TextContent) completedContent;
                    if (textContent.getText() != null && !textContent.getText().isEmpty()) {
                        results.add(completedContent.completed());
                    }
                }
            }
            
            updateMessageAttrs(state.message, state.metadata, state.usage);
            results.add(state.message.completed());
            
            // Reset for next message
            state.message = new Message(MessageType.MESSAGE, "assistant");
            state.index = null;
            state.shouldStartMessage = true;
        }
    }
    
    /**
     * Process thinking block with incremental updates and deduplication.
     */
    private void processThinkingBlock(ThinkingBlock block, boolean last, StreamState state, List<Event> results) {
        String reasoning = block.getThinking();
        if (reasoning == null || reasoning.isEmpty()) {
            return;
        }
        
        // Start reasoning message if needed
        if (state.shouldStartReasoningMessage) {
            state.index = null;
            updateMessageAttrs(state.reasoningMessage, state.metadata, state.usage);
            results.add(state.reasoningMessage.inProgress());
            state.shouldStartReasoningMessage = false;
        }
        
        // Remove prefix (deduplication logic)
        String newReasoning = removePrefix(reasoning, state.localTruncateReasoningMemory);
        state.localTruncateReasoningMemory = reasoning;
        
        // Create delta content
        TextContent textDeltaContent = new TextContent(true, state.index, newReasoning);
        textDeltaContent = (TextContent) state.reasoningMessage.addDeltaContent(textDeltaContent);
        state.index = textDeltaContent.getIndex();
        
        // Only yield valid text
        // Return textDeltaContent directly
        if (textDeltaContent.getText() != null && !textDeltaContent.getText().isEmpty()) {
            results.add(textDeltaContent);
        }
        
        // Complete reasoning message if last or tool_start
        if (last || state.toolStart) {
            if (state.index != null && state.index < state.reasoningMessage.getContent().size()) {
                Content completedContent = state.reasoningMessage.getContent().get(state.index);
                if (completedContent instanceof TextContent) {
                    TextContent textContent = (TextContent) completedContent;
                    if (textContent.getText() != null && !textContent.getText().isEmpty()) {
                        results.add(completedContent.completed());
                    }
                }
            }
            
            updateMessageAttrs(state.reasoningMessage, state.metadata, state.usage);
            results.add(state.reasoningMessage.completed());
            
            // Reset for next reasoning message
            state.reasoningMessage = new Message(MessageType.REASONING, "assistant");
            state.index = null;
        }
    }
    
    /**
     * Process tool_use block with staged building (create empty, fill on last).
     */
    private void processToolUseBlock(ToolUseBlock block, boolean last, StreamState state, List<Event> results) {
        String callId = block.getId();
        
        if (last) {
            // Fill in the complete arguments
            Message pluginCallMessage = state.toolUseMessagesDict.get(callId);
            if (pluginCallMessage == null) {
                logger.warn("Tool use message not found for call_id: " + callId);
                return;
            }
            
            // Serialize input to JSON
            String jsonStr;
            try {
                jsonStr = objectMapper.writeValueAsString(block.getInput());
            } catch (Exception e) {
                logger.error("Failed to serialize tool input", e);
                jsonStr = "{}";
            }
            
            // Create FunctionCall data
            FunctionCall functionCall = new FunctionCall(
                block.getId(),
                block.getName(),
                jsonStr
            );
            
            // Convert to Map for DataContent
            Map<String, Object> callData = new HashMap<>();
            callData.put("call_id", functionCall.getCallId());
            callData.put("name", functionCall.getName());
            callData.put("arguments", functionCall.getArguments());
            
            DataContent dataDeltaContent = new DataContent();
            dataDeltaContent.setDelta(true);
            dataDeltaContent.setIndex(state.index);
            dataDeltaContent.setData(callData);
            
            dataDeltaContent = (DataContent) pluginCallMessage.addDeltaContent(dataDeltaContent);
            results.add(dataDeltaContent.completed());
            
            updateMessageAttrs(pluginCallMessage, state.metadata, state.usage);
            results.add(pluginCallMessage.completed());
            state.index = null;
        } else {
            // Create new tool call message if not exists
            if (!state.toolUseMessagesDict.containsKey(callId)) {
                Message pluginCallMessage = new Message(MessageType.PLUGIN_CALL, "assistant");
                
                // Create FunctionCall with empty arguments
                FunctionCall functionCall = new FunctionCall(
                    block.getId(),
                    block.getName(),
                    ""
                );
                
                // Convert to Map for DataContent
                Map<String, Object> callData = new HashMap<>();
                callData.put("call_id", functionCall.getCallId());
                callData.put("name", functionCall.getName());
                callData.put("arguments", functionCall.getArguments());
                
                DataContent dataDeltaContent = new DataContent();
                dataDeltaContent.setDelta(true);
                dataDeltaContent.setIndex(state.index);
                dataDeltaContent.setData(callData);
                
                updateMessageAttrs(pluginCallMessage, state.metadata, state.usage);
                results.add(pluginCallMessage.inProgress());
                
                dataDeltaContent = (DataContent) pluginCallMessage.addDeltaContent(dataDeltaContent);
                results.add(dataDeltaContent);
                
                state.toolUseMessagesDict.put(callId, pluginCallMessage);
            }
        }
    }
    
    /**
     * Process tool_result block.
     */
    private void processToolResultBlock(ToolResultBlock block, StreamState state, List<Event> results) {
        // Serialize output to JSON
        String jsonStr;
        try {
            // Convert output blocks to JSON
            List<ContentBlock> output = block.getOutput();
            if (output == null || output.isEmpty()) {
                jsonStr = "null";
            } else {
                // Convert ContentBlocks to a serializable format
                List<Map<String, Object>> outputList = output.stream()
                    .map(this::contentBlockToMap)
                    .collect(Collectors.toList());
                jsonStr = objectMapper.writeValueAsString(outputList);
            }
        } catch (Exception e) {
            logger.error("Failed to serialize tool output", e);
            jsonStr = "{}";
        }
        
        // Create FunctionCallOutput data
        FunctionCallOutput functionCallOutput = new FunctionCallOutput(
            block.getId(),
            block.getName(),
            jsonStr
        );
        
        // Convert to Map for DataContent
        Map<String, Object> outputData = new HashMap<>();
        outputData.put("call_id", functionCallOutput.getCallId());
        outputData.put("name", functionCallOutput.getName());
        outputData.put("output", functionCallOutput.getOutput());
        
        DataContent dataDeltaContent = new DataContent();
        dataDeltaContent.setIndex(state.index);
        dataDeltaContent.setData(outputData);
        
        Message pluginOutputMessage = new Message(MessageType.PLUGIN_CALL_OUTPUT, "tool");
        pluginOutputMessage.setContent(List.of(dataDeltaContent));
        
        updateMessageAttrs(pluginOutputMessage, state.metadata, state.usage);
        results.add(pluginOutputMessage.completed());
        
        // Reset message state
        state.message = new Message(MessageType.MESSAGE, "assistant");
        state.shouldStartMessage = true;
        state.index = null;
    }
    
    /**
     * Process image block.
     */
    private void processImageBlock(ImageBlock block, StreamState state, List<Event> results) {
        // Start message if needed
        if (state.shouldStartMessage) {
            state.index = null;
            updateMessageAttrs(state.message, state.metadata, state.usage);
            results.add(state.message.inProgress());
            state.shouldStartMessage = false;
        }
        
        ImageContent deltaContent = new ImageContent();
        deltaContent.setDelta(true);
        deltaContent.setIndex(state.index);
        
        Source source = block.getSource();
        if (source instanceof URLSource) {
            String url = ((URLSource) source).getUrl();
            deltaContent.setImageUrl(url);
        } else if (source instanceof Base64Source) {
            Base64Source base64Source = (Base64Source) source;
            String mediaType = base64Source.getMediaType();
            if (mediaType == null) {
                mediaType = "image/jpeg";
            }
            String base64Data = base64Source.getData();
            String url = "data:" + mediaType + ";base64," + base64Data;
            deltaContent.setImageUrl(url);
        }
        
        deltaContent = (ImageContent) state.message.addDeltaContent(deltaContent);
        state.index = deltaContent.getIndex();
        results.add(deltaContent);
    }
    
    /**
     * Process audio block.
     */
    private void processAudioBlock(AudioBlock block, StreamState state, List<Event> results) {
        // Start message if needed
        if (state.shouldStartMessage) {
            state.index = null;
            updateMessageAttrs(state.message, state.metadata, state.usage);
            results.add(state.message.inProgress());
            state.shouldStartMessage = false;
        }
        
        AudioContent deltaContent = new AudioContent();
        deltaContent.setDelta(true);
        deltaContent.setIndex(state.index);
        
        Source source = block.getSource();
        if (source instanceof URLSource) {
            String url = ((URLSource) source).getUrl();
            deltaContent.setData(url);
            // Try to extract format from URL
            try {
                URI uri = new URI(url);
                String path = uri.getPath();
                if (path != null && path.contains(".")) {
                    String format = path.substring(path.lastIndexOf('.') + 1);
                    deltaContent.setFormat(format);
                }
            } catch (URISyntaxException e) {
                // Ignore, format remains null
            }
        } else if (source instanceof Base64Source) {
            Base64Source base64Source = (Base64Source) source;
            String mediaType = base64Source.getMediaType();
            String base64Data = base64Source.getData();
            String url = "data:" + mediaType + ";base64," + base64Data;
            deltaContent.setData(url);
            deltaContent.setFormat(mediaType);
        }
        
        deltaContent = (AudioContent) state.message.addDeltaContent(deltaContent);
        state.index = deltaContent.getIndex();
        results.add(deltaContent);
    }
    
    /**
     * Process unknown block type (fallback to text).
     */
    private void processUnknownBlock(ContentBlock block, StreamState state, List<Event> results) {
        // Start message if needed
        if (state.shouldStartMessage) {
            state.index = null;
            updateMessageAttrs(state.message, state.metadata, state.usage);
            results.add(state.message.inProgress());
            state.shouldStartMessage = false;
        }
        
        TextContent deltaContent = new TextContent(true, state.index, block.toString());
        deltaContent = (TextContent) state.message.addDeltaContent(deltaContent);
        state.index = deltaContent.getIndex();
        results.add(deltaContent);
    }
    
    /**
     * Update message attributes (metadata, usage).
     */
    private void updateMessageAttrs(Message message, Map<String, Object> metadata, Map<String, Object> usage) {
        if (metadata != null) {
            message.setMetadata(metadata);
        }
        if (usage != null) {
            message.setUsage(usage);
        }
    }
    
    /**
     * Remove prefix from string
     */
    private String removePrefix(String str, String prefix) {
        if (prefix == null || prefix.isEmpty() || !str.startsWith(prefix)) {
            return str;
        }
        return str.substring(prefix.length());
    }
    
    /**
     * Convert ContentBlock to Map for JSON serialization.
     */
    private Map<String, Object> contentBlockToMap(ContentBlock block) {
        Map<String, Object> map = new HashMap<>();
        if (block instanceof TextBlock) {
            map.put("type", "text");
            map.put("text", ((TextBlock) block).getText());
        } else if (block instanceof ImageBlock) {
            map.put("type", "image");
            // Convert source to map
            Source source = ((ImageBlock) block).getSource();
            Map<String, Object> sourceMap = new HashMap<>();
            if (source instanceof URLSource) {
                sourceMap.put("type", "url");
                sourceMap.put("url", ((URLSource) source).getUrl());
            } else if (source instanceof Base64Source) {
                sourceMap.put("type", "base64");
                sourceMap.put("media_type", ((Base64Source) source).getMediaType());
                sourceMap.put("data", ((Base64Source) source).getData());
            }
            map.put("source", sourceMap);
        } else if (block instanceof AudioBlock) {
            map.put("type", "audio");
            // Convert source to map
            Source source = ((AudioBlock) block).getSource();
            Map<String, Object> sourceMap = new HashMap<>();
            if (source instanceof URLSource) {
                sourceMap.put("type", "url");
                sourceMap.put("url", ((URLSource) source).getUrl());
            } else if (source instanceof Base64Source) {
                sourceMap.put("type", "base64");
                sourceMap.put("media_type", ((Base64Source) source).getMediaType());
                sourceMap.put("data", ((Base64Source) source).getData());
            }
            map.put("source", sourceMap);
                        } else {
            // Fallback: use toString
            map.put("type", "text");
            map.put("text", block.toString());
        }
        return map;
    }
    
    /**
     * State class to maintain streaming state across events.
     */
    private static class StreamState {
        String msgId = null;
        String lastContent = "";
        Map<String, Object> metadata = null;
        Map<String, Object> usage = null;
        boolean toolStart = false;
        Message message = new Message(MessageType.MESSAGE, "assistant");
        Message reasoningMessage = new Message(MessageType.REASONING, "assistant");
        String localTruncateMemory = "";
        String localTruncateReasoningMemory = "";
        boolean shouldStartMessage = true;
        boolean shouldStartReasoningMessage = true;
        Map<String, Message> toolUseMessagesDict = new HashMap<>();
        Integer index = null;
        List<Message> pendingMessages = new ArrayList<>();
        
        void reset(String newMsgId) {
            localTruncateMemory = "";
            localTruncateReasoningMemory = "";
            lastContent = "";
            message = new Message(MessageType.MESSAGE, "assistant");
            reasoningMessage = new Message(MessageType.REASONING, "assistant");
            shouldStartMessage = true;
            shouldStartReasoningMessage = true;
            index = null;
            toolStart = false;
            toolUseMessagesDict.clear();
            msgId = newMsgId;
        }
    }
}

