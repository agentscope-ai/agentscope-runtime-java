# Simple Deployment

`AgentApp` is the all-in-one application service wrapper in **AgentScope Runtime Java**.
It provides an HTTP service framework for your agent logic and exposes it as an API, supporting the following features:

- **Streaming responses (SSE)** for real-time output
- Built-in **health check** endpoints
- Built-in **A2A protocol** support

**Important Note**:
In the current version, `AgentApp` does not automatically include the `/process` endpoint.
You must explicitly register a controller and corresponding request handler function for the service to handle incoming requests using custom endpoints.

The following sections will introduce each feature in detail with specific examples.

------

## Initialization and Basic Running

**Features**

Create a minimal `AgentApp` instance and start a `SpringBoot`-based HTTP service skeleton.
In the initial state, the service only provides:

- Welcome page `/`
- Health check `/health`
- Readiness probe `/readiness`
- Liveness probe `/liveness`
- A2A protocol support `/a2a`

**Note**:

- The `/process` business processing endpoint is not exposed by default.
- The `Handler` class needs to implement the `streamQuery` method in `AgentScopeAgentHandler`, returning a `Flux<Event>` streaming result.

**Usage Example**

```java
MyAgentScopeAgentHandler agentHandler = new MyAgentScopeAgentHandler();
// Initialize agentHandler properties

AgentApp agentApp = new AgentApp(agentHandler);
// Service will be exposed at http://localhost:10001
agentApp.run("localhost",10001);
```

------

## A2A Streaming Output (SSE)

**Features**
Enable clients to receive generated results in real-time (suitable for chat, code generation, and other scenarios with progressive output).

**Usage Example (Client)**

```bash
curl --location --request POST 'http://localhost:10001/a2a/' \
--header 'Content-Type: application/json' \
--header 'Accept: */*' \
--header 'Host: localhost:10001' \
--header 'Connection: keep-alive' \
--data-raw '{
  "method": "message/stream",
  "id": "2d2b4dc8-8ea2-437b-888d-3aaf3a8239dc",
  "jsonrpc": "2.0",
  "params": {
    "configuration": {
      "blocking": false
    },
    "message": {
      "role": "user",
      "kind": "message",
      "metadata": {
        "userId": "me",
        "sessionId": "test1"
      },
      "parts": [
        {
          "text": "Hello, please calculate the 30th Fibonacci number using Python",
          "kind": "text"
        }
      ],
      "messageId": "c4911b64c8404b7a8bf7200dd225b152"
    }
  }
}'
```

**Response Format**

```bash
id:2d2b4dc8-8ea2-437b-888d-3aaf3a8239dc
event:jsonrpc
data:{"jsonrpc":"2.0","id":"2d2b4dc8-8ea2-437b-888d-3aaf3a8239dc","result":{"id":"8a3021a6-1b4e-4fc9-b70b-a39f03aa476c","contextId":"ea00d2b3-ce89-4707-b457-ef58a68b35ff","status":{"state":"submitted","timestamp":"2025-12-10T08:18:30.104001Z"},"artifacts":[],"history":[{"role":"user","parts":[{"text":"Hello, please calculate the 30th Fibonacci number using Python","kind":"text"}],"messageId":"c4911b64c8404b7a8bf7200dd225b152","contextId":"ea00d2b3-ce89-4707-b457-ef58a68b35ff","taskId":"8a3021a6-1b4e-4fc9-b70b-a39f03aa476c","metadata":{"userId":"me","sessionId":"test1"},"kind":"message"}],"kind":"task"}}

id:2d2b4dc8-8ea2-437b-888d-3aaf3a8239dc
event:jsonrpc
data:{"jsonrpc":"2.0","id":"2d2b4dc8-8ea2-437b-888d-3aaf3a8239dc","result":{"taskId":"8a3021a6-1b4e-4fc9-b70b-a39f03aa476c","status":{"state":"working","timestamp":"2025-12-10T08:18:30.107693Z"},"contextId":"ea00d2b3-ce89-4707-b457-ef58a68b35ff","final":false,"kind":"status-update"}}

id:2d2b4dc8-8ea2-437b-888d-3aaf3a8239dc
event:jsonrpc
data:{"jsonrpc":"2.0","id":"2d2b4dc8-8ea2-437b-888d-3aaf3a8239dc","result":{"taskId":"8a3021a6-1b4e-4fc9-b70b-a39f03aa476c","artifact":{"artifactId":"ef4de8cc-5042-4d17-800a-78257b7d51ba","name":"agent-response","parts":[{"text":"Calling tool run_ipython_cell with arguments: {\"code\":\"def fibonacci(n):\\n    if n <= 0:\\n        return 'Input should be a positive integer.'\\n    elif n == 1:\\n        return 0\\n    elif n == 2:\\n        return 1\\n    else:\\n        a, b = 0, 1\\n        for _ in range(2, n):\\n            a, b = b, a + b\\n        return b\\n\\n# Calculate the 30th Fibonacci number\\nfibonacci(30)\"} (call ID: call_3b7697e5158b44ea95cc11)","kind":"text"}],"metadata":{"type":"toolCall"}},"contextId":"ea00d2b3-ce89-4707-b457-ef58a68b35ff","append":false,"lastChunk":false,"kind":"artifact-update"}}

id:2d2b4dc8-8ea2-437b-888d-3aaf3a8239dc
event:jsonrpc
data:{"jsonrpc":"2.0","id":"2d2b4dc8-8ea2-437b-888d-3aaf3a8239dc","result":{"taskId":"8a3021a6-1b4e-4fc9-b70b-a39f03aa476c","artifact":{"artifactId":"ef4de8cc-5042-4d17-800a-78257b7d51ba","name":"agent-response","parts":[{"text":"Tool run_ipython_cell returned result: [{\"text\":\"{\\\"meta\\\":null,\\\"content\\\":[{\\\"type\\\":\\\"text\\\",\\\"text\\\":\\\"Out[1]: 514229\\\\n\\\",\\\"annotations\\\":null,\\\"description\\\":\\\"stdout\\\"}],\\\"isError\\\":false}\",\"type\":\"text\"}] (call ID: call_3b7697e5158b44ea95cc11)","kind":"text"}],"metadata":{"type":"toolResponse"}},"contextId":"ea00d2b3-ce89-4707-b457-ef58a68b35ff","append":true,"lastChunk":false,"kind":"artifact-update"}}

......

id:2d2b4dc8-8ea2-437b-888d-3aaf3a8239dc
event:jsonrpc
data:{"jsonrpc":"2.0","id":"2d2b4dc8-8ea2-437b-888d-3aaf3a8239dc","result":{"taskId":"8a3021a6-1b4e-4fc9-b70b-a39f03aa476c","status":{"state":"completed","message":{"role":"agent","parts":[{"text":"run_ipython_cellrun_ipython_cellThe 30th Fibonacci number is 514229.","kind":"text"}],"messageId":"a4258fb3-5b79-461d-934a-8802dd0bebb8","contextId":"ea00d2b3-ce89-4707-b457-ef58a68b35ff","taskId":"8a3021a6-1b4e-4fc9-b70b-a39f03aa476c","metadata":{"type":"final_response"},"kind":"message"},"timestamp":"2025-12-10T08:18:38.298168Z"},"contextId":"ea00d2b3-ce89-4707-b457-ef58a68b35ff","final":true,"kind":"status-update"}}
```

------

## Health Check Endpoints

**Features**

Automatically provides health probe endpoints for convenient container or cluster deployment.

**Endpoint List**

- `GET /health`: Returns status and timestamp
- `GET /readiness`: Determines if ready
- `GET /liveness`: Determines if alive
- `GET /`: Welcome message

**Usage Example**

```bash
curl http://localhost:8090/health
curl http://localhost:8090/readiness
curl http://localhost:8090/liveness
curl http://localhost:8090/
```

------

## Custom Agent Access Logic

**Features**

Implement the `streamQuery` method of `AgentScopeAgentHandler` as the actual execution logic triggered when the Agent is called.

### Basic Usage

```java
public Flux<io.agentscope.core.agent.Event> streamQuery(AgentRequest request, Object messages) {
    String sessionId = request.getSessionId();
    String userId = request.getUserId();

    try {
        // 1. Export session state from state service (if available)
        Map<String, Object> state = null;
        if (stateService != null) {
            try {
                state = stateService.exportState(userId, sessionId, null).join();
            } catch (Exception e) {
                logger.warn("Failed to export session state: {}", e.getMessage());
            }
        }

        // 2. Initialize toolkit and register available tools
        Toolkit toolkit = new Toolkit();

        if (sandboxService != null) {
            try {
                // Create sandbox instance for current session
                Sandbox sandbox = sandboxService.connect(userId, sessionId, BaseSandbox.class);
                // Register Python code execution tool
                toolkit.registerTool(ToolkitInit.RunPythonCodeTool(sandbox));
                logger.debug("Registered execute_python_code tool");
            } catch (Exception e) {
                logger.warn("Failed to create sandbox or register tool, skipping tool registration: {}", e.getMessage());
            }
        }

        // 3. Create short-term memory adapter (based on session history service)
        MemoryAdapter memory = null;
        if (sessionHistoryService != null) {
            memory = new MemoryAdapter(sessionHistoryService, userId, sessionId);
        }

        // 4. Create long-term memory adapter (if available)
        LongTermMemoryAdapter longTermMemory = null;
        if (memoryService != null) {
            longTermMemory = new LongTermMemoryAdapter(memoryService, userId, sessionId);
        }

        // 5. Build ReAct agent
        ReActAgent.Builder agentBuilder = ReActAgent.builder()
            .name("Friday")
            .sysPrompt("You are an intelligent assistant named Friday.")
            .toolkit(toolkit)
            .model(
                DashScopeChatModel.builder()
                    .apiKey(apiKey)
                    .modelName("qwen-max")
                    .stream(true)
                    .formatter(new DashScopeChatFormatter())
                    .build()
            );

        // Configure long-term memory (if exists)
        if (longTermMemory != null) {
            agentBuilder.longTermMemory(longTermMemory)
                        .longTermMemoryMode(LongTermMemoryMode.BOTH);
            logger.debug("Long-term memory configured");
        }

        // Configure short-term memory (if exists)
        if (memory != null) {
            agentBuilder.memory(memory);
            logger.debug("Short-term memory adapter configured");
        }

        ReActAgent agent = agentBuilder.build();

        // 6. Load session state (if exists)
        if (state != null && !state.isEmpty()) {
            try {
                agent.loadStateDict(state);
                logger.debug("Session state loaded, session ID: {}", sessionId);
            } catch (Exception e) {
                logger.warn("Failed to load state: {}", e.getMessage());
            }
        }

        // 7. Convert input messages to Msg list
        List<Msg> agentMessages;
        if (messages instanceof List) {
            @SuppressWarnings("unchecked")
            List<Msg> msgList = (List<Msg>) messages;
            agentMessages = msgList;
        } else if (messages instanceof Msg) {
            agentMessages = List.of((Msg) messages);
        } else {
            logger.warn("Unsupported input message type: {}, using empty message list",
                messages != null ? messages.getClass().getName() : "null");
            agentMessages = List.of();
        }

        // 8. Prepare query message: for multiple messages, store first N-1 in memory, use last one as current query
        Msg queryMessage;
        if (agentMessages.isEmpty()) {
            queryMessage = Msg.builder()
                .role(io.agentscope.core.message.MsgRole.USER)
                .build();
        } else if (agentMessages.size() == 1) {
            queryMessage = agentMessages.get(0);
        } else {
            for (int i = 0; i < agentMessages.size() - 1; i++) {
                agent.getMemory().addMessage(agentMessages.get(i));
            }
            queryMessage = agentMessages.get(agentMessages.size() - 1);
        }

        // 9. Configure streaming response options: include reasoning and tool call events, enable incremental output
        StreamOptions streamOptions = StreamOptions.builder()
            .eventTypes(EventType.REASONING, EventType.TOOL_RESULT)
            .incremental(true)
            .build();

        // 10. Start agent streaming inference and save final state when stream ends
        return agent.stream(queryMessage, streamOptions)
            .doOnNext(event -> logger.info("Agent event: {}", event))
            .doFinally(signalType -> {
                if (stateService != null) {
                    try {
                        Map<String, Object> finalState = agent.stateDict();
                        if (finalState != null && !finalState.isEmpty()) {
                            stateService.saveState(userId, finalState, sessionId, null)
                                .exceptionally(e -> {
                                    logger.error("Failed to save session state: {}", e.getMessage(), e);
                                    return null;
                                });
                        }
                    } catch (Exception e) {
                        logger.error("Exception occurred while saving state: {}", e.getMessage(), e);
                    }
                }
            })
            .doOnError(error -> logger.error("Agent streaming inference error: {}", error.getMessage(), error));

    } catch (Exception e) {
        logger.error("streamQuery execution exception: {}", e.getMessage(), e);
        return Flux.error(e);
    }
}
```

### Key Features

1. **Function Signature**:
    - `request`: Encapsulates the **request information** for this agent call
    - `messages`: Represents the incoming **message content**, which may be a single message, message list, or other intermediate representation
2. **Streaming Output**: The function acts as a generator, returning results via streaming
3. **State Management**: You can use `this.stateService` to save and restore state
4. **Sandbox Management**: You can use `this.sandboxService` to build sandbox tools
5. **Session History**: You can use `this.sessionHistoryService` to manage session history
6. **Memory Management**: You can use `this.memoryService` to manage long-term memory


### Complete Example: AgentApp with State Management

**AgentScopeDeployExample.java**

```java
import io.agentscope.runtime.app.AgentApp;
import io.agentscope.runtime.engine.services.agent_state.InMemoryStateService;
import io.agentscope.runtime.engine.services.memory.persistence.memory.service.InMemoryMemoryService;
import io.agentscope.runtime.engine.services.memory.persistence.session.InMemorySessionHistoryService;
import io.agentscope.runtime.engine.services.sandbox.SandboxService;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.client.config.KubernetesClientConfig;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;
import org.jetbrains.annotations.NotNull;

/**
 * AgentScope agent deployment example.
 *
 * <p>This example demonstrates how to start a local agent service based on ReActAgent, including the following capabilities:
 * <ul>
 *   <li>Session state management (in-memory storage)</li>
 *   <li>Short-term memory (session history) and long-term memory (user-level memory store)</li>
 *   <li>Python sandbox tool support (isolated execution environment via Kubernetes)</li>
 *   <li>Streaming agent inference service via HTTP interface</li>
 * </ul>
 *
 * <p>Please ensure the environment variable {@code AI_DASHSCOPE_API_KEY} is set before starting.
 */
public class AgentScopeDeployExample {

    public static void main(String[] args) {
        // Verify DashScope API key is configured
        if (System.getenv("AI_DASHSCOPE_API_KEY") == null) {
            System.err.println("Error: Environment variable AI_DASHSCOPE_API_KEY is not set");
            System.exit(1);
        }

        runAgent();
    }

    /**
     * Initialize and start the agent service.
     */
    private static void runAgent() {
        // Create custom agent handler
        MyAgentScopeAgentHandler agentHandler = new MyAgentScopeAgentHandler();

        // Configure in-memory state and memory services (suitable for development/testing)
        agentHandler.setStateService(new InMemoryStateService());
        agentHandler.setSessionHistoryService(new InMemorySessionHistoryService());
        agentHandler.setMemoryService(new InMemoryMemoryService());

        // Configure sandbox service (for secure Python tool code execution)
        agentHandler.setSandboxService(buildSandboxService());

        // Start Agent service application, listening on localhost:10001
        AgentApp agentApp = new AgentApp(agentHandler);
        agentApp.run("localhost", 10001);
    }

    /**
     * Build sandbox service instance, using Kubernetes client configuration by default.
     *
     * @return Configured SandboxService instance
     */
    @NotNull
    private static SandboxService buildSandboxService() {
        // Use default Kubernetes configuration (can be replaced with actual cluster configuration for deployment)
        var clientConfig = KubernetesClientConfig.builder().build();
        var managerConfig = ManagerConfig.builder()
            .containerDeployment(clientConfig)
            .build();

        return new SandboxService(new SandboxManager(managerConfig));
    }
}
```

**MyAgentScopeAgentHandler.java**

```java
import java.util.List;
import java.util.Map;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.EventType;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.memory.LongTermMemoryMode;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.runtime.adapters.agentscope.AgentScopeAgentHandler;
import io.agentscope.runtime.adapters.agentscope.memory.LongTermMemoryAdapter;
import io.agentscope.runtime.adapters.agentscope.memory.MemoryAdapter;
import io.agentscope.runtime.engine.agents.agentscope.tools.ToolkitInit;
import io.agentscope.runtime.engine.schemas.AgentRequest;
import io.agentscope.runtime.sandbox.box.BaseSandbox;
import io.agentscope.runtime.sandbox.box.Sandbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

/**
 * Example implementation of AgentScope agent handler.
 *
 * <p>This implementation supports the following core features:
 * <ul>
 *   <li>Load and save session state from state service</li>
 *   <li>Integrate short-term memory (based on session history) and long-term memory (based on user memory store)</li>
 *   <li>Dynamically register tools (such as Python sandbox execution environment)</li>
 *   <li>Use Qwen large model for streaming inference</li>
 *   <li>Return the entire inference process as an event stream (including reasoning steps, tool calls, etc.)</li>
 * </ul>
 */
public class MyAgentScopeAgentHandler extends AgentScopeAgentHandler {
    private static final Logger logger = LoggerFactory.getLogger(MyAgentScopeAgentHandler.class);
    private final String apiKey;

    /**
     * Constructor: Load DashScope API key from environment variable.
     */
    public MyAgentScopeAgentHandler() {
        this.apiKey = System.getenv("AI_DASHSCOPE_API_KEY");
    }

    @Override
    public Flux<io.agentscope.core.agent.Event> streamQuery(AgentRequest request, Object messages) {
        String sessionId = request.getSessionId();
        String userId = request.getUserId();

        try {
            // 1. Try to load persisted state for current session from state service
            Map<String, Object> state = null;
            if (stateService != null) {
                try {
                    state = stateService.exportState(userId, sessionId, null).join();
                } catch (Exception e) {
                    logger.warn("Failed to load session state: {}", e.getMessage());
                }
            }

            // 2. Initialize toolkit and register available tools
            Toolkit toolkit = new Toolkit();
            if (sandboxService != null) {
                try {
                    Sandbox sandbox = sandboxService.connect(userId, sessionId, BaseSandbox.class);
                    toolkit.registerTool(ToolkitInit.RunPythonCodeTool(sandbox));
                    logger.debug("Python code execution tool registered");
                } catch (Exception e) {
                    logger.warn("Sandbox initialization or tool registration failed, skipping tool support: {}", e.getMessage());
                }
            }

            // 3. Create short-term memory adapter (for managing current session message history)
            MemoryAdapter memory = null;
            if (sessionHistoryService != null) {
                memory = new MemoryAdapter(sessionHistoryService, userId, sessionId);
            }

            // 4. Create long-term memory adapter (for accessing user-level persistent memory)
            LongTermMemoryAdapter longTermMemory = null;
            if (memoryService != null) {
                longTermMemory = new LongTermMemoryAdapter(memoryService, userId, sessionId);
            }

            // 5. Build ReAct agent instance
            ReActAgent.Builder agentBuilder = ReActAgent.builder()
                .name("Friday")
                .sysPrompt("You are an intelligent assistant named Friday.")
                .toolkit(toolkit)
                .model(
                    DashScopeChatModel.builder()
                        .apiKey(apiKey)
                        .modelName("qwen-max")
                        .stream(true)
                        .formatter(new DashScopeChatFormatter())
                        .build()
                );

            if (longTermMemory != null) {
                agentBuilder.longTermMemory(longTermMemory)
                            .longTermMemoryMode(LongTermMemoryMode.BOTH);
                logger.debug("Long-term memory enabled");
            }

            if (memory != null) {
                agentBuilder.memory(memory);
                logger.debug("Short-term memory adapter configured");
            }

            ReActAgent agent = agentBuilder.build();

            // 6. If state data exists, restore agent internal state
            if (state != null && !state.isEmpty()) {
                try {
                    agent.loadStateDict(state);
                    logger.debug("Session state restored successfully, session ID: {}", sessionId);
                } catch (Exception e) {
                    logger.warn("Failed to restore state: {}", e.getMessage());
                }
            }

            // 7. Convert input messages to standard Msg list
            List<Msg> agentMessages;
            if (messages instanceof List) {
                @SuppressWarnings("unchecked")
                List<Msg> msgList = (List<Msg>) messages;
                agentMessages = msgList;
            } else if (messages instanceof Msg) {
                agentMessages = List.of((Msg) messages);
            } else {
                logger.warn("Unsupported message type: {}, using empty message list", 
                    messages != null ? messages.getClass().getName() : "null");
                agentMessages = List.of();
            }

            // 8. Prepare query message: for multiple messages, store first N-1 in memory, use last one as current query
            Msg queryMessage;
            if (agentMessages.isEmpty()) {
                queryMessage = Msg.builder()
                    .role(io.agentscope.core.message.MsgRole.USER)
                    .build();
            } else if (agentMessages.size() == 1) {
                queryMessage = agentMessages.get(0);
            } else {
                for (int i = 0; i < agentMessages.size() - 1; i++) {
                    agent.getMemory().addMessage(agentMessages.get(i));
                }
                queryMessage = agentMessages.get(agentMessages.size() - 1);
            }

            // 9. Configure streaming output options: include reasoning process and tool call results, enable incremental mode
            StreamOptions streamOptions = StreamOptions.builder()
                .eventTypes(EventType.REASONING, EventType.TOOL_RESULT)
                .incremental(true)
                .build();

            // 10. Start streaming inference and automatically save final state when stream ends
            return agent.stream(queryMessage, streamOptions)
                .doOnNext(event -> logger.info("Agent event: {}", event))
                .doFinally(signalType -> {
                    if (stateService != null) {
                        try {
                            Map<String, Object> finalState = agent.stateDict();
                            if (finalState != null && !finalState.isEmpty()) {
                                stateService.saveState(userId, finalState, sessionId, null)
                                    .exceptionally(e -> {
                                        logger.error("Failed to save session state: {}", e.getMessage(), e);
                                        return null;
                                    });
                            }
                        } catch (Exception e) {
                            logger.error("Exception occurred while saving state: {}", e.getMessage(), e);
                        }
                    }
                })
                .doOnError(error -> logger.error("Agent streaming inference error: {}", error.getMessage(), error));

        } catch (Exception e) {
            logger.error("streamQuery execution exception: {}", e.getMessage(), e);
            return Flux.error(e);
        }
    }

    @Override
    public boolean isHealthy() {
        return true;
    }

    @Override
    public String getName() {
        return "DemoAgent";
    }

    @Override
    public String getDescription() {
        return "An intelligent assistant example implemented based on AgentScope.";
    }
}
```

## Local Deployment

**Features**

Start the local application with one click via the `run()` method.

**Usage Example**

```java
agentApp.run("localhost", 10001);
```

For more deployment options and detailed instructions, please refer to the [Advanced Deployment](advanced_deployment.md) documentation.

AgentScope Runtime provides Serverless deployment solutions. You can deploy your Agent application to K8s or AgentRun. Refer to the [Advanced Deployment](advanced_deployment.md) documentation for more configuration details on K8s and AgentRun deployment.

