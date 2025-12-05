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
package io.agentscope.runtime.adapters.agentscope.memory;

import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.runtime.engine.schemas.Message;
import io.agentscope.runtime.engine.schemas.Role;
import io.agentscope.runtime.engine.schemas.Session;
import io.agentscope.runtime.engine.services.memory.persistence.session.InMemorySessionHistoryService;
import io.agentscope.runtime.engine.services.memory.service.SessionHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MemoryAdapter.
 */
class MemoryAdapterTest {

    private SessionHistoryService sessionHistoryService;
    private MemoryAdapter memoryAdapter;
    private static final String TEST_USER_ID = "test_user";
    private static final String TEST_SESSION_ID = "test_session";

    @BeforeEach
    void setUp() {
        sessionHistoryService = new InMemorySessionHistoryService();
        memoryAdapter = new MemoryAdapter(sessionHistoryService, TEST_USER_ID, TEST_SESSION_ID);
    }

    @Test
    void testGetComponentName() {
        assertEquals("memory", memoryAdapter.getComponentName());
    }

    @Test
    void testAddMessage() {
        Msg msg = Msg.builder()
                .id("msg-1")
                .role(MsgRole.USER)
                .content(List.of(TextBlock.builder().text("Hello").build()))
                .build();

        memoryAdapter.addMessage(msg);

        List<Msg> messages = memoryAdapter.getMessages();
        assertNotNull(messages);
        assertEquals(1, messages.size());
        assertEquals("Hello", ((TextBlock) messages.get(0).getContent().get(0)).getText());
    }

    @Test
    void testAddMessageWithNull() {
        // Should not throw exception
        assertDoesNotThrow(() -> memoryAdapter.addMessage(null));
    }

    @Test
    void testGetMessagesWhenEmpty() {
        List<Msg> messages = memoryAdapter.getMessages();
        assertNotNull(messages);
        assertTrue(messages.isEmpty());
    }

    @Test
    void testGetMessagesAfterAddingMultiple() {
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

        memoryAdapter.addMessage(msg1);
        memoryAdapter.addMessage(msg2);

        List<Msg> messages = memoryAdapter.getMessages();
        assertNotNull(messages);
        assertEquals(2, messages.size());
    }

    @Test
    void testDeleteMessage() {
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

        memoryAdapter.addMessage(msg1);
        memoryAdapter.addMessage(msg2);

        List<Msg> messages = memoryAdapter.getMessages();
        assertEquals(2, messages.size());

        memoryAdapter.deleteMessage(0);

        messages = memoryAdapter.getMessages();
        assertEquals(1, messages.size());
        assertEquals("Hi", ((TextBlock) messages.get(0).getContent().get(0)).getText());
    }

    @Test
    void testDeleteMessageWithInvalidIndex() {
        Msg msg = Msg.builder()
                .id("msg-1")
                .role(MsgRole.USER)
                .content(List.of(TextBlock.builder().text("Hello").build()))
                .build();

        memoryAdapter.addMessage(msg);

        // Should not throw exception for invalid index
        assertDoesNotThrow(() -> memoryAdapter.deleteMessage(-1));
        assertDoesNotThrow(() -> memoryAdapter.deleteMessage(10));

        List<Msg> messages = memoryAdapter.getMessages();
        assertEquals(1, messages.size());
    }

    @Test
    void testClear() {
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

        memoryAdapter.addMessage(msg1);
        memoryAdapter.addMessage(msg2);

        memoryAdapter.clear();

        List<Msg> messages = memoryAdapter.getMessages();
        assertNotNull(messages);
        assertTrue(messages.isEmpty());
    }

    @Test
    void testStateDict() {
        Map<String, Object> state = memoryAdapter.stateDict();
        assertNotNull(state);
        assertTrue(state.isEmpty());
    }

    @Test
    void testLoadStateDict() {
        Map<String, Object> state = Map.of("key", "value");
        // Should not throw exception
        assertDoesNotThrow(() -> memoryAdapter.loadStateDict(state, true));
        assertDoesNotThrow(() -> memoryAdapter.loadStateDict(state, false));
    }

    @Test
    void testAddMessageCreatesSessionIfNotExists() {
        // Use a new session ID that doesn't exist
        MemoryAdapter newAdapter = new MemoryAdapter(
                sessionHistoryService, TEST_USER_ID, "new_session"
        );

        Msg msg = Msg.builder()
                .id("msg-1")
                .role(MsgRole.USER)
                .content(List.of(TextBlock.builder().text("Hello").build()))
                .build();

        newAdapter.addMessage(msg);

        List<Msg> messages = newAdapter.getMessages();
        assertNotNull(messages);
        assertEquals(1, messages.size());
    }

    @Test
    void testGetMessagesReloadsFromBackend() {
        // Add message directly to service
        Session session = new Session(TEST_SESSION_ID, TEST_USER_ID, new ArrayList<>());
        Message runtimeMessage = new Message();
        runtimeMessage.setRole(Role.USER);
        runtimeMessage.setId("msg-1");
        session.getMessages().add(runtimeMessage);

        // Create session in service
        sessionHistoryService.createSession(TEST_USER_ID, Optional.of(TEST_SESSION_ID)).join();

        // Add message through service
        sessionHistoryService.appendMessage(session, List.of(runtimeMessage)).join();

        // Get messages through adapter
        List<Msg> messages = memoryAdapter.getMessages();
        assertNotNull(messages);
        assertEquals(1, messages.size());
    }

    @Test
    void testDeleteMessageRecreatesSession() {
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

        Msg msg3 = Msg.builder()
                .id("msg-3")
                .role(MsgRole.USER)
                .content(List.of(TextBlock.builder().text("How are you?").build()))
                .build();

        memoryAdapter.addMessage(msg1);
        memoryAdapter.addMessage(msg2);
        memoryAdapter.addMessage(msg3);

        assertEquals(3, memoryAdapter.getMessages().size());

        // Delete middle message
        memoryAdapter.deleteMessage(1);

        List<Msg> messages = memoryAdapter.getMessages();
        assertEquals(2, messages.size());
        assertEquals("Hello", ((TextBlock) messages.get(0).getContent().get(0)).getText());
        assertEquals("How are you?", ((TextBlock) messages.get(1).getContent().get(0)).getText());
    }
}

