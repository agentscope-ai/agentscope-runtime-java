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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AgentScopeStreamAdapter.
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
    void testAdaptFrameworkStreamWithTextBlock() {
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
        assertTrue(events.size() >= 3);
        
        // First event should be inProgress message
        assertTrue(events.get(0) instanceof Message);
        Message firstMessage = (Message) events.get(0);
        assertEquals(MessageType.MESSAGE, firstMessage.getType());
        assertEquals("assistant", firstMessage.getRole());
        
        // Second event should be content
        assertTrue(events.get(1) instanceof Content);
        
        // Last event should be completed message
        Message lastMessage = (Message) events.get(events.size() - 1);
        assertEquals(RunStatus.COMPLETED, lastMessage.getStatus());
    }

    @Test
    void testAdaptFrameworkStreamWithIncrementalText() {
        // First event with partial text
        Msg msg1 = Msg.builder()
                .id("msg-1")
                .role(MsgRole.ASSISTANT)
                .content(List.of(TextBlock.builder().text("Hello").build()))
                .build();

        io.agentscope.core.agent.Event event1 = new io.agentscope.core.agent.Event(
                messageEventType != null ? messageEventType : getDefaultEventType(), msg1, false
        );

        // Second event with complete text
        Msg msg2 = Msg.builder()
                .id("msg-1")
                .role(MsgRole.ASSISTANT)
                .content(List.of(TextBlock.builder().text("Hello, world!").build()))
                .build();

        io.agentscope.core.agent.Event event2 = new io.agentscope.core.agent.Event(
                messageEventType != null ? messageEventType : getDefaultEventType(), msg2, true
        );

        Flux<io.agentscope.core.agent.Event> sourceStream = Flux.just(event1, event2);
        Flux<io.agentscope.runtime.engine.schemas.Event> result = adapter.adaptFrameworkStream(sourceStream);

        List<io.agentscope.runtime.engine.schemas.Event> events = result.collectList().block();
        assertNotNull(events);
        assertTrue(events.size() >= 5);
    }

    @Test
    void testAdaptFrameworkStreamWithThinkingBlock() {
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
        assertTrue(events.size() >= 3);
        
        // First event should be reasoning message
        assertTrue(events.get(0) instanceof Message);
        Message firstMessage = (Message) events.get(0);
        assertEquals(MessageType.REASONING, firstMessage.getType());
        
        // Second event should be content
        assertTrue(events.get(1) instanceof Content);
        
        // Last event should be completed message
        Message lastMessage = (Message) events.get(events.size() - 1);
        assertEquals(RunStatus.COMPLETED, lastMessage.getStatus());
    }

//    @Test
//    void testAdaptFrameworkStreamWithToolUseBlock() {
//        Map<String, Object> input = Map.of("query", "test");
//        ToolUseBlock toolUse = new ToolUseBlock("call-1", "test_tool", input);
//
//        Msg msg = Msg.builder()
//                .id("msg-1")
//                .role(MsgRole.ASSISTANT)
//                .content(List.of(toolUse))
//                .build();
//
//        io.agentscope.core.agent.Event event = new io.agentscope.core.agent.Event(
//                messageEventType != null ? messageEventType : getDefaultEventType(), msg, true
//        );
//
//        Flux<io.agentscope.core.agent.Event> sourceStream = Flux.just(event);
//        Flux<io.agentscope.runtime.engine.schemas.Event> result = adapter.adaptFrameworkStream(sourceStream);
//
//        List<io.agentscope.runtime.engine.schemas.Event> events = result.collectList().block();
//        assertNotNull(events);
//        assertTrue(events.size() >= 3);
//
//        // First event should be plugin call message
//        assertTrue(events.get(0) instanceof Message);
//        Message firstMessage = (Message) events.get(0);
//        assertEquals(MessageType.PLUGIN_CALL, firstMessage.getType());
//
//        // Second event should be content
//        assertTrue(events.get(1) instanceof Content);
//
//        // Last event should be completed message
//        Message lastMessage = (Message) events.get(events.size() - 1);
//        assertEquals(RunStatus.COMPLETED, lastMessage.getStatus());
//    }

    @Test
    void testAdaptFrameworkStreamWithToolResultBlock() {
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
        assertTrue(events.size() >= 1);
        
        // First event should be plugin call output message
        assertTrue(events.get(0) instanceof Message);
        Message message = (Message) events.get(0);
        assertEquals(MessageType.PLUGIN_CALL_OUTPUT, message.getType());
    }

//    @Test
//    void testAdaptFrameworkStreamWithImageBlock() {
//        ImageBlock imageBlock = ImageBlock.builder()
//                .source(URLSource.builder().url("https://example.com/image.jpg").build())
//                .build();
//
//        Msg msg = Msg.builder()
//                .id("msg-1")
//                .role(MsgRole.USER)
//                .content(List.of(imageBlock))
//                .build();
//
//        io.agentscope.core.agent.Event event = new io.agentscope.core.agent.Event(
//                messageEventType != null ? messageEventType : getDefaultEventType(), msg, true
//        );
//
//        Flux<io.agentscope.core.agent.Event> sourceStream = Flux.just(event);
//        Flux<io.agentscope.runtime.engine.schemas.Event> result = adapter.adaptFrameworkStream(sourceStream);
//
//        List<io.agentscope.runtime.engine.schemas.Event> events = result.collectList().block();
//        assertNotNull(events);
//        assertTrue(events.size() >= 3);
//
//        // First event should be message
//        assertTrue(events.get(0) instanceof Message);
//        Message firstMessage = (Message) events.get(0);
//        assertEquals(MessageType.MESSAGE, firstMessage.getType());
//
//        // Second event should be image content
//        assertTrue(events.get(1) instanceof ImageContent);
//        ImageContent imageContent = (ImageContent) events.get(1);
//        assertEquals("https://example.com/image.jpg", imageContent.getImageUrl());
//
//        // Last event should be completed message
//        Message lastMessage = (Message) events.get(events.size() - 1);
//        assertEquals(RunStatus.COMPLETED, lastMessage.getStatus());
//    }

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
    void testAdaptFrameworkStreamThrowsExceptionForInvalidType() {
        assertThrows(IllegalArgumentException.class, () -> {
            adapter.adaptFrameworkStream("invalid");
        });
    }

    @Test
    void testAdaptFrameworkStreamWithMultipleMessages() {
        Msg msg1 = Msg.builder()
                .id("msg-1")
                .role(MsgRole.USER)
                .content(List.of(TextBlock.builder().text("Hello").build()))
                .build();

        Msg msg2 = Msg.builder()
                .id("msg-2")
                .role(MsgRole.ASSISTANT)
                .content(List.of(TextBlock.builder().text("Hi").build()))
                .build();

        io.agentscope.core.agent.Event event1 = new io.agentscope.core.agent.Event(
                messageEventType != null ? messageEventType : getDefaultEventType(), msg1, true
        );

        io.agentscope.core.agent.Event event2 = new io.agentscope.core.agent.Event(
                messageEventType != null ? messageEventType : getDefaultEventType(), msg2, true
        );

        Flux<io.agentscope.core.agent.Event> sourceStream = Flux.just(event1, event2);
        Flux<io.agentscope.runtime.engine.schemas.Event> result = adapter.adaptFrameworkStream(sourceStream);

        List<io.agentscope.runtime.engine.schemas.Event> events = result.collectList().block();
        assertNotNull(events);
        assertTrue(events.size() >= 6);
    }

    @Test
    void testAdaptFrameworkStreamWithBase64Image() {
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
                messageEventType != null ? messageEventType : getDefaultEventType(), msg, true
        );

        Flux<io.agentscope.core.agent.Event> sourceStream = Flux.just(event);
        Flux<io.agentscope.runtime.engine.schemas.Event> result = adapter.adaptFrameworkStream(sourceStream);

        List<io.agentscope.runtime.engine.schemas.Event> events = result.collectList().block();
        assertNotNull(events);
        assertTrue(events.size() >= 1);
        
        // Should contain image content with base64 data URI
        boolean foundBase64Image = events.stream()
                .filter(e -> e instanceof ImageContent)
                .map(e -> (ImageContent) e)
                .anyMatch(ic -> ic.getImageUrl() != null && 
                               ic.getImageUrl().startsWith("data:image/jpeg;base64,"));
        assertTrue(foundBase64Image);
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

