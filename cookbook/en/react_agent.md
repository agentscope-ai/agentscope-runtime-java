# Deploying ReAct Agent with Tool Sandbox

This tutorial demonstrates how to create and deploy a *"Reasoning and Acting" (ReAct)* agent using AgentScope Runtime Java with the [**AgentScope Java Framework**](https://github.com/agentscope-ai/agentscope-java).

```{note}
The ReAct (Reasoning and Acting) paradigm enables agents to interleave reasoning traces with task-specific actions, making it particularly effective for tool-interactive tasks. By combining AgentScope's `ReActAgent` with AgentScope Runtime's infrastructure, you get both intelligent decision-making and secure tool execution.
```

## Prerequisites

### 🔧 Installation Requirements

- **Java 17** or higher
- **Maven 3.6+**
- **Docker** (for sandbox tool execution)

### 📦 Project Dependencies

Add the following dependencies to your `pom.xml` file:

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
    
    <!-- Web Deployment Support -->
    <dependency>
        <groupId>io.agentscope</groupId>
        <artifactId>agentscope-runtime-web</artifactId>
        <version>0.1.1</version>
    </dependency>
</dependencies>
```

### 🐳 Sandbox Setup

```{note}
Make sure your browser sandbox environment is ready to use. For details, see {doc}`sandbox`.
```

Ensure the browser sandbox image is available:

```bash
docker pull agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-browser:latest && docker tag agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-browser:latest agentscope/runtime-sandbox-browser:latest
```

### 🔑 API Key Configuration

You need to prepare an API key for your chosen LLM provider. This example uses DashScope (Qwen), but you can adapt it to other providers:

```bash
export AI_DASHSCOPE_API_KEY="your_api_key_here"
```

## Step-by-Step Implementation

### Step 1: Import Dependencies

First, import all necessary modules:

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

### Step 2: Configure Browser Tools

Define the browser tools your agent can access (if you want to configure other tools for your agent, refer to tool usage in {doc}`sandbox`):

```java
// Create toolkit
Toolkit toolkit = new Toolkit();

// Register browser tools
toolkit.registerTool(ToolkitInit.BrowserNavigateTool());
toolkit.registerTool(ToolkitInit.BrowserTakeScreenshotTool());
toolkit.registerTool(ToolkitInit.BrowserSnapshotTool());
toolkit.registerTool(ToolkitInit.BrowserClickTool());
toolkit.registerTool(ToolkitInit.BrowserTypeTool());

System.out.println("✅ Browser tools configured");
```

**Available browser tools include:**
- `BrowserNavigateTool()` - Navigate to specified URL
- `BrowserClickTool()` - Click page elements
- `BrowserTypeTool()` - Type text on the page
- `BrowserTakeScreenshotTool()` - Capture page screenshots
- `BrowserSnapshotTool()` - Get page snapshots
- `BrowserTabNewTool()` - Create new tab
- `BrowserTabSelectTool()` - Select tab
- `BrowserTabCloseTool()` - Close tab
- `BrowserWaitForTool()` - Wait for page elements
- `BrowserResizeTool()` - Resize browser window
- `BrowserCloseTool()` - Close browser
- `BrowserConsoleMessagesTool()` - Get console messages
- `BrowserHandleDialogTool()` - Handle dialogs
- `BrowserFileUploadTool()` - Upload files
- `BrowserPressKeyTool()` - Key press operations
- `BrowserNavigateBackTool()` - Navigate back
- `BrowserNavigateForwardTool()` - Navigate forward
- `BrowserNetworkRequestsTool()` - Get network requests
- `BrowserPdfSaveTool()` - Save PDF
- `BrowserDragTool()` - Drag operations
- `BrowserHoverTool()` - Hover operations
- `BrowserSelectOptionTool()` - Select options
- `BrowserTabListTool()` - List tabs

### Step 3: Define System Prompt

Create a system prompt that establishes the role, objectives, and operational guidelines for your agent's web browsing tasks:

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

System.out.println("✅ System prompt configured");
```

### Step 4: Initialize Context Manager

Initialize the context manager to manage session history and memory:

```java
private ContextManager initializeContextManager() throws Exception {
    // Create session history service
    SessionHistoryService sessionHistoryService = new InMemorySessionHistoryService();
    
    // Create memory service
    MemoryService memoryService = new InMemoryMemoryService();
    
    // Create context manager
    ContextManager contextManager = new ContextManager(
        ContextComposer.class,
        sessionHistoryService,
        memoryService
    );
    
    // Start services
    sessionHistoryService.start().get();
    memoryService.start().get();
    contextManager.start().get();
    
    System.out.println("✅ ContextManager initialized successfully");
    return contextManager;
}
```

### Step 5: Initialize Agent and Model

Set up the ReAct agent using the large language model of your choice from the AgentScope framework:

```java
// Create toolkit
Toolkit toolkit = new Toolkit();
toolkit.registerTool(ToolkitInit.BrowserNavigateTool());
toolkit.registerTool(ToolkitInit.BrowserClickTool());
toolkit.registerTool(ToolkitInit.BrowserTypeTool());
toolkit.registerTool(ToolkitInit.BrowserTakeScreenshotTool());
toolkit.registerTool(ToolkitInit.BrowserSnapshotTool());

// Create ReActAgent
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

// Create AgentScopeAgent
AgentScopeAgent agentScopeAgent = AgentScopeAgent.builder()
    .agent(agentBuilder)
    .build();

System.out.println("✅ Agent initialized successfully");
```

### Step 6: Configure Sandbox Manager

Configure the sandbox manager to support browser tool execution:

```java
// Create sandbox manager configuration (using default Docker configuration)
ManagerConfig managerConfig = ManagerConfig.builder().build();

// Create sandbox manager
SandboxManager sandboxManager = new SandboxManager(managerConfig);

// Create environment manager
EnvironmentManager environmentManager = new DefaultEnvironmentManager(sandboxManager);

System.out.println("✅ Sandbox manager configured successfully");
```

### Step 7: Create Runner

The Runner combines the agent, context manager, and environment manager:

```java
Runner runner = Runner.builder()
    .agent(agentScopeAgent)
    .contextManager(contextManager)
    .environmentManager(environmentManager)
    .build();

System.out.println("✅ Runner created successfully");
```

### Step 8: Deploy Agent

Use `LocalDeployManager` to deploy the agent as an A2A service:

```java
LocalDeployManager.builder()
    .port(8090)
    .build()
    .deploy(runner);

System.out.println("✅ Agent deployed successfully on port 8090");
```

After running, the server will start and listen on: `http://localhost:8090/a2a/`

### Step 9: Send Request

You can use `curl` to send a request to the A2A API:

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

You will see a response streamed in **Server-Sent Events (SSE)** format with **A2A** protocol:

```json
event:jsonrpc
data:{"jsonrpc":"2.0","id":"xxx","result":{"taskId":"xxx","status":{"state":"working","message":{"role":"agent","parts":[{"text":"text","kind":"text"}],"messageId":"xxx","contextId":"xxx","taskId":"xxx","metadata":{},"kind":"message"},"timestamp":"xxx"},"contextId":"xxx","final":false,"kind":"status-update"}}
```

## Summary

By following these steps, you have successfully set up, interacted with, and deployed a ReAct agent using the AgentScope framework and AgentScope Runtime. This configuration allows the agent to safely use browser tools in a sandboxed environment, ensuring secure and effective web interactions. Adjust the system prompt, tools, or model as needed to customize the agent's behavior for specific tasks or applications.

In addition to basic HTTP API access, you can also interact with the agent using different protocols, such as: Response API, Agent API, etc. For details, refer to {doc}`protocol`.

## Next Steps

- Browse **complete implementation examples** in the `examples/browser_use_fullstack_runtime` directory
- Check the **protocol documentation** to learn about A2A protocol and other communication methods
- Learn about **sandbox tools** and **environment manager** configuration options
- Explore more browser tool features and usage methods

For more detailed information, please refer to:
- {doc}`quickstart` - Quick start guide
- {doc}`manager` - Manager module documentation
- {doc}`protocol` - Protocol documentation
- {doc}`sandbox` - Sandbox documentation




