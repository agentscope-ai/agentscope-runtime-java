# AgentScope Runtime Java Examples

This guide walks through the end-to-end workflow for running the sample projects under `examples/`. Each section expands on the building blocks required to spin up an agent, configure the sandboxed execution environment, and expose the agent through the A2A interface.

> **Tip:** The snippets below are extracted from the folders inside `examples/simple_agent_use_examples`. It shows you the quickest way to build your first agent the deploy it in your computer. Refer to the project files for complete runnable code.

---

## Prerequisites

- Java 17 or later
- Maven 3.6+
- Docker daemon running locally (required for sandboxed tools)
- Optional: Redis, Kubernetes, or Alibaba AgentRun credentials depending on your sandbox backend of choice

---

## Step 1 – Initialize the Runtime Context Manager

AgentScope Runtime Java ships with an in-memory implementation of the context manager services. The snippet below shows how to bootstrap the context manager and start its dependent services.

```java
private void initializeContextManager() {
    try {
        SessionHistoryService sessionHistoryService = new InMemorySessionHistoryService();
        MemoryService memoryService = new InMemoryMemoryService();
        this.contextManager = new ContextManager(
            ContextComposer.class,
            sessionHistoryService,
            memoryService
        );

        sessionHistoryService.start().get();
        memoryService.start().get();
        this.contextManager.start().get();

        System.out.println("ContextManager and its services initialized successfully");
    } catch (Exception e) {
        System.err.println("Failed to initialize ContextManager services: " + e.getMessage());
        throw new RuntimeException("ContextManager initialization failed", e);
    }
}
```

---

## Step 2 – Configure the Sandbox Manager (Optional but Recommended)

`ManagerConfig` controls how sandboxes are created, maintained, and supplied with supporting resources. The defaults use an in-memory lifecycle manager, local Docker runtime, and no pre-provisioned files. You can opt into additional providers as needed.

### 2.1 Lifecycle Backend

Switch sandbox lifecycle management from memory lifecycle tracking to Redis:

```java
RedisManagerConfig redisConfig = RedisManagerConfig.builder()
    .redisServer("localhost")
    .redisPort(6379)
    .redisDb(0)
    .redisPortKey("_persist_test_ports")
    .redisContainerPoolKey("_persist_test_pool")
    .build();
```

### 2.2 File Synchronization

Preload resources into the sandbox and persist output files after execution by defining a storage provider. When not set, the default is not to download files

```java
LocalFileSystemConfig localConfig = LocalFileSystemConfig.builder()
    .storageFolderPath("/path/to/assets")
    .build();
```

```java
OssConfig ossConfig = OssConfig.builder()
    .ossEndpoint(ossEndpoint)
    .ossAccessKeyId(ossAccessKeyId)
    .ossAccessKeySecret(ossAccessKeySecret)
    .ossBucketName(ossBucketName)
    .storageFolderPath("oss-folder")
    .build();
```

> **Note:** Volume binding is currently supported only when using the local Docker deployment target.

### 2.3 Container Runtime

Docker is selected by default. You can swap in Kubernetes or Alibaba AgentRun by providing the corresponding client configuration.

```java
BaseClientConfig dockerConfig = DockerClientConfig.builder()
    .host("127.0.0.1")
    .port(2375)
    .build();
```

```java
BaseClientConfig k8sConfig = KubernetesClientConfig.builder()
    .kubeConfigPath(System.getenv("KUBECONFIG_PATH"))
    .build();
```

```java
BaseClientConfig agentRunConfig = AgentRunClientConfig.builder()
    .agentRunAccessKeyId(System.getenv("AGENT_RUN_ACCESS_KEY_ID"))
    .agentRunAccountId(System.getenv("AGENT_RUN_ACCOUNT_ID"))
    .agentRunAccessKeySecret(System.getenv("AGENT_RUN_ACCESS_KEY_SECRET"))
    .build();
```

### 2.4 Configure remote runtime hosting sandbox

If you want to use a remote runtime to proxy all sandbox lifecycle management and tool invocation work for the current runtime, you can configure the `baseURL` and `bearerToken` properties in the manageability config. The default runtime sandbox will be managed by itself.

```java
ManagerConfig config = ManagerConfig.builder()
    .baseUrl("Remote Runtime Base Url")
    .bearerToken("Remote Runtime Bearer Token")
    .build();
```

### 2.5 Pool Size and Port Range

```java
ManagerConfig config = ManagerConfig.builder()
    .poolSize(0) // number of warm sandboxes to maintain
    .portRange(new PortRange(49152, 59152))
    .fileSystemConfig(localConfig)
    .containerDeployment(dockerConfig)
    .redisConfig(redisConfig)
    .build();
```

---

## Step 3 – Create the Native FrameWork Agent

### 3.1 Initialize Tools

AgentScope Runtime Java offers multiple sandbox types:

- **Base sandbox** – execute code snippets or shell commands
- **File-system sandbox** – manage files
- **Browser sandbox** – drive a built-in headless browser

Built-in tools are exposed via `ToolcallsInit` and can be added directly to a Spring AI Alibaba agent.

```java
ToolCallback pythonTool = ToolcallsInit.RunPythonCodeTool();
```

When using AgentScope, Built-in tools are exposed via `ToolkitInit`.

```java
AgentTool tool = ToolkitInit.RunPythonCodeTool();
```

> [!NOTE]
> The usage of Spring AI Alibaba Agent is similar to AgentScope, and the following is only an example of AgentScope

### 3.2 Configure MCP Tools

Sandboxes can act as stdio-based MCP servers. Construct MCP tool callbacks by passing the MCP configuration, sandbox type, and sandbox manager:

```java
String mcpServerConfig = """
{
  "mcpServers": {
    "time": {
      "command": "uvx",
      "args": [
        "mcp-server-time",
        "--local-timezone=America/New_York"
      ]
    }
  }
}
""";

List<AgentTool> mcpTools = ToolkitInit.getMcpTools(
                    mcpServerConfig,
                    SandboxType.BASE,
                    environmentManager.getSandboxManager());
```

### 3.3 Assemble the FrameWork Agent

```java
Builder builder = ReactAgent.builder()
    .name("saa_agent")
    .tools(mcpCallbacks)
    .model(chatModel);
```

---

## Step 4 – Wrap the Agent with `Runtime Agent（AgentScopeAgent or SaaAgent）`

```java
AgentScopeAgent agentScopeAgent = AgentScopeAgent.builder().agent(builder).build();
```

---

## Step 5 – Create and Initialize the Runner

The Runner wires the agent, context manager, and (optionally) sandbox-aware environment manager together. `agent` is mandatory; `environmentManager` is required when the agent invokes sandbox tools.

```java
SandboxManager sandboxManager = new SandboxManager(config);
EnvironmentManager environmentManager = new DefaultEnvironmentManager(sandboxManager);

Runner runner = Runner.builder()
    .agent(saaAgent)
    .contextManager(contextManager)
    .environmentManager(environmentManager)
    .build();
```

---

## Step 6 – Deploy as an A2A Application

Expose the agent through the standard A2A protocol using the local deployment manager. The default server listens on `localhost:8080`; override the port or host during builder configuration as needed.

```java
LocalDeployManager.builder()
    .port(10001)
    .build()
    .deploy(runner);
```

---

## Step 7 – Call the A2A Endpoint

Use any HTTP client to stream messages to the deployed agent. The example below triggers a Python calculation via the base sandbox.

```bash
curl --location --request POST 'http://localhost:10001/a2a/' \
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
            "text": "Hello, please calculate the 10th Fibonacci number using Python"
          }
        ],
        "messageId": "c4911b64c8404b7a8bf7200dd225b152"
      }
    }
  }'
```

Change the prompt to query for the current time to exercise an MCP-backed tool instead. The response format follows the A2A streaming specification.

---

## Where to Go Next

- Browse **complete implementations** in `examples/simple_agent_use_examples`
- Go to the `browser_use_fullstack_runtime` folder and try to **visualize the Agent's operations** in the sandbox
- Experiment with **different sandbox combinations and toolchains**
- Extend the Runner to integrate with **your own deployment pipelines**
