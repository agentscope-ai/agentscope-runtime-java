# 简单部署

`AgentApp` 是 **AgentScope Runtime Java** 中的全能型应用服务封装器。
它为你的 agent 逻辑提供 HTTP 服务框架，并可将其作为 API 暴露，支持以下功能：

- **流式响应（SSE）**，实现实时输出
- 内置 **健康检查** 接口
- 内置 **A2A协议** 支持

**重要说明**：
在当前版本中，`AgentApp` 不会自动包含 `/process` 端点。
你必须显式地注册一个控制器以及对应的请求处理函数，服务才能使用自定义端点处理传入的请求。

下面的章节将通过具体示例深入介绍每项功能。

------

## 初始化与基本运行

**功能**

创建一个最小的 `AgentApp` 实例，并启动基于 `SpringBoot` 的 HTTP 服务骨架。
初始状态下，服务只提供：

- 欢迎页 `/`
- 健康检查 `/health`
- 就绪探针 `/readiness`
- 存活探针 `/liveness`
- A2A 协议支持 `/a2a`

**注意**：

- 默认不会暴露 `/process` 业务处理端点。
- `Handler` 类需要实现 `AgentScopeAgentHandler` 中的`streamQuery` 方法，返回 `Flux<Event>` 流式结果。

**用法示例**

```java
MyAgentScopeAgentHandler agentHandler = new MyAgentScopeAgentHandler();
// 初始化 agentHandler 属性

AgentApp agentApp = new AgentApp(agentHandler);
// 服务会暴露在 http://localhost:10001
agentApp.run("localhost",10001);
```

------

## A2A 流式输出（SSE）

**功能**
让客户端实时接收生成结果（适合聊天、代码生成等逐步输出场景）。

**用法示例（客户端）**

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
          "text": "你好，给我用python计算一下第 30 个斐波那契数",
          "kind": "text"
        }
      ],
      "messageId": "c4911b64c8404b7a8bf7200dd225b152"
    }
  }
}'
```

**返回格式**

```bash
id:2d2b4dc8-8ea2-437b-888d-3aaf3a8239dc
event:jsonrpc
data:{"jsonrpc":"2.0","id":"2d2b4dc8-8ea2-437b-888d-3aaf3a8239dc","result":{"id":"8a3021a6-1b4e-4fc9-b70b-a39f03aa476c","contextId":"ea00d2b3-ce89-4707-b457-ef58a68b35ff","status":{"state":"submitted","timestamp":"2025-12-10T08:18:30.104001Z"},"artifacts":[],"history":[{"role":"user","parts":[{"text":"你好，给我用python计算一下第30个斐波那契数","kind":"text"}],"messageId":"c4911b64c8404b7a8bf7200dd225b152","contextId":"ea00d2b3-ce89-4707-b457-ef58a68b35ff","taskId":"8a3021a6-1b4e-4fc9-b70b-a39f03aa476c","metadata":{"userId":"me","sessionId":"test1"},"kind":"message"}],"kind":"task"}}

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
data:{"jsonrpc":"2.0","id":"2d2b4dc8-8ea2-437b-888d-3aaf3a8239dc","result":{"taskId":"8a3021a6-1b4e-4fc9-b70b-a39f03aa476c","status":{"state":"completed","message":{"role":"agent","parts":[{"text":"run_ipython_cellrun_ipython_cell第30个斐波那契数是 514229。","kind":"text"}],"messageId":"a4258fb3-5b79-461d-934a-8802dd0bebb8","contextId":"ea00d2b3-ce89-4707-b457-ef58a68b35ff","taskId":"8a3021a6-1b4e-4fc9-b70b-a39f03aa476c","metadata":{"type":"final_response"},"kind":"message"},"timestamp":"2025-12-10T08:18:38.298168Z"},"contextId":"ea00d2b3-ce89-4707-b457-ef58a68b35ff","final":true,"kind":"status-update"}}
```

------

## 健康检查接口

**功能**

自动提供健康探针接口，方便容器或集群部署。

**接口列表**

- `GET /health`：返回状态与时间戳
- `GET /readiness`：判断是否就绪
- `GET /liveness`：判断是否存活
- `GET /`：欢迎信息

**用法示例**

```bash
curl http://localhost:8090/health
curl http://localhost:8090/readiness
curl http://localhost:8090/liveness
curl http://localhost:8090/
```

------

## 自定义 Agent 访问逻辑

**功能**

实现 `AgentScopeAgentHandler` 的 `streamQuery` 方法，作为 Agent 被调用时实际触发的执行逻辑

### 基本用法

```java
public Flux<io.agentscope.core.agent.Event> streamQuery(AgentRequest request, Object messages) {
    String sessionId = request.getSessionId();
    String userId = request.getUserId();

    try {
        // 1. 从状态服务导出会话状态（如有）
        Map<String, Object> state = null;
        if (stateService != null) {
            try {
                state = stateService.exportState(userId, sessionId, null).join();
            } catch (Exception e) {
                logger.warn("导出会话状态失败: {}", e.getMessage());
            }
        }

        // 2. 初始化工具集并注册可用工具
        Toolkit toolkit = new Toolkit();

        if (sandboxService != null) {
            try {
                // 为当前会话创建沙箱实例
                Sandbox sandbox = sandboxService.connect(userId, sessionId, BaseSandbox.class);
                // 注册 Python 代码执行工具
                toolkit.registerTool(ToolkitInit.RunPythonCodeTool(sandbox));
                logger.debug("已注册 execute_python_code 工具");
            } catch (Exception e) {
                logger.warn("创建沙箱或注册工具失败，跳过工具注册: {}", e.getMessage());
            }
        }

        // 3. 创建短期记忆适配器（基于会话历史服务）
        MemoryAdapter memory = null;
        if (sessionHistoryService != null) {
            memory = new MemoryAdapter(sessionHistoryService, userId, sessionId);
        }

        // 4. 创建长期记忆适配器（如有）
        LongTermMemoryAdapter longTermMemory = null;
        if (memoryService != null) {
            longTermMemory = new LongTermMemoryAdapter(memoryService, userId, sessionId);
        }

        // 5. 构建 ReAct 智能体
        ReActAgent.Builder agentBuilder = ReActAgent.builder()
            .name("Friday")
            .sysPrompt("你是一个名为 Friday 的智能助手。")
            .toolkit(toolkit)
            .model(
                DashScopeChatModel.builder()
                    .apiKey(apiKey)
                    .modelName("qwen-max")
                    .stream(true)
                    .formatter(new DashScopeChatFormatter())
                    .build()
            );

        // 配置长期记忆（如存在）
        if (longTermMemory != null) {
            agentBuilder.longTermMemory(longTermMemory)
                        .longTermMemoryMode(LongTermMemoryMode.BOTH);
            logger.debug("已配置长期记忆");
        }

        // 配置短期记忆（如存在）
        if (memory != null) {
            agentBuilder.memory(memory);
            logger.debug("已配置短期记忆适配器");
        }

        ReActAgent agent = agentBuilder.build();

        // 6. 加载会话状态（如存在）
        if (state != null && !state.isEmpty()) {
            try {
                agent.loadStateDict(state);
                logger.debug("已加载会话状态，会话 ID: {}", sessionId);
            } catch (Exception e) {
                logger.warn("加载状态失败: {}", e.getMessage());
            }
        }

        // 7. 将输入消息转换为 Msg 列表
        List<Msg> agentMessages;
        if (messages instanceof List) {
            @SuppressWarnings("unchecked")
            List<Msg> msgList = (List<Msg>) messages;
            agentMessages = msgList;
        } else if (messages instanceof Msg) {
            agentMessages = List.of((Msg) messages);
        } else {
            logger.warn("输入消息类型不支持: {}，使用空消息列表",
                messages != null ? messages.getClass().getName() : "null");
            agentMessages = List.of();
        }

        // 8. 准备查询消息：多条消息时，前 N-1 条存入记忆，最后一条作为当前查询
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

        // 9. 配置流式响应选项：包含推理与工具调用事件，启用增量输出
        StreamOptions streamOptions = StreamOptions.builder()
            .eventTypes(EventType.REASONING, EventType.TOOL_RESULT)
            .incremental(true)
            .build();

        // 10. 启动智能体流式推理，并在流结束时保存最终状态
        return agent.stream(queryMessage, streamOptions)
            .doOnNext(event -> logger.info("智能体事件: {}", event))
            .doFinally(signalType -> {
                if (stateService != null) {
                    try {
                        Map<String, Object> finalState = agent.stateDict();
                        if (finalState != null && !finalState.isEmpty()) {
                            stateService.saveState(userId, finalState, sessionId, null)
                                .exceptionally(e -> {
                                    logger.error("保存会话状态失败: {}", e.getMessage(), e);
                                    return null;
                                });
                        }
                    } catch (Exception e) {
                        logger.error("保存状态时发生异常: {}", e.getMessage(), e);
                    }
                }
            })
            .doOnError(error -> logger.error("智能体流式推理出错: {}", error.getMessage(), error));

    } catch (Exception e) {
        logger.error("streamQuery 执行异常: {}", e.getMessage(), e);
        return Flux.error(e);
    }
}
```

### 关键特性

1. **函数签名**：
    - `request`：封装了本次智能体调用的**请求信息**
    - `messages`：表示传入的**消息内容**，可能为单条消息、消息列表、或其他中间表示
2. **流式输出**：函数作为生成器，使用流式返回结果
3. **状态管理**：可以使用 `this.stateService` 进行状态保存和恢复
4. **沙箱管理**：可以使用 `this.sandboxService` 构建沙箱工具
5. **会话历史**：可以使用 `this.sessionHistoryService`  管理会话历史
6. **记忆管理**：可以使用 `this.memoryService` 管理长期记忆


### 完整示例：带状态管理的 AgentApp

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
 * AgentScope 智能体部署示例。
 *
 * <p>本示例演示如何启动一个基于 ReActAgent 的本地智能体服务，包含以下能力：
 * <ul>
 *   <li>会话状态管理（内存存储）</li>
 *   <li>短期记忆（会话历史）与长期记忆（用户级记忆库）</li>
 *   <li>Python 沙箱工具支持（通过 Kubernetes 启动隔离执行环境）</li>
 *   <li>通过 HTTP 接口提供流式智能体推理服务</li>
 * </ul>
 *
 * <p>启动前请确保已设置环境变量 {@code AI_DASHSCOPE_API_KEY}。
 */
public class AgentScopeDeployExample {

    public static void main(String[] args) {
        // 校验 DashScope API 密钥是否已配置
        if (System.getenv("AI_DASHSCOPE_API_KEY") == null) {
            System.err.println("错误：未设置环境变量 AI_DASHSCOPE_API_KEY");
            System.exit(1);
        }

        runAgent();
    }

    /**
     * 初始化并启动智能体服务。
     */
    private static void runAgent() {
        // 创建自定义智能体处理器
        MyAgentScopeAgentHandler agentHandler = new MyAgentScopeAgentHandler();

        // 配置内存版状态与记忆服务（适用于开发/测试）
        agentHandler.setStateService(new InMemoryStateService());
        agentHandler.setSessionHistoryService(new InMemorySessionHistoryService());
        agentHandler.setMemoryService(new InMemoryMemoryService());

        // 配置沙箱服务（用于安全执行 Python 工具代码）
        agentHandler.setSandboxService(buildSandboxService());

        // 启动 Agent 服务应用，监听 localhost:10001
        AgentApp agentApp = new AgentApp(agentHandler);
        agentApp.run("localhost", 10001);
    }

    /**
     * 构建沙箱服务实例，默认使用 Kubernetes 客户端配置。
     *
     * @return 配置完成的 SandboxService 实例
     */
    @NotNull
    private static SandboxService buildSandboxService() {
        // 使用默认 Kubernetes 配置（实际部署时可替换为真实集群配置）
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
 * AgentScope 智能体处理器的示例实现。
 *
 * <p>该实现支持以下核心功能：
 * <ul>
 *   <li>从状态服务加载和保存会话状态</li>
 *   <li>集成短期记忆（基于会话历史）与长期记忆（基于用户记忆库）</li>
 *   <li>动态注册工具（如 Python 沙箱执行环境）</li>
 *   <li>使用 Qwen 大模型进行流式推理</li>
 *   <li>以事件流形式返回推理全过程（包括推理步骤、工具调用等）</li>
 * </ul>
 */
public class MyAgentScopeAgentHandler extends AgentScopeAgentHandler {
    private static final Logger logger = LoggerFactory.getLogger(MyAgentScopeAgentHandler.class);
    private final String apiKey;

    /**
     * 构造函数：从环境变量加载 DashScope API 密钥。
     */
    public MyAgentScopeAgentHandler() {
        this.apiKey = System.getenv("AI_DASHSCOPE_API_KEY");
    }

    @Override
    public Flux<io.agentscope.core.agent.Event> streamQuery(AgentRequest request, Object messages) {
        String sessionId = request.getSessionId();
        String userId = request.getUserId();

        try {
            // 1. 尝试从状态服务加载当前会话的持久化状态
            Map<String, Object> state = null;
            if (stateService != null) {
                try {
                    state = stateService.exportState(userId, sessionId, null).join();
                } catch (Exception e) {
                    logger.warn("加载会话状态失败: {}", e.getMessage());
                }
            }

            // 2. 初始化工具集并注册可用工具
            Toolkit toolkit = new Toolkit();
            if (sandboxService != null) {
                try {
                    Sandbox sandbox = sandboxService.connect(userId, sessionId, BaseSandbox.class);
                    toolkit.registerTool(ToolkitInit.RunPythonCodeTool(sandbox));
                    logger.debug("已注册 Python 代码执行工具");
                } catch (Exception e) {
                    logger.warn("沙箱初始化或工具注册失败，跳过工具支持: {}", e.getMessage());
                }
            }

            // 3. 创建短期记忆适配器（用于管理当前会话消息历史）
            MemoryAdapter memory = null;
            if (sessionHistoryService != null) {
                memory = new MemoryAdapter(sessionHistoryService, userId, sessionId);
            }

            // 4. 创建长期记忆适配器（用于访问用户级别的持久化记忆）
            LongTermMemoryAdapter longTermMemory = null;
            if (memoryService != null) {
                longTermMemory = new LongTermMemoryAdapter(memoryService, userId, sessionId);
            }

            // 5. 构建 ReAct 智能体实例
            ReActAgent.Builder agentBuilder = ReActAgent.builder()
                .name("Friday")
                .sysPrompt("你是一个名为 Friday 的智能助手。")
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
                logger.debug("已启用长期记忆");
            }

            if (memory != null) {
                agentBuilder.memory(memory);
                logger.debug("已配置短期记忆适配器");
            }

            ReActAgent agent = agentBuilder.build();

            // 6. 若存在状态数据，则恢复智能体内部状态
            if (state != null && !state.isEmpty()) {
                try {
                    agent.loadStateDict(state);
                    logger.debug("成功恢复会话状态，会话 ID: {}", sessionId);
                } catch (Exception e) {
                    logger.warn("恢复状态失败: {}", e.getMessage());
                }
            }

            // 7. 将输入消息转换为标准 Msg 列表
            List<Msg> agentMessages;
            if (messages instanceof List) {
                @SuppressWarnings("unchecked")
                List<Msg> msgList = (List<Msg>) messages;
                agentMessages = msgList;
            } else if (messages instanceof Msg) {
                agentMessages = List.of((Msg) messages);
            } else {
                logger.warn("不支持的消息类型: {}，使用空消息列表", 
                    messages != null ? messages.getClass().getName() : "null");
                agentMessages = List.of();
            }

            // 8. 准备查询消息：多条消息时，前 N-1 条存入记忆，最后一条作为当前查询
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

            // 9. 配置流式输出选项：包含推理过程和工具调用结果，启用增量模式
            StreamOptions streamOptions = StreamOptions.builder()
                .eventTypes(EventType.REASONING, EventType.TOOL_RESULT)
                .incremental(true)
                .build();

            // 10. 启动流式推理，并在流结束时自动保存最终状态
            return agent.stream(queryMessage, streamOptions)
                .doOnNext(event -> logger.info("智能体事件: {}", event))
                .doFinally(signalType -> {
                    if (stateService != null) {
                        try {
                            Map<String, Object> finalState = agent.stateDict();
                            if (finalState != null && !finalState.isEmpty()) {
                                stateService.saveState(userId, finalState, sessionId, null)
                                    .exceptionally(e -> {
                                        logger.error("保存会话状态失败: {}", e.getMessage(), e);
                                        return null;
                                    });
                            }
                        } catch (Exception e) {
                            logger.error("保存状态时发生异常: {}", e.getMessage(), e);
                        }
                    }
                })
                .doOnError(error -> logger.error("智能体流式推理出错: {}", error.getMessage(), error));

        } catch (Exception e) {
            logger.error("streamQuery 执行异常: {}", e.getMessage(), e);
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
        return "基于 AgentScope 实现的智能助手示例。";
    }
}
```

## 本地部署

**功能**

通过 `run()` 方法一键拉起本地应用。

**用法示例**

```java
agentApp.run("localhost", 10001);
```

更多部署选项和详细说明，请参考 [高级部署](deployment/advanced_deployment.md) 文档。

AgentScope Runtime 提供了 Serverless 的部署方案，您可以将您的 Agent 应用部署到 K8s 或 AgentRun 上。参考 [高级部署](deployment/advanced_deployment.md) 文档，查看 K8s 和 AgentRun 部署部分获取更多配置详情.
