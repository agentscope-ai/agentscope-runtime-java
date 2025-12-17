# Quick Start

This tutorial demonstrates how to build a simple agent application in the **AgentScope Runtime Java** framework and deploy it as a service.

## Prerequisites

### 🔧 Installation Requirements

Add AgentScope Runtime Java adapter dependencies for the AgentScope framework and application startup dependencies. Base dependencies are already included through transitive dependencies of the adapter:

```xml
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-runtime-agentscope</artifactId>
    <version>1.0.0</version>
</dependency>

<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-runtime-web</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 🔑 API Key Configuration

You need to provide an API key for your chosen large language model provider. This example uses Alibaba Cloud's Qwen model, with DashScope as the service provider, so you need to use its API_KEY. You can set the key as an environment variable as follows:

```bash
export DASHSCOPE_API_KEY="your_api_key_here"
```

## Step-by-Step Implementation

### Step 1: Build Agent and Execution Logic

#### 1.1 Import Dependencies

First, import all necessary dependencies:

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
```

#### 1.2 Build Implementation of AgentScopeAgentHandler Interface

The following four methods define the properties and execution logic of the entire application. The `isHealthy` method returns the health status of the current application, the `getName` method returns the name of the current application, the `getDescription` method returns the description of the current application, and the **`streamQuery`** method is the core execution logic of the entire AgentScopeAgentHandler, used for setting memory, building the Agent, and customizing Agent execution logic.

```java
public class MyAgentScopeAgentHandler extends AgentScopeAgentHandler {
    @Override
    public boolean isHealthy() {
        return false;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public Flux<?> streamQuery(AgentRequest request, Object messages) {
        return null;
    }
}
```

#### 1.3 Implement Core Execution Logic streamQuery Method

##### 1.3.1 Get Necessary ID Information

```java
String sessionId = request.getSessionId();
String userId = request.getUserId();
```

##### 1.3.2 Export Historical State from StateService

```java
Map<String, Object> state = null;
if (stateService != null) {
  try {
    state = stateService.exportState(userId, sessionId, null).join();
  }
  catch (Exception e) {
    logger.warn("Failed to export state: {}", e.getMessage());
  }
}
```

- **Purpose**: Restore the **previous round's Agent state** for this user in this session (e.g., internal variables, conversation phase, task progress, etc.).
- `roundId = null` means get the **latest round's** state.
- Use `.join()` to block and wait (because subsequent Agent construction requires synchronous state).
- On failure, only warn, don't affect the main flow (Agent can start from empty state).

##### 1.3.3 Create Toolkit and Register Tools

```java
Toolkit toolkit = new Toolkit();
if (sandboxService != null) {
    Sandbox sandbox = sandboxService.connect(userId, sessionId, BaseSandbox.class);
    toolkit.registerTool(ToolkitInit.RunPythonCodeTool(sandbox));
}
```

- **Toolkit**: Collection of tools that the Agent can call.
- **Sandbox**: Secure sandbox environment, including base sandbox, file system sandbox, browser sandbox, etc. Each `(userId, sessionId)` has an independent instance.
- Register sandbox tools.
- If sandbox creation fails, skip tool registration. The Agent can still run but cannot call this tool.

##### 1.3.4 Create Short-Term Memory Adapter (MemoryAdapter)

```java
MemoryAdapter memory = null;
if (sessionHistoryService != null) {
    memory = new MemoryAdapter(sessionHistoryService, userId, sessionId);
}
```

- **Function**: Provides **historical message records for the current session** (such as conversation history between user and Agent).
- `sessionHistoryService` is the underlying storage service.
- The adapter converts the generic service interface to the `Memory` interface required by the AgentScope framework.

##### 1.3.5 Create Long-Term Memory Adapter (LongTermMemoryAdapter)

```java
LongTermMemoryAdapter longTermMemory = null;
if (memoryService != null) {
    longTermMemory = new LongTermMemoryAdapter(memoryService, userId, sessionId);
}
```

- **Function**: Access the user's **cross-session long-term memory** (such as personal preferences, knowledge base summaries, etc.).
- Usually based on vector databases or structured storage.
- Later, we will configure the Agent to **reference both short-term and long-term memory** when generating responses.

##### 1.3.6 Build ReActAgent Instance

```java
ReActAgent.Builder agentBuilder = ReActAgent.builder()
        .name("Friday")
        .sysPrompt("You're a helpful assistant named Friday.")
        .toolkit(toolkit)
        .model(
            DashScopeChatModel.builder()
                .apiKey(apiKey)
                .modelName("qwen-max")
                .stream(true)
                .formatter(new DashScopeChatFormatter())
                .build());
```

- Use **Builder pattern** to assemble Agent:
  - Name, system prompt
  - Bind toolset (`toolkit`)
  - Configure large model (here using Qwen `qwen-max` via DashScope API)
  - Enable streaming output (`.stream(true)`)
- **Note**: At this point, the Agent has not yet loaded state or memory.

##### 1.3.7 Inject Memory Modules

```java
if (longTermMemory != null) {
    agentBuilder.longTermMemory(longTermMemory)
                .longTermMemoryMode(LongTermMemoryMode.BOTH);
}
if (memory != null) {
    agentBuilder.memory(memory);
}
```

- **`memory`** → Short-term memory (current session history)
- **`longTermMemory`** → Long-term memory (cross-session knowledge)
- `LongTermMemoryMode.BOTH`: Indicates that long-term memory is used in both **thinking and generation** phases.

##### 1.3.8 Load Historical State into Agent

```java
if (state != null && !state.isEmpty()) {
    agent.loadStateDict(state);
}
```

- Deserialize the state dictionary obtained in Step 2 into the Agent's internal state.
- Enables the Agent to "continue" from the previous conversation state (e.g., continue unfinished tasks).

##### 1.3.9 Process Input Messages (messages)

```java
List<Msg> agentMessages;
if (messages instanceof List) {
  @SuppressWarnings("unchecked")
  List<Msg> msgList = (List<Msg>) messages;
  agentMessages = msgList;
}
else if (messages instanceof Msg) {
  agentMessages = List.of((Msg) messages);
}
else {
  logger.warn("Unexpected messages type: {}, using empty list",
      messages != null ? messages.getClass().getName() : "null");
  agentMessages = List.of();
}

Msg queryMessage;
if (agentMessages.size() > 1) {
    // Add the first N-1 messages to memory
    for (int i = 0; i < agentMessages.size() - 1; i++) {
        agent.getMemory().addMessage(agentMessages.get(i));
    }
    queryMessage = agentMessages.get(agentMessages.size() - 1); // Use the last one as the current query
} else {
    queryMessage = agentMessages.get(0) or empty Msg;
}
```

##### 1.3.10 Start Streaming Inference and Return Event Stream

```java
StreamOptions streamOptions = StreamOptions.builder()
    .eventTypes(EventType.REASONING, EventType.TOOL_RESULT)
    .incremental(true)
    .build();

Flux<Event> agentScopeEvents = agent.stream(queryMessage, streamOptions);
```

- **`stream()`**: Starts the ReAct loop (Thought → Action → Observation → ... → Final Answer)
- **`StreamOptions`**:
  - `eventTypes`: Only return reasoning steps and tool results (filter out internal logs, etc.)
  - `incremental = true`: Enable incremental streaming output (e.g., character-by-character generation)
- Returns **AgentScope native Event stream** (not Runtime Event), converted by the outer `StreamAdapter`.

##### 1.3.11 Save Final State When Stream Completes

```java
return agentScopeEvents
      .doOnNext(event -> {
        logger.info("Agent event: {}", event);
      })
      .doFinally(signalType -> {
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
          }
          catch (Exception e) {
            logger.error("Error saving state: {}", e.getMessage(), e);
          }
        }
      })
      .doOnError(error -> {
        logger.error("Error in agent stream: {}", error.getMessage(), error);
      });
```

- **Regardless of success/failure/cancellation**, save the Agent's **final state** when the stream ends.
- `roundId = null` → Automatically assign a new round ID (see `InMemoryStateService` implementation).
- Use `exceptionally` to handle save exceptions, avoiding impact on the main flow.

> 🔁 Implements "state persistence loop": Load → Execute → Save.

### Step 2: Build AgentApp

#### 2.1 Import Dependencies

```java
import io.agentscope.runtime.app.AgentApp;
import io.agentscope.runtime.engine.services.agent_state.InMemoryStateService;
import io.agentscope.runtime.engine.services.memory.persistence.memory.service.InMemoryMemoryService;
import io.agentscope.runtime.engine.services.memory.persistence.session.InMemorySessionHistoryService;
import io.agentscope.runtime.engine.services.sandbox.SandboxService;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.client.config.BaseClientConfig;
import io.agentscope.runtime.sandbox.manager.client.config.KubernetesClientConfig;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;
import org.jetbrains.annotations.NotNull;
```

#### 2.2 Build SandboxService Sandbox Management Service

```java
private static SandboxService buidSandboxService() {
  BaseClientConfig clientConfig = KubernetesClientConfig.builder().build();
  ManagerConfig managerConfig = ManagerConfig.builder()
      .containerDeployment(clientConfig)
      .build();
  return new SandboxService(
      new SandboxManager(managerConfig)
  );
}
```

* Sandbox runtime environment supports **Docker**, **K8s**, and **AgentRun**. If `clientConfig` is not configured, **local Docker** is used as the runtime environment by default
* Use `managerConfig` to build **SandboxManager**, and then build **SandboxService**

#### 2.3 Build AgentApp

##### 2.3.1 Initialize agentHandler

```java
MyAgentScopeAgentHandler agentHandler = new MyAgentScopeAgentHandler();
agentHandler.setStateService(new InMemoryStateService());
agentHandler.setSessionHistoryService(new InMemorySessionHistoryService());
agentHandler.setMemoryService(new InMemoryMemoryService());
agentHandler.setSandboxService(buidSandboxService());
```

Instantiate the **AgentScopeAgentHandler** class we just wrote and register services.

##### 2.3.2 Build AgentApp and Start with One Click

```java
AgentApp agentApp = new AgentApp(agentHandler);
agentApp.run(10001);
```

Initialize **AgentApp** using the instantiated **AgentScopeAgentHandler** class and start on port 10001.

### Step 3: Access Agent via A2A Protocol

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
          "text": "你好，给我用python计算一下第10个斐波那契数",
          "kind": "text"
        }
      ],
      "messageId": "c4911b64c8404b7a8bf7200dd225b152"
    }
  }
}'
```

You will see a response streamed in **Server-Sent Events (SSE)** format:

```
id:2d2b4dc8-8ea2-437b-888d-3aaf3a8239dc
event:jsonrpc
data:{"jsonrpc":"2.0","id":"2d2b4dc8-8ea2-437b-888d-3aaf3a8239dc","result":{"id":"92ccdc36-006f-4d66-a47c-18d0cb171506","contextId":"fd5bccd1-770f-4872-8c9a-f086c094f90a","status":{"state":"submitted","timestamp":"2025-12-09T10:53:47.612001Z"},"artifacts":[],"history":[{"role":"user","parts":[{"text":"你好，给我用python计算一下第10个斐波那契数","kind":"text"}],"messageId":"c4911b64c8404b7a8bf7200dd225b152","contextId":"fd5bccd1-770f-4872-8c9a-f086c094f90a","taskId":"92ccdc36-006f-4d66-a47c-18d0cb171506","metadata":{"userId":"me","sessionId":"test1"},"kind":"message"}],"kind":"task"}}

id:2d2b4dc8-8ea2-437b-888d-3aaf3a8239dc
event:jsonrpc
data:{"jsonrpc":"2.0","id":"2d2b4dc8-8ea2-437b-888d-3aaf3a8239dc","result":{"taskId":"92ccdc36-006f-4d66-a47c-18d0cb171506","status":{"state":"working","timestamp":"2025-12-09T10:53:47.614736Z"},"contextId":"fd5bccd1-770f-4872-8c9a-f086c094f90a","final":false,"kind":"status-update"}}

......

id:2d2b4dc8-8ea2-437b-888d-3aaf3a8239dc
event:jsonrpc
data:{"jsonrpc":"2.0","id":"2d2b4dc8-8ea2-437b-888d-3aaf3a8239dc","result":{"taskId":"92ccdc36-006f-4d66-a47c-18d0cb171506","artifact":{"artifactId":"293bb1b0-1442-4ca2-997f-575b798dfad1","name":"agent-response","parts":[{"text":"是55。","kind":"text"}],"metadata":{"type":"chunk"}},"contextId":"fd5bccd1-770f-4872-8c9a-f086c094f90a","append":true,"lastChunk":false,"kind":"artifact-update"}}

id:2d2b4dc8-8ea2-437b-888d-3aaf3a8239dc
event:jsonrpc
data:{"jsonrpc":"2.0","id":"2d2b4dc8-8ea2-437b-888d-3aaf3a8239dc","result":{"taskId":"92ccdc36-006f-4d66-a47c-18d0cb171506","status":{"state":"completed","message":{"role":"agent","parts":[{"text":"run_ipython_cellrun_ipython_cell第10个斐波那契数是55。","kind":"text"}],"messageId":"7b878071-d63e-4710-81e0-91d50a57c373","contextId":"fd5bccd1-770f-4872-8c9a-f086c094f90a","taskId":"92ccdc36-006f-4d66-a47c-18d0cb171506","metadata":{"type":"final_response"},"kind":"message"},"timestamp":"2025-12-09T10:53:51.538933Z"},"contextId":"fd5bccd1-770f-4872-8c9a-f086c094f90a","final":true,"kind":"status-update"}}
```

## Chapter Guide

The following chapters include the following sections:
- [Sandbox and Tools](tool.md): Help you add tools to your Agent
- [Deployment](deployment.md): Help you deploy your Agent and package it as a service
- [Usage](use.md): Help you call the deployed service
- [How to Contribute](contribute.md): Reference documentation for contributing code to this project
