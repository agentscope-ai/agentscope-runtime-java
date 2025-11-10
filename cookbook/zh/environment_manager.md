# 环境管理器（Environment Manager）

## 概述

`EnvironmentManager` 通过 `SandboxManager` 提供沙箱化环境与工具的生命周期与访问能力。

默认实现 `DefaultEnvironmentManager` 会注入一个 `SandboxManager` 实例，用于管理环境的创建、连接与释放。

## 基础用法

```java
// 创建环境管理器（内部会创建 SandboxManager）
EnvironmentManager environmentManager = new DefaultEnvironmentManager();

// 获取沙盒管理器
SandboxManager sandboxManager = environmentManager.getSandboxManager();

try {
    // 创建沙盒（使用 try-with-resources 自动管理）
    try (BaseSandbox sandbox = new BaseSandbox(
        sandboxManager,
        "u1",  // userId
        "s1",  // sessionId
        300    // timeout in seconds
    )) {
        // 使用沙盒
        // ...
    } // 沙盒自动释放

} catch (Exception e) {
    e.printStackTrace();
} finally {
    // 清理所有沙盒
    sandboxManager.cleanupAllSandboxes();
}
```

或者，如果您自己管理 `SandboxManager` 的生命周期：

```java
// 创建并管理 SandboxManager
try (SandboxManager sandboxManager = new SandboxManager()) {
    // 创建环境管理器，传入 SandboxManager
    EnvironmentManager environmentManager = new DefaultEnvironmentManager(sandboxManager);
    
    // 使用环境管理器
    try (BaseSandbox sandbox = new BaseSandbox(
        sandboxManager,
        "u1",
        "s1",
        300
    )) {
        // 使用沙盒
        // ...
    }
} // SandboxManager 自动清理所有资源
```

未来，`EnvironmentManager` 将不仅支持 `SandboxManager`，也会扩展到其它与环境交互的服务。

**沙盒管理器**旨在管理和提供对不同用户和会话的沙盒化工具执行（详见{doc}`sandbox`）沙盒的访问。沙盒通过会话ID和用户ID的复合键进行组织，为每个用户会话提供隔离的执行上下文。该服务支持多种沙盒类型，包括 BASE、BROWSER、FILESYSTEM、PYTHON 等。

## 沙盒管理器概述

沙盒管理器为沙盒管理提供统一接口，支持不同的沙盒类型，如代码执行、文件操作和其他专用沙盒。以下是初始化沙盒管理器的示例：

```java
// 创建默认沙盒管理器
SandboxManager sandboxManager = new SandboxManager();

// 或者使用自定义配置
ManagerConfig config = ManagerConfig.builder()
    .poolSize(10)
    .build();
SandboxManager sandboxManager = new SandboxManager(config);

// 或者使用远程沙盒服务
// ManagerConfig config = ManagerConfig.builder()
//   .poolSize(10)
//	 .baseUrl("remote_url")
//   .bearerToken("bearer_token")
//   .build();
// SandboxManager sandboxManager = new SandboxManager(config);
```

### 核心功能

#### 创建沙盒

在 Java 中，沙盒通过 `Sandbox` 类的构造函数创建。沙盒管理器会自动从池中获取或创建容器：

```java
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxType;
import io.agentscope.runtime.sandbox.box.BaseSandbox;

SandboxManager sandboxManager = new SandboxManager();
String sessionId = "session1";
String userId = "user1";

// 创建基础沙盒
BaseSandbox baseSandbox = new BaseSandbox(
    sandboxManager,
    userId,
    sessionId,
    300
)
```

#### 使用不同类型的沙盒

Java 版本支持多种沙盒类型。每种沙盒类型都有对应的实现类：

```java
// 创建基础沙盒
BaseSandbox baseSandbox = new BaseSandbox(
    sandboxManager,
    userId,
    sessionId,
    300
);

// 创建浏览器沙盒
BrowserSandbox browserSandbox = new BrowserSandbox(
    sandboxManager,
    userId,
    sessionId,
    300
);

// 创建文件系统沙盒
FilesystemSandbox filesystemSandbox = new FilesystemSandbox(
    sandboxManager,
    userId,
    sessionId,
    300
);

// 创建训练沙盒
TrainingSandbox trainingSandbox = new TrainingSandbox(
    sandboxManager,
    userId,
    sessionId,
    SandboxType.TRAINING,
    300
);
```

#### 沙盒重用

沙盒管理器高效地为同一用户会话和沙盒类型重用现有沙盒：

```java
// 第一次创建
BaseSandbox sandbox1 = new BaseSandbox(
    sandboxManager,
    userId,
    sessionId,
    SandboxType.BASE,
    300
);

// 第二次创建相同类型的沙盒会重用现有容器
BaseSandbox sandbox2 = new BaseSandbox(
    sandboxManager,
    userId,
    sessionId,
    SandboxType.BASE,
    300
);

// sandbox1 和 sandbox2 使用相同的容器实例
```

#### 使用 try-with-resources 自动释放

Java 的 `Sandbox` 类实现了 `AutoCloseable` 接口，可以使用 try-with-resources 自动释放：

```java
try (BaseSandbox sandbox = new BaseSandbox(
    sandboxManager,
    userId,
    sessionId,
    SandboxType.BASE,
    300
)) {
    // 使用沙盒
    // ...
} // 自动释放沙盒
```

#### 手动释放沙盒

您也可以手动释放沙盒：

```java
// 方式1：调用 release() 方法
sandbox.release();

// 方式2：通过 SandboxManager 释放
sandboxManager.releaseSandbox(SandboxType.BASE, userId, sessionId);

// 方式3：清理所有沙盒
sandboxManager.cleanupAllSandboxes();
```

### 服务生命周期

`SandboxManager` 实现了 `AutoCloseable` 接口，可以使用 try-with-resources 自动管理资源：

```java
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;

// 使用 try-with-resources 自动管理生命周期
try (SandboxManager sandboxManager = new SandboxManager()) {
    // 沙盒管理器在创建时自动初始化
    
    // 使用沙盒管理器
    // ...
    
} // 自动清理所有沙盒并关闭资源
```

或者手动管理生命周期：

```java
SandboxManager sandboxManager = new SandboxManager();

try {
    // 使用沙盒管理器
    // ...
} catch (Exception e) {
    e.printStackTrace();
} finally {
    // 手动清理所有沙盒
    sandboxManager.cleanupAllSandboxes();
    
    // 关闭沙盒管理器（会自动清理所有资源）
    sandboxManager.close();
}
```

### 环境管理器方法

`EnvironmentManager` 接口提供了以下方法：

```java
EnvironmentManager envManager = new DefaultEnvironmentManager();

// 获取沙盒管理器
SandboxManager sandboxManager = envManager.getSandboxManager();

// 获取环境变量
String value = envManager.getEnvironmentVariable("KEY");

// 设置环境变量
envManager.setEnvironmentVariable("KEY", "VALUE");

// 获取所有环境变量
Map<String, String> allVars = envManager.getAllEnvironmentVariables();

// 检查环境是否可用
boolean available = envManager.isEnvironmentAvailable();

// 初始化环境
CompletableFuture<Void> initFuture = envManager.initializeEnvironment();
initFuture.get(); // 等待初始化完成

// 获取环境信息
Map<String, Object> envInfo = envManager.getEnvironmentInfo();

// 清理环境
CompletableFuture<Void> cleanupFuture = envManager.cleanupEnvironment();
cleanupFuture.get(); // 等待清理完成
```

### 支持的沙盒类型

Java 版本支持以下沙盒类型，每种类型都有对应的实现类：

| 沙盒类型 | 实现类 | 说明 |
|---------|--------|------|
| `BASE` | `BaseSandbox` | 基础沙盒，提供基本的执行环境 |
| `BROWSER` | `BrowserSandbox` | 浏览器沙盒，支持浏览器自动化 |
| `FILESYSTEM` | `FilesystemSandbox` | 文件系统沙盒，提供文件操作能力 |
| `GUI` | `GuiSandbox` | GUI 沙盒，支持图形界面操作 |
| `TRAINING` | `TrainingSandbox` | 训练沙盒，用于训练场景 |
| `APPWORLD` | `APPWorldSandbox` | 应用世界沙盒，扩展自 TrainingSandbox |
| `BFCL` | `BFCLSandbox` | BFCL 沙盒，扩展自 TrainingSandbox |
| `WEBSHOP` | `WebShopSandbox` | 电商沙盒，扩展自 TrainingSandbox |

其他沙盒类型（如 `PYTHON`、`NODE`、`JAVA`）可以通过 `BaseSandbox` 使用相应的 `SandboxType` 来创建。

### 配置沙盒管理器

您可以使用 `ManagerConfig` 来自定义沙盒管理器的行为：

```java
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;

ManagerConfig config = ManagerConfig.builder()
    .poolSize(10)  // 容器池大小
    .portRange(new int[]{8000, 9000})  // 端口范围
    .redisConfig(redisConfig)	// 是否启用redis管理沙箱
  	.containerDeployment(clientConfig)	// 容器的部署介质（Docker、K8s、Agentrun）
    .build();

SandboxManager sandboxManager = new SandboxManager(config);
```
