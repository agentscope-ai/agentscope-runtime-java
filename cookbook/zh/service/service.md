# 服务与适配器

## 概述

AgentScope Runtime Java 中的服务（`Service`）为智能体运行环境提供核心能力，包括：

- **会话历史管理**
- **记忆存储**
- **沙箱管理**
- **智能体状态管理**

所有服务都实现了统一的抽象接口 `ServiceWithLifecycleManager`（生命周期管理模式），提供标准方法：

- `start()`：启动服务
- `stop()`：停止服务
- `health()`：检查服务健康状态

> 在实际编写智能体应用时，我们通常**不会直接操作这些服务的各种底层方法**，而是通过 **框架适配器Adapters** 来使用。适配器会：
> 
> 1. 负责把 Runtime 的服务对象注入到智能体框架的兼容模块中
> 2. 让框架内的 agent 能无缝调用 Runtime 提供的功能（如会话记忆、工具沙箱等）
> 3. 保证服务生命周期与 Runner/Engine 一致

## 为什么要通过适配器使用服务？

- **解耦**：智能体框架不用直接感知底层服务实现
- **跨框架复用**：相同的服务可以接入不同的智能体框架
- **统一生命周期**：Runner/Engine 统一启动和关闭所有服务
- **增强可维护性**：更换服务实现（如切换为数据库存储）时，无需修改智能体业务代码

## 可用服务及适配器用法

### 1. 会话历史服务（SessionHistoryService）

管理用户-智能体的对话会话，存储并检索会话消息历史。

#### AgentScope用法

在 AgentScope 框架中，通过 Runtime 的 `MemoryAdapter` 适配器来绑定会话历史服务到 `Memory` 模块：

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

更多可用服务类型与详细的用法请参见[会话历史服务](session_history.md)。

### 2. 记忆服务（MemoryService）

`MemoryService` 管理长期记忆存储。在Agent 中，记忆储存终端用户之前的对话。 例如，终端用户可能在之前的对话中提到他们的姓名。 记忆服务通常用来**跨会话**的存储这些信息，以便智能体在下次对话中使用。

#### AgentScope用法

在 AgentScope 框架中，通过Runtime的`AgentScopeLongTermMemory`适配器来绑定会话历史服务到`LongTermMemory`模块：

```java
import io.agentscope.runtime.adapters.agentscope.memory.LongTermMemoryAdapter;
import io.agentscope.runtime.engine.services.memory.persistence.memory.service.InMemoryMemoryService;
import io.agentscope.runtime.engine.services.memory.service.MemoryService;

public class Main {
    public static void main(String[] args) {
        MemoryService memoryService = new InMemoryMemoryService();
        LongTermMemoryAdapter longTermMemory = null;
      
        longTermMemory = new LongTermMemoryAdapter(
                memoryService,
                "User1",
                "Test Session"
        );
    }
}
```

更多可用服务类型与详细的用法请参见[记忆服务](memory.md)。

### 3. 沙箱服务（SandboxService）

**沙箱服务** 管理并为不同用户和会话提供沙箱化工具执行环境的访问。沙箱通过会话ID和用户ID的复合键组织，为每个用户会话提供隔离的执行环境。

#### AgentScope用法

在 AgentScope 框架中，通过Runtime **封装的沙箱方法**（`ToolkitInit`） 来绑定沙箱服务提供的沙箱的方法到`ToolKit`模块：

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

更多可用服务类型与详细的用法请参见[沙箱服务](sandbox.md)。

### 4. StateService

存取智能体的可序列化状态，让智能体在多轮会话甚至跨会话间保持上下文。

#### AgentScope用法

在 AgentScope 框架中，无需通过适配器，直接调用`StateService`的`export_state`和`save_state`来保：

```{code-cell}
from agentscope_runtime.engine.services.agent_state import InMemoryStateService

state_service = InMemoryStateService()
state = await state_service.export_state(session_id, user_id)
agent.load_state_dict(state)

await state_service.save_state(session_id, user_id, state=agent.state_dict())
```

更多可用服务类型与详细的用法请参见[智能体状态服务](state.md)。

## 服务的接口

所有服务必须实现 `ServiceWithLifecycleManager` 抽象类，例如：

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

生命周期模式示例：

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
