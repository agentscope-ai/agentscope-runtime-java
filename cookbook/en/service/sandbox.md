# Sandbox Service

## Overview

**Sandbox Service** is used to provide isolated **tool execution environments** (sandbox) for different users and sessions, allowing agents to use tools (such as browsers, code executors, etc.) in a controlled and secure environment. For sandboxes, please refer to [Sandbox](../sandbox/sandbox.md)

During agent operation, typical roles of Sandbox Service include:

- **Create Execution Environment**: Generate corresponding sandbox instances (such as browser sandbox) for a new user/session.
- **Connect to Existing Environment**: During multi-turn conversation execution, agents connect to previous sandboxes to continue operations.
- **Tool Invocation**: Provide callable methods (such as `navigate`, `takeScreenshot`, etc.) that can be registered as tools in Agents.
- **Release Environment**: Release corresponding environment resources when sessions end or requirements change.
- **Multi-Type Support**: Supports different types of sandboxes (`BASE`, `BROWSER`, `FILESYSTEM`, `GUI`, etc.).

Differences in Sandbox Service across different implementations are mainly reflected in:
**Runtime Mode** (embedded/remote), **Supported Types**, **Management Methods**, and **Scalability**.

> In business code, it is not recommended to directly write underlying management logic for sandbox services and `SandboxManager`.
> 
> It is more recommended to **use AgentScope Runtime Java's encapsulated sandbox methods** bound to the agent framework's tool module**:
> - Shield underlying sandbox API details
> - Unified lifecycle management by Runner/Engine
> - Ensure switching runtime modes or sandbox types doesn't affect business logic

## Using Sandbox Tools in AgentScope

In the **AgentScope** framework, we can use **encapsulated sandbox methods** (`ToolkitInit`) and register them in the Agent's `Toolkit`:

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
        // 1. Start the sandbox service
        BaseClientStarter clientStarter = DockerClientStarter.builder().build();
        ManagerConfig managerConfig = ManagerConfig.builder()
                .clientStarter(clientStarter)
                .build();
        SandboxService sandboxService = new SandboxService(managerConfig);
        sandboxService.start();

        // 2. Connect to or create a sandbox (here, a browser-type sandbox is created)
        Sandbox sandbox = new BrowserSandbox(sandboxService, "user", "session");

        // 3. Obtain tool methods and register them into the Agent's Toolkit
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(ToolkitInit.BrowserNavigateTool(sandbox));
        toolkit.registerTool(ToolkitInit.BrowserTakeScreenshotTool(sandbox));

        // From this point on, the Agent can safely invoke these tools within the sandbox
    }
}
```

## Optional Runtime Modes and Types

### 1. **Embedded Mode**

- **Characteristics**: Sandbox manager runs in the same process as AgentScope Runtime Java.
- **Configuration**: `baseUrl=null`
- **Advantages**: Simple deployment, no external API required; suitable for local development and single-machine testing.
- **Disadvantages**: Environment is released when process exits; not suitable for distributed deployment.

### 2. **Remote API Mode**

- **Features**: Connects to remote sandbox instances via the Sandbox Management API (`SandboxService`).- **Configuration**: `baseUrl="http://host:port"`, `bearerToken="..."`
- **Advantages**: Can share environments across processes/machines, supports distributed scaling.
- **Disadvantages**: Requires deployment and operation of remote sandbox management services.

### Supported Sandbox Types

| Type Value   | Description                     | Common Use Cases                          |
|--------------|----------------------------------|-------------------------------------------|
| `BASE`       | Basic sandbox environment        | General-purpose tool execution            |
| `BROWSER`    | Browser sandbox                  | Web navigation, screenshots, data scraping|
| `FILESYSTEM` | Filesystem sandbox               | Secure read/write operations in isolated filesystem |
| `GUI`        | Graphical User Interface sandbox | Interacting with GUI apps (clicks, input, screenshots) |
| `APPWORLD`   | Application World simulation sandbox | Simulating cross-application interactions in a virtual environment |
| `BFCL`       | BFCL (Business Function Computing Layer) sandbox | Running domain-specific workflow scripts (implementation-dependent) |
| `AGENTBAY`   | AgentBay session-aware sandbox   | Persistent environments for multi-agent collaboration or complex task orchestration |
| `MOBILE`     | Mobile device sandbox            | Simulating mobile device operations       |

## Runtime Mode Switching Examples

### **Embedded Mode (Suitable for Development/Testing)**

```java
// Local mode (default uses local Docker)
ManagerConfig managerConfig = ManagerConfig.builder()
                .build();
SandboxService sandboxService = new SandboxService(managerConfig);

sandboxService.start();

Sandbox sandbox = sandboxService.connect("DevSession", "User1", BrowserSandbox.class);
```

### **Remote Mode (Suitable for Production Deployment)**

```java
// Local mode (default uses local Docker)
ManagerConfig managerConfig = ManagerConfig.builder()
                .baseUrl("https://sandbox-manager.com")
                .bearerToken("YOUR_AUTH_TOKEN")
                .build();
SandboxService sandboxService = new SandboxService(managerConfig);
sandboxService.start();

Sandbox sandbox = new BrowserSandbox(sandboxService, "ProdSession", "UserABC");
```

### Release Environment

Explicitly release resources when the session ends:

```java
// Release sandbox directly by container ID
sandboxService.release("container_id");

// Or release via the sandbox instance
sandbox.release();
```

## Selection Recommendations

- Quick Prototyping / Single-Machine Development/Testing:
    - Embedded mode (`baseUrl=null`)
    - Choose `BROWSER`/`BASE` types as needed
- Production Environment / Multi-User Distributed:
    - Remote API mode (requires deploying the `SandboxService` via the `web` module)
    - Consider clustering and authentication mechanisms (`bearerToken`)
- High Security or Isolation Requirements:
    - Create independent sandboxes for different user sessions
    - Use `release()` to release resources promptly

## Summary

- **SandboxService** is the core component for managing sandbox execution environments, supporting multiple environment types.
- It is recommended to register sandbox methods to tool modules through **encapsulated sandbox methods** (`ToolkitInit`), avoiding direct operation of underlying APIs.
- Optional **Embedded Mode** (simple, single-machine) or **Remote Mode** (scalable, production-grade).
- Lifecycle is managed by `Runner/Engine`, ensuring consistent startup, health checks, and release.
- Switching modes or types only requires changing service initialization parameters without affecting Agent business logic.
