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
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.runtime.engine.services.memory.persistence.memory.service.InMemoryMemoryService;
import io.agentscope.runtime.engine.services.memory.service.MemoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LongTermMemoryAdapter.
 */
class LongTermMemoryAdapterTest {

    private MemoryService memoryService;
    private LongTermMemoryAdapter longTermMemoryAdapter;
    private static final String TEST_USER_ID = "test_user";
    private static final String TEST_SESSION_ID = "test_session";

    @BeforeEach
    void setUp() {
        memoryService = new InMemoryMemoryService();
        longTermMemoryAdapter = new LongTermMemoryAdapter(
                memoryService, TEST_USER_ID, TEST_SESSION_ID
        );
    }

    @Test
    void testRecordWithSingleMessage() {
        Msg msg = Msg.builder()
                .id("msg-1")
                .role(MsgRole.ASSISTANT)
                .content(List.of(TextBlock.builder().text("Remember this").build()))
                .build();

        Mono<Void> result = longTermMemoryAdapter.record(List.of(msg));

        result.block(); // Should complete without error
        assertNotNull(result);
    }

    @Test
    void testRecordWithMultipleMessages() {
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

        Mono<Void> result = longTermMemoryAdapter.record(List.of(msg1, msg2));

        result.block(); // Should complete without error
        assertNotNull(result);
    }

    @Test
    void testRecordWithNullList() {
        Mono<Void> result = longTermMemoryAdapter.record(null);

        result.block(); // Should complete without error
        assertNotNull(result);
    }

    @Test
    void testRecordWithEmptyList() {
        Mono<Void> result = longTermMemoryAdapter.record(new ArrayList<>());

        result.block(); // Should complete without error
        assertNotNull(result);
    }

    @Test
    void testRecordWithNullMessages() {
        List<Msg> messages = new ArrayList<>();
        messages.add(null);
        messages.add(Msg.builder()
                .id("msg-1")
                .role(MsgRole.ASSISTANT)
                .content(List.of(TextBlock.builder().text("Valid message").build()))
                .build());

        Mono<Void> result = longTermMemoryAdapter.record(messages);

        result.block(); // Should complete without error
        assertNotNull(result);
    }

    @Test
    void testRetrieveWithDefaultLimit() {
        // First, record some messages
        Msg msg1 = Msg.builder()
                .id("msg-1")
                .role(MsgRole.ASSISTANT)
                .content(List.of(TextBlock.builder().text("Important information").build()))
                .build();

        longTermMemoryAdapter.record(List.of(msg1)).block();

        // Now retrieve
        Msg queryMsg = Msg.builder()
                .id("query-1")
                .role(MsgRole.USER)
                .content(List.of(TextBlock.builder().text("information").build()))
                .build();

        Mono<String> result = longTermMemoryAdapter.retrieve(queryMsg);

        String text = result.block();
        assertNotNull(text);
    }

    @Test
    void testRetrieveWithCustomLimit() {
        // First, record some messages
        Msg msg1 = Msg.builder()
                .id("msg-1")
                .role(MsgRole.ASSISTANT)
                .content(List.of(TextBlock.builder().text("First memory").build()))
                .build();

        Msg msg2 = Msg.builder()
                .id("msg-2")
                .role(MsgRole.ASSISTANT)
                .content(List.of(TextBlock.builder().text("Second memory").build()))
                .build();

        longTermMemoryAdapter.record(List.of(msg1, msg2)).block();

        // Now retrieve with custom limit
        Msg queryMsg = Msg.builder()
                .id("query-1")
                .role(MsgRole.USER)
                .content(List.of(TextBlock.builder().text("memory").build()))
                .build();

        Mono<String> result = longTermMemoryAdapter.retrieve(queryMsg, 10);

        String text = result.block();
        assertNotNull(text);
    }

    @Test
    void testRetrieveWithNullMessage() {
        Mono<String> result = longTermMemoryAdapter.retrieve(null);

        String text = result.block();
        assertNotNull(text);
    }

    @Test
    void testRecordToMemory() {
        String thinking = "This is important information to remember";
        List<String> content = List.of("User likes coffee", "User works at tech company");

        Mono<ToolResultBlock> result = longTermMemoryAdapter.recordToMemory(thinking, content);

        ToolResultBlock block = result.block();
        assertNotNull(block);
        assertNotNull(block.getOutput());
    }

    @Test
    void testRecordToMemoryWithEmptyContent() {
        String thinking = "Empty content";
        List<String> content = new ArrayList<>();

        Mono<ToolResultBlock> result = longTermMemoryAdapter.recordToMemory(thinking, content);

        ToolResultBlock block = result.block();
        assertNotNull(block);
    }

    @Test
    void testRetrieveFromMemory() {
        // First, record some information
        String thinking = "User preferences";
        List<String> content = List.of("User likes Java programming", "User prefers Spring Boot");
        longTermMemoryAdapter.recordToMemory(thinking, content).block();

        // Now retrieve
        List<String> keywords = List.of("Java", "Spring");
        Mono<ToolResultBlock> result = longTermMemoryAdapter.retrieveFromMemory(keywords, 5);

        ToolResultBlock block = result.block();
        assertNotNull(block);
        assertNotNull(block.getOutput());
    }

    @Test
    void testRetrieveFromMemoryWithEmptyKeywords() {
        List<String> keywords = new ArrayList<>();
        Mono<ToolResultBlock> result = longTermMemoryAdapter.retrieveFromMemory(keywords, 5);

        ToolResultBlock block = result.block();
        assertNotNull(block);
    }

    @Test
    void testRetrieveFromMemoryWithMultipleKeywords() {
        // First, record some information
        String thinking = "Multiple topics";
        List<String> content = List.of("Topic A: Machine Learning", "Topic B: Deep Learning", "Topic C: NLP");
        longTermMemoryAdapter.recordToMemory(thinking, content).block();

        // Retrieve with multiple keywords
        List<String> keywords = List.of("Machine Learning", "NLP");
        Mono<ToolResultBlock> result = longTermMemoryAdapter.retrieveFromMemory(keywords, 3);

        ToolResultBlock block = result.block();
        assertNotNull(block);
    }

    @Test
    void testRecordToMemoryHandlesException() {
        // Create a mock service that throws exception
        MemoryService mockService = mock(MemoryService.class);
        when(mockService.addMemory(any(), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Test exception")));

        LongTermMemoryAdapter adapterWithMock = new LongTermMemoryAdapter(
                mockService, TEST_USER_ID, TEST_SESSION_ID
        );

        String thinking = "Test";
        List<String> content = List.of("Test content");

        Mono<ToolResultBlock> result = adapterWithMock.recordToMemory(thinking, content);

        ToolResultBlock block = result.block();
        assertNotNull(block);
        // Should contain error message
    }

    @Test
    void testRetrieveFromMemoryHandlesException() {
        // Create a mock service that throws exception
        MemoryService mockService = mock(MemoryService.class);
        when(mockService.searchMemory(any(), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Test exception")));

        LongTermMemoryAdapter adapterWithMock = new LongTermMemoryAdapter(
                mockService, TEST_USER_ID, TEST_SESSION_ID
        );

        List<String> keywords = List.of("test");
        Mono<ToolResultBlock> result = adapterWithMock.retrieveFromMemory(keywords, 5);

        ToolResultBlock block = result.block();
        assertNotNull(block);
        // Should contain error message
    }

    @Test
    void testRetrieveWithNoResults() {
        // Retrieve without recording anything
        Msg queryMsg = Msg.builder()
                .id("query-1")
                .role(MsgRole.USER)
                .content(List.of(TextBlock.builder().text("nonexistent").build()))
                .build();

        Mono<String> result = longTermMemoryAdapter.retrieve(queryMsg);

        String text = result.block();
        assertNotNull(text);
        assertTrue(text.isEmpty() || text.contains("Error"));
    }
}

