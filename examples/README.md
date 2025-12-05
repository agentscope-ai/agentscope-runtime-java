# AgentScope Runtime Java Examples

This guide walks through the end-to-end workflow for running the sample projects under `examples/`. Each section expands on the building blocks required to spin up an agent, configure the sandboxed execution environment, and expose the agent through the A2A interface.

> **Tip:** The snippets below are extracted from the folders inside `examples/simple_agent_use_examples`. It shows you the quickest way to build your first agent the deploy it in your computer. Refer to the project files for complete runnable code.

---

## Prerequisites

- Java 17 or later
- Maven 3.6+
- Docker daemon running locally (required for sandboxed tools)
- Optional: Redis, Kubernetes, or Alibaba AgentRun credentials depending on your sandbox backend of choice

---

## Step 1 – Create AgentScope Agent

Create a custom agent handler by extending `AgentScopeAgentHandler`. This handler will manage the agent lifecycle, including state management, memory, and tool registration.

### 1.1 Create Agent By Extending AgentScopeAgentHandler

```java
public class MyAgentScopeAgentHandler extends AgentScopeAgentHandler {

    @Override
    public Flux<io.agentscope.core.agent.Event> streamQuery(AgentRequest request, Object messages) {
        String sessionId = request.getSessionId();
        String userId = request.getUserId();

        try {
            // Export state from StateService
            Map<String, Object> state = null;
            if (stateService != null) {
                try {
                    state = stateService.exportState(userId, sessionId, null).join();
                } catch (Exception e) {
                    logger.warn("Failed to export state: {}", e.getMessage());
                }
            }

            // Create Toolkit and register tools
            Toolkit toolkit = new Toolkit();

            if (sandboxService != null) {
                try {
                    Sandbox sandbox = sandboxService.connect(userId, sessionId, BaseSandbox.class);
                    toolkit.registerTool(ToolkitInit.RunPythonCodeTool(sandbox));
                } catch (Exception e) {
                    logger.warn("Failed to create sandbox or register tools: {}", e.getMessage());
                }
            }

            // Create MemoryAdapter for session history
            MemoryAdapter memory = null;
            if (sessionHistoryService != null) {
                memory = new MemoryAdapter(sessionHistoryService, userId, sessionId);
            }

            // Create LongTermMemoryAdapter
            LongTermMemoryAdapter longTermMemory = null;
            if (memoryService != null) {
                longTermMemory = new LongTermMemoryAdapter(memoryService, userId, sessionId);
            }

            // Create ReActAgent
            ReActAgent.Builder agentBuilder = ReActAgent.builder()
                    .name("Friday")
                    .sysPrompt("You're a helpful assistant named Friday.")
                    .toolkit(toolkit)
                    .model(DashScopeChatModel.builder()
                            .apiKey(apiKey)
                            .modelName("qwen-max")
                            .stream(true)
                            .formatter(new DashScopeChatFormatter())
                            .build());

            if (longTermMemory != null) {
                agentBuilder.longTermMemory(longTermMemory)
                        .longTermMemoryMode(LongTermMemoryMode.BOTH);
            }

            if (memory != null) {
                agentBuilder.memory(memory);
            }

            ReActAgent agent = agentBuilder.build();

            // Load state if available
            if (state != null && !state.isEmpty()) {
                try {
                    agent.loadStateDict(state);
                } catch (Exception e) {
                    logger.warn("Failed to load state: {}", e.getMessage());
                }
            }

            // Convert messages to List<Msg>
            List<Msg> agentMessages;
            if (messages instanceof List) {
                @SuppressWarnings("unchecked")
                List<Msg> msgList = (List<Msg>) messages;
                agentMessages = msgList;
            } else if (messages instanceof Msg) {
                agentMessages = List.of((Msg) messages);
            } else {
                logger.warn("Unexpected messages type: {}",
                    messages != null ? messages.getClass().getName() : "null");
                agentMessages = List.of();
            }

            // Prepare query message
            Msg queryMessage;
            if (agentMessages.isEmpty()) {
                queryMessage = Msg.builder().role(MsgRole.USER).build();
            } else if (agentMessages.size() == 1) {
                queryMessage = agentMessages.get(0);
            } else {
                // Add all but the last to memory, use the last one for query
                for (int i = 0; i < agentMessages.size() - 1; i++) {
                    agent.getMemory().addMessage(agentMessages.get(i));
                }
                queryMessage = agentMessages.get(agentMessages.size() - 1);
            }

            // Configure streaming options
            StreamOptions streamOptions = StreamOptions.builder()
                    .eventTypes(EventType.REASONING, EventType.TOOL_RESULT)
                    .incremental(true)
                    .build();

            // Stream agent responses
            return agent.stream(queryMessage, streamOptions)
                    .doFinally(signalType -> {
                        // Save state after completion
                        if (stateService != null) {
                            try {
                                Map<String, Object> finalState = agent.stateDict();
                                if (finalState != null && !finalState.isEmpty()) {
                                    stateService.saveState(userId, finalState, sessionId, null)
                                            .exceptionally(e -> {
                                                logger.error("Failed to save state: {}", e.getMessage(), e);
                                                return null;
                                            });
                                }
                            } catch (Exception e) {
                                logger.error("Error saving state: {}", e.getMessage(), e);
                            }
                        }
                    })
                    .doOnError(error -> {
                        logger.error("Error in agent stream: {}", error.getMessage(), error);
                    });
        } catch (Exception e) {
            logger.error("Error in streamQuery: {}", e.getMessage(), e);
            return Flux.error(e);
        }
    }
}
```

---

## Step 2 – Initialize Runtime Services

AgentScope Runtime Java provides in-memory implementations of the required services. You'll need to initialize these services and configure them with your agent handler:

- **StateService** – manages agent state persistence
- **SessionHistoryService** – manages conversation history
- **MemoryService** – manages long-term memory
- **SandboxService** – manages sandbox lifecycle for tool execution

```java
// Initialize services
agentHandler.setStateService(new InMemoryStateService());
agentHandler.setSessionHistoryService(new InMemorySessionHistoryService());
agentHandler.setMemoryService(new InMemoryMemoryService());
agentHandler.setSandboxService(buidSandboxService()); // see step 3
```

---

## Step 3 – Configure the Sandbox Service

Create a `SandboxService` by configuring the `SandboxManager` with `ManagerConfig`. This controls how sandboxes are created, maintained, and supplied with supporting resources. The defaults use an in-memory lifecycle manager, local Docker runtime, and no pre-provisioned files. You can opt into additional providers as needed.

```java
private static SandboxService buildSandboxService() {
    BaseClientConfig clientConfig = KubernetesClientConfig.builder().build();
    ManagerConfig managerConfig = ManagerConfig.builder()
            .containerDeployment(clientConfig)
            .build();
    return new SandboxService(new SandboxManager(managerConfig));
}
```

### 3.1 Container Runtime

Docker is selected by default. You can swap in Kubernetes or Alibaba AgentRun by providing the corresponding client configuration.

```java
BaseClientConfig dockerConfig = DockerClientConfig.builder()
    .host("127.0.0.1")
    .port(2375)
    .build();
```

```java
BaseClientConfig k8sConfig = KubernetesClientConfig.builder()
    .kubeConfigPath(System.getenv("KUBECONFIG_PATH"))
    .build();
```

```java
BaseClientConfig agentRunConfig = AgentRunClientConfig.builder()
    .agentRunAccessKeyId(System.getenv("AGENT_RUN_ACCESS_KEY_ID"))
    .agentRunAccountId(System.getenv("AGENT_RUN_ACCOUNT_ID"))
    .agentRunAccessKeySecret(System.getenv("AGENT_RUN_ACCESS_KEY_SECRET"))
    .build();
```

### 3.2 Configure remote runtime hosting sandbox

If you want to use a remote runtime to proxy all sandbox lifecycle management and tool invocation work for the current runtime, you can configure the `baseURL` and `bearerToken` properties in the manageability config. The default runtime sandbox will be managed by itself.

```java
ManagerConfig config = ManagerConfig.builder()
    .baseUrl("Remote Runtime Base Url")
    .bearerToken("Remote Runtime Bearer Token")
    .build();
```

### 3.3 Pool Size and Port Range

```java
ManagerConfig config = ManagerConfig.builder()
    .poolSize(0) // number of warm sandboxes to maintain
    .portRange(new PortRange(49152, 59152))
    .fileSystemConfig(localConfig)
    .containerDeployment(dockerConfig)
    .redisConfig(redisConfig)
    .build();
```

---

## Step 4 – Configure and Deploy the Agent

Configure the agent handler with the required services and deploy using `AgentApp`:

```java
// Create and configure the agent handler
MyAgentScopeAgentHandler agentHandler = new MyAgentScopeAgentHandler();
agentHandler.setStateService(new InMemoryStateService());
agentHandler.setSessionHistoryService(new InMemorySessionHistoryService());
agentHandler.setMemoryService(new InMemoryMemoryService());
agentHandler.setSandboxService(buildSandboxService());

// Deploy using AgentApp
AgentApp agentApp = new AgentApp(agentHandler);
agentApp.run(10001); // Server will listen on port 10001
```

The `AgentApp` automatically handles:
- Creating and managing the `Runner`
- Setting up the A2A protocol endpoints
- Starting the HTTP server
- Converting framework events to runtime events

---

## Step 5 – Call the A2A Endpoint

Use any HTTP client to stream messages to the deployed agent. The example below triggers a Python calculation via the base sandbox.

```bash
curl --location --request POST 'http://localhost:10001/a2a/' \
  --header 'Content-Type: application/json' \
  --data-raw '{
    "method": "message/stream",
    "id": "2d2b4dc8-8ea2-437b-888d-3aaf3a8239dc",
    "jsonrpc": "2.0",
    "params": {
      "message": {
        "role": "user",
        "kind": "message",
        "contextId": "okokok",
        "metadata": {
          "userId": "me",
          "sessionId": "test12"
        },
        "parts": [
          {
            "kind": "text",
            "text": "Hello, please calculate the 10th Fibonacci number using Python"
          }
        ],
        "messageId": "c4911b64c8404b7a8bf7200dd225b152"
      }
    }
  }'
```

Change the prompt to query for the current time to exercise an MCP-backed tool instead. The response format follows the A2A streaming specification.

---

## Where to Go Next

- Browse **complete implementations** in `examples/simple_agent_use_examples/agentscope_use_example`
- Go to the `browser_use_fullstack_runtime` folder and try to **visualize the Agent's operations** in the sandbox
- Experiment with **different sandbox combinations and toolchains**
- Learn how to create **custom tools** using the `@Tool` annotation
- Explore **MCP (Model Context Protocol) tools** integration
