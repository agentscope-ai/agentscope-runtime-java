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

import io.agentscope.core.message.*;
import io.agentscope.runtime.engine.schemas.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AgentScopeMessageAdapter.
 */
class AgentScopeMessageAdapterTest {

    private AgentScopeMessageAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AgentScopeMessageAdapter();
    }

    @Test
    void testFrameworkMsgToMessageWithSingleTextBlock() {
        Msg msg = Msg.builder()
                .id("msg-1")
                .name("assistant")
                .role(MsgRole.ASSISTANT)
                .content(List.of(TextBlock.builder().text("Hello, world!").build()))
                .metadata(Map.of("key", "value"))
                .build();

        List<Message> result = adapter.frameworkMsgToMessage(msg);

        assertNotNull(result);
        assertEquals(1, result.size());
        Message message = result.get(0);
        assertEquals("assistant", message.getRole());
        assertEquals(MessageType.MESSAGE, message.getType());
        assertNotNull(message.getContent());
        assertEquals(1, message.getContent().size());
        assertTrue(message.getContent().get(0) instanceof TextContent);
        TextContent textContent = (TextContent) message.getContent().get(0);
        assertEquals("Hello, world!", textContent.getText());
    }

    @Test
    void testFrameworkMsgToMessageWithList() {
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

        List<Message> result = adapter.frameworkMsgToMessage(List.of(msg1, msg2));

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("user", result.get(0).getRole());
        assertEquals("assistant", result.get(1).getRole());
    }

    @Test
    void testFrameworkMsgToMessageWithThinkingBlock() {
        Msg msg = Msg.builder()
                .id("msg-1")
                .role(MsgRole.ASSISTANT)
                .content(List.of(ThinkingBlock.builder().thinking("Let me think...").build()))
                .build();

        List<Message> result = adapter.frameworkMsgToMessage(msg);

        assertNotNull(result);
        assertEquals(1, result.size());
        Message message = result.get(0);
        assertEquals(MessageType.REASONING, message.getType());
        assertNotNull(message.getContent());
        assertEquals(1, message.getContent().size());
        assertTrue(message.getContent().get(0) instanceof TextContent);
    }

    @Test
    void testFrameworkMsgToMessageWithToolUseBlock() {
        Map<String, Object> input = Map.of("query", "test");
        ToolUseBlock toolUse = new ToolUseBlock("call-1", "test_tool", input);
        
        Msg msg = Msg.builder()
                .id("msg-1")
                .role(MsgRole.ASSISTANT)
                .content(List.of(toolUse))
                .build();

        List<Message> result = adapter.frameworkMsgToMessage(msg);

        assertNotNull(result);
        assertEquals(1, result.size());
        Message message = result.get(0);
        assertEquals(MessageType.PLUGIN_CALL, message.getType());
        assertNotNull(message.getContent());
        assertEquals(1, message.getContent().size());
        assertTrue(message.getContent().get(0) instanceof DataContent);
    }

    @Test
    void testFrameworkMsgToMessageWithImageBlock() {
        ImageBlock imageBlock = ImageBlock.builder()
                .source(URLSource.builder().url("https://example.com/image.jpg").build())
                .build();

        Msg msg = Msg.builder()
                .id("msg-1")
                .role(MsgRole.USER)
                .content(List.of(imageBlock))
                .build();

        List<Message> result = adapter.frameworkMsgToMessage(msg);

        assertNotNull(result);
        assertEquals(1, result.size());
        Message message = result.get(0);
        assertNotNull(message.getContent());
        assertTrue(message.getContent().get(0) instanceof ImageContent);
        ImageContent imageContent = (ImageContent) message.getContent().get(0);
        assertEquals("https://example.com/image.jpg", imageContent.getImageUrl());
    }

    @Test
    void testFrameworkMsgToMessageWithEmptyContent() {
        Msg msg = Msg.builder()
                .id("msg-1")
                .role(MsgRole.ASSISTANT)
                .content(new ArrayList<>())
                .build();

        List<Message> result = adapter.frameworkMsgToMessage(msg);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFrameworkMsgToMessageWithNullContent() {
        Msg msg = Msg.builder()
                .id("msg-1")
                .role(MsgRole.ASSISTANT)
                .build();

        List<Message> result = adapter.frameworkMsgToMessage(msg);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFrameworkMsgToMessageThrowsExceptionForInvalidType() {
        assertThrows(IllegalArgumentException.class, () -> {
            adapter.frameworkMsgToMessage("invalid");
        });
    }

    @Test
    void testMessageToFrameworkMsgWithSingleMessage() {
        Message message = new Message(MessageType.MESSAGE, "user");
        message.setId("msg-1");
        TextContent textContent = new TextContent();
        textContent.setText("Hello");
        message.setContent(List.of(textContent));

        Object result = adapter.messageToFrameworkMsg(message);

        assertNotNull(result);
        assertTrue(result instanceof Msg);
        Msg msg = (Msg) result;
        assertEquals("user", msg.getRole().name().toLowerCase());
        assertNotNull(msg.getContent());
        assertEquals(1, msg.getContent().size());
    }

    @Test
    void testMessageToFrameworkMsgWithList() {
        Message msg1 = new Message(MessageType.MESSAGE, "user");
        msg1.setId("msg-1");
        TextContent text1 = new TextContent();
        text1.setText("Hello");
        msg1.setContent(List.of(text1));

        Message msg2 = new Message(MessageType.MESSAGE, "assistant");
        msg2.setId("msg-2");
        TextContent text2 = new TextContent();
        text2.setText("Hi");
        msg2.setContent(List.of(text2));

        Object result = adapter.messageToFrameworkMsg(List.of(msg1, msg2));

        assertNotNull(result);
        assertTrue(result instanceof List);
        @SuppressWarnings("unchecked")
        List<Msg> msgList = (List<Msg>) result;
        assertEquals(2, msgList.size());
    }

    @Test
    void testMessageToFrameworkMsgWithPluginCall() {
        Message message = new Message(MessageType.PLUGIN_CALL, "assistant");
        message.setId("msg-1");
        DataContent dataContent = new DataContent();
        Map<String, Object> data = new HashMap<>();
        data.put("call_id", "call-1");
        data.put("name", "test_tool");
        data.put("arguments", "{\"query\":\"test\"}");
        dataContent.setData(data);
        message.setContent(List.of(dataContent));

        Object result = adapter.messageToFrameworkMsg(message);

        assertNotNull(result);
        assertTrue(result instanceof Msg);
        Msg msg = (Msg) result;
        assertNotNull(msg.getContent());
        assertTrue(msg.getContent().get(0) instanceof ToolUseBlock);
    }

    @Test
    void testMessageToFrameworkMsgWithPluginCallOutput() {
        Message message = new Message(MessageType.PLUGIN_CALL_OUTPUT, "tool");
        message.setId("msg-1");
        DataContent dataContent = new DataContent();
        Map<String, Object> data = new HashMap<>();
        data.put("call_id", "call-1");
        data.put("name", "test_tool");
        data.put("output", "\"result\"");
        dataContent.setData(data);
        message.setContent(List.of(dataContent));

        Object result = adapter.messageToFrameworkMsg(message);

        assertNotNull(result);
        assertTrue(result instanceof Msg);
        Msg msg = (Msg) result;
        assertNotNull(msg.getContent());
        assertTrue(msg.getContent().get(0) instanceof ToolResultBlock);
    }

    @Test
    void testMessageToFrameworkMsgWithReasoning() {
        Message message = new Message(MessageType.REASONING, "assistant");
        message.setId("msg-1");
        TextContent textContent = new TextContent();
        textContent.setText("Let me think...");
        message.setContent(List.of(textContent));

        Object result = adapter.messageToFrameworkMsg(message);

        assertNotNull(result);
        assertTrue(result instanceof Msg);
        Msg msg = (Msg) result;
        assertNotNull(msg.getContent());
        assertTrue(msg.getContent().get(0) instanceof ThinkingBlock);
    }

    @Test
    void testMessageToFrameworkMsgWithImage() {
        Message message = new Message(MessageType.MESSAGE, "user");
        message.setId("msg-1");
        ImageContent imageContent = new ImageContent();
        imageContent.setImageUrl("https://example.com/image.jpg");
        message.setContent(List.of(imageContent));

        Object result = adapter.messageToFrameworkMsg(message);

        assertNotNull(result);
        assertTrue(result instanceof Msg);
        Msg msg = (Msg) result;
        assertNotNull(msg.getContent());
        assertTrue(msg.getContent().get(0) instanceof ImageBlock);
    }

    @Test
    void testMessageToFrameworkMsgThrowsExceptionForInvalidType() {
        assertThrows(IllegalArgumentException.class, () -> {
            adapter.messageToFrameworkMsg("invalid");
        });
    }

    @Test
    void testRoundTripConversion() {
        // Create original AgentScope Msg
        Msg originalMsg = Msg.builder()
                .id("msg-1")
                .name("assistant")
                .role(MsgRole.ASSISTANT)
                .content(List.of(TextBlock.builder().text("Hello, world!").build()))
                .metadata(Map.of("key", "value"))
                .build();

        // Convert to runtime Message
        List<Message> runtimeMessages = adapter.frameworkMsgToMessage(originalMsg);
        assertNotNull(runtimeMessages);
        assertEquals(1, runtimeMessages.size());

        // Convert back to AgentScope Msg
        Object result = adapter.messageToFrameworkMsg(runtimeMessages.get(0));
        assertNotNull(result);
        assertTrue(result instanceof Msg);
        Msg convertedMsg = (Msg) result;

        // Verify basic properties
        assertEquals(originalMsg.getRole(), convertedMsg.getRole());
        assertNotNull(convertedMsg.getContent());
        assertEquals(1, convertedMsg.getContent().size());
        assertTrue(convertedMsg.getContent().get(0) instanceof TextBlock);
        TextBlock textBlock = (TextBlock) convertedMsg.getContent().get(0);
        assertEquals("Hello, world!", textBlock.getText());
    }

    @Test
    void testMessageToFrameworkMsgWithMetadata() {
        Message message = new Message(MessageType.MESSAGE, "user");
        message.setId("msg-1");
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("original_id", "original-1");
        metadata.put("original_name", "original-name");
        message.setMetadata(metadata);
        TextContent textContent = new TextContent();
        textContent.setText("Hello");
        message.setContent(List.of(textContent));

        Object result = adapter.messageToFrameworkMsg(message);

        assertNotNull(result);
        assertTrue(result instanceof Msg);
        Msg msg = (Msg) result;
        assertEquals("original-1", msg.getId());
        assertEquals("original-name", msg.getName());
    }
}

