# Session History Service

## Overview

**Session History Service** is used to manage user conversation sessions, providing agents with a structured way to handle conversation history and message storage in multi-turn conversations. Each session has a unique `session_id` and contains the complete message list (message objects `Message`) from the start of the session to the current point.

During agent operation, typical roles of Session History Service include:

- **Create Session**: Create a session when a user initiates a conversation for the first time.
- **Read Session**: Retrieve existing history records during conversation to ensure context continuity.
- **Append Messages**: Messages sent by agents or users are appended to the session storage.
- **List Sessions**: View all sessions for a user.
- **Delete Session**: Clean up a session according to business requirements.

Differences in Session History Service across different implementations are mainly reflected in **storage location**, **whether persistence is enabled**, **scalability**, and **production readiness**.

> In most cases, **it is not recommended to directly call underlying session history service classes** (such as `InMemorySessionHistoryService`, `RedisSessionHistoryService`, etc.) in business code.
> It is more recommended to use them through **adapters**, which enables:
>
> - Shield underlying implementation details, switch storage types without business awareness
> - Unified lifecycle management by Runner/Engine
> - Ensure cross-framework reuse and decoupling

## Using Adapter in AgentScope

In the **AgentScope** framework, we use the `MemoryAdapter` adapter to bind the underlying session history service to the agent's `Memory` module:

```java
import io.agentscope.runtime.adapters.agentscope.memory.MemoryAdapter;
import io.agentscope.runtime.engine.services.memory.persistence.session.InMemorySessionHistoryService;
import io.agentscope.runtime.engine.services.memory.service.SessionHistoryService;

public class Main {
    public static void main(String[] args) {
        // Choose backend implementation (this example uses InMemory for local testing)
        SessionHistoryService sessionHistoryService = new InMemorySessionHistoryService();
        MemoryAdapter memory = null;

        // Wrap with adapter, bind to Memory module
        memory = new MemoryAdapter(
                sessionHistoryService,
                "User1",
                "TestSession"
        );

        // After this, you can directly use memory in the Agent to access session history
    }
}
```

## Optional Backend Implementation Types

Although you don't need to care about underlying calls when using through adapters, you need to understand the characteristics of available implementation types for configuration and selection:

| Service Type                            | Import Path                                                     | Storage Location                  | Persistence          | Production Ready | Characteristics & Pros/Cons                            | Use Cases                       |
| ----------------------------------- | ------------------------------------------------------------ | ------------------------- | --------------- | ---------- | ---------------------------------------- | ------------------------------ |
| **InMemorySessionHistoryService**   | `import io.agentscope.runtime.engine.services.memory.persistence.session.InMemorySessionHistoryService` | Process Memory                  | ❌ No            | ❌          | Fast, no dependencies; lost on exit                 | Development/Testing, Unit Tests             |
| **RedisSessionHistoryService**      | `import io.agentscope.runtime.engine.services.memory.persistence.session.RedisSessionHistoryService` | Redis In-Memory Database          | ✅ Yes (RDB/AOF) | ✅          | Fast, supports clustering, cross-process sharing; requires Redis operations | High-performance Production Deployment, Distributed Session Sharing |
| **TableStoreSessionHistoryService** | `import io.agentscope.runtime.engine.services.memory.persistence.session.TableStoreSessionHistoryService` | Alibaba Cloud Tablestore Cloud Database | ✅ Yes            | ✅          | Massive storage, high availability, complex index queries; requires cloud services | Enterprise Production, Long-term History Archive       |

## Methods to Switch Different Implementations

The benefit of adapters is that you only need to replace the `service` instance to switch storage backends without modifying business logic:

```java
RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
// Configure redisTemplate connection factory and other properties

SessionHistoryService sessionHistoryService = new RedisSessionHistoryService(redisTemplate);
MemoryAdapter memory = null;
memory = new MemoryAdapter(
        sessionHistoryService,
        "User1",
        "TestSession"
);

// Agent code doesn't need to change
```

For example, switching from `InMemory` to `Tablestore`:

```java
SyncClient client = new SyncClient(
        "https://your-instance.cn-region.ots.aliyuncs.com",
        "your-access-key-id",
        "your-access-key-secret",
        "your-instance-name"
);

TableStoreSessionHistoryService sessionHistoryService = new TableStoreSessionHistoryService(client);
MemoryAdapter memory = null;
memory = new MemoryAdapter(
        sessionHistoryService,
        "User1",
        "TestSession"
);
```

## Selection Recommendations

- **Development/Testing / Quick Prototyping**: `InMemorySessionHistoryService`
- **High-Performance Shared Sessions in Production**: `RedisSessionHistoryService` (can be combined with Redis clustering and persistence mechanisms)
- **Enterprise Production & Massive Data Storage**: `TablestoreSessionHistoryService` (requires Alibaba Cloud account and resources)

## Summary

- Session History Service is the core component for agents to maintain context memory
- It is recommended to use through **adapters** (such as `MemoryAdapter`) to decouple from business logic
- When selecting, choose backend implementations based on data volume, persistence requirements, and operational conditions
- `adapter` makes switching storage backends very simple without modifying agent logic
