package io.agentscope.runtime.adapters.agentscope;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.message.*;
import io.agentscope.runtime.adapters.MessageAdapter;
import io.agentscope.runtime.engine.helpers.ResponseBuilder;
import io.agentscope.runtime.engine.schemas.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Adapter for converting between AgentScope Java messages and runtime messages.
 * the actual AgentScope Java API types.
 */
public class AgentScopeMessageAdapter implements MessageAdapter {
    private static final Logger logger = LoggerFactory.getLogger(AgentScopeMessageAdapter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Convert AgentScope Msg(s) into one or more runtime Message objects.
     *
     * @param agentscopeMsg AgentScope Msg object or list of Msg objects
     * @return List of runtime Message objects
     */
    @Override
    public List<Message> frameworkMsgToMessage(Object agentscopeMsg) {
        List<Msg> msgs;

        // Handle single message or list
        if (agentscopeMsg instanceof Msg) {
            msgs = List.of((Msg) agentscopeMsg);
        } else if (agentscopeMsg instanceof List) {
            @SuppressWarnings("unchecked")
            List<Msg> msgList = (List<Msg>) agentscopeMsg;
            msgs = msgList;
        } else {
            throw new IllegalArgumentException(
                    "Expected Msg or List<Msg>, got " +
                            (agentscopeMsg != null ? agentscopeMsg.getClass().getName() : "null")
            );
        }

        List<Message> results = new ArrayList<>();

        for (Msg msg : msgs) {
            String role = msg.getRole() != null ? msg.getRole().name().toLowerCase() : "assistant";
            List<ContentBlock> content = msg.getContent();
            String msgId = msg.getId();
            String msgName = msg.getName();
            Map<String, Object> metadata = msg.getMetadata();

            // Handle empty content or string content (shouldn't happen with AgentScope Java API,
            // but handle for compatibility)
            if (content == null || content.isEmpty()) {
                continue;
            }

            // Check if content is a single text block (common case)
            if (content.size() == 1 && content.get(0) instanceof TextBlock) {
                TextBlock textBlock = (TextBlock) content.get(0);
                ResponseBuilder rb = new ResponseBuilder();
                ResponseBuilder.MessageBuilder mb = rb.createMessageBuilder(
                        role, MessageType.MESSAGE
                );

                // Add metadata
                Map<String, Object> msgMetadata = new HashMap<>();
                msgMetadata.put("original_id", msgId);
                msgMetadata.put("original_name", msgName);
                msgMetadata.put("metadata", metadata);
                mb.getMessageData().setMetadata(msgMetadata);

                ResponseBuilder.ContentBuilder cb = mb.createContentBuilder("text");
                cb.setText(textBlock.getText());
                cb.complete();
                mb.complete();
                results.add(mb.getMessageData());
                continue;
            }

            // Process list of blocks - group blocks by high-level message type
            ResponseBuilder.MessageBuilder currentMb = null;
            String currentType = null;

            for (ContentBlock block : content) {
                String btype = getBlockType(block);

                switch (btype) {
                case "text":
                    if (!MessageType.MESSAGE.equals(currentType)) {
                        if (currentMb != null) {
                            currentMb.complete();
                            results.add(currentMb.getMessageData());
                        }
                        ResponseBuilder rb = new ResponseBuilder();
                        currentMb = rb.createMessageBuilder(role, MessageType.MESSAGE);
                        Map<String, Object> msgMetadata = new HashMap<>();
                        msgMetadata.put("original_id", msgId);
                        msgMetadata.put("original_name", msgName);
                        msgMetadata.put("metadata", metadata);
                        currentMb.getMessageData().setMetadata(msgMetadata);
                        currentType = MessageType.MESSAGE;
                    }
                    ResponseBuilder.ContentBuilder cb = currentMb.createContentBuilder("text");
                    if (block instanceof TextBlock) {
                        cb.setText(((TextBlock) block).getText());
                    }
                    cb.complete();
                    break;

                case "thinking":
                    if (!MessageType.REASONING.equals(currentType)) {
                        if (currentMb != null) {
                            currentMb.complete();
                            results.add(currentMb.getMessageData());
                        }
                        ResponseBuilder rb = new ResponseBuilder();
                        currentMb = rb.createMessageBuilder(role, MessageType.REASONING);
                        Map<String, Object> msgMetadata = new HashMap<>();
                        msgMetadata.put("original_id", msgId);
                        msgMetadata.put("original_name", msgName);
                        msgMetadata.put("metadata", metadata);
                        currentMb.getMessageData().setMetadata(msgMetadata);
                        currentType = MessageType.REASONING;
                    }
                    ResponseBuilder.ContentBuilder cb2 = currentMb.createContentBuilder("text");
                    if (block instanceof ThinkingBlock) {
                        cb2.setText(((ThinkingBlock) block).getThinking());
                    }
                    cb2.complete();
                    break;

                case "tool_use":
                    if (currentMb != null) {
                        currentMb.complete();
                        results.add(currentMb.getMessageData());
                    }
                    ResponseBuilder rb = new ResponseBuilder();
                    currentMb = rb.createMessageBuilder(role, MessageType.PLUGIN_CALL);
                    Map<String, Object> msgMetadata = new HashMap<>();
                    msgMetadata.put("original_id", msgId);
                    msgMetadata.put("original_name", msgName);
                    msgMetadata.put("metadata", metadata);
                    currentMb.getMessageData().setMetadata(msgMetadata);
                    currentType = MessageType.PLUGIN_CALL;
                    ResponseBuilder.ContentBuilder cb3 = currentMb.createContentBuilder("data");

                    if (block instanceof ToolUseBlock) {
                        ToolUseBlock toolUseBlock = (ToolUseBlock) block;
                        Object input = toolUseBlock.getInput();
                        String arguments;
                        if (input instanceof Map || input instanceof List) {
                            try {
                                arguments = objectMapper.writeValueAsString(input);
                            } catch (JsonProcessingException e) {
                                arguments = String.valueOf(input);
                            }
                        } else {
                            arguments = String.valueOf(input);
                        }

                        FunctionCall callData = new FunctionCall(
                                toolUseBlock.getId(),
                                toolUseBlock.getName(),
                                arguments
                        );
                        Map<String, Object> callMap = new HashMap<>();
                        callMap.put("call_id", callData.getCallId());
                        callMap.put("name", callData.getName());
                        callMap.put("arguments", callData.getArguments());
                        cb3.setData(callMap);
                    }
                    cb3.complete();
                    break;

                case "tool_result":
                    if (currentMb != null) {
                        currentMb.complete();
                        results.add(currentMb.getMessageData());
                    }
                    ResponseBuilder rb2 = new ResponseBuilder();
                    currentMb = rb2.createMessageBuilder(role, MessageType.PLUGIN_CALL_OUTPUT);
                    Map<String, Object> msgMetadata2 = new HashMap<>();
                    msgMetadata2.put("original_id", msgId);
                    msgMetadata2.put("original_name", msgName);
                    msgMetadata2.put("metadata", metadata);
                    currentMb.getMessageData().setMetadata(msgMetadata2);
                    currentType = MessageType.PLUGIN_CALL_OUTPUT;
                    ResponseBuilder.ContentBuilder cb4 = currentMb.createContentBuilder("data");

                    if (block instanceof ToolResultBlock) {
                        ToolResultBlock toolResultBlock = (ToolResultBlock) block;
                        List<ContentBlock> outputBlocks = toolResultBlock.getOutput();
                        String outputStr;
                        if (outputBlocks == null || outputBlocks.isEmpty()) {
                            outputStr = "null";
                        } else {
                            // Convert ContentBlock list to JSON-serializable format
                            try {
                                List<Map<String, Object>> outputList = new ArrayList<>();
                                for (ContentBlock outputBlock : outputBlocks) {
                                    outputList.add(convertContentBlockToMap(outputBlock));
                                }
                                outputStr = objectMapper.writeValueAsString(outputList);
                            } catch (JsonProcessingException e) {
                                outputStr = String.valueOf(outputBlocks);
                            }
                        }

                        FunctionCallOutput outputData = new FunctionCallOutput(
                                toolResultBlock.getId(),
                                toolResultBlock.getName(),
                                outputStr
                        );
                        Map<String, Object> outputMap = new HashMap<>();
                        outputMap.put("call_id", outputData.getCallId());
                        if (outputData.getName() != null) {
                            outputMap.put("name", outputData.getName());
                        }
                        outputMap.put("output", outputData.getOutput());
                        cb4.setData(outputMap);
                    }
                    cb4.complete();
                    break;

                case "image":
                    if (!MessageType.MESSAGE.equals(currentType)) {
                        if (currentMb != null) {
                            currentMb.complete();
                            results.add(currentMb.getMessageData());
                        }
                        ResponseBuilder rb3 = new ResponseBuilder();
                        currentMb = rb3.createMessageBuilder(role, MessageType.MESSAGE);
                        Map<String, Object> msgMetadata3 = new HashMap<>();
                        msgMetadata3.put("original_id", msgId);
                        msgMetadata3.put("original_name", msgName);
                        msgMetadata3.put("metadata", metadata);
                        currentMb.getMessageData().setMetadata(msgMetadata3);
                        currentType = MessageType.MESSAGE;
                    }
                    ResponseBuilder.ContentBuilder cb5 = currentMb.createContentBuilder("image");

                    if (block instanceof ImageBlock) {
                        ImageBlock imageBlock = (ImageBlock) block;
                        Source source = imageBlock.getSource();
                        if (source instanceof URLSource) {
                            cb5.setImageUrl(((URLSource) source).getUrl());
                        } else if (source instanceof Base64Source) {
                            Base64Source base64Source = (Base64Source) source;
                            String mediaType = base64Source.getMediaType();
                            if (mediaType == null || mediaType.isEmpty()) {
                                mediaType = "image/jpeg";
                            }
                            String base64Data = base64Source.getData();
                            String url = "data:" + mediaType + ";base64," + base64Data;
                            cb5.setImageUrl(url);
                        }
                    }
                    cb5.complete();
                    break;

                case "audio":
                    if (!MessageType.MESSAGE.equals(currentType)) {
                        if (currentMb != null) {
                            currentMb.complete();
                            results.add(currentMb.getMessageData());
                        }
                        ResponseBuilder rb4 = new ResponseBuilder();
                        currentMb = rb4.createMessageBuilder(role, MessageType.MESSAGE);
                        Map<String, Object> msgMetadata4 = new HashMap<>();
                        msgMetadata4.put("original_id", msgId);
                        msgMetadata4.put("original_name", msgName);
                        msgMetadata4.put("metadata", metadata);
                        currentMb.getMessageData().setMetadata(msgMetadata4);
                        currentType = MessageType.MESSAGE;
                    }
                    ResponseBuilder.ContentBuilder cb6 = currentMb.createContentBuilder("audio");

                    if (block instanceof AudioBlock) {
                        AudioBlock audioBlock = (AudioBlock) block;
                        Source source = audioBlock.getSource();
                        AudioContent audioContent = (AudioContent) cb6.getContentData();

                        if (source instanceof URLSource) {
                            String url = ((URLSource) source).getUrl();
                            audioContent.setData(url);
                            try {
                                URI uri = new URI(url);
                                String path = uri.getPath();
                                if (path != null && path.contains(".")) {
                                    String[] parts = path.split("\\.");
                                    audioContent.setFormat(parts[parts.length - 1]);
                                }
                            } catch (URISyntaxException e) {
                                // Ignore
                            }
                        } else if (source instanceof Base64Source) {
                            Base64Source base64Source = (Base64Source) source;
                            String mediaType = base64Source.getMediaType();
                            String base64Data = base64Source.getData();
                            String url = "data:" + mediaType + ";base64," + base64Data;
                            audioContent.setData(url);
                            audioContent.setFormat(mediaType);
                        }
                    }
                    cb6.complete();
                    break;

                default:
                    // Fallback to MESSAGE type
                    if (!MessageType.MESSAGE.equals(currentType)) {
                        if (currentMb != null) {
                            currentMb.complete();
                            results.add(currentMb.getMessageData());
                        }
                        ResponseBuilder rb5 = new ResponseBuilder();
                        currentMb = rb5.createMessageBuilder(role, MessageType.MESSAGE);
                        Map<String, Object> msgMetadata5 = new HashMap<>();
                        msgMetadata5.put("original_id", msgId);
                        msgMetadata5.put("original_name", msgName);
                        msgMetadata5.put("metadata", metadata);
                        currentMb.getMessageData().setMetadata(msgMetadata5);
                        currentType = MessageType.MESSAGE;
                    }
                    ResponseBuilder.ContentBuilder cb7 = currentMb.createContentBuilder("text");
                    cb7.setText(String.valueOf(block));
                    cb7.complete();
                    break;
                }
            }

            // Finalize last open message builder
            if (currentMb != null) {
                currentMb.complete();
                results.add(currentMb.getMessageData());
            }
        }

        return results;
    }

    /**
     * Convert runtime Message(s) to AgentScope Msg(s).
     *
     * @param messages Runtime Message(s) - single Message or List<Message>
     * @return AgentScope Msg object(s) - single Msg or List<Msg>
     */
    @Override
    public Object messageToFrameworkMsg(Object messages) {
        List<Message> msgList;

        // Handle single message or list
        if (messages instanceof Message) {
            msgList = List.of((Message) messages);
        } else if (messages instanceof List) {
            @SuppressWarnings("unchecked")
            List<Message> messageList = (List<Message>) messages;
            msgList = messageList;
        } else {
            throw new IllegalArgumentException(
                    "Expected Message or List<Message>, got " +
                            (messages != null ? messages.getClass().getName() : "null")
            );
        }

        List<Msg> convertedList = new ArrayList<>();
        for (Message message : msgList) {
            convertedList.add(convertOneMessage(message));
        }

        // Group by original_id
        Map<String, Msg> grouped = new LinkedHashMap<>();
        for (int i = 0; i < msgList.size() && i < convertedList.size(); i++) {
            Message msg = msgList.get(i);
            Msg origMsg = convertedList.get(i);

            String origId = msg.getId();
            Map<String, Object> metadata = msg.getMetadata();
            if (metadata != null && metadata.containsKey("original_id")) {
                origId = (String) metadata.get("original_id");
            }

            if (!grouped.containsKey(origId)) {
                Msg.Builder builder = Msg.builder()
                        .id(origId)
                        .name(origMsg.getName())
                        .role(origMsg.getRole())
                        .metadata(origMsg.getMetadata());

                // Copy content
                List<ContentBlock> content = new ArrayList<>(origMsg.getContent());
                builder.content(content);

                grouped.put(origId, builder.build());
            } else {
                // Extend content
                Msg existing = grouped.get(origId);
                List<ContentBlock> newContent = new ArrayList<>(existing.getContent());
                newContent.addAll(origMsg.getContent());

                Msg updated = Msg.builder()
                        .id(existing.getId())
                        .name(existing.getName())
                        .role(existing.getRole())
                        .metadata(existing.getMetadata())
                        .content(newContent)
                        .build();
                grouped.put(origId, updated);
            }
        }

        if (msgList.size() == 1) {
            return grouped.values().iterator().next();
        }
        return new ArrayList<>(grouped.values());
    }

    /**
     * Convert a single runtime Message to AgentScope Msg.
     */
    private Msg convertOneMessage(Message message) {
        // Normalize role
        String roleLabel;
        if (Role.TOOL.equals(message.getRole())) {
            roleLabel = "system"; // AgentScope Java doesn't support tool as role
        } else {
            roleLabel = message.getRole() != null ? message.getRole() : "assistant";
        }

        MsgRole msgRole = MsgRole.valueOf(roleLabel.toUpperCase());
        String name = message.getMetadata() != null &&
                message.getMetadata().containsKey("original_name") ?
                (String) message.getMetadata().get("original_name") :
                message.getRole();

        String id = message.getId();
        if (message.getMetadata() != null && message.getMetadata().containsKey("original_id")) {
            id = (String) message.getMetadata().get("original_id");
        }

        Map<String, Object> metadata = message.getMetadata();
        if (metadata != null && metadata.containsKey("metadata")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> innerMetadata = (Map<String, Object>) metadata.get("metadata");
            metadata = innerMetadata;
        }

        List<ContentBlock> contentBlocks = new ArrayList<>();

        // Handle different message types
        if (MessageType.PLUGIN_CALL.equals(message.getType()) ||
                MessageType.FUNCTION_CALL.equals(message.getType())) {
            // Convert to ToolUseBlock
            if (message.getContent() != null && !message.getContent().isEmpty()) {
                Content content = message.getContent().get(0);
                if (content instanceof DataContent) {
                    DataContent dataContent = (DataContent) content;
                    Map<String, Object> data = dataContent.getData();
                    if (data != null) {
                        String callId = (String) data.get("call_id");
                        String toolName = (String) data.get("name");
                        String argumentsStr = (String) data.get("arguments");

                        Map<String, Object> input;
                        try {
                            input = objectMapper.readValue(argumentsStr, Map.class);
                        } catch (JsonProcessingException e) {
                            input = Map.of("raw", argumentsStr);
                        }

                        contentBlocks.add(new ToolUseBlock(callId, toolName, input));
                    }
                }
            }
        } else if (MessageType.PLUGIN_CALL_OUTPUT.equals(message.getType()) ||
                MessageType.FUNCTION_CALL_OUTPUT.equals(message.getType())) {
            // Convert to ToolResultBlock
            if (message.getContent() != null && !message.getContent().isEmpty()) {
                Content content = message.getContent().get(0);
                if (content instanceof DataContent) {
                    DataContent dataContent = (DataContent) content;
                    Map<String, Object> data = dataContent.getData();
                    if (data != null) {
                        String callId = (String) data.get("call_id");
                        String toolName = (String) data.get("name");
                        String outputStr = (String) data.get("output");

                        // Parse output - match Python's logic with is_valid_block check
                        Object output;
                        try {
                            output = objectMapper.readValue(outputStr, Object.class);
                        } catch (JsonProcessingException e) {
                            // If parsing fails, use the string directly
                            output = outputStr;
                        }

                        List<ContentBlock> outputBlocks;
                        if (output instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Object> outputList = (List<Object>) output;
                            // Check if all items are valid blocks
                            boolean allValid = outputList.stream()
                                    .allMatch(this::isValidBlock);
                            if (allValid) {
                                outputBlocks = outputList.stream()
                                        .map(item -> mapToContentBlock((Map<String, Object>) item))
                                        .filter(Objects::nonNull)
                                        .collect(Collectors.toList());
                            } else {
                                // Not all valid blocks, use string output
                                outputBlocks = List.of(TextBlock.builder().text(outputStr).build());
                            }
                        } else if (output instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> outputMap = (Map<String, Object>) output;
                            if (isValidBlock(outputMap)) {
                                ContentBlock block = mapToContentBlock(outputMap);
                                outputBlocks = block != null ? List.of(block) : 
                                    List.of(TextBlock.builder().text(outputStr).build());
                            } else {
                                // Not a valid block, use string output
                                outputBlocks = List.of(TextBlock.builder().text(outputStr).build());
                            }
                        } else {
                            // Not dict or list, use string output
                            outputBlocks = List.of(TextBlock.builder().text(outputStr).build());
                        }

                        contentBlocks.add(ToolResultBlock.of(callId, toolName, outputBlocks));
                    }
                }
            }
        } else if (MessageType.REASONING.equals(message.getType())) {
            // Convert to ThinkingBlock
            if (message.getContent() != null && !message.getContent().isEmpty()) {
                Content content = message.getContent().get(0);
                if (content instanceof TextContent) {
                    contentBlocks.add(ThinkingBlock.builder()
                            .thinking(((TextContent) content).getText())
                            .build());
                }
            }
        } else {
            // Convert content blocks
            if (message.getContent() != null) {
                for (Content cnt : message.getContent()) {
                    ContentBlock block = convertContentToBlock(cnt);
                    if (block != null) {
                        contentBlocks.add(block);
                    }
                }
            }
        }

        return Msg.builder()
                .id(id)
                .name(name)
                .role(msgRole)
                .content(contentBlocks)
                .metadata(metadata != null ? metadata : Map.of())
                .build();
    }

    /**
     * Convert runtime Content to AgentScope ContentBlock.
     */
    private ContentBlock convertContentToBlock(Content content) {
        String contentType = content.getType();

        switch (contentType) {
        case "text":
            if (content instanceof TextContent) {
                return TextBlock.builder()
                        .text(((TextContent) content).getText())
                        .build();
            }
            break;

        case "image":
            if (content instanceof ImageContent) {
                String imageUrl = ((ImageContent) content).getImageUrl();
                if (imageUrl != null) {
                    if (imageUrl.startsWith("data:")) {
                        // Parse data URI
                        String[] parts = imageUrl.split(";");
                        if (parts.length >= 2) {
                            String mediaType = parts[0].replace("data:", "");
                            String base64Data = parts[1].substring(parts[1].indexOf(",") + 1);
                            return ImageBlock.builder()
                                    .source(Base64Source.builder()
                                            .mediaType(mediaType)
                                            .data(base64Data)
                                            .build())
                                    .build();
                        }
                    } else {
                        return ImageBlock.builder()
                                .source(URLSource.builder().url(imageUrl).build())
                                .build();
                    }
                }
            }
            break;

        case "audio":
            if (content instanceof AudioContent) {
                AudioContent audioContent = (AudioContent) content;
                String data = audioContent.getData();
                if (data != null) {
                    if (data.startsWith("data:")) {
                        String[] parts = data.split(";");
                        if (parts.length >= 2) {
                            String mediaType = parts[0].replace("data:", "");
                            String base64Data = parts[1].substring(parts[1].indexOf(",") + 1);
                            return AudioBlock.builder()
                                    .source(Base64Source.builder()
                                            .mediaType(mediaType)
                                            .data(base64Data)
                                            .build())
                                    .build();
                        }
                    } else {
                        try {
                            URI uri = new URI(data);
                            if (uri.getScheme() != null && uri.getHost() != null) {
                                // Valid URL
                                return AudioBlock.builder()
                                        .source(URLSource.builder().url(data).build())
                                        .build();
                            } else {
                                // Not a valid URL, treat as base64
                                String format = audioContent.getFormat();
                                return AudioBlock.builder()
                                        .source(Base64Source.builder()
                                                .mediaType(format != null ? "audio/" + format : "audio/mpeg")
                                                .data(data)
                                                .build())
                                        .build();
                            }
                        } catch (URISyntaxException e) {
                            // Not a valid URL, treat as base64
                            String format = audioContent.getFormat();
                            return AudioBlock.builder()
                                    .source(Base64Source.builder()
                                            .mediaType(format != null ? "audio/" + format : "audio/mpeg")
                                            .data(data)
                                            .build())
                                    .build();
                        }
                    }
                }
            }
            break;
        }

        return null;
    }

    /**
     * Check if an object matches a valid ContentBlock structure.
     * Matches Python's matches_typed_dict_structure logic.
     */
    private boolean isValidBlock(Object obj) {
        if (!(obj instanceof Map)) {
            return false;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) obj;
        String type = (String) map.getOrDefault("type", "text");
        
        // Check required fields for each block type
        switch (type) {
        case "text":
            return map.containsKey("text");
        case "thinking":
            return map.containsKey("thinking");
        case "image":
            return map.containsKey("source");
        case "audio":
            return map.containsKey("source");
        case "video":
            return map.containsKey("source");
        default:
            return false;
        }
    }

    /**
     * Convert Map to ContentBlock (for tool result output parsing).
     * Converting dict to ContentBlock.
     */
    private ContentBlock mapToContentBlock(Map<String, Object> map) {
        String type = (String) map.getOrDefault("type", "text");
        switch (type) {
        case "text":
            return TextBlock.builder()
                    .text((String) map.getOrDefault("text", ""))
                    .build();
        case "thinking":
            return ThinkingBlock.builder()
                    .thinking((String) map.getOrDefault("thinking", ""))
                    .build();
        case "image":
            @SuppressWarnings("unchecked")
            Map<String, Object> sourceMap = (Map<String, Object>) map.get("source");
            if (sourceMap != null) {
                String sourceType = (String) sourceMap.getOrDefault("type", "url");
                if ("url".equals(sourceType)) {
                    return ImageBlock.builder()
                            .source(URLSource.builder()
                                    .url((String) sourceMap.get("url"))
                                    .build())
                            .build();
                } else if ("base64".equals(sourceType)) {
                    return ImageBlock.builder()
                            .source(Base64Source.builder()
                                    .mediaType((String) sourceMap.get("media_type"))
                                    .data((String) sourceMap.get("data"))
                                    .build())
                            .build();
                }
            }
            return null;
        case "audio":
            @SuppressWarnings("unchecked")
            Map<String, Object> audioSourceMap = (Map<String, Object>) map.get("source");
            if (audioSourceMap != null) {
                String sourceType = (String) audioSourceMap.getOrDefault("type", "url");
                if ("url".equals(sourceType)) {
                    return AudioBlock.builder()
                            .source(URLSource.builder()
                                    .url((String) audioSourceMap.get("url"))
                                    .build())
                            .build();
                } else if ("base64".equals(sourceType)) {
                    return AudioBlock.builder()
                            .source(Base64Source.builder()
                                    .mediaType((String) audioSourceMap.get("media_type"))
                                    .data((String) audioSourceMap.get("data"))
                                    .build())
                            .build();
                }
            }
            return null;
        default:
            // Fallback to text
            return TextBlock.builder().text(String.valueOf(map)).build();
        }
    }

    /**
     * Get block type string from ContentBlock.
     */
    private String getBlockType(ContentBlock block) {
        if (block instanceof TextBlock) {
            return "text";
        } else if (block instanceof ThinkingBlock) {
            return "thinking";
        } else if (block instanceof ToolUseBlock) {
            return "tool_use";
        } else if (block instanceof ToolResultBlock) {
            return "tool_result";
        } else if (block instanceof ImageBlock) {
            return "image";
        } else if (block instanceof AudioBlock) {
            return "audio";
        } else if (block instanceof VideoBlock) {
            return "video";
        }
        return "text";
    }

    /**
     * Convert ContentBlock to Map for JSON serialization.
     */
    private Map<String, Object> convertContentBlockToMap(ContentBlock block) {
        Map<String, Object> map = new HashMap<>();

        if (block instanceof TextBlock) {
            map.put("type", "text");
            map.put("text", ((TextBlock) block).getText());
        } else if (block instanceof ThinkingBlock) {
            map.put("type", "thinking");
            map.put("thinking", ((ThinkingBlock) block).getThinking());
        } else if (block instanceof ImageBlock) {
            map.put("type", "image");
            ImageBlock imageBlock = (ImageBlock) block;
            Source source = imageBlock.getSource();
            if (source instanceof URLSource) {
                map.put("source", Map.of(
                        "type", "url",
                        "url", ((URLSource) source).getUrl()
                ));
            } else if (source instanceof Base64Source) {
                Base64Source base64Source = (Base64Source) source;
                map.put("source", Map.of(
                        "type", "base64",
                        "media_type", base64Source.getMediaType(),
                        "data", base64Source.getData()
                ));
            }
        } else if (block instanceof AudioBlock) {
            map.put("type", "audio");
            AudioBlock audioBlock = (AudioBlock) block;
            Source source = audioBlock.getSource();
            if (source instanceof URLSource) {
                map.put("source", Map.of(
                        "type", "url",
                        "url", ((URLSource) source).getUrl()
                ));
            } else if (source instanceof Base64Source) {
                Base64Source base64Source = (Base64Source) source;
                map.put("source", Map.of(
                        "type", "base64",
                        "media_type", base64Source.getMediaType(),
                        "data", base64Source.getData()
                ));
            }
        }

        return map;
    }
}
