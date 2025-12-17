# Memory Service

## Overview

**Memory Service** is used to manage agents' **long-term memory**, storing, retrieving, and managing user conversations and other related information for knowledge reference, personalized responses, or task tracking in subsequent interactions.

The difference from **Session History Service** is:

- Session History Service mainly saves **short-term context** (recent few rounds of conversation)
- Memory Service can save **long-term, cross-session** information, such as user preferences, long-term task plans, knowledge bases, etc.

The core interface of Memory Service defines four important functions:

- **Add Memory**: Store a batch of messages or information into memory storage
- **Search Memory**: Filter relevant information based on the current user's query or context content
- **List Memory**: Support paginated traversal of all memory content for a user
- **Delete Memory**: Clean up all memory for a specified session or user

Similar to Session History Service, Memory Service also has multiple backend implementations, supporting different storage methods and production-grade requirements.

> It is recommended to always use Memory Service through **adapters** rather than directly calling underlying implementation classes in business logic.
> This enables:
>
> - Seamless switching of storage types without awareness
> - Lifecycle managed uniformly by the framework
> - Decoupling from business logic

## Using Adapter in AgentScope

In the **AgentScope** framework, you can encapsulate the underlying `MemoryService` as the agent's **LongTermMemory** module through `LongTermMemoryAdapter` or other memory adapters:

```java
import io.agentscope.runtime.adapters.agentscope.memory.LongTermMemoryAdapter;
import io.agentscope.runtime.engine.services.memory.persistence.memory.service.InMemoryMemoryService;
import io.agentscope.runtime.engine.services.memory.service.MemoryService;

public class Main {
    public static void main(String[] args) {
        // Choose backend implementation (this example uses InMemory for local testing)
        MemoryService memoryService = new InMemoryMemoryService();
        LongTermMemoryAdapter longTermMemory = null;
      
        // Wrap with adapter, bind to LongTermMemory module
        longTermMemory = new LongTermMemoryAdapter(
                memoryService,
                "User1",
                "Test Session"
        );
      
        // After this, you can directly use long_term_memory in the Agent to access cross-session long-term memory
    }
}
```

## Optional Backend Implementation Types

Although you don't need to care about underlying calls when using through adapters, you need to understand the characteristics of available implementation types for configuration and selection:

| Service Type                    | Import Path                                                     | Storage Location          | Persistence          | Production Ready | Characteristics & Pros/Cons                               | Use Cases                       |
| --------------------------- | ------------------------------------------------------------ | ----------------- | --------------- | ---------- | ------------------------------------------- | ------------------------------ |
| **InMemoryMemoryService**   | `import io.agentscope.runtime.engine.services.memory.persistence.memory.service.InMemoryMemoryService` | Process Memory          | ❌ No            | ❌          | Fast, no dependencies; lost on process exit                | Development/Testing, Unit Tests             |
| **RedisMemoryService**      | `import io.agentscope.runtime.engine.services.memory.persistence.memory.service.RedisMemoryService` | Redis In-Memory Database  | ✅ Yes (RDB/AOF) | ✅          | High performance, cross-process sharing, clusterable; requires Redis operations    | High-performance Production Deployment, Distributed Shared Memory |
| **TablestoreMemoryService** | `import io.agentscope.runtime.engine.services.memory.persistence.memory.service.TableStoreMemoryService` | Alibaba Cloud Tablestore | ✅ Yes            | ✅          | Massive storage, full-text/vector search, high availability; requires cloud resources   | Enterprise Production, Long-term Knowledge Archive       |
| **Mem0MemoryService**       | `import io.agentscope.runtime.engine.services.memory.persistence.memory.service.Mem0MemoryService` | mem0.ai Cloud Service    | ✅ Yes            | ✅          | Built-in AI semantic memory, external API calls; requires API key | Semantic Long-term Memory, Intelligent Matching     |

## Methods to Switch Different Implementations

Adapters make switching storage backends very simple; you only need to replace the `service` instance.

### Example: Switch to Redis Storage

```java
RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
// Configure redisTemplate connection factory and other properties
MemoryService memoryService = new RedisMemoryService(redisTemplate);

LongTermMemoryAdapter longTermMemory = null;
longTermMemory = new LongTermMemoryAdapter(
        memoryService,
        "User1",
        "Test Session"
);
```

### Example: Switch to Tablestore Storage

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

## Selection Recommendations

- **Development/Testing / Prototype Validation** → `InMemoryMemoryService`
- **Production High-Performance Shared Memory** → `RedisMemoryService`
- **Enterprise Production & Massive Long-term Storage** → `TablestoreMemoryService`
- **Intelligent Memory with Semantic Queries** → `Mem0MemoryService`

------

## Summary

- Memory Service is the core component for **cross-session, long-term knowledge storage**
- Use the `LongTermMemoryAdapter` adapter to decouple from business logic
- Multiple backend implementations can be flexibly selected and switched as needed
