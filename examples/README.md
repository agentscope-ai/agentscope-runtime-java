> 作者：薛惠天
>

### 分步教程
1. **初始化运行时的上下文管理器：**

AgentScope Runtime Java目前内置了基于内存的上下文管理器

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

2. **初始化Runner：**

```java
Runner runner = new Runner(contextManager);
```

3. **（可选）初始化ManagerConfig：**

ManagerConfig是用于初始化沙箱管理器的配置类，默认使用内存管理沙箱生命周期，使用本地Docker部署沙箱，使用本地文件系统且没有用户指定的文件。

**可选配置1：使用redis替换内存管理沙箱生命周期**

```java
RedisManagerConfig redisConfig = RedisManagerConfig.builder()
        .redisServer("localhost")
        .redisPort(6379)
        .redisDb(0)
        .redisPortKey("_persist_test_ports")
        .redisContainerPoolKey("_persist_test_pool")
        .build();
```

**可选配置2：配置文件下载初始化**

用户可以指定沙箱在启动的时候下载哪些文件，在指定storagePath属性后，沙箱在启动的时候会首先将storagePath的文件首先下载到指定的挂载路径上，在销毁沙箱之后将挂载路径的文件再拷贝回storagePath，文件会挂载在代码执行的根路径的`sessions_mount_dir`文件夹之中，会每次创建一个随机的sessionId文件夹

+ **使用本地文件系统作为storagePath：**

```java
LocalFileSystemConfig localConfig = LocalFileSystemConfig.builder()
        .storageFolderPath("要拷贝的源文件夹路径")
        .build();
```

+ **使用OSS作为存储介质：**

```java
OssConfig ossConfig = OssConfig.builder()
        .ossEndpoint(ossEndpoint)
        .ossAccessKeyId(ossAccessKeyId)
        .ossAccessKeySecret(ossAccessKeySecret)
        .ossBucketName(ossBucketName)
        .storageFolderPath("OSS的文件夹名称")
        .build();
```

**可选配置3：使用docker或k8s来作为容器管理框架**

默认使用docker，要使用k8s，需要配置一个KubernetesClientConfig，参数如果传空的话，会直接使用本地的默认k8s环境

```java
BaseClientConfig clientConfig = KubernetesClientConfig.builder()
        .kubeConfigPath(System.getenv("KUBECONFIG_PATH"))
        .build();
```

**可选配置4：容器池的大小**

**可选配置5：可以使用的端口序列**

默认使用49152到59152端口，用户可以配置自定义端口range：

```java
PortRange portRange = new PortRange(49152, 59152);
```

**使用刚才的自定义配置初始化managerConfig：**

```java
ManagerConfig config = new ManagerConfig.Builder()
                    .poolSize(0)
                    .fileSystemConfig(ossConfig)
                    .portRange(startPort, endPort)
                    .containerDeployment(kubeconfig)
                    .redisConfig(redisConfig)
                    .build();
```

4. **创建runner：**

```java
Runner runner = new Runner(contextManager);
```

5. **注册managerConfig：**

```java
runner.registerManagerConfig(config);
```

6. **创建原生Spring AI Alibaba agent：**
+ **初始化要调用的工具**
    - **内置工具：**

AgentScope Runtime Java提供了三个沙箱，基础沙箱提供代码执行和终端命令执行功能，文件系统沙箱提供文件管理功能，浏览器沙箱提供内置浏览器功能，工具已经在ToolsInit被初始化完成包装为SAA可以直接兼容的ToolCallBack，直接调用即可，下面是一个示例

    - 对于不需要沙箱执行的工具，直接创建SAA的ToolCallBack类型工具即可
    - 对于需要沙箱执行的工具，分为内置的工具以及添加的自定义mcp工具两种

```java
ToolsInit.RunPythonCodeTool()
```

    - **MCP工具：**

	AgentScope Runtime Java提供了将沙箱作为stdio类型的mcp server的功能，执行方式如下：

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

通过配置mcpServerConfig，并制定sandboxManager和沙箱类型，可以直接创建为SAA可以直接兼容的ToolCallBack List

用户也可以使用`McpConfigConverter`来构建MCP Tools

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

+ **创建Spring AI Alibaba Agent：**

```java
Builder builder = ReactAgent.builder()
        .name("saa_agent")
        .tools(toolCallbacks)
        .model(chatModel);

ReactAgent reactAgent = builder.build();
```

7. **将Spring AI Alibaba Agent包装为AgentScope Runtime Java提供的SaaAgent：**

```java
SaaAgent saaAgent = SaaAgent.builder()
        .agentBuilder(reactAgent)
        .build();
```

9. **一键式部署为A2A应用：**

```java
LocalDeployManager deployManager = LocalDeployManager.builder().build();
deployManager.deploy(runner);
```

使用SpringBoot进行部署，因此对于自定义配置，需要在resources文件夹中进行修改，如修改端口等，目前是默认10001

10. **从终端访问部署的A2A应用：**

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
                    "text": "你好，给我用python计算一下第十个斐波那契数",
                    "kind": "text"
                }
            ],
            "messageId": "c4911b64c8404b7a8bf7200dd225b152"
        }
    }
}'
```

这里是让Agent去调用基础沙箱执行python代码

如果替换成询问当前时间，Agent会去访问沙箱中部署的mcp工具，并获取到当前时间

返回的格式为标准的A2A流式响应

### 完整示例
```java
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.Builder;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import io.agentscope.runtime.autoconfig.deployer.LocalDeployManager;
import io.agentscope.runtime.engine.Runner;
import io.agentscope.runtime.engine.agents.saa.SaaAgent;
import io.agentscope.runtime.engine.memory.context.ContextComposer;
import io.agentscope.runtime.engine.memory.context.ContextManager;
import io.agentscope.runtime.engine.memory.persistence.memory.service.InMemoryMemoryService;
import io.agentscope.runtime.engine.memory.persistence.session.InMemorySessionHistoryService;
import io.agentscope.runtime.engine.memory.service.MemoryService;
import io.agentscope.runtime.engine.memory.service.SessionHistoryService;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;
import io.agentscope.runtime.sandbox.manager.model.container.RedisManagerConfig;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxType;
import io.agentscope.runtime.sandbox.manager.model.fs.LocalFileSystemConfig;
import io.agentscope.runtime.sandbox.manager.model.fs.OssConfig;
import io.agentscope.runtime.sandbox.tools.ToolsInit;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;

public class CompleteAgentExample {

    private DashScopeChatModel chatModel;
    private ContextManager contextManager;

    private String ossEndpoint;
    private String ossAccessKeyId;
    private String ossAccessKeySecret;
    private String ossBucketName;

    public CompleteAgentExample() {
        initializeChatModel();
        initializeOssConfig();
        initializeContextManager();
    }

    public void initializeOssConfig() {
        ossEndpoint = System.getenv("OSS_ENDPOINT");
        ossAccessKeyId = System.getenv("OSS_ACCESS_KEY_ID");
        ossAccessKeySecret = System.getenv("OSS_ACCESS_KEY_SECRET");
        ossBucketName = System.getenv("OSS_BUCKET_NAME");
    }

    private void initializeChatModel() {
        // Create DashScopeApi instance using the API key from environment variable
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
                .build();

        // Create DashScope ChatModel instance
        this.chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .build();
    }

    private void initializeContextManager() {
        try {
            // Create SessionHistoryService for managing conversation history
            SessionHistoryService sessionHistoryService = new InMemorySessionHistoryService();

            // Create MemoryService for managing agent memory
            MemoryService memoryService = new InMemoryMemoryService();

            // Create ContextManager with the required services
            this.contextManager = new ContextManager(
                    ContextComposer.class,
                    sessionHistoryService,
                    memoryService);

            // Start the context manager services
            sessionHistoryService.start().get();
            memoryService.start().get();
            this.contextManager.start().get();

            System.out.println("ContextManager and its services initialized successfully");
        } catch (Exception e) {
            System.err.println("Failed to initialize ContextManager services: " + e.getMessage());
            throw new RuntimeException("ContextManager initialization failed", e);
        }
    }

    public ManagerConfig buildManagerConfig() {
        // 使用本地文件系统示例
        LocalFileSystemConfig localConfig = LocalFileSystemConfig.builder()
                .storageFolderPath(System.getenv("STORAGE_PATH"))
                .build();

        // 使用OSS示例
        OssConfig ossConfig = OssConfig.builder()
                .ossEndpoint(ossEndpoint)
                .ossAccessKeyId(ossAccessKeyId)
                .ossAccessKeySecret(ossAccessKeySecret)
                .ossBucketName(ossBucketName)
                .storageFolderPath("folder")
                .build();

        // 使用redis管理容器池示例
        RedisManagerConfig redisConfig = RedisManagerConfig.builder()
                .redisServer("localhost")
                .redisPort(6379)
                .redisDb(0)
                .redisPortKey("_persist_test_ports")
                .redisContainerPoolKey("_persist_test_pool")
                .build();

        return new ManagerConfig.Builder()
                .poolSize(0)
                .fileSystemConfig(ossConfig)
                // 配置端口映射示例
                .portRange(9000, 10000)
                .redisConfig(redisConfig)
                .build();
    }


    public void runExample() throws GraphStateException {
        Runner runner = new Runner(contextManager);
        runner.registerManagerConfig(buildManagerConfig());

        System.out.println("=== SaaAgent example Using custom MCP server ===");

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

        System.out.println("Created " + mcpTools.size() + " MCP tools");
        mcpTools.forEach(tool -> System.out.println("  - " + tool));

        System.out.println("Built " + mcpTools.size() + " ToolCallbacks");

        List<ToolCallback> tools = mcpTools;

        tools.add(ToolsInit.RunPythonCodeTool());

        Builder builder = ReactAgent.builder()
                .name("saa_agent")
                .tools(tools)
                .model(chatModel);

        ReactAgent reactAgent = builder.build();

        // Create SaaAgent using the ReactAgent Builder
        SaaAgent saaAgent = SaaAgent.builder()
                .agentBuilder(reactAgent)
                .build();


        runner.registerAgent(saaAgent);
        runner.registerContextManager(contextManager);

        LocalDeployManager deployManager = new LocalDeployManager();
        deployManager.deployStreaming();
    }

    public static void main(String[] args) {
        if (System.getenv("AI_DASHSCOPE_API_KEY") == null) {
            System.err.println("Please set the AI_DASHSCOPE_API_KEY environment variable");
            System.exit(1);
        }

        if (System.getenv("STORAGE_PATH") == null) {
            System.err.println("Please set the STORAGE_PATH environment variable");
            System.exit(1);
        }

        CompleteAgentExample example = new CompleteAgentExample();

        try {
            example.runExample();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

