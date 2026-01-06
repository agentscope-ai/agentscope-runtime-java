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
            .flatMap(event -> {
                List<Event> newEvents = processMessageContent(event);
                return Flux.fromIterable(newEvents);
            });
    }
    
    /**
     * Process message content blocks.
     * Returns both Message and Content objects.
     */
    private List<Event> processMessageContent(io.agentscope.core.agent.Event event) {
        List<Event> results = new ArrayList<>();
        Msg msg = event.getMessage();
        List<ContentBlock> content = msg.getContent();
        
        // Handle string content.
        // Content is always List<ContentBlock>, but we check for empty
        if (content == null || content.isEmpty()) {
            return results;
        }
        
        Map<String, Object> metadata = msg.getMetadata();
        
        for (ContentBlock element : content) {
            if(!event.isLast()||element instanceof ToolUseBlock || element instanceof ToolResultBlock){
                if (element instanceof TextBlock textBlock) {
                    processTextBlock(textBlock, metadata, results);
                } else if (element instanceof ThinkingBlock thinkingBlock) {
                    processThinkingBlock(thinkingBlock, metadata, results);
                } else if (element instanceof ToolUseBlock toolUseBlock) {
                    processToolUseBlock(toolUseBlock, metadata, results);
                } else if (element instanceof ToolResultBlock toolResultBlock) {
                    processToolResultBlock(toolResultBlock, metadata, results);
                } else if (element instanceof ImageBlock imageBlock) {
                    processImageBlock(imageBlock, metadata, results);
                } else if (element instanceof AudioBlock audioBlock) {
                    processAudioBlock(audioBlock, metadata, results);
                } else {
                    // Fallback: convert to text
                    processUnknownBlock(element, metadata, results);
                }
            }
        }
        
        return results;
    }
    
    /**
     * Process text block with incremental updates and deduplication.
     */
    private void processTextBlock(TextBlock block, Map<String, Object> metadata, List<Event> results) {
        String text = block.getText();
        if (text == null || text.isEmpty()) {
            return;
        }
        
        Message message = new Message(MessageType.MESSAGE, "assistant");
        updateMessageAttrs(message, metadata, null);
        
        TextContent textContent = new TextContent(true, null, text);
        textContent = (TextContent) message.addDeltaContent(textContent);
        results.add(textContent);
    }
    
    /**
     * Process thinking block with incremental updates and deduplication.
     */
    private void processThinkingBlock(ThinkingBlock block, Map<String, Object> metadata, List<Event> results) {
        String reasoning = block.getThinking();
        if (reasoning == null || reasoning.isEmpty()) {
            return;
        }
        
        Message reasoningMessage = new Message(MessageType.REASONING, "assistant");
        updateMessageAttrs(reasoningMessage, metadata, null);
        
        TextContent textContent = new TextContent(true, null, reasoning);
        textContent = (TextContent) reasoningMessage.addDeltaContent(textContent);
        results.add(textContent);
    }
    
    /**
     * Process tool_use block with staged building (create empty, fill on last).
     */
    private void processToolUseBlock(ToolUseBlock block, Map<String, Object> metadata, List<Event> results) {
        // Serialize input to JSON
        String jsonStr;
        try {
            jsonStr = objectMapper.writeValueAsString(block.getInput());
        } catch (Exception e) {
            logger.error("Failed to serialize tool input", e);
            jsonStr = "{}";
        }
        
        FunctionCall functionCall = new FunctionCall(
            block.getId(),
            block.getName(),
            jsonStr
        );
        
        Map<String, Object> callData = new HashMap<>();
        callData.put("call_id", functionCall.getCallId());
        callData.put("name", functionCall.getName());
        callData.put("arguments", functionCall.getArguments());
        
        DataContent dataDeltaContent = new DataContent();
        dataDeltaContent.setDelta(true);
        dataDeltaContent.setData(callData);
        
        Message pluginCallMessage = new Message(MessageType.MCP_TOOL_CALL, "assistant");
        pluginCallMessage.addDeltaContent(dataDeltaContent);
        updateMessageAttrs(pluginCallMessage, metadata, null);
        
        results.add(pluginCallMessage.completed());
    }
    
    /**
     * Process tool_result block.
     */
    private void processToolResultBlock(ToolResultBlock block, Map<String, Object> metadata, List<Event> results) {
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
        dataDeltaContent.setData(outputData);
        
        Message pluginOutputMessage = new Message(MessageType.MCP_APPROVAL_RESPONSE, "tool");
        pluginOutputMessage.setContent(List.of(dataDeltaContent));
        
        updateMessageAttrs(pluginOutputMessage, metadata, null);
        results.add(pluginOutputMessage.completed());
    }
    
    /**
     * Process image block.
     */
    private void processImageBlock(ImageBlock block, Map<String, Object> metadata, List<Event> results) {
        Message message = new Message(MessageType.MESSAGE, "assistant");
        updateMessageAttrs(message, metadata, null);
        
        ImageContent deltaContent = new ImageContent();
        deltaContent.setDelta(true);
        
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
        
        deltaContent = (ImageContent) message.addDeltaContent(deltaContent);
        results.add(deltaContent);
        results.add(message.completed());
    }
    
    /**
     * Process audio block.
     */
    private void processAudioBlock(AudioBlock block, Map<String, Object> metadata, List<Event> results) {
        Message message = new Message(MessageType.MESSAGE, "assistant");
        updateMessageAttrs(message, metadata, null);
        
        AudioContent deltaContent = new AudioContent();
        deltaContent.setDelta(true);
        
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
        
        deltaContent = (AudioContent) message.addDeltaContent(deltaContent);
        results.add(deltaContent);
        results.add(message.completed());
    }
    
    /**
     * Process unknown block type (fallback to text).
     */
    private void processUnknownBlock(ContentBlock block, Map<String, Object> metadata, List<Event> results) {
        Message message = new Message(MessageType.MESSAGE, "assistant");
        updateMessageAttrs(message, metadata, null);
        
        TextContent deltaContent = new TextContent(true, null, block.toString());
        deltaContent = (TextContent) message.addDeltaContent(deltaContent);
        results.add(deltaContent);
        results.add(message.completed());
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
}

