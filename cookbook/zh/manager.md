# 管理器模块（Manager Module）

该模块提供了一个统一的接口，用于注册、启动和停止多个服务（例如会话历史服务、记忆服务和沙箱服务），并具有自动生命周期管理功能。

## 概览

- **ServiceManager**：服务管理的抽象基类，提供通用的生命周期和访问API
- **ContextManager**：专注于上下文相关服务（如 `SessionHistoryService` 与 `MemoryService`），继承自 `ServiceManager`
- **EnvironmentManager**：专注于环境/工具相关能力（通过 `SandboxManager`），是一个接口而非继承自 `ServiceManager`

### 服务接口要求

所有服务必须实现 `Service` 接口，该接口定义了三个核心方法：

```java
import io.agentscope.runtime.engine.shared.Service;
import java.util.concurrent.CompletableFuture;

public interface Service {
    CompletableFuture<Void> start();
    CompletableFuture<Void> stop();
    CompletableFuture<Boolean> health();
}
```

服务也可以通过继承 `ServiceWithLifecycleManager` 抽象类来实现，该类同时实现了 `Service` 接口和 `AutoCloseable` 接口：

```java
import io.agentscope.runtime.engine.shared.ServiceWithLifecycleManager;
import java.util.concurrent.CompletableFuture;

public class MockService extends ServiceWithLifecycleManager {
    private String name;
    private boolean started = false;
    private boolean stopped = false;

    public MockService(String name) {
        this.name = name;
    }

    @Override
    public CompletableFuture<Void> start() {
        return CompletableFuture.runAsync(() -> {
            started = true;
        });
    }

    @Override
    public CompletableFuture<Void> stop() {
        return CompletableFuture.runAsync(() -> {
            stopped = true;
        });
    }

    @Override
    public CompletableFuture<Boolean> health() {
        return CompletableFuture.completedFuture(started && !stopped);
    }
}
```

### 服务生命周期管理

`ServiceManager` 是一个抽象基类，子类需要实现 `registerDefaultServices()` 方法来注册默认服务：

```java
import io.agentscope.runtime.engine.shared.ServiceManager;
import io.agentscope.runtime.engine.shared.Service;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class MyServiceManager extends ServiceManager {
    @Override
    protected void registerDefaultServices() {
        // 注册默认服务
    }

    public static void main(String[] args) throws Exception {
        MyServiceManager manager = new MyServiceManager();
        
        // 注册服务类（通过类注册，注意：服务类必须有无参构造函数）
        manager.register(MockService.class, "service1");
        manager.register(MockService.class, "service2");
        
        // 或者注册服务实例（通过实例注册，推荐方式，可以传入构造参数）
        manager.registerService("service3", new MockService("service3"));
        
        // 启动所有服务
        manager.start().get();
        
        try {
            // 使用服务
            List<String> serviceNames = manager.listServices();
            Map<String, Boolean> health = manager.healthCheck().get();
            System.out.println("Services: " + serviceNames);
            System.out.println("Health: " + health);
        } finally {
            // 停止所有服务
            manager.stop().get();
            // 或者使用 try-with-resources（因为实现了 AutoCloseable）
            // manager.close();
        }
    }
}
```

### 服务访问方式

`ServiceManager` 提供了以下方法访问服务：

- 获取服务：`getService(String name)` - 如果服务不存在会抛出异常
- 获取服务（带默认值）：`getService(String name, Service defaultService)` - 如果服务不存在返回默认值
- 检查服务是否存在：`hasService(String name)` - 返回布尔值
- 列出所有服务名称：`listServices()` - 返回服务名称列表
- 获取所有服务：`getAllServices()` - 返回服务名称到服务实例的映射
- 健康检查：`healthCheck()` - 返回服务名称到健康状态的映射

```java
// 获取服务
Service service1 = manager.getService("service1");

// 获取服务（带默认值）
Service service2 = manager.getService("service2", new MockService("default"));

// 检查服务是否存在
if (manager.hasService("service1")) {
    Service service = manager.getService("service1");
}

// 列出所有服务
List<String> names = manager.listServices();

// 获取所有服务
Map<String, Service> allServices = manager.getAllServices();

// 健康检查
Map<String, Boolean> health = manager.healthCheck().get();
```

## 上下文管理器（Context Manager）

`ContextManager` 继承自 `ServiceManager`，默认装配上下文服务（`session`、`memory`），并提供上下文组合方法。

### 创建 ContextManager

可以通过多种方式创建 `ContextManager`：

```java
import io.agentscope.runtime.engine.memory.context.ContextManager;
import io.agentscope.runtime.engine.memory.context.ContextComposer;
import io.agentscope.runtime.engine.memory.persistence.memory.service.InMemoryMemoryService;
import io.agentscope.runtime.engine.memory.persistence.session.InMemorySessionHistoryService;
import io.agentscope.runtime.engine.memory.service.MemoryService;
import io.agentscope.runtime.engine.memory.service.SessionHistoryService;
import java.util.concurrent.CompletableFuture;

// 方式1：使用默认构造函数（使用内存实现）
ContextManager contextManager1 = new ContextManager();

// 方式2：指定服务实现
SessionHistoryService sessionHistoryService = new InMemorySessionHistoryService();
MemoryService memoryService = new InMemoryMemoryService();
ContextManager contextManager2 = new ContextManager(
    ContextComposer.class,
    sessionHistoryService,
    memoryService
);

// 方式3：使用 ContextManagerFactory（推荐）
import io.agentscope.runtime.engine.memory.context.ContextManagerFactory;

// 创建默认的上下文管理器
ContextManager defaultManager = ContextManagerFactory.createDefault();

// 创建自定义的上下文管理器
ContextManager customManager = ContextManagerFactory.createCustom(
    memoryService,
    sessionHistoryService
);
```

### 使用 ContextManager

```java
import io.agentscope.runtime.engine.memory.model.Message;
import io.agentscope.runtime.engine.memory.model.Session;
import java.util.ArrayList;
import java.util.List;

// 启动服务
contextManager.start().get();

try {
    // 获取服务
    SessionHistoryService sessionService = contextManager.getSessionHistoryService();
    MemoryService memoryService = contextManager.getMemoryService();
    
    // 或者通过服务名称获取
    SessionHistoryService sessionService2 = (SessionHistoryService) contextManager.getService("session");
    MemoryService memoryService2 = (MemoryService) contextManager.getService("memory");
    
    // 组合会话
    Session session = contextManager.composeSession("userId", "sessionId").get();
    
    // 组合上下文
    List<Message> requestInput = new ArrayList<>();
    contextManager.composeContext(session, requestInput).get();
    
    // 追加消息
    List<Message> eventOutput = new ArrayList<>();
    contextManager.append(session, eventOutput).get();
    
} finally {
    // 停止服务
    contextManager.stop().get();
}
```

更多细节见 {doc}`context_manager`。

## 环境管理器（Environment Manager）

`EnvironmentManager` 是一个接口，专注于环境/工具相关能力（通过 `SandboxManager`）。`DefaultEnvironmentManager` 是其默认实现。

### 创建 EnvironmentManager

```java
import io.agentscope.runtime.engine.service.EnvironmentManager;
import io.agentscope.runtime.engine.service.impl.DefaultEnvironmentManager;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

// 方式1：使用默认构造函数（会创建新的 SandboxManager）
EnvironmentManager envManager1 = new DefaultEnvironmentManager();

// 方式2：指定 SandboxManager
SandboxManager sandboxManager = new SandboxManager();
EnvironmentManager envManager2 = new DefaultEnvironmentManager(sandboxManager);
```

### 使用 EnvironmentManager

```java
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxType;
import io.agentscope.runtime.sandbox.manager.model.container.ContainerModel;

// 初始化环境
envManager.initializeEnvironment().get();

try {
    // 获取 SandboxManager（用于管理沙箱环境）
    SandboxManager sandboxManager = envManager.getSandboxManager();
    
    // 通过 SandboxManager 创建沙箱
    ContainerModel container = sandboxManager.createFromPool(
        SandboxType.BASE, 
        "userId", 
        "sessionId"
    );
    
    // 释放沙箱
    sandboxManager.releaseSandbox(SandboxType.BASE, "userId", "sessionId");
    
    // 获取环境变量
    String value = envManager.getEnvironmentVariable("KEY");
    
    // 设置环境变量
    envManager.setEnvironmentVariable("KEY", "value");
    
    // 获取所有环境变量
    Map<String, String> allVars = envManager.getAllEnvironmentVariables();
    
    // 检查环境是否可用
    boolean available = envManager.isEnvironmentAvailable();
    
    // 获取环境信息
    Map<String, Object> info = envManager.getEnvironmentInfo();
    
} finally {
    // 清理环境
    envManager.cleanupEnvironment().get();
}
```

更多细节见 {doc}`environment_manager`。

