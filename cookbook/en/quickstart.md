# Quick Start

This tutorial demonstrates how to build a simple agent and deploy it as a service in the **AgentScope Runtime Java** framework.

## Prerequisites

### 🔧 Installation Requirements

- **Java 17** or higher
- **Maven 3.6+**
- **Docker** (optional, for sandbox tool execution)

### 📦 Project Dependencies

Add the following dependencies to your `pom.xml` file:

```xml
<dependencies>
    <!-- AgentScope Runtime Core -->
    <dependency>
        <groupId>io.agentscope</groupId>
        <artifactId>agentscope-runtime-core</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </dependency>
    
    <!-- AgentScope Agent (choose one) -->
    <dependency>
        <groupId>io.agentscope</groupId>
        <artifactId>agentscope-runtime-agentscope</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </dependency>
    
    <!-- Or Spring AI Alibaba Agent -->
    <dependency>
        <groupId>io.agentscope</groupId>
        <artifactId>agentscope-runtime-saa</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </dependency>
    
    <!-- Web Deployment Support -->
    <dependency>
        <groupId>io.agentscope</groupId>
        <artifactId>agentscope-runtime-web</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```

### 🔑 API Key Configuration

You need to provide an API key for your chosen LLM provider. This example uses DashScope (Qwen):

```bash
export AI_DASHSCOPE_API_KEY="your_api_key_here"
```

## Step-by-Step Implementation

### Step 1: Initialize Context Manager

The context manager is used to manage session history and memory services:

```java
import io.agentscope.runtime.engine.memory.context.ContextManager;
import io.agentscope.runtime.engine.memory.context.ContextComposer;
import io.agentscope.runtime.engine.memory.persistence.memory.service.InMemoryMemoryService;
import io.agentscope.runtime.engine.memory.persistence.session.InMemorySessionHistoryService;
import io.agentscope.runtime.engine.memory.service.MemoryService;
import io.agentscope.runtime.engine.memory.service.SessionHistoryService;

private ContextManager initializeContextManager() {
    try {
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
    } catch (Exception e) {
        System.err.println("Failed to initialize ContextManager: " + e.getMessage());
        throw new RuntimeException("ContextManager initialization failed", e);
    }
}
```

### Step 2: Create Agent

You can choose to use **AgentScope Agent** or **Spring AI Alibaba (SAA) Agent**.

#### Method 1: Using AgentScope Agent

```java
import io.agentscope.core.ReActAgent;
import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.runtime.engine.agents.agentscope.AgentScopeAgent;
import io.agentscope.runtime.engine.agents.agentscope.tools.ToolkitInit;
import io.agentscope.core.model.DashScopeChatModel;

// Create toolkit
Toolkit toolkit = new Toolkit();
toolkit.registerTool(ToolkitInit.RunPythonCodeTool());
toolkit.registerTool(ToolkitInit.RunShellCommandTool());

// Create ReActAgent
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

// Create AgentScopeAgent
AgentScopeAgent agentScopeAgent = AgentScopeAgent.builder()
    .agent(agentBuilder)
    .build();

System.out.println("✅ AgentScope agent created successfully");
```

#### Method 2: Using Spring AI Alibaba (SAA) Agent

```java
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.Builder;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import io.agentscope.runtime.engine.agents.saa.SaaAgent;
import io.agentscope.runtime.engine.agents.saa.tools.ToolcallsInit;
import java.util.List;

// Create DashScope API
DashScopeApi dashScopeApi = DashScopeApi.builder()
    .apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
    .build();

// Create DashScope ChatModel
DashScopeChatModel chatModel = DashScopeChatModel.builder()
    .dashScopeApi(dashScopeApi)
    .build();

// Create ReactAgent Builder
Builder agentBuilder = ReactAgent.builder()
    .name("Friday")
    .model(chatModel)
    .tools(List.of(
        ToolcallsInit.RunPythonCodeTool(),
        ToolcallsInit.RunShellCommandTool()
    ));

// Create SaaAgent
SaaAgent saaAgent = SaaAgent.builder()
    .agent(agentBuilder)
    .build();

System.out.println("✅ SAA agent created successfully");
```

### Step 3: Configure Sandbox Manager (Optional but Recommended)

If you need to use sandbox tools (such as Python code execution, file operations, etc.), you need to configure the sandbox manager:

```java
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;
import io.agentscope.runtime.engine.service.EnvironmentManager;
import io.agentscope.runtime.engine.service.impl.DefaultEnvironmentManager;

// Create sandbox manager configuration (using default Docker configuration)
ManagerConfig managerConfig = ManagerConfig.builder().build();

// Create sandbox manager
SandboxManager sandboxManager = new SandboxManager(managerConfig);

// Create environment manager
EnvironmentManager environmentManager = new DefaultEnvironmentManager(sandboxManager);

System.out.println("✅ Sandbox manager configured successfully");
```

### Step 4: Create Runner

The Runner combines the agent, context manager, and environment manager:

```java
import io.agentscope.runtime.engine.Runner;

Runner runner = Runner.builder()
    .agent(agentScopeAgent)  // or saaAgent
    .contextManager(contextManager)
    .environmentManager(environmentManager)  // Required if using sandbox tools
    .build();

System.out.println("✅ Runner created successfully");
```

### Step 5: Deploy Agent

Use `LocalDeployManager` to deploy the agent as an A2A service:

```java
import io.agentscope.runtime.LocalDeployManager;

// Deploy agent (default port 8080)
LocalDeployManager.builder()
    .port(8090)
    .build()
    .deploy(runner);

System.out.println("✅ Agent deployed successfully on port 8090");
```

### Step 6: Send Request

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
            "text": "What is the capital of France?"
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

### Step 7: Using Sandbox Tools (Optional)

If you want your agent to execute Python code or use other sandbox tools, you can add the corresponding tools when creating the agent:

```java
// For AgentScope Agent
Toolkit toolkit = new Toolkit();
toolkit.registerTool(ToolkitInit.RunPythonCodeTool());
toolkit.registerTool(ToolkitInit.RunShellCommandTool());
toolkit.registerTool(ToolkitInit.BrowserNavigateTool());

// For SAA Agent
Builder agentBuilder = ReactAgent.builder()
    .name("Friday")
    .model(chatModel)
    .tools(List.of(
        ToolcallsInit.RunPythonCodeTool(),
        ToolcallsInit.RunShellCommandTool(),
        ToolcallsInit.BrowserNavigateBackTool()
    ));
```

Then you can have the agent execute code through requests:

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

## Complete Example

Here is a complete runnable example:

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
        // Check API key
        if (System.getenv("AI_DASHSCOPE_API_KEY") == null) {
            System.err.println("Please set the AI_DASHSCOPE_API_KEY environment variable");
            System.exit(1);
        }
        
        try {
            // Step 1: Initialize context manager
            ContextManager contextManager = initializeContextManager();
            
            // Step 2: Create agent
            AgentScopeAgent agent = createAgent();
            
            // Step 3: Configure sandbox manager
            EnvironmentManager environmentManager = createEnvironmentManager();
            
            // Step 4: Create Runner
            Runner runner = Runner.builder()
                .agent(agent)
                .contextManager(contextManager)
                .environmentManager(environmentManager)
                .build();
            
            // Step 5: Deploy agent
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

## Next Steps

- Browse **complete implementation examples** in the `examples/simple_agent_use_examples` directory
- Check the **protocol documentation** to learn about A2A protocol and other communication methods
- Learn about **context manager** and **memory service** detailed usage
- Explore **sandbox tools** and **environment manager** configuration options

For more detailed information, please refer to:
- {doc}`manager` - Manager module documentation
- {doc}`context_manager` - Context manager documentation
- {doc}`protocol` - Protocol documentation
- {doc}`environment_manager` - Environment manager documentation



