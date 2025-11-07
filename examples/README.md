> Author: Xue Huitian
>

### Step-by-Step Tutorial
1. **Initialize the Runtime Context Manager:**

AgentScope Runtime Java currently has a built-in in-memory context manager

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

2. **(Optional) Initialize ManagerConfig:**

ManagerConfig is a configuration class used to initialize the sandbox manager. By default, it uses in-memory management for sandbox lifecycle, local Docker for sandbox deployment, and local file system without user-specified files.

**Optional Configuration 1: Use Redis to replace in-memory management for sandbox lifecycle**

```java
RedisManagerConfig redisConfig = RedisManagerConfig.builder()
        .redisServer("localhost")
        .redisPort(6379)
        .redisDb(0)
        .redisPortKey("_persist_test_ports")
        .redisContainerPoolKey("_persist_test_pool")
        .build();
```

**Optional Configuration 2: Configure file download initialization**

Users can specify which files the sandbox should download when starting. After specifying the storagePath property, the sandbox will first download files from storagePath to the specified mount path when starting, and copy files from the mount path back to storagePath after destroying the sandbox. Files will be mounted in the `sessions_mount_dir` folder at the root path of code execution, and a random sessionId folder will be created each time.

+ **Use local file system as storagePath:**

```java
LocalFileSystemConfig localConfig = LocalFileSystemConfig.builder()
        .storageFolderPath("path to the source folder to copy")
        .build();
```

+ **Use OSS as storage medium:**

```java
OssConfig ossConfig = OssConfig.builder()
        .ossEndpoint(ossEndpoint)
        .ossAccessKeyId(ossAccessKeyId)
        .ossAccessKeySecret(ossAccessKeySecret)
        .ossBucketName(ossBucketName)
        .storageFolderPath("OSS folder name")
        .build();
```

**Optional Configuration 3: Use Docker, K8s, or AgentRun as container management framework**

Docker is used by default. To use K8s, you need to configure a KubernetesClientConfig. If parameters are passed as empty, it will directly use the local default K8s environment. To use Alibaba Cloud FC AgentRun, you need to configure three required parameters: `AGENT_RUN_ACCESS_KEY_ID`, `AGENT_RUN_ACCOUNT_ID`, and `AGENT_RUN_ACCESS_KEY_SECRET`.

* **Use K8s as container management framework**

```java
BaseClientConfig clientConfig = KubernetesClientConfig.builder()
        .kubeConfigPath(System.getenv("KUBECONFIG_PATH"))
        .build();
```

* **Use AgentRun as container management framework**

```Java
BaseClientConfig clientConfig = AgentRunClientConfig.builder()
        .agentRunAccessKeyId(System.getenv("AGENT_RUN_ACCESS_KEY_ID"))
        .agentRunAccountId(System.getenv("AGENT_RUN_ACCOUNT_ID"))
        .agentRunAccessKeySecret(System.getenv("AGENT_RUN_ACCESS_KEY_SECRET"))
        .build();
```

**Optional Configuration 4: Container pool size**

**Optional Configuration 5: Available port range**

By default, ports 49152 to 59152 are used. Users can configure a custom port range:

```java
PortRange portRange = new PortRange(49152, 59152);
```

**Initialize managerConfig with the custom configurations above:**

```java
ManagerConfig config = new ManagerConfig.Builder()
                    .poolSize(0)
                    .fileSystemConfig(ossConfig)
                    .portRange(startPort, endPort)
                    .containerDeployment(kubeconfig)
                    .redisConfig(redisConfig)
                    .build();
```

3. **Create a native Spring AI Alibaba agent:**

+ **Initialize tools to be called**
    - **Built-in tools:**

AgentScope Runtime Java provides three sandboxes: the base sandbox provides code execution and terminal command execution functionality, the file system sandbox provides file management functionality, and the browser sandbox provides built-in browser functionality. Tools have been initialized in ToolsInit and wrapped as ToolCallBack compatible with SAA, which can be called directly. Below is an example:

    - For tools that don't require sandbox execution, directly create SAA's ToolCallBack type tools
    - For tools that require sandbox execution, there are two types: built-in tools and custom MCP tools

```java
ToolsInit.RunPythonCodeTool()
```

    - **MCP tools:**
    
    AgentScope Runtime Java provides the functionality to use sandboxes as stdio-type MCP servers. The execution method is as follows:

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
List<ToolCallback> mcpTools = ToolsInit.getMcpTools(
                    mcpServerConfig,
                    SandboxType.BASE,
                    Runner.getSandboxManager());
```

By configuring mcpServerConfig and specifying sandboxManager and sandbox type, you can directly create a ToolCallBack List compatible with SAA.

Users can also use `McpConfigConverter` to build MCP Tools

```java
McpConfigConverter converter = McpConfigConverter.builder()
        .serverConfigs(mcpServerConfig)
        .sandboxType(SandboxType.BASE)
        .sandboxManager(Runner.getSandboxManager())
        .build();

List<MCPTool> mcpToolInstances = converter.toBuiltinTools();

List<ToolCallback> toolCallbacks = mcpToolInstances.stream()
        .map(MCPTool::buildTool)
        .toList();
```

+ **Create Spring AI Alibaba Agent:**

```java
Builder builder = ReactAgent.builder()
        .name("saa_agent")
        .tools(toolCallbacks)
        .model(chatModel);
```

4. **Wrap Spring AI Alibaba Agent as SaaAgent provided by AgentScope Runtime Java:**

```java
SaaAgent saaAgent = SaaAgent.builder()
        .agentBuilder(builder)
        .build();
```

5. **Create and initialize Runner**

When initializing Runner, `agent` is a required attribute, and an exception will be thrown if it is not assigned. When using sandbox tools, the `environmentManager` attribute must also be added.

```java
SandboxManager sandboxManager = new SandboxManager(managerConfig);
EnvironmentManager environmentManager = new DefaultEnvironmentManager(sandboxManager);
        Runner runner = Runner.builder().agent(agent).contextManager(contextManager).environmentManager(environmentManager).build();
```

6. **One-click deployment as A2A application:**

```java
LocalDeployManager.builder().port(10001).build().deploy(runner);
```

Deployment uses SpringBoot, with port defaulting to `8080` and host defaulting to `localhost`. For custom configuration, you need to pass relevant configuration information during build.

7. **Access the deployed A2A application from terminal:**

```shell
curl --location --request POST 'http://localhost:10001/a2a/' \
--header 'User-Agent: Apifox/1.0.0 (https://apifox.com)' \
--header 'Content-Type: application/json' \
--header 'Accept: */*' \
--header 'Host: localhost:10002' \
--header 'Connection: keep-alive' \
--data-raw '{
    "method": "message/stream",
    "id": "2d2b4dc8-8ea2-437b-888d-3aaf3a8239dc",
    "jsonrpc": "2.0",
    "params": {
        "message": {
            "role": "user",
            "kind": "message",
            "contextId": "okokok",
            "metadata":{
                "userId": "me",
                "sessionId": "test12"
            },
            "parts": [
                {
                    "text": "Hello, please calculate the 10th Fibonacci number using Python",
                    "kind": "text"
                }
            ],
            "messageId": "c4911b64c8404b7a8bf7200dd225b152"
        }
    }
}'
```

This makes the Agent call the base sandbox to execute Python code.

If replaced with asking for the current time, the Agent will access the MCP tool deployed in the sandbox and get the current time.

The returned format is a standard A2A streaming response.

#### Complete examples can be found in examples/simple_agent_use_examples
