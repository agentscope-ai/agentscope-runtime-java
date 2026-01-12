# Services and Adapters

## Overview

Services (`Service`) in AgentScope Runtime Java provide core capabilities for the agent runtime environment, including:

- **Session History Management**
- **Memory Storage**
- **Sandbox Management**
- **Agent State Management**

All services implement a unified abstract interface `ServiceWithLifecycleManager` (lifecycle management mode), providing standard methods:

- `start()`: Start the service
- `stop()`: Stop the service
- `health()`: Check service health status

> When actually writing agent applications, we usually **do not directly operate various underlying methods of these services**, but use them through **framework adapters (Adapters)**. Adapters will:
> 
> 1. Be responsible for injecting Runtime service objects into compatible modules of the agent framework
> 2. Enable agents within the framework to seamlessly call Runtime-provided functionality (such as session memory, tool sandboxes, etc.)
> 3. Ensure service lifecycle is consistent with Runner/Engine

## Why Use Services Through Adapters?

- **Decoupling**: Agent frameworks don't need to directly perceive underlying service implementations
- **Cross-framework Reuse**: The same services can be integrated into different agent frameworks
- **Unified Lifecycle**: Runner/Engine uniformly starts and shuts down all services
- **Enhanced Maintainability**: When switching service implementations (such as switching to database storage), there's no need to modify agent business code

## Available Services and Adapter Usage

### 1. Session History Service (SessionHistoryService)

Manages user-agent conversation sessions, storing and retrieving session message history.

#### AgentScope Usage

In the AgentScope framework, bind the session history service to the `Memory` module through Runtime's `MemoryAdapter` adapter:

```java
import io.agentscope.runtime.adapters.agentscope.memory.MemoryAdapter;
import io.agentscope.runtime.engine.services.memory.persistence.session.InMemorySessionHistoryService;
import io.agentscope.runtime.engine.services.memory.service.SessionHistoryService;

public class Main {
    public static void main(String[] args) {
        SessionHistoryService sessionHistoryService = new InMemorySessionHistoryService();
        MemoryAdapter memory = null;

        memory = new MemoryAdapter(
                sessionHistoryService,
                "User1",
                "TestSession"
        );
    }
}
```

For more available service types and detailed usage, see [Session History Service](session_history.md).

### 2. Memory Service (MemoryService)

`MemoryService` manages long-term memory storage. In Agents, memory stores previous conversations from end users. For example, end users may have mentioned their name in previous conversations. Memory services are typically used to **cross-session** store this information so agents can use it in the next conversation.

#### AgentScope Usage

In the AgentScope framework, bind the session history service to the `LongTermMemory` module through Runtime's `AgentScopeLongTermMemory` adapter:

```java
import io.agentscope.core.tool.Toolkit;
import io.agentscope.runtime.engine.agents.agentscope.tools.ToolkitInit;
import io.agentscope.runtime.sandbox.box.BrowserSandbox;
import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.manager.ManagerConfig;
import io.agentscope.runtime.sandbox.manager.SandboxService;
import io.agentscope.runtime.sandbox.manager.client.container.BaseClientStarter;
import io.agentscope.runtime.sandbox.manager.client.container.docker.DockerClientStarter;

public class Main {
    public static void main(String[] args) {
        // Create and start the sandbox service using Docker as the backend
        BaseClientStarter clientStarter = DockerClientStarter.builder().build();
        ManagerConfig managerConfig = ManagerConfig.builder()
                .clientStarter(clientStarter)
                .build();
        SandboxService sandboxService = new SandboxService(managerConfig);
        sandboxService.start();

        // Connect to (or create) a browser-type sandbox with specified user and session IDs
        Sandbox sandbox = new BrowserSandbox(sandboxService, "user_1", "session_1");

        // Initialize a toolkit and register browser-related tools bound to this sandbox
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(ToolkitInit.BrowserNavigateTool(sandbox));
        toolkit.registerTool(ToolkitInit.BrowserTakeScreenshotTool(sandbox));

        // The Agent can now use these tools to perform safe, isolated browser operations
    }
}
```

For more available service types and detailed usage, see [Memory Service](memory.md).

### 3. Sandbox Service (SandboxService)

**Sandbox Service** manages and provides access to sandboxed tool execution environments for different users and sessions. Sandboxes are organized by composite keys of session ID and user ID, providing isolated execution environments for each user session.

#### AgentScope Usage

In the AgentScope framework, bind sandbox methods provided by the sandbox service to the `ToolKit` module through Runtime's **encapsulated sandbox methods** (`ToolkitInit`):

```java
import io.agentscope.core.tool.Toolkit;
import io.agentscope.runtime.engine.agents.agentscope.tools.ToolkitInit;
import io.agentscope.runtime.engine.services.sandbox.SandboxService;
import io.agentscope.runtime.sandbox.box.BrowserSandbox;
import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.client.config.BaseClientConfig;
import io.agentscope.runtime.sandbox.manager.client.config.KubernetesClientConfig;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;

public class Main {
    public static void main(String[] args) {
        BaseClientConfig clientConfig = KubernetesClientConfig.builder().build();
        ManagerConfig managerConfig = ManagerConfig.builder()
                .containerDeployment(clientConfig)
                .build();
        SandboxService sandboxService = new SandboxService(
                new SandboxManager(managerConfig)
        );
        sandboxService.start();

        Sandbox sandbox = sandboxService.connect("TestSession", "User1", BrowserSandbox.class);

        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(ToolkitInit.BrowserNavigateTool(sandbox));
        toolkit.registerTool(ToolkitInit.BrowserTakeScreenshotTool(sandbox));
    }
}
```

For more available service types and detailed usage, see [Sandbox Service](sandbox.md).

### 4. StateService

Stores and retrieves agent serializable state, allowing agents to maintain context across multiple rounds or even across sessions.

#### AgentScope Usage

In the AgentScope framework, **no adapter is needed**. You can directly call `StateService`'s `export_state` and `save_state` methods to persist and load state:

```{code-cell}
from agentscope_runtime.engine.services.agent_state import InMemoryStateService

state_service = InMemoryStateService()
state = await state_service.export_state(session_id, user_id)
agent.load_state_dict(state)

await state_service.save_state(session_id, user_id, state=agent.state_dict())
```

For more available service types and detailed usage, see [Agent State Service](state.md).

## Service Interfaces

All services must implement the `ServiceWithLifecycleManager` abstract class, for example:

```java
import io.agentscope.runtime.engine.shared.ServiceWithLifecycleManager;

import java.util.concurrent.CompletableFuture;

public class MockService extends ServiceWithLifecycleManager {
    @Override
    public CompletableFuture<Void> start() {
        return null;
    }

    @Override
    public CompletableFuture<Void> stop() {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> health() {
        return null;
    }
}
```

Lifecycle mode example:

```java
import io.agentscope.runtime.engine.services.memory.persistence.memory.service.InMemoryMemoryService;
import io.agentscope.runtime.engine.services.memory.service.MemoryService;

import java.util.concurrent.CompletableFuture;

public class Main {
    public static void main(String[] args) {
        MemoryService memoryService = new InMemoryMemoryService();
        memoryService.start();
        CompletableFuture<Boolean> healthFuture = memoryService.health();

        healthFuture.thenAccept(isHealthy -> {
            if (isHealthy) {
                System.out.println("Service is healthy!");
            } else {
                System.err.println("Service is DOWN!");
            }
        });
        memoryService.stop();
    }
}
```
