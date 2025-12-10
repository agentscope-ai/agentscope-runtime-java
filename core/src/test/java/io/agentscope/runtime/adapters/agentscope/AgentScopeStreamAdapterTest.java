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

import io.agentscope.core.agent.EventType;
import io.agentscope.core.message.*;
import io.agentscope.runtime.engine.schemas.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AgentScopeStreamAdapter.
 * 
 * <p>Tests are based on the actual implementation logic:
 * <ul>
 *   <li>TextBlock and ThinkingBlock: only processed when !isLast, returns TextContent (delta=true)</li>
 *   <li>ToolUseBlock: always processed, returns completed Message (MCP_TOOL_CALL)</li>
 *   <li>ToolResultBlock: always processed, returns completed Message (MCP_APPROVAL_RESPONSE)</li>
 *   <li>ImageBlock and AudioBlock: only processed when !isLast, returns Content and completed Message</li>
 * </ul>
 */
class AgentScopeStreamAdapterTest {

    private AgentScopeStreamAdapter adapter;
    private EventType messageEventType;

    @BeforeEach
    void setUp() {
        adapter = new AgentScopeStreamAdapter();
        messageEventType = getDefaultEventType();
    }

    @Test
    void testAdaptFrameworkStreamWithTextBlockNotLast() {
        // TextBlock is only processed when !isLast
        Msg msg = Msg.builder()
                .id("msg-1")
                .role(MsgRole.ASSISTANT)
                .content(List.of(TextBlock.builder().text("Hello").build()))
                .build();

        io.agentscope.core.agent.Event event = new io.agentscope.core.agent.Event(
                messageEventType != null ? messageEventType : getDefaultEventType(), msg, false
        );

        Flux<io.agentscope.core.agent.Event> sourceStream = Flux.just(event);
        Flux<io.agentscope.runtime.engine.schemas.Event> result = adapter.adaptFrameworkStream(sourceStream);

        List<io.agentscope.runtime.engine.schemas.Event> events = result.collectList().block();
        assertNotNull(events);
        assertEquals(1, events.size());
        
        // Should return TextContent with delta=true
        assertTrue(events.get(0) instanceof TextContent);
        TextContent textContent = (TextContent) events.get(0);
        assertTrue(textContent.getDelta());
        assertEquals("Hello", textContent.getText());
    }

    @Test
    void testAdaptFrameworkStreamWithTextBlockIsLast() {
        // TextBlock is NOT processed when isLast=true (unless it's ToolUseBlock or ToolResultBlock)
        Msg msg = Msg.builder()
                .id("msg-1")
                .role(MsgRole.ASSISTANT)
                .content(List.of(TextBlock.builder().text("Hello").build()))
                .build();

        io.agentscope.core.agent.Event event = new io.agentscope.core.agent.Event(
                messageEventType != null ? messageEventType : getDefaultEventType(), msg, true
        );

        Flux<io.agentscope.core.agent.Event> sourceStream = Flux.just(event);
        Flux<io.agentscope.runtime.engine.schemas.Event> result = adapter.adaptFrameworkStream(sourceStream);

        List<io.agentscope.runtime.engine.schemas.Event> events = result.collectList().block();
        assertNotNull(events);
        assertTrue(events.isEmpty());
    }

    @Test
    void testAdaptFrameworkStreamWithIncrementalText() {
        // First event with partial text (not last)
        Msg msg1 = Msg.builder()
                .id("msg-1")
                .role(MsgRole.ASSISTANT)
                .content(List.of(TextBlock.builder().text("Hello").build()))
                .build();

        io.agentscope.core.agent.Event event1 = new io.agentscope.core.agent.Event(
                messageEventType != null ? messageEventType : getDefaultEventType(), msg1, false
        );

        // Second event with complete text (not last)
        Msg msg2 = Msg.builder()
                .id("msg-1")
                .role(MsgRole.ASSISTANT)
                .content(List.of(TextBlock.builder().text("Hello, world!").build()))
                .build();

        io.agentscope.core.agent.Event event2 = new io.agentscope.core.agent.Event(
                messageEventType != null ? messageEventType : getDefaultEventType(), msg2, false
        );

        Flux<io.agentscope.core.agent.Event> sourceStream = Flux.just(event1, event2);
        Flux<io.agentscope.runtime.engine.schemas.Event> result = adapter.adaptFrameworkStream(sourceStream);

        List<io.agentscope.runtime.engine.schemas.Event> events = result.collectList().block();
        assertNotNull(events);
        assertEquals(2, events.size());
        
        // Both should be TextContent
        assertTrue(events.get(0) instanceof TextContent);
        assertTrue(events.get(1) instanceof TextContent);
    }

    @Test
    void testAdaptFrameworkStreamWithThinkingBlockNotLast() {
        // ThinkingBlock is only processed when !isLast
        Msg msg = Msg.builder()
                .id("msg-1")
                .role(MsgRole.ASSISTANT)
                .content(List.of(ThinkingBlock.builder().thinking("Let me think...").build()))
                .build();

        io.agentscope.core.agent.Event event = new io.agentscope.core.agent.Event(
                messageEventType != null ? messageEventType : getDefaultEventType(), msg, false
        );

        Flux<io.agentscope.core.agent.Event> sourceStream = Flux.just(event);
        Flux<io.agentscope.runtime.engine.schemas.Event> result = adapter.adaptFrameworkStream(sourceStream);

        List<io.agentscope.runtime.engine.schemas.Event> events = result.collectList().block();
        assertNotNull(events);
        assertEquals(1, events.size());
        
        // Should return TextContent with delta=true
        assertTrue(events.get(0) instanceof TextContent);
        TextContent textContent = (TextContent) events.get(0);
        assertTrue(textContent.getDelta());
        assertEquals("Let me think...", textContent.getText());
    }

    @Test
    void testAdaptFrameworkStreamWithThinkingBlockIsLast() {
        // ThinkingBlock is NOT processed when isLast=true
        Msg msg = Msg.builder()
                .id("msg-1")
                .role(MsgRole.ASSISTANT)
                .content(List.of(ThinkingBlock.builder().thinking("Let me think...").build()))
                .build();

        io.agentscope.core.agent.Event event = new io.agentscope.core.agent.Event(
                messageEventType != null ? messageEventType : getDefaultEventType(), msg, true
        );

        Flux<io.agentscope.core.agent.Event> sourceStream = Flux.just(event);
        Flux<io.agentscope.runtime.engine.schemas.Event> result = adapter.adaptFrameworkStream(sourceStream);

        List<io.agentscope.runtime.engine.schemas.Event> events = result.collectList().block();
        assertNotNull(events);
        assertTrue(events.isEmpty());
    }

    @Test
    void testAdaptFrameworkStreamWithToolUseBlock() {
        // ToolUseBlock is always processed regardless of isLast
        Map<String, Object> input = Map.of("query", "test");
        ToolUseBlock toolUse = new ToolUseBlock("call-1", "test_tool", input);

        Msg msg = Msg.builder()
                .id("msg-1")
                .role(MsgRole.ASSISTANT)
                .content(List.of(toolUse))
                .build();

        io.agentscope.core.agent.Event event = new io.agentscope.core.agent.Event(
                messageEventType != null ? messageEventType : getDefaultEventType(), msg, true
        );

        Flux<io.agentscope.core.agent.Event> sourceStream = Flux.just(event);
        Flux<io.agentscope.runtime.engine.schemas.Event> result = adapter.adaptFrameworkStream(sourceStream);

        List<io.agentscope.runtime.engine.schemas.Event> events = result.collectList().block();
        assertNotNull(events);
        assertEquals(1, events.size());
        
        // Should return completed Message with MCP_TOOL_CALL type
        assertTrue(events.get(0) instanceof Message);
        Message message = (Message) events.get(0);
        assertEquals(MessageType.MCP_TOOL_CALL, message.getType());
        assertEquals(RunStatus.COMPLETED, message.getStatus());
        assertEquals("assistant", message.getRole());
        
        // Verify content contains function call data
        assertNotNull(message.getContent());
        assertFalse(message.getContent().isEmpty());
        assertTrue(message.getContent().get(0) instanceof DataContent);
        DataContent dataContent = (DataContent) message.getContent().get(0);
        assertNotNull(dataContent.getData());
        Map<String, Object> data = dataContent.getData();
        assertEquals("call-1", data.get("call_id"));
        assertEquals("test_tool", data.get("name"));
    }

    @Test
    void testAdaptFrameworkStreamWithToolResultBlock() {
        // ToolResultBlock is always processed regardless of isLast
        ToolResultBlock toolResult = ToolResultBlock.of(
                "call-1",
                "test_tool",
                List.of(TextBlock.builder().text("result").build())
        );
        
        Msg msg = Msg.builder()
                .id("msg-1")
                .role(MsgRole.TOOL)
                .content(List.of(toolResult))
                .build();

        io.agentscope.core.agent.Event event = new io.agentscope.core.agent.Event(
                messageEventType != null ? messageEventType : getDefaultEventType(), msg, true
        );

        Flux<io.agentscope.core.agent.Event> sourceStream = Flux.just(event);
        Flux<io.agentscope.runtime.engine.schemas.Event> result = adapter.adaptFrameworkStream(sourceStream);

        List<io.agentscope.runtime.engine.schemas.Event> events = result.collectList().block();
        assertNotNull(events);
        assertEquals(1, events.size());
        
        // Should return completed Message with MCP_APPROVAL_RESPONSE type
        assertTrue(events.get(0) instanceof Message);
        Message message = (Message) events.get(0);
        assertEquals(MessageType.MCP_APPROVAL_RESPONSE, message.getType());
        assertEquals(RunStatus.COMPLETED, message.getStatus());
        assertEquals("tool", message.getRole());
        
        // Verify content contains function call output data
        assertNotNull(message.getContent());
        assertFalse(message.getContent().isEmpty());
        assertTrue(message.getContent().get(0) instanceof DataContent);
        DataContent dataContent = (DataContent) message.getContent().get(0);
        assertNotNull(dataContent.getData());
        Map<String, Object> data = dataContent.getData();
        assertEquals("call-1", data.get("call_id"));
        assertEquals("test_tool", data.get("name"));
        assertNotNull(data.get("output"));
    }

    @Test
    void testAdaptFrameworkStreamWithImageBlockNotLast() {
        // ImageBlock is only processed when !isLast
        ImageBlock imageBlock = ImageBlock.builder()
                .source(URLSource.builder().url("https://example.com/image.jpg").build())
                .build();

        Msg msg = Msg.builder()
                .id("msg-1")
                .role(MsgRole.USER)
                .content(List.of(imageBlock))
                .build();

        io.agentscope.core.agent.Event event = new io.agentscope.core.agent.Event(
                messageEventType != null ? messageEventType : getDefaultEventType(), msg, false
        );

        Flux<io.agentscope.core.agent.Event> sourceStream = Flux.just(event);
        Flux<io.agentscope.runtime.engine.schemas.Event> result = adapter.adaptFrameworkStream(sourceStream);

        List<io.agentscope.runtime.engine.schemas.Event> events = result.collectList().block();
        assertNotNull(events);
        assertEquals(2, events.size());
        
        // First event should be ImageContent
        assertTrue(events.get(0) instanceof ImageContent);
        ImageContent imageContent = (ImageContent) events.get(0);
        assertTrue(imageContent.getDelta());
        assertEquals("https://example.com/image.jpg", imageContent.getImageUrl());
        
        // Second event should be completed Message
        assertTrue(events.get(1) instanceof Message);
        Message message = (Message) events.get(1);
        assertEquals(MessageType.MESSAGE, message.getType());
        assertEquals(RunStatus.COMPLETED, message.getStatus());
    }

    @Test
    void testAdaptFrameworkStreamWithImageBlockIsLast() {
        // ImageBlock is NOT processed when isLast=true
        ImageBlock imageBlock = ImageBlock.builder()
                .source(URLSource.builder().url("https://example.com/image.jpg").build())
                .build();

        Msg msg = Msg.builder()
                .id("msg-1")
                .role(MsgRole.USER)
                .content(List.of(imageBlock))
                .build();

        io.agentscope.core.agent.Event event = new io.agentscope.core.agent.Event(
                messageEventType != null ? messageEventType : getDefaultEventType(), msg, true
        );

        Flux<io.agentscope.core.agent.Event> sourceStream = Flux.just(event);
        Flux<io.agentscope.runtime.engine.schemas.Event> result = adapter.adaptFrameworkStream(sourceStream);

        List<io.agentscope.runtime.engine.schemas.Event> events = result.collectList().block();
        assertNotNull(events);
        assertTrue(events.isEmpty());
    }

    @Test
    void testAdaptFrameworkStreamWithBase64Image() {
        // Base64 image should be converted to data URI
        ImageBlock imageBlock = ImageBlock.builder()
                .source(Base64Source.builder()
                        .mediaType("image/jpeg")
                        .data("base64data")
                        .build())
                .build();

        Msg msg = Msg.builder()
                .id("msg-1")
                .role(MsgRole.USER)
                .content(List.of(imageBlock))
                .build();

        io.agentscope.core.agent.Event event = new io.agentscope.core.agent.Event(
                messageEventType != null ? messageEventType : getDefaultEventType(), msg, false
        );

        Flux<io.agentscope.core.agent.Event> sourceStream = Flux.just(event);
        Flux<io.agentscope.runtime.engine.schemas.Event> result = adapter.adaptFrameworkStream(sourceStream);

        List<io.agentscope.runtime.engine.schemas.Event> events = result.collectList().block();
        assertNotNull(events);
        assertEquals(2, events.size());
        
        // Should contain image content with base64 data URI
        assertTrue(events.get(0) instanceof ImageContent);
        ImageContent imageContent = (ImageContent) events.get(0);
        assertNotNull(imageContent.getImageUrl());
        assertTrue(imageContent.getImageUrl().startsWith("data:image/jpeg;base64,"));
        assertEquals("data:image/jpeg;base64,base64data", imageContent.getImageUrl());
    }

    @Test
    void testAdaptFrameworkStreamWithAudioBlockNotLast() {
        // AudioBlock is only processed when !isLast
        AudioBlock audioBlock = AudioBlock.builder()
                .source(URLSource.builder().url("https://example.com/audio.mp3").build())
                .build();

        Msg msg = Msg.builder()
                .id("msg-1")
                .role(MsgRole.USER)
                .content(List.of(audioBlock))
                .build();

        io.agentscope.core.agent.Event event = new io.agentscope.core.agent.Event(
                messageEventType != null ? messageEventType : getDefaultEventType(), msg, false
        );

        Flux<io.agentscope.core.agent.Event> sourceStream = Flux.just(event);
        Flux<io.agentscope.runtime.engine.schemas.Event> result = adapter.adaptFrameworkStream(sourceStream);

        List<io.agentscope.runtime.engine.schemas.Event> events = result.collectList().block();
        assertNotNull(events);
        assertEquals(2, events.size());
        
        // First event should be AudioContent
        assertTrue(events.get(0) instanceof AudioContent);
        AudioContent audioContent = (AudioContent) events.get(0);
        assertTrue(audioContent.getDelta());
        assertEquals("https://example.com/audio.mp3", audioContent.getData());
        
        // Second event should be completed Message
        assertTrue(events.get(1) instanceof Message);
        Message message = (Message) events.get(1);
        assertEquals(MessageType.MESSAGE, message.getType());
        assertEquals(RunStatus.COMPLETED, message.getStatus());
    }

    @Test
    void testAdaptFrameworkStreamWithBase64Audio() {
        // Base64 audio should be converted to data URI
        AudioBlock audioBlock = AudioBlock.builder()
                .source(Base64Source.builder()
                        .mediaType("audio/mpeg")
                        .data("base64audiodata")
                        .build())
                .build();

        Msg msg = Msg.builder()
                .id("msg-1")
                .role(MsgRole.USER)
                .content(List.of(audioBlock))
                .build();

        io.agentscope.core.agent.Event event = new io.agentscope.core.agent.Event(
                messageEventType != null ? messageEventType : getDefaultEventType(), msg, false
        );

        Flux<io.agentscope.core.agent.Event> sourceStream = Flux.just(event);
        Flux<io.agentscope.runtime.engine.schemas.Event> result = adapter.adaptFrameworkStream(sourceStream);

        List<io.agentscope.runtime.engine.schemas.Event> events = result.collectList().block();
        assertNotNull(events);
        assertEquals(2, events.size());
        
        // Should contain audio content with base64 data URI
        assertTrue(events.get(0) instanceof AudioContent);
        AudioContent audioContent = (AudioContent) events.get(0);
        assertNotNull(audioContent.getData());
        assertTrue(audioContent.getData().startsWith("data:audio/mpeg;base64,"));
        assertEquals("audio/mpeg", audioContent.getFormat());
    }

    @Test
    void testAdaptFrameworkStreamWithEmptyContent() {
        Msg msg = Msg.builder()
                .id("msg-1")
                .role(MsgRole.ASSISTANT)
                .content(List.of())
                .build();

        io.agentscope.core.agent.Event event = new io.agentscope.core.agent.Event(
                messageEventType != null ? messageEventType : getDefaultEventType(), msg, true
        );

        Flux<io.agentscope.core.agent.Event> sourceStream = Flux.just(event);
        Flux<io.agentscope.runtime.engine.schemas.Event> result = adapter.adaptFrameworkStream(sourceStream);

        List<io.agentscope.runtime.engine.schemas.Event> events = result.collectList().block();
        assertNotNull(events);
        assertTrue(events.isEmpty());
    }

    @Test
    void testAdaptFrameworkStreamWithNullContent() {
        Msg msg = Msg.builder()
                .id("msg-1")
                .role(MsgRole.ASSISTANT)
                .content(null)
                .build();

        io.agentscope.core.agent.Event event = new io.agentscope.core.agent.Event(
                messageEventType != null ? messageEventType : getDefaultEventType(), msg, true
        );

        Flux<io.agentscope.core.agent.Event> sourceStream = Flux.just(event);
        Flux<io.agentscope.runtime.engine.schemas.Event> result = adapter.adaptFrameworkStream(sourceStream);

        List<io.agentscope.runtime.engine.schemas.Event> events = result.collectList().block();
        assertNotNull(events);
        assertTrue(events.isEmpty());
    }

    @Test
    void testAdaptFrameworkStreamThrowsExceptionForInvalidType() {
        assertThrows(IllegalArgumentException.class, () -> {
            adapter.adaptFrameworkStream("invalid");
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            adapter.adaptFrameworkStream(null);
        });
    }

    @Test
    void testAdaptFrameworkStreamWithMultipleMessages() {
        // First message with text (not last)
        Msg msg1 = Msg.builder()
                .id("msg-1")
                .role(MsgRole.USER)
                .content(List.of(TextBlock.builder().text("Hello").build()))
                .build();

        io.agentscope.core.agent.Event event1 = new io.agentscope.core.agent.Event(
                messageEventType != null ? messageEventType : getDefaultEventType(), msg1, false
        );

        // Second message with text (not last)
        Msg msg2 = Msg.builder()
                .id("msg-2")
                .role(MsgRole.ASSISTANT)
                .content(List.of(TextBlock.builder().text("Hi").build()))
                .build();

        io.agentscope.core.agent.Event event2 = new io.agentscope.core.agent.Event(
                messageEventType != null ? messageEventType : getDefaultEventType(), msg2, false
        );

        Flux<io.agentscope.core.agent.Event> sourceStream = Flux.just(event1, event2);
        Flux<io.agentscope.runtime.engine.schemas.Event> result = adapter.adaptFrameworkStream(sourceStream);

        List<io.agentscope.runtime.engine.schemas.Event> events = result.collectList().block();
        assertNotNull(events);
        assertEquals(2, events.size());
        
        // Both should be TextContent
        assertTrue(events.get(0) instanceof TextContent);
        assertTrue(events.get(1) instanceof TextContent);
    }

    @Test
    void testAdaptFrameworkStreamWithMixedContentBlocks() {
        // Mix of different content blocks (not last)
        Msg msg = Msg.builder()
                .id("msg-1")
                .role(MsgRole.ASSISTANT)
                .content(List.of(
                        TextBlock.builder().text("Hello").build(),
                        ThinkingBlock.builder().thinking("Thinking...").build()
                ))
                .build();

        io.agentscope.core.agent.Event event = new io.agentscope.core.agent.Event(
                messageEventType != null ? messageEventType : getDefaultEventType(), msg, false
        );

        Flux<io.agentscope.core.agent.Event> sourceStream = Flux.just(event);
        Flux<io.agentscope.runtime.engine.schemas.Event> result = adapter.adaptFrameworkStream(sourceStream);

        List<io.agentscope.runtime.engine.schemas.Event> events = result.collectList().block();
        assertNotNull(events);
        assertEquals(2, events.size());
        
        // Both should be TextContent
        assertTrue(events.get(0) instanceof TextContent);
        assertTrue(events.get(1) instanceof TextContent);
    }

    @Test
    void testAdaptFrameworkStreamWithToolUseAndToolResult() {
        // ToolUseBlock (always processed)
        Map<String, Object> input = Map.of("query", "test");
        ToolUseBlock toolUse = new ToolUseBlock("call-1", "test_tool", input);

        Msg msg1 = Msg.builder()
                .id("msg-1")
                .role(MsgRole.ASSISTANT)
                .content(List.of(toolUse))
                .build();

        io.agentscope.core.agent.Event event1 = new io.agentscope.core.agent.Event(
                messageEventType != null ? messageEventType : getDefaultEventType(), msg1, true
        );

        // ToolResultBlock (always processed)
        ToolResultBlock toolResult = ToolResultBlock.of(
                "call-1",
                "test_tool",
                List.of(TextBlock.builder().text("result").build())
        );

        Msg msg2 = Msg.builder()
                .id("msg-2")
                .role(MsgRole.TOOL)
                .content(List.of(toolResult))
                .build();

        io.agentscope.core.agent.Event event2 = new io.agentscope.core.agent.Event(
                messageEventType != null ? messageEventType : getDefaultEventType(), msg2, true
        );

        Flux<io.agentscope.core.agent.Event> sourceStream = Flux.just(event1, event2);
        Flux<io.agentscope.runtime.engine.schemas.Event> result = adapter.adaptFrameworkStream(sourceStream);

        List<io.agentscope.runtime.engine.schemas.Event> events = result.collectList().block();
        assertNotNull(events);
        assertEquals(2, events.size());
        
        // First should be MCP_TOOL_CALL
        assertTrue(events.get(0) instanceof Message);
        Message toolCallMessage = (Message) events.get(0);
        assertEquals(MessageType.MCP_TOOL_CALL, toolCallMessage.getType());
        
        // Second should be MCP_APPROVAL_RESPONSE
        assertTrue(events.get(1) instanceof Message);
        Message toolResultMessage = (Message) events.get(1);
        assertEquals(MessageType.MCP_APPROVAL_RESPONSE, toolResultMessage.getType());
    }

    @Test
    void testAdaptFrameworkStreamWithMetadata() {
        // Test that metadata is preserved
        Map<String, Object> metadata = Map.of("key", "value");
        Msg msg = Msg.builder()
                .id("msg-1")
                .role(MsgRole.ASSISTANT)
                .content(List.of(TextBlock.builder().text("Hello").build()))
                .metadata(metadata)
                .build();

        io.agentscope.core.agent.Event event = new io.agentscope.core.agent.Event(
                messageEventType != null ? messageEventType : getDefaultEventType(), msg, false
        );

        Flux<io.agentscope.core.agent.Event> sourceStream = Flux.just(event);
        Flux<io.agentscope.runtime.engine.schemas.Event> result = adapter.adaptFrameworkStream(sourceStream);

        List<io.agentscope.runtime.engine.schemas.Event> events = result.collectList().block();
        assertNotNull(events);
        assertEquals(1, events.size());
        
        // Metadata should be set on the Message (though TextContent doesn't expose it directly)
        // The adapter creates a Message internally and sets metadata on it
        assertTrue(events.get(0) instanceof TextContent);
    }

    @Test
    void testAdaptFrameworkStreamWithEmptyTextBlock() {
        // Empty text should be skipped
        Msg msg = Msg.builder()
                .id("msg-1")
                .role(MsgRole.ASSISTANT)
                .content(List.of(TextBlock.builder().text("").build()))
                .build();

        io.agentscope.core.agent.Event event = new io.agentscope.core.agent.Event(
                messageEventType != null ? messageEventType : getDefaultEventType(), msg, false
        );

        Flux<io.agentscope.core.agent.Event> sourceStream = Flux.just(event);
        Flux<io.agentscope.runtime.engine.schemas.Event> result = adapter.adaptFrameworkStream(sourceStream);

        List<io.agentscope.runtime.engine.schemas.Event> events = result.collectList().block();
        assertNotNull(events);
        assertTrue(events.isEmpty());
    }

    @Test
    void testAdaptFrameworkStreamWithEmptyThinkingBlock() {
        // Empty thinking should be skipped
        Msg msg = Msg.builder()
                .id("msg-1")
                .role(MsgRole.ASSISTANT)
                .content(List.of(ThinkingBlock.builder().thinking("").build()))
                .build();

        io.agentscope.core.agent.Event event = new io.agentscope.core.agent.Event(
                messageEventType != null ? messageEventType : getDefaultEventType(), msg, false
        );

        Flux<io.agentscope.core.agent.Event> sourceStream = Flux.just(event);
        Flux<io.agentscope.runtime.engine.schemas.Event> result = adapter.adaptFrameworkStream(sourceStream);

        List<io.agentscope.runtime.engine.schemas.Event> events = result.collectList().block();
        assertNotNull(events);
        assertTrue(events.isEmpty());
    }
    
    private EventType getDefaultEventType() {
        // Try to get any available EventType value
        try {
            // Try REASONING first as it's known to exist
            Field field = EventType.class.getField("REASONING");
            return (EventType) field.get(null);
        } catch (Exception e) {
            try {
                // If REASONING doesn't work, try to get the first enum value
                return EventType.values()[0];
            } catch (Exception ex) {
                // If EventType is not an enum, return null
                return null;
            }
        }
    }
}
