# 部署配备工具沙箱的ReAct智能体

本教程演示了如何使用AgentScope Runtime Java与[**AgentScope Java框架**](https://github.com/agentscope-ai/agentscope-java)创建和部署 *"推理与行动"(ReAct)* 智能体。

```{note}
ReAct（推理与行动）范式使智能体能够将推理轨迹与特定任务的行动交织在一起，使其在工具交互任务中特别有效。通过将AgentScope的`ReActAgent`与AgentScope Runtime的基础设施相结合，您可以同时获得智能决策和安全的工具执行。
```

## 前置要求

### 🔧 安装要求

- **Java 17** 或更高版本
- **Maven 3.6+**
- **Docker**（用于沙箱工具执行）

### 📦 项目依赖

在您的 `pom.xml` 文件中添加以下依赖：

```xml
<dependencies>
    <!-- AgentScope Runtime Core -->
    <dependency>
        <groupId>io.agentscope</groupId>
        <artifactId>agentscope-runtime-core</artifactId>
        <version>0.1.1</version>
    </dependency>
    
    <!-- AgentScope Agent -->
    <dependency>
        <groupId>io.agentscope</groupId>
        <artifactId>agentscope-runtime-agentscope</artifactId>
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

### 🐳 沙箱设置

```{note}
确保您的浏览器沙箱环境已准备好使用，详细信息请参见{doc}`sandbox`。
```

确保浏览器沙箱镜像可用：

```bash
docker pull agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-browser:latest && docker tag agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-browser:latest agentscope/runtime-sandbox-browser:latest
```

### 🔑 API密钥配置

您需要为您选择的LLM提供商准备API密钥。此示例使用DashScope（Qwen），但您可以将其适配到其他提供商：

```bash
export AI_DASHSCOPE_API_KEY="your_api_key_here"
```

## 分步实现

### 步骤1：导入依赖项

首先导入所有必要的模块：

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
```

### 步骤2：配置浏览器工具

定义您的智能体可访问的浏览器工具（如果您想为智能体配置其他工具，请参考{doc}`sandbox`中的工具用法）：

```java
// 创建工具包
Toolkit toolkit = new Toolkit();

// 注册浏览器工具
toolkit.registerTool(ToolkitInit.BrowserNavigateTool());
toolkit.registerTool(ToolkitInit.BrowserTakeScreenshotTool());
toolkit.registerTool(ToolkitInit.BrowserSnapshotTool());
toolkit.registerTool(ToolkitInit.BrowserClickTool());
toolkit.registerTool(ToolkitInit.BrowserTypeTool());

System.out.println("✅ 已配置浏览器工具");
```

**可用的浏览器工具包括：**
- `BrowserNavigateTool()` - 导航到指定URL
- `BrowserClickTool()` - 点击页面元素
- `BrowserTypeTool()` - 在页面中输入文本
- `BrowserTakeScreenshotTool()` - 截取页面截图
- `BrowserSnapshotTool()` - 获取页面快照
- `BrowserTabNewTool()` - 创建新标签页
- `BrowserTabSelectTool()` - 选择标签页
- `BrowserTabCloseTool()` - 关闭标签页
- `BrowserWaitForTool()` - 等待页面元素
- `BrowserResizeTool()` - 调整浏览器窗口大小
- `BrowserCloseTool()` - 关闭浏览器
- `BrowserConsoleMessagesTool()` - 获取控制台消息
- `BrowserHandleDialogTool()` - 处理对话框
- `BrowserFileUploadTool()` - 上传文件
- `BrowserPressKeyTool()` - 按键操作
- `BrowserNavigateBackTool()` - 后退
- `BrowserNavigateForwardTool()` - 前进
- `BrowserNetworkRequestsTool()` - 获取网络请求
- `BrowserPdfSaveTool()` - 保存PDF
- `BrowserDragTool()` - 拖拽操作
- `BrowserHoverTool()` - 悬停操作
- `BrowserSelectOptionTool()` - 选择选项
- `BrowserTabListTool()` - 列出标签页

### 步骤3：定义系统提示词

创建一个系统提示词，为您的智能体建立角色、目标和网页浏览任务的操作指南：

```java
String SYSTEM_PROMPT = """You are a Web-Using AI assistant.

# Objective
Your goal is to complete given tasks by controlling a browser to navigate web pages.

## Web Browsing Guidelines
- Use the `browser_navigate` command to jump to specific webpages when needed.
- Use `generate_response` to answer the user once you have all the required information.
- Always answer in English.

### Observing Guidelines
- Always take action based on the elements on the webpage. Never create URLs or generate new pages.
- If the webpage is blank or an error, such as 404, is found, try refreshing it or go back to the previous page and find another webpage.
""";

System.out.println("✅ 系统提示词已配置");
```

### 步骤4：初始化上下文管理器

初始化上下文管理器以管理会话历史和记忆：

```java
private ContextManager initializeContextManager() throws Exception {
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
}
```

### 步骤5：初始化智能体和模型

使用AgentScope框架中您选择的大模型设置ReAct智能体：

```java
// 创建工具包
Toolkit toolkit = new Toolkit();
toolkit.registerTool(ToolkitInit.BrowserNavigateTool());
toolkit.registerTool(ToolkitInit.BrowserClickTool());
toolkit.registerTool(ToolkitInit.BrowserTypeTool());
toolkit.registerTool(ToolkitInit.BrowserTakeScreenshotTool());
toolkit.registerTool(ToolkitInit.BrowserSnapshotTool());

// 创建 ReActAgent
ReActAgent.Builder agentBuilder = ReActAgent.builder()
    .name("Friday")
    .sysPrompt(SYSTEM_PROMPT)
    .toolkit(toolkit)
    .memory(new InMemoryMemory())
    .model(io.agentscope.core.model.DashScopeChatModel.builder()
        .apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
        .modelName("qwen-max")
        .stream(true)
        .enableThinking(true)
        .formatter(new DashScopeChatFormatter())
        .build());

// 创建 AgentScopeAgent
AgentScopeAgent agentScopeAgent = AgentScopeAgent.builder()
    .agent(agentBuilder)
    .build();

System.out.println("✅ 智能体初始化成功");
```

### 步骤6：配置沙箱管理器

配置沙箱管理器以支持浏览器工具执行：

```java
// 创建沙箱管理器配置（使用默认Docker配置）
ManagerConfig managerConfig = ManagerConfig.builder().build();

// 创建沙箱管理器
SandboxManager sandboxManager = new SandboxManager(managerConfig);

// 创建环境管理器
EnvironmentManager environmentManager = new DefaultEnvironmentManager(sandboxManager);

System.out.println("✅ 沙箱管理器配置成功");
```

### 步骤7：创建 Runner

Runner 将智能体、上下文管理器和环境管理器组合在一起：

```java
Runner runner = Runner.builder()
    .agent(agentScopeAgent)
    .contextManager(contextManager)
    .environmentManager(environmentManager)
    .build();

System.out.println("✅ Runner created successfully");
```

### 步骤8：部署智能体

使用 `LocalDeployManager` 将智能体部署为 A2A 服务：

```java
LocalDeployManager.builder()
    .port(8090)
    .build()
    .deploy(runner);

System.out.println("✅ Agent deployed successfully on port 8090");
```

运行后，服务器会启动并监听：`http://localhost:8090/a2a/`

### 步骤9：发送请求

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
        "contextId": "id",
        "metadata": {
          "userId": "me",
          "sessionId": "my_session"
        },
        "parts": [
          {
            "kind": "text",
            "text": "Navigate to https://example.com and tell me what is on the page"
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

## 总结

通过遵循这些步骤，您已经成功设置、交互并部署了使用AgentScope框架和AgentScope Runtime的ReAct智能体。此配置允许智能体在沙箱环境中安全地使用浏览器工具，确保安全有效的网页交互。根据需要调整系统提示词、工具或模型，以自定义智能体的行为来适应特定任务或应用程序。

除了基本的 HTTP API 访问外，您还可以使用不同的协议与智能体进行交互，例如：Response API、Agent API等。详情请参考 {doc}`protocol`。

## 下一步

- 浏览 **完整实现示例** 在 `examples/browser_use_fullstack_runtime` 目录中
- 查看 **协议文档** 了解 A2A 协议和其他通信方式
- 了解 **沙箱工具** 和 **环境管理器** 的配置选项
- 探索更多浏览器工具的功能和使用方法

更多详细信息，请参考：
- {doc}`quickstart` - 快速开始指南
- {doc}`manager` - 管理器模块文档
- {doc}`protocol` - 协议文档
- {doc}`sandbox` - 沙箱文档
