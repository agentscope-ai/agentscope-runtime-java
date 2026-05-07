# 自定义沙箱示例

一个完整的示例，演示沙箱系统的两层自定义能力：**沙箱运行时后端**（与 Docker / K8s / AgentRun / FC 并列）和**沙箱类型**（工具调用、钩子埋点、会话复用）。所有实现均为 fake/stub 教学用途，无需真实运行时环境。

## 架构概览

沙箱系统分为两个独立的扩展层：

```
┌─────────────────────────────────────────────────────────────────┐
│                        应用层（Sandbox）                         │
│  你的业务代码通过 Sandbox 对象调用工具、执行 Python/Shell 等       │
│                                                                 │
│  内置类型：BaseSandbox / BrowserSandbox / FilesystemSandbox ...  │
│  自定义：继承 Sandbox + @RegisterSandbox + SandboxProvider SPI   │
├─────────────────────────────────────────────────────────────────┤
│                     基础设施层（BaseClientStarter + BaseClient）  │
│  决定沙箱实例在 **哪里** 运行、**如何** 管理其生命周期              │
│                                                                 │
│  内置实现：DockerClientStarter / KubernetesClientStarter /       │
│           AgentRunClientStarter / FcClientStarter                │
│  自定义：继承 BaseClientStarter + BaseClient                     │
└─────────────────────────────────────────────────────────────────┘
```

**两层的关系：**
- **基础设施层** 回答 "沙箱跑在哪" — Docker 本地运行？K8s 集群？云函数？ECS？还是你自己的编排系统？
- **应用层** 回答 "沙箱里干什么" — 跑什么工具？调用前后做什么？怎么注入配置？

两层完全正交，你可以：
- 两层都用 fake/stub 实现（本示例的做法）
- 用内置后端（Docker、K8s 等）运行自定义沙箱
- 用自定义运行时后端运行内置沙箱
- 两层都自定义

## 你将学到

**基础设施层 — 自定义沙箱运行时后端：**
1. `BaseClientStarter`：配置如何连接到运行时后端
2. `BaseClient`：实现沙箱实例的完整生命周期管理（创建、启动、停止、删除）

**应用层 — 自定义沙箱类型：**
3. 继承 `Sandbox`，通过 `@RegisterSandbox` + SPI 注册
4. 重写 `callTool()` 添加前后置钩子
5. 封装 `runPython()` / `runShell()` 等便捷方法
6. 会话复用与环境变量注入

## 前置条件

- Java 17+
- Maven 3.6+
- 核心模块已安装（在项目根目录执行 `mvn clean install -DskipTests`）

## 快速开始

### 1) 编译

```bash
mvn clean compile
```

### 2) 运行

```bash
mvn exec:java -Dexec.mainClass="io.agentscope.Main"
```

### 3) 预期输出

示例作为单一的端到端流程运行：

```
╔══════════════════════════════════════════════════════════════╗
║  Custom Sandbox Example — Full Lifecycle Demo               ║
╚══════════════════════════════════════════════════════════════╝

--- Step 1: Create CustomClientStarter ---
--- Step 3: Start SandboxService (triggers connect()) ---
[CustomClientStarter] Creating client (host=my-platform.example.com, port=443, label=demo)
[CustomClient] Connecting to platform at my-platform.example.com:443 ...
[CustomClient] Connected successfully (label=demo)

--- Step 4: Create CustomSandbox (triggers container lifecycle) ---
[CustomClient] Checking if image/template exists: ...
[CustomClient] Creating runtime instance: ...
[CustomClient] Instance created: id=fake-xxxxxxxxxxxx, ip=127.0.0.1, ports=[8080]
[CustomClient] Starting instance: fake-xxxxxxxxxxxx

--- Step 5: Run Python (hooks fire automatically) ---
[HOOK:before] Tool 'run_ipython_cell' called with args: {code=x = 42...}
[HOOK:after]  Tool 'run_ipython_cell' completed in 0ms
  Result: [fake output] Python executed: x = 42...

--- Step 6: Run Shell Command ---
[HOOK:before] Tool 'run_shell_command' called with args: {command=echo ...}
  Result: [fake output] Shell executed: echo ...

--- Step 8: Session Reuse (same userId + sessionId) ---
--- Step 9: Environment Variable Injection ---
--- Step 10: Close sandbox (triggers cleanup lifecycle) ---

╔══════════════════════════════════════════════════════════════╗
║  ✅ All steps completed successfully!                         ║
╚══════════════════════════════════════════════════════════════╝
```

---

## Part 1：自定义沙箱运行时后端

> 与 Docker / Kubernetes / AgentRun / FC 并列的自定义实现。
> 沙箱运行时后端可以是任何计算资源 — Docker 容器、ECS 实例、虚拟机、K8s Pod、Serverless 函数，或者你自己的编排平台。

本示例使用 **fake/stub** 实现（`CustomClient` + `CustomClientStarter`）来演示生命周期流程和每个方法的职责。将 TODO 桩代码替换为你平台的 API 调用即可变为真实实现。

### 核心接口

```
BaseClientStarter              BaseClient
┌───────────────────┐          ┌──────────────────────────┐
│ + startClient()   │─创建──▶  │ + connect()              │
│ + getContainerType│          │ + createContainer()      │
└───────────────────┘          │ + startContainer()       │
                               │ + stopContainer()        │
                               │ + removeContainer()      │
                               │ + getContainerStatus()   │
                               │ + imageExists()          │
                               │ + pullImage()            │
                               │ + inspectContainer()     │
                               │ + isConnected()          │
                               └──────────────────────────┘
```

### 现有实现对照

| 实现 | ClientStarter | BaseClient | 容器类型 | 场景 |
|------|---------------|------------|---------|------|
| Docker | `DockerClientStarter` | `DockerClient` | `DOCKER` | 本地开发、单机部署 |
| Kubernetes | `KubernetesClientStarter` | `KubernetesClient` | `KUBERNETES` | K8s 集群 |
| AgentRun | `AgentRunClientStarter` | `AgentRunClient` | `AGENTRUN` | 阿里云 AgentRun |
| FC | `FcClientStarter` | `FcClient` | `FC` | 阿里云函数计算 |
| **你的实现** | `MyClientStarter` | `MyClient` | 自定义 | 你的编排平台 |

### 第一步：实现 BaseClient

`BaseClient` 是管理沙箱运行时实例的核心抽象。运行时实例可以是任何东西 — Docker 容器、虚拟机、K8s Pod、甚至是一台通过 SSH 连接的远程机器。

> **说明：** 尽管 API 中使用了 "container" 一词（如 `createContainer`、`ContainerCreateResult`），但它是一个通用抽象，并不局限于 Docker 容器。

框架（`SandboxService`）按照以下固定顺序调用这些方法：

```
connect() → imageExists() → pullImage() → createContainer() → startContainer()
  → （沙箱运行，工具通过 HTTP 调用） →
stopContainer() → removeContainer()
```

每个方法都注释了不同平台（Docker、K8s、ECS、Serverless）的对应操作：

```java
public class CustomClient extends BaseClient {

    @Override
    public boolean connect() {
        // Docker:      通过 TCP 或 Unix Socket 连接 Docker daemon
        // Kubernetes:  加载 kubeconfig，连接 K8s API server
        // ECS/VM:      初始化云 SDK 客户端，验证凭证
        // SSH:         建立 SSH 连接池到远程机器
        return true;
    }

    @Override
    public boolean imageExists(String imageName) {
        // Docker:      检查本地是否有该镜像（docker images）
        // Kubernetes:  通常返回 true（K8s 在创建 Pod 时自动拉取）
        // ECS/VM:      检查该区域是否存在 VM 镜像/AMI/快照
        return true;
    }

    @Override
    public ContainerCreateResult createContainer(String containerName, String imageName,
            List<String> ports, List<VolumeBinding> volumeBindings,
            Map<String, String> environment, Map<String, Object> runtimeConfig) {
        // Docker:      docker create + 端口映射 + 卷挂载
        // Kubernetes:  创建 Pod spec，配置容器、Service、PVC
        // ECS/VM:      调用 RunInstances API，配置安全组、VPC、UserData
        // Serverless:  用沙箱镜像创建/更新函数

        String instanceId = "fake-" + UUID.randomUUID().toString().substring(0, 12);
        List<String> exposedPorts = List.of("8080");
        String instanceIp = "127.0.0.1";

        // 框架用 ip + ports 构建沙箱 HTTP 端点：
        //   http://{ip}:{port}/fastapi/tools/run_ipython_cell
        return new ContainerCreateResult(instanceId, exposedPorts, instanceIp);
    }

    @Override
    public void startContainer(String containerId) { /* 启动实例 */ }

    @Override
    public void stopContainer(String containerId) { /* 停止实例 */ }

    @Override
    public void removeContainer(String containerId) { /* 清理资源 */ }

    @Override
    public String getContainerStatus(String containerId) { return "running"; }

    @Override
    public boolean isConnected() { return true; }

    @Override
    public boolean pullImage(String imageName) { return true; }

    @Override
    public boolean inspectContainer(String containerIdOrName) { return true; }

    @Override
    public boolean containerNameExists(String containerName) { return false; }
}
```

> 完整实现请查看 [`CustomClient.java`](src/main/java/io/agentscope/CustomClient.java)，每个方法都有详细的 Javadoc。

**`ContainerCreateResult` — 框架需要你返回的信息：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `containerId` | `String` | 实例唯一标识符（容器 ID、VM 实例 ID、Pod 名称） |
| `ports` | `List<String>` | 沙箱 HTTP API 监听的端口号 |
| `ip` | `String` | 实例可访问的 IP 地址或主机名 |

### 第二步：实现 BaseClientStarter

`BaseClientStarter` 是一个工厂，持有连接配置并创建 `BaseClient` 实例。在 `SandboxService.start()` 时被调用。

根据你的平台需要，修改配置字段：

| 平台 | 典型配置字段 |
|------|------------|
| Docker | host, port, certPath |
| Kubernetes | kubeConfigPath, namespace |
| ECS/VM | accessKeyId, accessKeySecret, regionId, securityGroupId |
| SSH | sshHost, sshPort, sshUser, privateKeyPath |
| 自定义 API | apiEndpoint, apiToken, maxRetries |

```java
public class CustomClientStarter extends BaseClientStarter {

    private final String host;
    private final int port;
    private final String label;

    private CustomClientStarter(Builder builder) {
        // ContainerClientType 决定 SandboxService 的行为差异：
        //   DOCKER:      本地拉取镜像，使用本地卷挂载
        //   KUBERNETES:  跳过镜像拉取（K8s 自行处理），使用 PVC
        //   AGENTRUN/FC: 跳过本地卷挂载，使用绝对路径
        // 选择最匹配你平台行为的类型。
        super(ContainerClientType.DOCKER);
        this.host = builder.host;
        this.port = builder.port;
        this.label = builder.label;
    }

    @Override
    public BaseClient startClient(PortManager portManager) {
        CustomClient client = new CustomClient(this);
        client.connect();
        return client;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String host = "localhost";
        private int port = 8080;
        private String label = "custom";

        public Builder host(String host) { this.host = host; return this; }
        public Builder port(int port) { this.port = port; return this; }
        public Builder label(String label) { this.label = label; return this; }
        public CustomClientStarter build() { return new CustomClientStarter(this); }
    }
}
```

> 完整实现请查看 [`CustomClientStarter.java`](src/main/java/io/agentscope/CustomClientStarter.java)。

> **关于 `ContainerClientType`：** 当前版本的 `ContainerClientType` 是一个枚举，只包含 `DOCKER`、`KUBERNETES`、`AGENTRUN`、`FC` 四种类型。如果你的自定义运行时在行为上类似 Docker（本地运行、支持卷挂载），可以复用 `ContainerClientType.DOCKER`；如果类似云服务（无需本地镜像拉取、不支持本地卷挂载），可以使用其他类型。如果需要增加新的枚举值，需要修改 `ContainerClientType` 源码并提交 PR。

### 第三步：接入使用

```java
// 第 1 步：创建自定义 BaseClientStarter
BaseClientStarter localStarter = CustomClientStarter.builder()
        .host("my-platform.example.com")
        .port(443)
        .label("demo")
        .build();

// 第 2 步：插入 ManagerConfig（替换默认的 DockerClientStarter）
ManagerConfig config = ManagerConfig.builder()
        .clientStarter(localStarter)
        .build();

// 第 3 步：启动 SandboxService — 触发 connect()
SandboxService service = new SandboxService(config);
service.start();

// 第 4 步：创建沙箱 — 触发: imageExists → createContainer → startContainer
try (CustomSandbox sandbox = new CustomSandbox(service, "user1", "session1")) {
    sandbox.listTools("");  // 触发懒初始化
}
```

> 完整可运行示例请查看 [`Main.java`](src/main/java/io/agentscope/Main.java) Part 1 部分。

### 完整类图

```
ManagerConfig
  └── clientStarter: BaseClientStarter     ← 你选择或自定义的运行时后端
        ├── DockerClientStarter            ← 内置：本地 Docker
        ├── KubernetesClientStarter        ← 内置：K8s 集群
        ├── AgentRunClientStarter          ← 内置：阿里云 AgentRun
        ├── FcClientStarter                ← 内置：阿里云函数计算
        └── CustomClientStarter             ← 示例：教学用 fake/stub 实现
              └── startClient() → CustomClient extends BaseClient
                    ├── connect()
                    ├── createContainer()   → 返回 ContainerCreateResult
                    ├── startContainer()
                    ├── stopContainer()
                    ├── removeContainer()
                    └── ...
```

---

## Part 2：自定义沙箱类型

> 定义沙箱里 **干什么**：工具调用、钩子埋点、会话管理

### 第一步：定义自定义沙箱

继承 `Sandbox` 并添加 `@RegisterSandbox` 注解：

```java
@RegisterSandbox(
    imageName = "your-registry/your-sandbox-image:latest",
    sandboxType = "custom",
    securityLevel = "medium",
    timeout = 60,
    description = "带工具执行钩子的自定义沙箱"
)
public class CustomSandbox extends Sandbox {

    public CustomSandbox(SandboxService managerApi, String userId, String sessionId) {
        super(managerApi, userId, sessionId, "custom");
    }
}
```

**注解参数说明：**

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `imageName` | 沙箱使用的容器镜像 | （必填） |
| `sandboxType` | 唯一的沙箱类型标识符 | `"base"` |
| `securityLevel` | 安全级别：low / medium / high | `"medium"` |
| `timeout` | 操作超时时间（秒） | `300` |
| `environment` | 环境变量（`KEY=VALUE` 格式） | `{}` |

### 第二步：添加前后置钩子

重写 `callTool()` 方法，拦截每次工具调用：

```java
@Override
public String callTool(String name, Map<String, Object> arguments) {
    // ---- 前置钩子 ----
    long startTime = System.currentTimeMillis();
    beforeToolCall(name, arguments);

    String result;
    try {
        // ---- 实际执行 ----
        // 真实应用中，调用 super.callTool(name, arguments)
        // 会发送 HTTP POST 到沙箱实例。
        // 本示例使用 fake 实现：
        result = fakeToolExecution(name, arguments);
    } catch (Exception e) {
        // ---- 异常钩子 ----
        onToolError(name, arguments, e);
        throw e;
    }

    // ---- 后置钩子 ----
    long elapsed = System.currentTimeMillis() - startTime;
    afterToolCall(name, arguments, result, elapsed);
    return result;
}

private String fakeToolExecution(String name, Map<String, Object> arguments) {
    switch (name) {
        case "run_ipython_cell":
            return "[fake output] Python executed: " + arguments.get("code");
        case "run_shell_command":
            return "[fake output] Shell executed: " + arguments.get("command");
        default:
            return "[fake output] Unknown tool: " + name;
    }
}
```

**钩子的典型用途：**
- 记录或审计每次工具调用（谁、调了什么、什么时候、传了什么参数）
- 在执行前校验或转换参数
- 计算执行耗时，用于性能监控
- 拦截执行结果做后处理
- 实现限流、权限控制或重试逻辑

### 第三步：添加便捷方法

将 `callTool()` 封装为领域化的便捷方法：

```java
public String runPython(String code) {
    Map<String, Object> arguments = new HashMap<>();
    arguments.put("code", code);
    return callTool("run_ipython_cell", arguments);
}

public String runShell(String command) {
    Map<String, Object> arguments = new HashMap<>();
    arguments.put("command", command);
    return callTool("run_shell_command", arguments);
}
```

### 第四步：通过 SPI 注册

创建 `SandboxProvider` 实现类：

```java
public class CustomSandboxProvider implements SandboxProvider {
    @Override
    public Collection<Class<?>> getSandboxClasses() {
        return Collections.singletonList(CustomSandbox.class);
    }
}
```

在 `META-INF/services/io.agentscope.runtime.sandbox.manager.registry.SandboxProvider` 中注册：

```
io.agentscope.CustomSandboxProvider
```

### 第五步：使用自定义沙箱

```java
// 创建 SandboxService（使用自定义后端）
BaseClientStarter clientStarter = CustomClientStarter.builder()
        .host("my-platform.example.com").port(443).label("demo").build();
ManagerConfig managerConfig = ManagerConfig.builder()
        .clientStarter(clientStarter)
        .build();
SandboxService sandboxService = new SandboxService(managerConfig);
sandboxService.start();

// 使用自定义沙箱
CustomSandbox sandbox = new CustomSandbox(sandboxService, "user1", "session1");

// 运行 Python — 钩子自动触发
String result = sandbox.runPython("print('Hello from sandbox!')");
System.out.println(result);

// 运行 Shell
sandbox.runShell("echo 'Hello World'");

// 直接调用底层工具
sandbox.callTool("run_ipython_cell", Map.of("code", "1 + 1"));
```

> 完整可运行示例请查看 [`Main.java`](src/main/java/io/agentscope/Main.java)。

### 会话复用

使用 **相同的 `userId` + `sessionId`** 创建新的沙箱对象，会复用已有的容器。Python 变量在多次调用间持久保留：

```java
// 第一个沙箱对象创建容器
CustomSandbox s1 = new CustomSandbox(service, "user1", "session1");
s1.runPython("x = 42");

// 第二个沙箱对象复用同一个容器 — x 仍然存在
CustomSandbox s2 = new CustomSandbox(service, "user1", "session1");
s2.runPython("print(x)");  // 输出: 42
```

> **注意：** 调用 `close()`（或退出 `try-with-resources`）会停止并销毁容器。

### 自定义环境变量

在创建容器时传入自定义环境变量：

```java
try (CustomSandbox sandbox = new CustomSandbox(
        sandboxService, "user1", "session1",
        Map.of("MY_API_KEY", "secret123", "APP_ENV", "production"))) {
    sandbox.runShell("echo $MY_API_KEY");  // 输出: secret123
}
```

---

## 内置工具参考

`base` 沙箱镜像开箱即用地提供以下工具：

| 工具名称 | 说明 | 参数 |
|---------|------|------|
| `run_ipython_cell` | 执行 Python 代码（IPython） | `code`：Python 代码字符串 |
| `run_shell_command` | 执行 Shell 命令 | `command`：Shell 命令字符串 |

## 项目结构

```
custom_sandbox_example/
├── src/main/java/io/agentscope/
│   ├── CustomClient.java            # Fake/stub BaseClient — 教学用生命周期合约（Part 1）
│   ├── CustomClientStarter.java     # 自定义 BaseClientStarter — 工厂 + 配置（Part 1）
│   ├── CustomSandbox.java          # 带钩子的自定义沙箱（Part 2 示例）
│   ├── CustomSandboxProvider.java   # SPI 注册提供者
│   └── Main.java                   # 入口：完整生命周期演示
├── src/main/resources/META-INF/services/
│   └── io.agentscope.runtime.sandbox.manager.registry.SandboxProvider
└── pom.xml
```

## 相关文档

- [沙箱使用指南（中文）](../../../cookbook/zh/sandbox/sandbox.md)
- [沙箱使用指南（English）](../../../cookbook/en/sandbox/sandbox.md)
- [AgentScope Runtime Java 项目主页](../../../README.md)
