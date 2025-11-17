# 快速开始

本教程演示如何在 **AgentScope Runtime Java** 框架中构建一个简单的智能体并将其部署为服务。

## 前置条件

### 🔧 安装要求

- **Java 17** 或更高版本
- **Maven 3.6+**
- **Docker**（可选，用于沙箱工具执行）

### 📦 项目依赖

在您的 `pom.xml` 文件中添加以下依赖：

```xml
<dependencies>
    <!-- AgentScope Agent (选择其一) -->
    <dependency>
        <groupId>io.agentscope</groupId>
        <artifactId>agentscope-runtime-agentscope</artifactId>
        <version>0.1.1</version>
    </dependency>
    
    <!-- 或 Spring AI Alibaba Agent (选择其一) -->
    <dependency>
        <groupId>io.agentscope</groupId>
        <artifactId>agentscope-runtime-saa</artifactId>
        <version>0.1.1</version>
    </dependency>
    
    <!-- Web部署支持 -->
    <dependency>
        <groupId>io.agentscope</groupId>
        <artifactId>agentscope-runtime-web</artifactId>
        <version>0.1.1</version>
    </dependency>
</dependencies>
```

### 🔑 API密钥配置

您需要为所选的大语言模型提供商提供API密钥。本示例使用DashScope（Qwen）：

```bash
export AI_DASHSCOPE_API_KEY="your_api_key_here"
```

## 分步实现

### 步骤1：初始化上下文管理器

上下文管理器用于管理会话历史和记忆服务：

```java
import io.agentscope.runtime.engine.memory.context.ContextManager;
import io.agentscope.runtime.engine.memory.context.ContextComposer;
import io.agentscope.runtime.engine.memory.persistence.memory.service.InMemoryMemoryService;
import io.agentscope.runtime.engine.memory.persistence.session.InMemorySessionHistoryService;
import io.agentscope.runtime.engine.memory.service.MemoryService;
import io.agentscope.runtime.engine.memory.service.SessionHistoryService;

private ContextManager initializeContextManager() {
    try {
        // 创建会话历史服务
        SessionHistoryService sessionHistoryService = new InMemorySessionHistoryService();
        
        // 创建记忆服务
        MemoryService memoryService = new InMemoryMemoryService();
        
        // 创建上下文管理器
        ContextManager contextManager = new ContextManager(
            ContextComposer.class,
            sessionHistoryService,
            memoryService
        );
        
        // 启动服务
        sessionHistoryService.start().get();
        memoryService.start().get();
        contextManager.start().get();
        
        System.out.println("✅ ContextManager initialized successfully");
        return contextManager;
    } catch (Exception e) {
        System.err.println("Failed to initialize ContextManager: " + e.getMessage());
        throw new RuntimeException("ContextManager initialization failed", e);
    }
}
```

### 步骤2：创建智能体

您可以选择使用 **AgentScope Agent** 或 **Spring AI Alibaba (SAA) Agent**。

#### 方式1：使用 AgentScope Agent

```java
import io.agentscope.core.ReActAgent;
import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.runtime.engine.agents.agentscope.AgentScopeAgent;
import io.agentscope.runtime.engine.agents.agentscope.tools.ToolkitInit;
import io.agentscope.core.model.DashScopeChatModel;

// 创建工具包
Toolkit toolkit = new Toolkit();
toolkit.registerTool(ToolkitInit.RunPythonCodeTool());
toolkit.registerTool(ToolkitInit.RunShellCommandTool());

// 创建 ReActAgent
ReActAgent.Builder agentBuilder = ReActAgent.builder()
    .name("Friday")
    .sysPrompt("You're a helpful assistant named Friday.")
    .toolkit(toolkit)
    .memory(new InMemoryMemory())
    .model(DashScopeChatModel.builder()
        .apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
        .modelName("qwen-turbo")
        .stream(true)
        .enableThinking(true)
        .formatter(new DashScopeChatFormatter())
        .build());

// 创建 AgentScopeAgent
AgentScopeAgent agentScopeAgent = AgentScopeAgent.builder()
    .agent(agentBuilder)
    .build();

System.out.println("✅ AgentScope agent created successfully");
```

#### 方式2：使用 Spring AI Alibaba (SAA) Agent

```java
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.Builder;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import io.agentscope.runtime.engine.agents.saa.SaaAgent;
import io.agentscope.runtime.engine.agents.saa.tools.ToolcallsInit;
import java.util.List;

// 创建 DashScope API
DashScopeApi dashScopeApi = DashScopeApi.builder()
    .apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
    .build();

// 创建 DashScope ChatModel
DashScopeChatModel chatModel = DashScopeChatModel.builder()
    .dashScopeApi(dashScopeApi)
    .build();

// 创建 ReactAgent Builder
Builder agentBuilder = ReactAgent.builder()
    .name("Friday")
    .model(chatModel)
    .tools(List.of(
        ToolcallsInit.RunPythonCodeTool(),
        ToolcallsInit.RunShellCommandTool()
    ));

// 创建 SaaAgent
SaaAgent saaAgent = SaaAgent.builder()
    .agent(agentBuilder)
    .build();

System.out.println("✅ SAA agent created successfully");
```

### 步骤3：配置沙箱管理器（可选但推荐）

如果您需要使用沙箱工具（如Python代码执行、文件操作等），需要配置沙箱管理器：

```java
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;
import io.agentscope.runtime.engine.service.EnvironmentManager;
import io.agentscope.runtime.engine.service.impl.DefaultEnvironmentManager;

// 创建沙箱管理器配置（使用默认Docker配置）
ManagerConfig managerConfig = ManagerConfig.builder().build();

// 创建沙箱管理器
SandboxManager sandboxManager = new SandboxManager(managerConfig);

// 创建环境管理器
EnvironmentManager environmentManager = new DefaultEnvironmentManager(sandboxManager);

System.out.println("✅ Sandbox manager configured successfully");
```

### 步骤4：创建 Runner

Runner 将智能体、上下文管理器和环境管理器组合在一起：

```java
import io.agentscope.runtime.engine.Runner;

Runner runner = Runner.builder()
    .agent(agentScopeAgent)  // 或 saaAgent
    .contextManager(contextManager)
    .environmentManager(environmentManager)  // 如果使用沙箱工具，需要设置
    .build();

System.out.println("✅ Runner created successfully");
```

### 步骤5：部署智能体

使用 `LocalDeployManager` 将智能体部署为 A2A 服务：

```java
import io.agentscope.runtime.LocalDeployManager;

// 部署智能体（默认端口 8080）
LocalDeployManager.builder()
    .port(8090)
    .build()
    .deploy(runner);

System.out.println("✅ Agent deployed successfully on port 8090");
```

### 步骤6：发送请求

您可以使用 `curl` 向 A2A API 发送请求：

```bash
curl --location --request POST 'http://localhost:8090/a2a/' \
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
            "text": "What is the capital of France?"
          }
        ],
        "messageId": "c4911b64c8404b7a8bf7200dd225b152"
      }
    }
  }'
```

您将会看到以 **Server-Sent Events (SSE)** 格式流式输出 **A2A** 协议的响应：

```json
event:jsonrpc
data:{"jsonrpc":"2.0","id":"xxx","result":{"taskId":"xxx","status":{"state":"working","message":{"role":"agent","parts":[{"text":"text","kind":"text"}],"messageId":"xxx","contextId":"xxx","taskId":"xxx","metadata":{},"kind":"message"},"timestamp":"xxx"},"contextId":"xxx","final":false,"kind":"status-update"}}
```

### 步骤7：使用沙箱工具（可选）

如果您想让智能体执行 Python 代码或使用其他沙箱工具，可以在创建智能体时添加相应的工具：

```java
// 对于 AgentScope Agent
Toolkit toolkit = new Toolkit();
toolkit.registerTool(ToolkitInit.RunPythonCodeTool());
toolkit.registerTool(ToolkitInit.RunShellCommandTool());
toolkit.registerTool(ToolkitInit.BrowserNavigateTool());

// 对于 SAA Agent
Builder agentBuilder = ReactAgent.builder()
    .name("Friday")
    .model(chatModel)
    .tools(List.of(
        ToolcallsInit.RunPythonCodeTool(),
        ToolcallsInit.RunShellCommandTool(),
        ToolcallsInit.BrowserNavigateBackTool()
    ));
```

然后可以通过请求让智能体执行代码：

```bash
curl --location --request POST 'http://localhost:8090/a2a/' \
  --header 'Content-Type: application/json' \
  --data-raw '{
    "method": "message/stream",
    "id": "test-id",
    "jsonrpc": "2.0",
    "params": {
      "message": {
        "role": "user",
        "kind": "message",
        "contextId": "id",
        "metadata": {
          "userId": "me",
          "sessionId": "my_session"
        },
        "parts": [
          {
            "kind": "text",
            "text": "Hello, please calculate the 10th Fibonacci number using Python"
          }
        ],
        "messageId": "test-message-id"
      }
    }
  }'
```

## 完整示例

以下是一个完整的可运行示例：

```java
import io.agentscope.core.ReActAgent;
import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.runtime.LocalDeployManager;
import io.agentscope.runtime.engine.Runner;
import io.agentscope.runtime.engine.agents.agentscope.AgentScopeAgent;
import io.agentscope.runtime.engine.agents.agentscope.tools.ToolkitInit;
import io.agentscope.runtime.engine.memory.context.ContextManager;
import io.agentscope.runtime.engine.memory.context.ContextComposer;
import io.agentscope.runtime.engine.memory.persistence.memory.service.InMemoryMemoryService;
import io.agentscope.runtime.engine.memory.persistence.session.InMemorySessionHistoryService;
import io.agentscope.runtime.engine.memory.service.MemoryService;
import io.agentscope.runtime.engine.memory.service.SessionHistoryService;
import io.agentscope.runtime.engine.service.EnvironmentManager;
import io.agentscope.runtime.engine.service.impl.DefaultEnvironmentManager;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;
import io.agentscope.core.model.DashScopeChatModel;

public class QuickStartExample {
    
    public static void main(String[] args) {
        // 检查 API 密钥
        if (System.getenv("AI_DASHSCOPE_API_KEY") == null) {
            System.err.println("Please set the AI_DASHSCOPE_API_KEY environment variable");
            System.exit(1);
        }
        
        try {
            // 步骤1：初始化上下文管理器
            ContextManager contextManager = initializeContextManager();
            
            // 步骤2：创建智能体
            AgentScopeAgent agent = createAgent();
            
            // 步骤3：配置沙箱管理器
            EnvironmentManager environmentManager = createEnvironmentManager();
            
            // 步骤4：创建 Runner
            Runner runner = Runner.builder()
                .agent(agent)
                .contextManager(contextManager)
                .environmentManager(environmentManager)
                .build();
            
            // 步骤5：部署智能体
            LocalDeployManager.builder()
                .port(8090)
                .build()
                .deploy(runner);
            
            System.out.println("✅ Agent deployed successfully on http://localhost:8090");
            
        } catch (Exception e) {
            System.err.println("Failed to deploy agent: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static ContextManager initializeContextManager() throws Exception {
        SessionHistoryService sessionHistoryService = new InMemorySessionHistoryService();
        MemoryService memoryService = new InMemoryMemoryService();
        
        ContextManager contextManager = new ContextManager(
            ContextComposer.class,
            sessionHistoryService,
            memoryService
        );
        
        sessionHistoryService.start().get();
        memoryService.start().get();
        contextManager.start().get();
        
        return contextManager;
    }
    
    private static AgentScopeAgent createAgent() {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(ToolkitInit.RunPythonCodeTool());
        
        ReActAgent.Builder agentBuilder = ReActAgent.builder()
            .name("Friday")
            .sysPrompt("You're a helpful assistant named Friday.")
            .toolkit(toolkit)
            .memory(new InMemoryMemory())
            .model(DashScopeChatModel.builder()
                .apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
                .modelName("qwen-turbo")
                .stream(true)
                .formatter(new DashScopeChatFormatter())
                .build());
        
        return AgentScopeAgent.builder()
            .agent(agentBuilder)
            .build();
    }
    
    private static EnvironmentManager createEnvironmentManager() {
        ManagerConfig managerConfig = ManagerConfig.builder().build();
        SandboxManager sandboxManager = new SandboxManager(managerConfig);
        return new DefaultEnvironmentManager(sandboxManager);
    }
}
```

## 下一步

- 浏览 **完整实现示例** 在 `examples/simple_agent_use_examples` 目录中
- 查看 **协议文档** 了解 A2A 协议和其他通信方式
- 了解 **上下文管理器** 和 **记忆服务** 的详细用法
- 探索 **沙箱工具** 和 **环境管理器** 的配置选项

更多详细信息，请参考：
- {doc}`manager` - 管理器模块文档
- {doc}`context_manager` - 上下文管理器文档
- {doc}`protocol` - 协议文档
- {doc}`environment_manager` - 环境管理器文档
