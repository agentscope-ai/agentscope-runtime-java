# 快速开始

本教程演示如何在 **AgentScope Runtime Java** 框架中构建一个简单的智能体应用并将其部署为服务。

## 前置条件

### 🔧 安装要求

添加 AgentScope Runtime Java 针对 AgentScope 框架的适配器依赖和应用启动依赖，基础依赖已通过适配器依赖传递依赖完成：

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

### 🔑 API密钥配置

您需要为所选的大语言模型提供商提供API密钥。本示例使用阿里云的Qwen模型，服务提供方是DashScope，所以需要使用其API_KEY，您可以按如下方式将key作为环境变量：

```bash
export DASHSCOPE_API_KEY="your_api_key_here"
```

## 分步实现

### 步骤1：构建 Agent 及其执行逻辑

#### 1.1 导入依赖

首先导入所有必要的依赖：

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

#### 1.2 构建 AgentScopeAgentHandler 接口的实现

下面的四个方法分别定义了整个应用的属性和执行逻辑，`isHealthy`方法用于返回当前应用的健康状况，`getName`方法用于返回当前应用的名称，`getDescription`方法用于返回当前应用的描述，**`streamQuery`**方法则是整个AgentScopeAgentHandler的逻辑执行核心，用于用户设置记忆、构建 Agent 以及自定义 Agent 执行逻辑

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

#### 1.3 实现核心执行逻辑 streamQuery 方法

##### 1.3.1 获取到必要的 id 信息

```java
String sessionId = request.getSessionId();
String userId = request.getUserId();
```

##### 1.3.2 从 StateService 导出历史状态

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

- **目的**：恢复该用户在该会话中的**上一轮 Agent 状态**（例如：内部变量、对话阶段、任务进度等）。
- `roundId = null` 表示取**最新一轮**的状态。
- 使用 `.join()` 阻塞等待（因为后续构建 Agent 需要同步状态）。
- 失败时仅警告，不影响主流程（Agent 可从空状态开始）。

##### 1.3.3 创建 Toolkit 并注册工具

```java
Toolkit toolkit = new Toolkit();
if (sandboxService != null) {
    Sandbox sandbox = sandboxService.connect(userId, sessionId, BaseSandbox.class);
    toolkit.registerTool(ToolkitInit.RunPythonCodeTool(sandbox));
}
```

- **Toolkit**：Agent 可调用的工具集合。
- **Sandbox**：安全沙箱环境，具体包含基础沙箱、文件系统沙箱、浏览器沙箱等，每个 `(userId, sessionId)` 独立实例。
- 注册沙箱工具。
- 如果沙箱创建失败，跳过工具注册，Agent 仍可运行，但无法调用此工具。

##### 1.3.4 创建短期记忆适配器（MemoryAdapter）

```java
MemoryAdapter memory = null;
if (sessionHistoryService != null) {
    memory = new MemoryAdapter(sessionHistoryService, userId, sessionId);
}
```

- **作用**：提供**当前会话的历史消息记录**（如用户和 Agent 的对话历史）。
- `sessionHistoryService` 是底层存储服务。
- 适配器将通用服务接口转换为 AgentScope 框架所需的 `Memory` 接口。

##### 1.3.5 创建长期记忆适配器（LongTermMemoryAdapter）

```java
LongTermMemoryAdapter longTermMemory = null;
if (memoryService != null) {
    longTermMemory = new LongTermMemoryAdapter(memoryService, userId, sessionId);
}
```

- **作用**：访问用户的**跨会话长期记忆**（如个人偏好、知识库摘要等）。
- 通常基于向量数据库或结构化存储。
- 后续会配置 Agent 在生成回复时**同时参考短期和长期记忆**。

##### 1.3.6 构建 ReActAgent 实例

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

- 使用 **Builder 模式**组装 Agent：
  - 名称、系统提示词（system prompt）
  - 绑定工具集（`toolkit`）
  - 配置大模型（这里用通义千问 `qwen-max`，通过 DashScope API）
  - 启用流式输出（`.stream(true)`）
- **注意**：此时 Agent 尚未加载状态或记忆。

##### 1.3.7 注入记忆模块

```java
if (longTermMemory != null) {
    agentBuilder.longTermMemory(longTermMemory)
                .longTermMemoryMode(LongTermMemoryMode.BOTH);
}
if (memory != null) {
    agentBuilder.memory(memory);
}
```

- **`memory`** → 短期记忆（当前会话历史）
- **`longTermMemory`** → 长期记忆（跨会话知识）
- `LongTermMemoryMode.BOTH`：表示在**思考和生成**阶段都使用长期记忆。

##### 1.3.8 加载历史状态到 Agent

```java
if (state != null && !state.isEmpty()) {
    agent.loadStateDict(state);
}
```

- 将 Step 2 获取的状态字典反序列化到 Agent 内部。
- 使 Agent 能“接续”上一次的对话状态（如继续未完成的任务）。

##### 1.3.9 处理输入消息（messages）

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
    // 将前 N-1 条消息加入 memory
    for (int i = 0; i < agentMessages.size() - 1; i++) {
        agent.getMemory().addMessage(agentMessages.get(i));
    }
    queryMessage = agentMessages.get(agentMessages.size() - 1); // 最后一条作为当前查询
} else {
    queryMessage = agentMessages.get(0) or empty Msg;
}
```

##### 1.3.10 启动流式推理并返回事件流

```java
StreamOptions streamOptions = StreamOptions.builder()
    .eventTypes(EventType.REASONING, EventType.TOOL_RESULT)
    .incremental(true)
    .build();

Flux<Event> agentScopeEvents = agent.stream(queryMessage, streamOptions);
```

- **`stream()`**：启动 ReAct 循环（Thought → Action → Observation → ... → Final Answer）
- **`StreamOptions`**：
  - `eventTypes`：只返回推理步骤和工具结果（过滤掉内部日志等）
  - `incremental = true`：启用增量流式输出（如逐字生成）
- 返回的是 **AgentScope 原生 Event 流**（不是 Runtime Event），由外层 `StreamAdapter` 转换。

##### 1.3.11 流完成时保存最终状态

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

- **无论成功/失败/取消**，在流结束时保存 Agent 的**最终状态**。
- `roundId = null` → 自动分配新轮次 ID（见 `InMemoryStateService` 实现）。
- 使用 `exceptionally` 处理保存异常，避免影响主流程。

> 🔁 实现了“状态持久化闭环”：加载 → 执行 → 保存。

### 步骤2：构建 AgentApp

#### 2.1 导入依赖

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

#### 2.2 构建 SandboxService 沙箱管理服务

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

* 沙箱运行环境支持 **Docker**、**K8s **以及 **AgentRun**，未配置 `clientConfig` 默认使用**本地 Docker **作为运行环境
* 使用`managerConfig`构建 **SandboxManager**，并由此构建 **SandboxService**

#### 2.3 构建 AgentApp

##### 2.3.1 初始化 agentHandler

```java
MyAgentScopeAgentHandler agentHandler = new MyAgentScopeAgentHandler();
agentHandler.setStateService(new InMemoryStateService());
agentHandler.setSessionHistoryService(new InMemorySessionHistoryService());
agentHandler.setMemoryService(new InMemoryMemoryService());
agentHandler.setSandboxService(buidSandboxService());
```

实例化刚刚编写的 **AgentScopeAgentHandler** 类，并注册服务

##### 2.3.2 构建 AgentApp 并一键启动

```java
AgentApp agentApp = new AgentApp(agentHandler);
agentApp.run(10001);
```

使用实例化的 **AgentScopeAgentHandler** 类初始化 **AgentApp**，并在10001端口上启动

### 步骤3：通过 A2A 协议访问 Agent

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

你将会看到以**Server-Sent Events（SSE）**格式流式输出的响应：

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

## 章节导读

后续的章节包括如下几个部分
- [沙箱与工具](tool.md): 帮助您在Agent中加入工具
- [部署](deployment.md): 帮助您部署Agent，打包成服务
- [使用](use.md): 帮助您调用部署后的服务
- [如何贡献](contribute.md): 贡献代码给本项目的参考文档