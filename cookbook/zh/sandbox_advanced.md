# 工具沙箱高级用法

```{note}
本节介绍 Java 版本的沙箱高级用法。我们强烈建议在继续之前先完成上一节的基础教程({doc}`sandbox`)。
```

## 远程沙箱服务概览

AgentScope Runtime Java 通过 `SandboxManagerController` 暴露与 Python 版本等价的远程管理接口。只要为 `Runner` 启动一个 `LocalDeployManager`，所有带有 `@RemoteWrapper` 注解的方法都会自动映射为 REST API，客户端只需在 `ManagerConfig` 中配置 `baseUrl` 即可透明地调用远程沙箱。

### 使用 LocalDeployManager 启动沙箱控制器

下面的示例演示如何在本机 `0.0.0.0:10001` 上启动远程沙箱控制器。示例中的 `NoopAgent` 仅用于满足 `Runner` 的依赖；在实际业务中可以替换为任意 Agent 实现。

```java
import io.agentscope.runtime.LocalDeployManager;
import io.agentscope.runtime.engine.Runner;
import io.agentscope.runtime.engine.agents.Agent;
import io.agentscope.runtime.engine.agents.AgentConfig;
import io.agentscope.runtime.engine.agents.BaseAgent;
import io.agentscope.runtime.engine.schemas.agent.AgentResponse;
import io.agentscope.runtime.engine.schemas.agent.Event;
import io.agentscope.runtime.engine.schemas.context.Context;
import io.agentscope.runtime.engine.service.EnvironmentManager;
import io.agentscope.runtime.engine.service.impl.DefaultEnvironmentManager;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;
import reactor.core.publisher.Flux;

public final class SandboxServerExample {

    private static final class NoopAgent extends BaseAgent {
        NoopAgent() {
            super(new AgentConfig());
        }

        @Override
        protected Flux<Event> execute(Context context, boolean stream) {
            AgentResponse response = new AgentResponse();
            response.created();
            response.completed();
            return Flux.just(response);
        }

        @Override
        public Agent copy() {
            return new NoopAgent();
        }
    }

    public static void main(String[] args) {
        ManagerConfig managerConfig = ManagerConfig.builder()
                .poolSize(2)
                .build();

        SandboxManager sandboxManager = new SandboxManager(managerConfig);
        EnvironmentManager environmentManager = new DefaultEnvironmentManager(sandboxManager);

        Runner runner = Runner.builder()
                .agent(new NoopAgent())
                .environmentManager(environmentManager)
                .build();

        LocalDeployManager deployManager = LocalDeployManager.builder()
                .endpointName("sandbox-manager")
                .host("0.0.0.0")
                .port(10001)
                .build();

        deployManager.deploy(runner);
        Runtime.getRuntime().addShutdownHook(new Thread(deployManager::shutdown));
    }
}
```

> **提示**：示例依赖 `reactor-core`，请确认已经引入 `spring-boot-starter-web` 或显式添加 `reactor-core` 依赖。

### HTTP 接口概览

部署后，`SandboxManagerController` 默认以 `/` 为前缀暴露接口。所有响应均为 JSON，并在 `data` 字段返回有效载荷。常用端点如下表所示：

| Path | 方法 | 作用 | 关键请求字段 |
| --- | --- | --- | --- |
| `/createFromPool` | POST | 为指定用户/会话分配或复用容器 | `sandboxType`, `userID`, `sessionID` |
| `/createContainer` | POST | 创建指定类型的沙箱容器 | `sandboxType`, `mountDir`, `storagePath`, `environment` |
| `/startSandbox` / `/stopSandbox` | POST | 启动或暂停现有沙箱 | `sandboxType`, `userID`, `sessionID` |
| `/getSandboxStatus` | POST | 查询沙箱运行状态 | `sandboxType`, `userID`, `sessionID` |
| `/getInfo` | POST | 根据名称/ID/会话ID 返回容器详情 | `identity` |
| `/release` | POST | 销毁并释放容器资源 | `identity` |
| `/listTools` | POST | 列出沙箱内可用工具 | `sandboxId`, `userId`, `sessionId`, `toolType` |
| `/callTool` | POST | 执行指定工具 | `sandboxId`, `toolName`, `arguments` |
| `/addMcpServers` | POST | 向沙箱注册 MCP 服务 | `sandboxId`, `serverConfigs`, `overwrite` |
| `/cleanupAllSandboxes` | POST | 停止并移除所有托管容器 | 无 |

> **说明**：如果需要为接口增加 Token 校验，可在 Spring 容器中添加自定义过滤器；客户端可通过 `ManagerConfig.builder().bearerToken("token")` 自动携带 `Authorization: Bearer ...` 头部。

### 将客户端切换到远程模式

当 `ManagerConfig` 设置了 `baseUrl` 时，`SandboxManager` 会切换到远程模式，所有调用都会通过 `RemoteHttpClient` 转发到前文启动的服务：

```java
import io.agentscope.runtime.sandbox.box.BaseSandbox;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;

ManagerConfig remoteConfig = ManagerConfig.builder()
        .baseUrl("http://127.0.0.1:10001")
        .bearerToken("optional-token")
        .build();

SandboxManager remoteManager = new SandboxManager(remoteConfig);

try (BaseSandbox sandbox = new BaseSandbox(remoteManager, "user_1", "session_1")) {
    String result = sandbox.runIpythonCell("print(\"Hello Sandbox\")");
    System.out.println(result);
}
```

如果未提供 `bearerToken`，客户端仍会尝试调用远程接口。需要鉴权时，请确保服务端已实现认证逻辑。

## ManagerConfig 进阶配置

`ManagerConfig` 控制了沙箱的容器后端、文件系统、端口策略、Redis 缓存等高级行为。下表列出了常用选项：

| 配置项 | 说明 | 默认值 / 示例 |
| --- | --- | --- |
| `containerPrefixKey` | 新建容器名称前缀 | `runtime_sandbox_container_` |
| `poolSize` | 预热容器池容量（0 表示禁用） | `0` |
| `portRange(start, end)` | 动态端口分配范围 | `49152` - `59152` |
| `fileSystemConfig.mountDir` | 宿主机挂载目录 | `sessions_mount_dir` |
| `fileSystemConfig.storageFolderPath` | 持久化同步目录（上传/下载） | `""` |
| `fileSystemConfig.readonlyMounts` | 只读挂载映射 | `null` |
| `redisConfig` | 启用 Redis 用于多进程共享容器信息 | `null` |
| `containerDeployment` | 容器后端（Docker/K8s/AgentRun） | Docker |
| `baseUrl` | 远程模式地址 | `null`（本地模式） |
| `bearerToken` | 远程模式下附加的认证 Token | `null` |

### 配置示例：本地 Docker + 只读挂载

```java
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;
import io.agentscope.runtime.sandbox.manager.model.fs.LocalFileSystemConfig;

LocalFileSystemConfig fileSystemConfig = LocalFileSystemConfig.builder()
        .mountDir("/var/agentscope/mounts")
        .storageFolderPath("/var/agentscope/storage")
        .addReadonlyMount("/etc/ssl/certs", "/etc/ssl/certs")
        .build();

ManagerConfig managerConfig = ManagerConfig.builder()
        .containerPrefixKey("sandbox_")
        .poolSize(3)
        .portRange(41000, 42000)
        .fileSystemConfig(fileSystemConfig)
        .build();
```

> **提示**：挂载目录需提前创建并赋予 Docker 访问权限；`storageFolderPath` 会在释放容器时同步文件内容。

### 配置示例：Kubernetes 集群

```java
import io.agentscope.runtime.sandbox.manager.client.config.KubernetesClientConfig;

ManagerConfig managerConfig = ManagerConfig.builder()
        .containerDeployment(
                KubernetesClientConfig.builder()
                        .kubeConfigPath(System.getenv("KUBECONFIG"))
                        .namespace("agentscope")
                        .build()
        )
        .build();
```

Kubernetes 模式会将沙箱以 Pod 的形式创建到指定命名空间，请确保镜像仓库凭证在集群中可用。

### 配置示例：启用 Redis 共享池

```java
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;
import io.agentscope.runtime.sandbox.manager.model.container.RedisManagerConfig;

RedisManagerConfig redisConfig = RedisManagerConfig.builder()
        .redisServer("redis.internal")
        .redisPort(6380)
        .redisPassword("s3cret")
        .redisPortKey("_runtime_ports")
        .redisContainerPoolKey("_runtime_pool")
        .build();

ManagerConfig managerConfig = ManagerConfig.builder()
        .redisConfig(redisConfig)
        .poolSize(10)
        .build();
```

启用 Redis 后，多个进程可以共享容器映射与端口占用信息，适用于多副本或水平扩展场景。

### 配置示例：部署到 AgentRun

```java
import io.agentscope.runtime.sandbox.manager.client.config.AgentRunClientConfig;

AgentRunClientConfig agentRunConfig = AgentRunClientConfig.builder()
        .agentRunAccessKeyId(System.getenv("AGENTRUN_AK"))
        .agentRunAccessKeySecret(System.getenv("AGENTRUN_SK"))
        .agentRunAccountId(System.getenv("AGENTRUN_ACCOUNT"))
        .agentRunRegionId("cn-hangzhou")
        .agentRunCpu(4.0f)
        .agentRunMemory(4096)
        .build();

ManagerConfig managerConfig = ManagerConfig.builder()
        .containerDeployment(agentRunConfig)
        .build();
```

当 `containerDeployment` 设置为 AgentRun 时，会调用阿里云 Serverless AgentRun 平台执行沙箱实例，请提前配置网络、VPC 及镜像权限。

## 注册自定义沙箱

通过 `@RegisterSandbox` 注解可以为自定义镜像注册新的沙箱类型（或为现有类型替换镜像）。环境变量支持 `${VAR}` 与 `${VAR:default}` 占位符，在运行时会读取宿主机环境变量。

```java
import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxType;
import io.agentscope.runtime.sandbox.manager.registry.RegisterSandbox;

@RegisterSandbox(
        imageName = "registry.example.com/runtime-sandbox-analytics:latest",
        sandboxType = SandboxType.BASE,
        timeout = 120,
        securityLevel = "medium",
        description = "Sandbox with analytics dependencies",
        environment = {
                "HF_TOKEN=${HF_TOKEN}",
                "JAVA_TOOL_OPTIONS=-Xmx2g"
        },
        resourceLimits = {
                "memory=3g",
                "cpu=3.0"
        }
)
public class AnalyticsSandbox extends Sandbox {
    public AnalyticsSandbox(
            SandboxManager managerApi,
            String userId,
            String sessionId) {
        super(managerApi, userId, sessionId, SandboxType.BASE, 120);
    }
}
```

注册流程：

```java
import io.agentscope.runtime.sandbox.manager.registry.SandboxAnnotationProcessor;
import io.agentscope.runtime.sandbox.manager.registry.SandboxRegistryInitializer;

SandboxRegistryInitializer.initialize();
SandboxAnnotationProcessor.processClass(AnalyticsSandbox.class);
```

如果设置了 `customType`，可以使用字符串类型构造沙箱。

## 构建自定义沙箱镜像

Java 版本不再提供 `runtime-sandbox-builder` 命令，请使用原生 Docker 工具链：

```bash
docker build -t registry.example.com/runtime-sandbox-analytics:latest -f docker/analytics/Dockerfile .
docker push registry.example.com/runtime-sandbox-analytics:latest
```

一个精简的 Dockerfile 示例：

```dockerfile
FROM agentscope/runtime-sandbox-base:latest

RUN apt-get update && apt-get install -y --no-install-recommends graphviz \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt /tmp/requirements.txt
RUN pip install --no-cache-dir -r /tmp/requirements.txt
```

构建完成后，将镜像地址写入 `@RegisterSandbox.imageName` 即可。

## 资源清理与运维建议

- `SandboxCleanupListener` 会在 Spring 上下文关闭时调用 `cleanupAllSandboxes`，确保容器被安全回收。
- 远程模式下可以显式调用 `/cleanupAllSandboxes` 接口执行清理任务。
- 建议在服务关闭前调用 `LocalDeployManager.shutdown()` 并等待任务完成。
- 对于长时间占用的沙箱，可使用 `SandboxManager.releaseSandbox(...)` 主动释放资源。
- 合理配置 `poolSize` 与 `portRange`，并结合日志监控（例如容器数量、端口占用、Redis 队列长度）提前发现资源泄漏。
