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

import io.agentscope.runtime.adapters.MessageAdapter;
import io.agentscope.runtime.adapters.StreamAdapter;
import io.agentscope.runtime.engine.schemas.AgentRequest;
import io.agentscope.runtime.engine.services.agent_state.StateService;
import io.agentscope.runtime.engine.services.memory.service.MemoryService;
import io.agentscope.runtime.engine.services.memory.service.SessionHistoryService;
import io.agentscope.runtime.engine.services.sandbox.SandboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AgentScopeAgentHandler abstract class.
 */
class AgentScopeAgentHandlerTest {

    private StateService stateService;
    private SessionHistoryService sessionHistoryService;
    private MemoryService memoryService;
    private SandboxService sandboxService;
    private TestAgentScopeAgentHandler handler;

    /**
     * Test implementation of AgentScopeAgentHandler for testing purposes.
     */
    private static class TestAgentScopeAgentHandler extends AgentScopeAgentHandler {
        private boolean healthy = false;
        private boolean started = false;

        public TestAgentScopeAgentHandler() {
            super();
        }

        public TestAgentScopeAgentHandler(StateService stateService, 
                                         SessionHistoryService sessionHistoryService,
                                         MemoryService memoryService,
                                         SandboxService sandboxService) {
            super(stateService, sessionHistoryService, memoryService, sandboxService);
        }

        @Override
        public String getName() {
            return "TestAgentHandler";
        }

        @Override
        public String getDescription() {
            return "Test implementation for unit testing";
        }

        @Override
        public void start() {
            super.start();
            started = true;
            healthy = true;
        }

        @Override
        public void stop() {
            super.stop();
            started = false;
            healthy = false;
        }

        @Override
        public boolean isHealthy() {
            return healthy;
        }

        @Override
        public Flux<?> streamQuery(AgentRequest request, Object messages) {
            if (!started) {
                throw new IllegalStateException("Handler not started");
            }
            return Flux.empty();
        }
    }

    @BeforeEach
    void setUp() {
        stateService = mock(StateService.class);
        sessionHistoryService = mock(SessionHistoryService.class);
        memoryService = mock(MemoryService.class);
        sandboxService = mock(SandboxService.class);

        when(sessionHistoryService.start()).thenReturn(CompletableFuture.completedFuture(null));
        when(sessionHistoryService.stop()).thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void testGetFrameworkType() {
        handler = new TestAgentScopeAgentHandler();
        assertEquals("agentscope", handler.getFrameworkType());
    }

    @Test
    void testGetName() {
        handler = new TestAgentScopeAgentHandler();
        assertEquals("TestAgentHandler", handler.getName());
    }

    @Test
    void testGetDescription() {
        handler = new TestAgentScopeAgentHandler();
        assertEquals("Test implementation for unit testing", handler.getDescription());
    }

    @Test
    void testGetStreamAdapter() {
        handler = new TestAgentScopeAgentHandler();
        StreamAdapter adapter = handler.getStreamAdapter();
        assertNotNull(adapter);
        assertTrue(adapter instanceof AgentScopeStreamAdapter);
    }

    @Test
    void testGetMessageAdapter() {
        handler = new TestAgentScopeAgentHandler();
        MessageAdapter adapter = handler.getMessageAdapter();
        assertNotNull(adapter);
        assertTrue(adapter instanceof AgentScopeMessageAdapter);
    }

    @Test
    void testStartWithServices() {
        handler = new TestAgentScopeAgentHandler(stateService, sessionHistoryService, 
                                                 memoryService, sandboxService);
        handler.start();
        
        assertTrue(handler.isHealthy());
        verify(stateService, times(1)).start();
        verify(sessionHistoryService, times(1)).start();
    }

    @Test
    void testStartWithoutServices() {
        handler = new TestAgentScopeAgentHandler();
        handler.start();
        
        assertTrue(handler.isHealthy());
        // Should not throw exception even without services
    }

    @Test
    void testStopWithServices() {
        handler = new TestAgentScopeAgentHandler(stateService, sessionHistoryService, 
                                                 memoryService, sandboxService);
        handler.start();
        handler.stop();
        
        assertFalse(handler.isHealthy());
        verify(stateService, times(1)).stop();
        verify(sessionHistoryService, times(1)).stop();
    }

    @Test
    void testStopWithoutServices() {
        handler = new TestAgentScopeAgentHandler();
        handler.start();
        handler.stop();
        
        assertFalse(handler.isHealthy());
        // Should not throw exception even without services
    }

    @Test
    void testIsHealthy() {
        handler = new TestAgentScopeAgentHandler();
        assertFalse(handler.isHealthy());
        
        handler.start();
        assertTrue(handler.isHealthy());
        
        handler.stop();
        assertFalse(handler.isHealthy());
    }

    @Test
    void testStreamQueryWhenNotStarted() {
        handler = new TestAgentScopeAgentHandler();
        AgentRequest request = new AgentRequest();
        
        assertThrows(IllegalStateException.class, () -> {
            handler.streamQuery(request, null).blockLast();
        });
    }

    @Test
    void testStreamQueryWhenStarted() {
        handler = new TestAgentScopeAgentHandler();
        handler.start();
        
        AgentRequest request = new AgentRequest();
        Flux<?> result = handler.streamQuery(request, null);
        
        assertNotNull(result);
        result.blockLast(); // Should complete without error
    }

    @Test
    void testSetStateService() {
        handler = new TestAgentScopeAgentHandler();
        handler.setStateService(stateService);
        handler.start();
        
        verify(stateService, times(1)).start();
    }

    @Test
    void testSetSessionHistoryService() {
        handler = new TestAgentScopeAgentHandler();
        handler.setSessionHistoryService(sessionHistoryService);
        handler.start();
        
        verify(sessionHistoryService, times(1)).start();
    }

    @Test
    void testSetMemoryService() {
        handler = new TestAgentScopeAgentHandler();
        handler.setMemoryService(memoryService);
        // MemoryService is not used in start/stop, so just verify it's set
        assertNotNull(handler);
    }

    @Test
    void testSetSandboxService() {
        handler = new TestAgentScopeAgentHandler();
        handler.setSandboxService(sandboxService);
        // SandboxService is not used in start/stop, so just verify it's set
        assertNotNull(handler);
    }
}

