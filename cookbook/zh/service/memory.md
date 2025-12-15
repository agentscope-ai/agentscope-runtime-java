# 记忆服务

## 概述

**记忆服务**（Memory Service）用于管理智能体的**长期记忆**，将用户对话和其他关联信息进行存储、检索、管理，以便在后续交互中进行知识引用、个性化回应或者任务跟踪。

与**会话历史服务**的区别在于：

- 会话历史服务主要保存**短期上下文**（最近几轮对话）
- 记忆服务则可以保存**长期、跨会话**的信息，例如用户的偏好、长期任务计划、知识库等

记忆服务的核心接口定义了四种重要功能：

- **新增记忆**：将一批消息或信息存入记忆存储
- **搜索记忆**：根据当前用户的查询或上下文内容筛选相关信息
- **列出记忆**：支持分页遍历某用户的所有记忆内容
- **删除记忆**：清理指定会话或用户的全部记忆

类似会话历史服务，记忆服务也有多种后端实现，支持不同的存储方式和生产级需求。

> 推荐总是通过**适配器（adapter）**使用记忆服务，而不是在业务逻辑中直接调用底层实现类。
> 这样可以：
>
> - 无感知切换存储类型
> - 生命周期由框架统一管理
> - 与业务逻辑解耦

## 在 AgentScope 中使用 Adapter

在 **AgentScope** 框架中，可以通过  `LongTermMemoryAdapter`  或其他记忆适配器，将底层  `MemoryService`  封装为智能体的 **LongTermMemory** 模块进行调用：

```java
import io.agentscope.runtime.adapters.agentscope.memory.LongTermMemoryAdapter;
import io.agentscope.runtime.engine.services.memory.persistence.memory.service.InMemoryMemoryService;
import io.agentscope.runtime.engine.services.memory.service.MemoryService;

public class Main {
    public static void main(String[] args) {
//        选择后端实现（此例为 InMemory，方便本地测试）
        MemoryService memoryService = new InMemoryMemoryService();
        LongTermMemoryAdapter longTermMemory = null;
      
//        用 adapter 包装，绑定到 LongTermMemory 模块
        longTermMemory = new LongTermMemoryAdapter(
                memoryService,
                "User1",
                "Test Session"
        );
      
//        之后在 Agent 内即可直接使用 long_term_memory 存取跨会话的长期记忆
    }
}
```

## 可选的后端实现类型

虽然通过适配器使用时无需关心底层调用，但为了配置和选型，需要了解可用实现类型的特点：

| 服务类型                    | 导入路径                                                     | 存储位置          | 持久化          | 生产可用性 | 特点 & 优缺点                               | 适用场景                       |
| --------------------------- | ------------------------------------------------------------ | ----------------- | --------------- | ---------- | ------------------------------------------- | ------------------------------ |
| **InMemoryMemoryService**   | `import io.agentscope.runtime.engine.services.memory.persistence.memory.service.InMemoryMemoryService` | 进程内存          | ❌ 否            | ❌          | 高速、无依赖；进程退出即丢失                | 开发调试、单元测试             |
| **RedisMemoryService**      | `import io.agentscope.runtime.engine.services.memory.persistence.memory.service.RedisMemoryService` | Redis 内存数据库  | ✅ 是（RDB/AOF） | ✅          | 高性能、跨进程共享、可集群；需运维 Redis    | 高性能生产部署、分布式共享记忆 |
| **TablestoreMemoryService** | `import io.agentscope.runtime.engine.services.memory.persistence.memory.service.TableStoreMemoryService` | 阿里云 Tablestore | ✅ 是            | ✅          | 海量存储、全文/向量检索、高可用；需云资源   | 企业级生产、长期知识存档       |
| **Mem0MemoryService**       | `import io.agentscope.runtime.engine.services.memory.persistence.memory.service.Mem0MemoryService` | mem0.ai 云服务    | ✅ 是            | ✅          | 内置 AI 语义记忆，外部 API 调用；需 API key | 语义化长期记忆、智能化匹配     |

## 切换不同实现的方法

适配器让切换存储后端非常简单，只需替换 `service` 实例即可。

### 示例：切换为 Redis 存储

```java
RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
// 配置redisTemplate的连接工厂等属性
MemoryService memoryService = new RedisMemoryService(redisTemplate);

LongTermMemoryAdapter longTermMemory = null;
longTermMemory = new LongTermMemoryAdapter(
        memoryService,
        "User1",
        "Test Session"
);
```

### 示例：切换为 Tablestore 存储

```java
SyncClient client = new SyncClient(
        "https://your-instance.cn-region.ots.aliyuncs.com",
        "your-access-key-id",
        "your-access-key-secret",
        "your-instance-name"
);
MemoryService memoryService = new TableStoreMemoryService(client);

LongTermMemoryAdapter longTermMemory = null;
longTermMemory = new LongTermMemoryAdapter(
        memoryService,
        "User1",
        "Test Session"
);
```

## 选型建议

- **开发调试 / 原型验证** → `InMemoryMemoryService`
- **生产环境高性能共享记忆** → `RedisMemoryService`
- **企业级生产 & 海量长期存储** → `TablestoreMemoryService`
- **需要语义查询的智能记忆** → `Mem0MemoryService`

------

## 小结

- 记忆服务是**跨会话、长期知识存储**的核心组件
- 使用 `LongTermMemoryAdapter` 适配器，可与业务逻辑解耦
- 多种后端实现可按场景灵活选择、随时切换
