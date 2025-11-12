# Advanced Sandbox Usage

```{note}
This section introduces advanced sandbox usage for the Java version. We strongly recommend completing the previous basic tutorial ({doc}`sandbox`) before proceeding.
```

## Remote Sandbox Service Overview

AgentScope Runtime Java exposes remote management interfaces equivalent to the Python version through `SandboxManagerController`. As long as you start a `LocalDeployManager` for `Runner`, all methods annotated with `@RemoteWrapper` are automatically mapped to REST APIs, and clients only need to configure `baseUrl` in `ManagerConfig` to transparently call remote sandboxes.

### Starting Sandbox Controller with LocalDeployManager

The following example demonstrates how to start a remote sandbox controller on the local machine at `0.0.0.0:10001`. The `NoopAgent` in the example is only used to satisfy `Runner`'s dependency; in actual business scenarios, it can be replaced with any Agent implementation.

```java
import io.agentscope.runtime.LocalDeployManager;
import io.agentscope.runtime.engine.Runner;
import io.agentscope.runtime.engine.agents.Agent;
import io.agentscope.runtime.engine.agents.AgentConfig;
import io.agentscope.runtime.engine.agents.BaseAgent;
import io.agentscope.runtime.engine.schemas.agent.AgentResponse;
import io.agentscope.runtime.engine.schemas.message.Event;
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

> **Tip**: The example depends on `reactor-core`. Please ensure you have introduced `spring-boot-starter-web` or explicitly added the `reactor-core` dependency.

### HTTP Interface Overview

After deployment, `SandboxManagerController` exposes interfaces with `/` as the default prefix. All responses are JSON, with payloads returned in the `data` field. Common endpoints are shown in the following table:

| Path | Method | Purpose | Key Request Fields |
| --- | --- | --- | --- |
| `/createFromPool` | POST | Allocate or reuse container for specified user/session | `sandboxType`, `userID`, `sessionID` |
| `/createContainer` | POST | Create sandbox container of specified type | `sandboxType`, `mountDir`, `storagePath`, `environment` |
| `/startSandbox` / `/stopSandbox` | POST | Start or pause existing sandbox | `sandboxType`, `userID`, `sessionID` |
| `/getSandboxStatus` | POST | Query sandbox running status | `sandboxType`, `userID`, `sessionID` |
| `/getInfo` | POST | Return container details based on name/ID/sessionID | `identity` |
| `/release` | POST | Destroy and release container resources | `identity` |
| `/listTools` | POST | List available tools in sandbox | `sandboxId`, `userId`, `sessionId`, `toolType` |
| `/callTool` | POST | Execute specified tool | `sandboxId`, `toolName`, `arguments` |
| `/addMcpServers` | POST | Register MCP services to sandbox | `sandboxId`, `serverConfigs`, `overwrite` |
| `/cleanupAllSandboxes` | POST | Stop and remove all managed containers | None |

> **Note**: If you need to add Token validation to interfaces, you can add custom filters in the Spring container; clients can automatically carry `Authorization: Bearer ...` headers through `ManagerConfig.builder().bearerToken("token")`.

### Switching Client to Remote Mode

When `ManagerConfig` sets `baseUrl`, `SandboxManager` switches to remote mode, and all calls are forwarded to the service started earlier through `RemoteHttpClient`:

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

If `bearerToken` is not provided, the client will still attempt to call remote interfaces. When authentication is required, ensure the server has implemented authentication logic.

## ManagerConfig Advanced Configuration

`ManagerConfig` controls advanced behaviors of sandbox container backend, file system, port strategy, Redis caching, etc. The following table lists common options:

| Configuration Item | Description | Default Value / Example |
| --- | --- | --- |
| `containerPrefixKey` | New container name prefix | `runtime_sandbox_container_` |
| `poolSize` | Pre-warmed container pool capacity (0 means disabled) | `0` |
| `portRange(start, end)` | Dynamic port allocation range | `49152` - `59152` |
| `fileSystemConfig.mountDir` | Host mount directory | `sessions_mount_dir` |
| `fileSystemConfig.storageFolderPath` | Persistent sync directory (upload/download) | `""` |
| `fileSystemConfig.readonlyMounts` | Read-only mount mappings | `null` |
| `redisConfig` | Enable Redis for multi-process container information sharing | `null` |
| `containerDeployment` | Container backend (Docker/K8s/AgentRun) | Docker |
| `baseUrl` | Remote mode address | `null` (local mode) |
| `bearerToken` | Authentication token appended in remote mode | `null` |

### Configuration Example: Local Docker + Read-only Mounts

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

> **Tip**: Mount directories must be created in advance and Docker access permissions granted; `storageFolderPath` will sync file content when releasing containers.

### Configuration Example: Kubernetes Cluster

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

Kubernetes mode will create sandboxes as Pods in the specified namespace. Please ensure image registry credentials are available in the cluster.

### Configuration Example: Enable Redis Shared Pool

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

After enabling Redis, multiple processes can share container mappings and port occupancy information, suitable for multi-replica or horizontal scaling scenarios.

### Configuration Example: Deploy to AgentRun

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

When `containerDeployment` is set to AgentRun, it will call Alibaba Cloud Serverless AgentRun platform to execute sandbox instances. Please configure network, VPC, and image permissions in advance.

## Registering Custom Sandboxes

You can register new sandbox types (or replace images for existing types) for custom images through the `@RegisterSandbox` annotation. Environment variables support `${VAR}` and `${VAR:default}` placeholders, which read host environment variables at runtime.

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

Registration process:

```java
import io.agentscope.runtime.sandbox.manager.registry.SandboxAnnotationProcessor;
import io.agentscope.runtime.sandbox.manager.registry.SandboxRegistryInitializer;

SandboxRegistryInitializer.initialize();
SandboxAnnotationProcessor.processClass(AnalyticsSandbox.class);
```

If `customType` is set, you can construct sandboxes using string types.

## Building Custom Sandbox Images

The Java version no longer provides the `runtime-sandbox-builder` command. Please use native Docker toolchain:

```bash
docker build -t registry.example.com/runtime-sandbox-analytics:latest -f docker/analytics/Dockerfile .
docker push registry.example.com/runtime-sandbox-analytics:latest
```

A minimal Dockerfile example:

```dockerfile
FROM agentscope/runtime-sandbox-base:latest

RUN apt-get update && apt-get install -y --no-install-recommends graphviz \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt /tmp/requirements.txt
RUN pip install --no-cache-dir -r /tmp/requirements.txt
```

After building, write the image address to `@RegisterSandbox.imageName`.

## Resource Cleanup and Operations Recommendations

- `SandboxCleanupListener` will call `cleanupAllSandboxes` when the Spring context closes, ensuring containers are safely reclaimed.
- In remote mode, you can explicitly call the `/cleanupAllSandboxes` interface to perform cleanup tasks.
- It is recommended to call `LocalDeployManager.shutdown()` before service shutdown and wait for tasks to complete.
- For long-occupied sandboxes, you can use `SandboxManager.releaseSandbox(...)` to actively release resources.
- Reasonably configure `poolSize` and `portRange`, and combine with log monitoring (such as container count, port occupancy, Redis queue length) to detect resource leaks early.



