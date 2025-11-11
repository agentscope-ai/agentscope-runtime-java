# Context Manager

## Overview

The context manager provides a convenient way to manage context lifecycle. It includes:

- **A set of context services**: Session history service (SessionHistoryService) and memory service (MemoryService)
- **ContextComposer**: A composer for orchestrating history and memory updates

## Service Architecture Overview

The context manager supports two core service types, each with multiple built-in implementations:

### SessionHistoryService

Used to maintain complete conversation history at session-level granularity without compressing memory.

**Interface Methods:**

- `createSession`: Create a new session
- `getSession`: Get a session
- `deleteSession`: Delete a session
- `listSessions`: List all sessions
- `appendMessage`: Add messages to history

**Built-in Implementations:**

| Implementation Class | Storage Method | Use Cases | Description |
|--------|----------|----------|------|
| `InMemorySessionHistoryService` | In-memory (Map) | Development, testing, single-machine deployment | Data stored in memory, lost after service restart |
| `RedisSessionHistoryService` | Redis | Production environment, distributed deployment | Data persisted to Redis, supports multi-instance sharing |
| `TableStoreSessionHistoryService` | Alibaba Cloud TableStore | Production environment, large-scale deployment | Uses Alibaba Cloud TableStore, supports massive data storage |

### MemoryService

Used to store and retrieve long-term memories. In agents, memories store end-user conversation information from previous sessions, which can be queried and used across sessions.

**Interface Methods:**
- `addMemory`: Add memory to the memory service
- `searchMemory`: Search memory from the memory service
- `deleteMemory`: Delete memory from the memory service
- `listMemory`: List all memories
- `getAllUsers`: Get all user lists

**Built-in Implementations:**

| Implementation Class | Storage Method | Use Cases | Description |
|--------|----------|----------|------|
| `InMemoryMemoryService` | In-memory (Map) | Development, testing, single-machine deployment | Simple keyword search, data stored in memory |
| `RedisMemoryService` | Redis | Production environment, distributed deployment | Data persisted to Redis, supports multi-instance sharing |
| `TableStoreMemoryService` | Alibaba Cloud TableStore | Production environment, large-scale deployment | Uses Alibaba Cloud TableStore KnowledgeStore, supports vector search |
| `Mem0MemoryService` | Mem0 API | Production environment, requires advanced memory management | Uses Mem0 cloud service, provides advanced memory management and search capabilities |

---

## SessionHistoryService

### Interface Definition

`SessionHistoryService` is an interface for managing session history, used to maintain complete memory at session-level granularity without compressing memory.

```java
import io.agentscope.runtime.engine.memory.service.SessionHistoryService;

public interface SessionHistoryService extends Service {
    CompletableFuture<Session> createSession(String userId, Optional<String> sessionId);
    CompletableFuture<Optional<Session>> getSession(String userId, String sessionId);
    CompletableFuture<Void> deleteSession(String userId, String sessionId);
    CompletableFuture<List<Session>> listSessions(String userId);
    CompletableFuture<Void> appendMessage(Session session, List<Message> messages);
}
```

### Implementation 1: InMemorySessionHistoryService

**Features:**
- Data stored in `ConcurrentHashMap` in memory
- Thread-safe
- Data lost after service restart
- Suitable for development, testing, and small-scale deployment

**Usage Example:**

```java
import io.agentscope.runtime.engine.memory.persistence.session.InMemorySessionHistoryService;
import io.agentscope.runtime.engine.memory.service.SessionHistoryService;
import io.agentscope.runtime.engine.memory.model.Session;
import java.util.Optional;

// Create service instance
SessionHistoryService sessionHistoryService = new InMemorySessionHistoryService();

// Start service
sessionHistoryService.start().get();

try {
    // Create session
    Session session = sessionHistoryService.createSession("userId", Optional.empty()).get();
    
    // Get session
    Optional<Session> retrievedSession = sessionHistoryService.getSession("userId", session.getId()).get();
    
    // Use service...
} finally {
    // Stop service
    sessionHistoryService.stop().get();
}
```

**Implementation Details:**
- Data structure: `Map<String, Map<String, Session>>` (user_id -> session_id -> Session)
- If session ID is not provided, UUID is automatically generated
- Empty or whitespace-only session IDs are replaced with auto-generated IDs
- Uses ConcurrentHashMap to ensure thread safety

### Implementation 2: RedisSessionHistoryService

**Features:**
- Data persisted to Redis
- Supports multi-instance data sharing
- Suitable for production environment and distributed deployment
- Requires Redis connection configuration

**Usage Example:**

```java
import io.agentscope.runtime.engine.memory.persistence.session.RedisSessionHistoryService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

// Configure Redis connection
RedisConnectionFactory connectionFactory = new LettuceConnectionFactory("localhost", 6379);
connectionFactory.afterPropertiesSet();

RedisTemplate<String, String> redisTemplate = new StringRedisTemplate(connectionFactory);
redisTemplate.afterPropertiesSet();

// Create service instance
SessionHistoryService sessionHistoryService = new RedisSessionHistoryService(redisTemplate);

// Start service
sessionHistoryService.start().get();

try {
    // Use service...
    Session session = sessionHistoryService.createSession("userId", Optional.empty()).get();
} finally {
    sessionHistoryService.stop().get();
}
```

**Implementation Details:**
- Uses Redis Hash to store session data
- Key format: `session:{userId}:{sessionId}`
- Uses Redis Set to maintain user session index: `session_index:{userId}`
- Supports JSON serialization/deserialization

### Implementation 3: TableStoreSessionHistoryService

**Features:**
- Uses Alibaba Cloud TableStore
- Supports massive data storage
- Suitable for production environment and large-scale deployment
- Requires Alibaba Cloud TableStore client configuration

**Usage Example:**

```java
import io.agentscope.runtime.engine.memory.persistence.session.TableStoreSessionHistoryService;
import com.alicloud.openservices.tablestore.SyncClient;
import com.aliyun.openservices.tablestore.agent.util.Pair;
import com.aliyun.openservices.tablestore.agent.model.MetaType;

// Configure TableStore client
String endpoint = "https://your-instance.cn-hangzhou.ots.aliyuncs.com";
String accessKeyId = "your-access-key-id";
String accessKeySecret = "your-access-key-secret";
String instanceName = "your-instance-name";

SyncClient client = new SyncClient(endpoint, accessKeyId, accessKeySecret, instanceName);

// Create service instance (using default table names)
SessionHistoryService sessionHistoryService = new TableStoreSessionHistoryService(client);

// Or use custom table names and indexes
String sessionTableName = "custom_session_table";
String messageTableName = "custom_message_table";
String sessionSecondaryIndexName = "custom_session_index";
String messageSecondaryIndexName = "custom_message_index";
List<Pair<String, MetaType>> sessionSecondaryIndexMeta = Collections.emptyList();

SessionHistoryService customService = new TableStoreSessionHistoryService(
    client,
    sessionTableName,
    messageTableName,
    sessionSecondaryIndexName,
    messageSecondaryIndexName,
    sessionSecondaryIndexMeta
);

// Start service
sessionHistoryService.start().get();

try {
    // Use service...
    Session session = sessionHistoryService.createSession("userId", Optional.empty()).get();
} finally {
    sessionHistoryService.stop().get();
}
```

**Implementation Details:**
- Uses TableStore MemoryStore API
- Default table names: `agentscope_runtime_session` and `agentscope_runtime_message`
- Supports secondary indexes to accelerate queries
- Automatically handles table creation and initialization

### Session Object Structure

Each session is represented by a `Session` object:

```java
import io.agentscope.runtime.engine.memory.model.Session;
import io.agentscope.runtime.engine.memory.model.Message;
import io.agentscope.runtime.engine.memory.model.MessageContent;
import io.agentscope.runtime.engine.memory.model.MessageType;
import java.util.ArrayList;
import java.util.List;

// Create session object
List<MessageContent> userContent = new ArrayList<>();
userContent.add(new MessageContent("text", "Hello"));

List<MessageContent> assistantContent = new ArrayList<>();
assistantContent.add(new MessageContent("text", "Hi there!"));

List<Message> messages = new ArrayList<>();
messages.add(new Message(MessageType.USER, userContent));
messages.add(new Message(MessageType.ASSISTANT, assistantContent));

Session session = new Session("session_123", "user_456", messages);

System.out.println("Session ID: " + session.getId());
System.out.println("User ID: " + session.getUserId());
System.out.println("Message count: " + session.getMessages().size());
```

### Core Function Examples

#### Create Session

```java
import io.agentscope.runtime.engine.memory.persistence.session.InMemorySessionHistoryService;
import io.agentscope.runtime.engine.memory.model.Session;
import java.util.Optional;

InMemorySessionHistoryService sessionHistoryService = new InMemorySessionHistoryService();
sessionHistoryService.start().get();

try {
    String userId = "test_user";
    
    // Create session with auto-generated ID
    Session session = sessionHistoryService.createSession(userId, Optional.empty()).get();
    System.out.println("Created session: " + session.getId());
    
    // Create session with custom ID
    Session customSession = sessionHistoryService.createSession(
        userId,
        Optional.of("my_custom_session_id")
    ).get();
    System.out.println("Custom session ID: " + customSession.getId());
} finally {
    sessionHistoryService.stop().get();
}
```

#### Retrieve Session

```java
try {
    String userId = "u1";
    String sessionId = "s1";
    
    // Retrieve existing session
    Optional<Session> retrievedSessionOpt = sessionHistoryService.getSession(userId, sessionId).get();
    if (retrievedSessionOpt.isPresent()) {
        Session retrievedSession = retrievedSessionOpt.get();
        System.out.println("Session found: " + retrievedSession.getId());
    } else {
        System.out.println("Session not found");
    }
} finally {
    sessionHistoryService.stop().get();
}
```

#### List Sessions

```java
try {
    String userId = "u_list";
    
    // Create multiple sessions
    Session session1 = sessionHistoryService.createSession(userId, Optional.empty()).get();
    Session session2 = sessionHistoryService.createSession(userId, Optional.empty()).get();
    
    // List all sessions
    List<Session> listedSessions = sessionHistoryService.listSessions(userId).get();
    System.out.println("Total sessions: " + listedSessions.size());
    
    for (Session s : listedSessions) {
        System.out.println("Session ID: " + s.getId());
    }
} finally {
    sessionHistoryService.stop().get();
}
```

#### Add Messages

```java
import io.agentscope.runtime.engine.memory.model.Message;
import io.agentscope.runtime.engine.memory.model.MessageContent;
import io.agentscope.runtime.engine.memory.model.MessageType;
import java.util.ArrayList;
import java.util.List;

try {
    String userId = "u_append";
    Session session = sessionHistoryService.createSession(userId, Optional.empty()).get();
    
    // Add single message
    List<MessageContent> userContent = new ArrayList<>();
    userContent.add(new MessageContent("text", "Hello, world!"));
    Message message1 = new Message(MessageType.USER, userContent);
    
    List<Message> messages = new ArrayList<>();
    messages.add(message1);
    sessionHistoryService.appendMessage(session, messages).get();
    
    // Add multiple messages at once
    List<MessageContent> userContent2 = new ArrayList<>();
    userContent2.add(new MessageContent("text", "How are you?"));
    Message message2 = new Message(MessageType.USER, userContent2);
    
    List<MessageContent> assistantContent = new ArrayList<>();
    assistantContent.add(new MessageContent("text", "I am fine, thank you."));
    Message message3 = new Message(MessageType.ASSISTANT, assistantContent);
    
    List<Message> moreMessages = new ArrayList<>();
    moreMessages.add(message2);
    moreMessages.add(message3);
    sessionHistoryService.appendMessage(session, moreMessages).get();
    
    // Verify messages were added
    Optional<Session> updatedSession = sessionHistoryService.getSession(userId, session.getId()).get();
    System.out.println("Total messages: " + updatedSession.get().getMessages().size());
} finally {
    sessionHistoryService.stop().get();
}
```

#### Delete Session

```java
try {
    String userId = "u_delete";
    Session sessionToDelete = sessionHistoryService.createSession(userId, Optional.empty()).get();
    String sessionId = sessionToDelete.getId();
    
    // Delete session
    sessionHistoryService.deleteSession(userId, sessionId).get();
    System.out.println("Session deleted: " + sessionId);
} finally {
    sessionHistoryService.stop().get();
}
```

---

## MemoryService

### Interface Definition

`MemoryService` is an interface for managing memories. In agents, memories store end-user conversation information from previous sessions, which can be queried and used across sessions.

```java
import io.agentscope.runtime.engine.memory.service.MemoryService;

public interface MemoryService extends Service {
    CompletableFuture<Void> addMemory(String userId, List<Message> messages, Optional<String> sessionId);
    CompletableFuture<List<Message>> searchMemory(String userId, List<Message> messages, Optional<Map<String, Object>> filters);
    CompletableFuture<Void> deleteMemory(String userId, Optional<String> sessionId);
    CompletableFuture<List<Message>> listMemory(String userId, Optional<Map<String, Object>> filters);
    CompletableFuture<List<String>> getAllUsers();
}
```

### Implementation 1: InMemoryMemoryService

**Features:**
- Data stored in `ConcurrentHashMap` in memory
- Simple keyword search (based on text matching)
- Thread-safe
- Data lost after service restart
- Suitable for development, testing, and small-scale deployment

**Usage Example:**

```java
import io.agentscope.runtime.engine.memory.persistence.memory.service.InMemoryMemoryService;
import io.agentscope.runtime.engine.memory.service.MemoryService;
import io.agentscope.runtime.engine.memory.model.Message;
import io.agentscope.runtime.engine.memory.model.MessageContent;
import io.agentscope.runtime.engine.memory.model.MessageType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Create service instance
MemoryService memoryService = new InMemoryMemoryService();

// Start service
memoryService.start().get();

try {
    String userId = "user1";
    
    // Add memory
    List<MessageContent> content = new ArrayList<>();
    content.add(new MessageContent("text", "My name is Alice"));
    Message message = new Message(MessageType.USER, content);
    
    List<Message> messages = new ArrayList<>();
    messages.add(message);
    memoryService.addMemory(userId, messages, Optional.empty()).get();
    
    // Search memory
    List<MessageContent> queryContent = new ArrayList<>();
    queryContent.add(new MessageContent("text", "name"));
    Message queryMessage = new Message(MessageType.USER, queryContent);
    
    List<Message> searchQuery = new ArrayList<>();
    searchQuery.add(queryMessage);
    
    Map<String, Object> filters = new HashMap<>();
    filters.put("top_k", 5);
    List<Message> retrieved = memoryService.searchMemory(userId, searchQuery, Optional.of(filters)).get();
    System.out.println("Retrieved " + retrieved.size() + " messages");
} finally {
    memoryService.stop().get();
}
```

**Implementation Details:**
- Data structure: `Map<String, Map<String, List<Message>>>` (user_id -> session_id -> messages)
- Uses default session ID ("default_session") when session is not specified
- Keyword-based search is case-insensitive
- Messages stored chronologically within each session
- Uses ConcurrentHashMap to ensure thread safety

### Implementation 2: RedisMemoryService

**Features:**
- Data persisted to Redis
- Supports multi-instance data sharing
- Simple keyword search
- Suitable for production environment and distributed deployment
- Requires Redis connection configuration

**Usage Example:**

```java
import io.agentscope.runtime.engine.memory.persistence.memory.service.RedisMemoryService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

// Configure Redis connection
RedisConnectionFactory connectionFactory = new LettuceConnectionFactory("localhost", 6379);
connectionFactory.afterPropertiesSet();

RedisTemplate<String, String> redisTemplate = new StringRedisTemplate(connectionFactory);
redisTemplate.afterPropertiesSet();

// Create service instance
MemoryService memoryService = new RedisMemoryService(redisTemplate);

// Start service
memoryService.start().get();

try {
    // Use service...
    String userId = "user1";
    List<Message> messages = new ArrayList<>();
    memoryService.addMemory(userId, messages, Optional.empty()).get();
} finally {
    memoryService.stop().get();
}
```

**Implementation Details:**
- Uses Redis Hash to store memory data
- Key format: `user_memory:{userId}`
- Uses JSON serialization/deserialization for messages
- Supports health checks (via Redis PING command)

### Implementation 3: TableStoreMemoryService

**Features:**
- Uses Alibaba Cloud TableStore KnowledgeStore
- Supports vector search and semantic search
- Supports massive data storage
- Suitable for production environment and large-scale deployment
- Requires Alibaba Cloud TableStore client configuration

**Usage Example:**

```java
import io.agentscope.runtime.engine.memory.persistence.memory.service.TableStoreMemoryService;
import com.alicloud.openservices.tablestore.SyncClient;

// Configure TableStore client
String endpoint = "https://your-instance.cn-hangzhou.ots.aliyuncs.com";
String accessKeyId = "your-access-key-id";
String accessKeySecret = "your-access-key-secret";
String instanceName = "your-instance-name";

SyncClient client = new SyncClient(endpoint, accessKeyId, accessKeySecret, instanceName);

// Create service instance (using default table name and index)
MemoryService memoryService = new TableStoreMemoryService(client);

// Or use custom table name and index
String tableName = "custom_memory_table";
String searchIndexName = "custom_search_index";
MemoryService customService = new TableStoreMemoryService(client, tableName, searchIndexName);

// Start service
memoryService.start().get();

try {
    // Use service...
    String userId = "user1";
    List<Message> messages = new ArrayList<>();
    memoryService.addMemory(userId, messages, Optional.empty()).get();
} finally {
    memoryService.stop().get();
}
```

**Implementation Details:**
- Uses TableStore KnowledgeStore API
- Default table name: `agentscope_runtime_memory`
- Default search index: `agentscope_runtime_knowledge_search_index`
- Supports vector embeddings and semantic search
- Automatically handles table and index creation

### Implementation 4: Mem0MemoryService

**Features:**
- Uses Mem0 cloud service API
- Provides advanced memory management and search capabilities
- Supports vector search and semantic understanding
- Suitable for production environment requiring advanced memory features
- Requires Mem0 API key

**Usage Example:**

```java
import io.agentscope.runtime.engine.memory.persistence.memory.service.Mem0MemoryService;

// Create service instance (requires API key)
String apiKey = System.getenv("MEM0_API_KEY");
MemoryService memoryService = new Mem0MemoryService(apiKey);

// Or specify organization ID and project ID
String orgId = "your-org-id";
String projectId = "your-project-id";
MemoryService memoryServiceWithIds = new Mem0MemoryService(apiKey, orgId, projectId);

// Start service
memoryService.start().get();

try {
    // Use service...
    String userId = "user1";
    List<Message> messages = new ArrayList<>();
    memoryService.addMemory(userId, messages, Optional.empty()).get();
    
    // Search memory
    List<Message> searchQuery = new ArrayList<>();
    Map<String, Object> filters = new HashMap<>();
    filters.put("top_k", 5);
    List<Message> retrieved = memoryService.searchMemory(userId, searchQuery, Optional.of(filters)).get();
} finally {
    memoryService.stop().get();
}
```

**Implementation Details:**
- Uses Mem0 HTTP API (https://api.mem0.ai)
- Supports Mem0 v1 and v2 APIs
- Automatically handles HTTP requests and responses
- Supports organization-level and project-level memory management
- Requires valid Mem0 API key

### Core Function Examples

#### Add Memory

```java
import io.agentscope.runtime.engine.memory.persistence.memory.service.InMemoryMemoryService;
import io.agentscope.runtime.engine.memory.model.Message;
import io.agentscope.runtime.engine.memory.model.MessageContent;
import io.agentscope.runtime.engine.memory.model.MessageType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

InMemoryMemoryService memoryService = new InMemoryMemoryService();
memoryService.start().get();

try {
    String userId = "user1";
    
    // Add memory without session ID
    List<MessageContent> content = new ArrayList<>();
    content.add(new MessageContent("text", "My favorite color is blue"));
    Message message = new Message(MessageType.USER, content);
    
    List<Message> messages = new ArrayList<>();
    messages.add(message);
    memoryService.addMemory(userId, messages, Optional.empty()).get();
    
    // Add memory with session ID
    memoryService.addMemory(userId, messages, Optional.of("session1")).get();
} finally {
    memoryService.stop().get();
}
```

#### Search Memory

```java
try {
    String userId = "user1";
    
    // Create search query
    List<MessageContent> queryContent = new ArrayList<>();
    queryContent.add(new MessageContent("text", "favorite color"));
    Message queryMessage = new Message(MessageType.USER, queryContent);
    
    List<Message> searchQuery = new ArrayList<>();
    searchQuery.add(queryMessage);
    
    // Search memory (can pass filters such as top_k)
    Map<String, Object> filters = new HashMap<>();
    filters.put("top_k", 5);
    List<Message> retrieved = memoryService.searchMemory(userId, searchQuery, Optional.of(filters)).get();
    
    System.out.println("Retrieved " + retrieved.size() + " messages");
    for (Message msg : retrieved) {
        System.out.println("Message: " + msg.getContent().get(0).getText());
    }
} finally {
    memoryService.stop().get();
}
```

#### List Memory

```java
try {
    String userId = "user1";
    
    // List memory (with pagination)
    Map<String, Object> filters = new HashMap<>();
    filters.put("page_size", 10);
    filters.put("page_num", 1);
    List<Message> memoryList = memoryService.listMemory(userId, Optional.of(filters)).get();
    
    System.out.println("Listed " + memoryList.size() + " memory items");
} finally {
    memoryService.stop().get();
}
```

#### Delete Memory

```java
try {
    String userId = "user1";
    String sessionId = "session1";
    
    // Delete memory for specific session
    memoryService.deleteMemory(userId, Optional.of(sessionId)).get();
    
    // Delete all memory for user
    memoryService.deleteMemory(userId, Optional.empty()).get();
} finally {
    memoryService.stop().get();
}
```

#### Get All Users

```java
try {
    List<String> allUsers = memoryService.getAllUsers().get();
    System.out.println("Total users: " + allUsers.size());
    for (String userId : allUsers) {
        System.out.println("User: " + userId);
    }
} finally {
    memoryService.stop().get();
}
```

---

## ContextManager

### Overview

`ContextManager` extends `ServiceManager` and is used to manage the lifecycle of session history services and memory services, and provides context composition functionality.

### Creating ContextManager

You can create `ContextManager` in multiple ways:

```java
import io.agentscope.runtime.engine.memory.context.ContextManager;
import io.agentscope.runtime.engine.memory.context.ContextComposer;
import io.agentscope.runtime.engine.memory.context.ContextManagerFactory;
import io.agentscope.runtime.engine.memory.persistence.memory.service.InMemoryMemoryService;
import io.agentscope.runtime.engine.memory.persistence.session.InMemorySessionHistoryService;
import io.agentscope.runtime.engine.memory.service.MemoryService;
import io.agentscope.runtime.engine.memory.service.SessionHistoryService;

// Method 1: Use default constructor (uses in-memory implementation)
ContextManager contextManager1 = new ContextManager();

// Method 2: Specify service implementations
SessionHistoryService sessionHistoryService = new InMemorySessionHistoryService();
MemoryService memoryService = new InMemoryMemoryService();
ContextManager contextManager2 = new ContextManager(
    ContextComposer.class,
    sessionHistoryService,
    memoryService
);

// Method 3: Use ContextManagerFactory (recommended)
ContextManager defaultManager = ContextManagerFactory.createDefault();

// Create custom context manager
ContextManager customManager = ContextManagerFactory.createCustom(
    memoryService,
    sessionHistoryService
);
```

### Using ContextManager

```java
import io.agentscope.runtime.engine.memory.model.Message;
import io.agentscope.runtime.engine.memory.model.Session;
import java.util.ArrayList;
import java.util.List;

// Start services
contextManager.start().get();

try {
    // Get services
    SessionHistoryService sessionService = contextManager.getSessionHistoryService();
    MemoryService memoryService = contextManager.getMemoryService();
    
    // Or get by service name
    SessionHistoryService session = (SessionHistoryService) contextManager.getService("session");
    MemoryService memory = (MemoryService) contextManager.getService("memory");
    
    // Compose session
    Session session = contextManager.composeSession("userId", "sessionId").get();
    
    // Compose context
    List<Message> requestInput = new ArrayList<>();
    contextManager.composeContext(session, requestInput).get();
    
    // Append messages
    List<Message> eventOutput = new ArrayList<>();
    contextManager.append(session, eventOutput).get();
    
} finally {
    // Stop services
    contextManager.stop().get();
}
```

### ContextComposer

`ContextComposer` is a class used to compose contexts. It will be called when the context manager creates contexts.

**Functions:**
- Updates messages generated by agents to `SessionHistoryService` and `MemoryService` in order
- Searches and concatenates before inputting messages to Agent, building input context
- Retrieves relevant memories from memory service and adds them to the session

**Usage:**

```java
import io.agentscope.runtime.engine.memory.context.ContextComposer;
import io.agentscope.runtime.engine.memory.model.Session;
import io.agentscope.runtime.engine.memory.model.Message;
import java.util.List;
import java.util.Optional;

// ContextComposer.compose method
ContextComposer.compose(
    requestInput,           // Current input messages
    session,                // Session object
    Optional.ofNullable(memoryService),           // Memory service (optional)
    Optional.ofNullable(sessionHistoryService)    // Session history service (optional)
).get();
```

---

## RAGService

`RAGService` is an interface for providing Retrieval-Augmented Generation (RAG) functionality. When end users make requests, agents retrieve relevant information from knowledge bases. Knowledge bases can be databases or document collections.

**Interface Methods:**
- `retrieve`: Retrieve relevant information from knowledge base

**Note:** RAGService is currently not supported in the Java version, and related features are under development. For RAG functionality, please refer to the Python version documentation.

---

## Service Lifecycle Management

All services follow a standard lifecycle pattern:

```java
import io.agentscope.runtime.engine.memory.persistence.memory.service.InMemoryMemoryService;
import java.util.concurrent.CompletableFuture;

InMemoryMemoryService memoryService = new InMemoryMemoryService();

try {
    // Start service
    memoryService.start().get();
    
    // Check service health status
    Boolean isHealthy = memoryService.health().get();
    System.out.println("Service health status: " + isHealthy);
    
    // Use service...
    
} catch (Exception e) {
    e.printStackTrace();
} finally {
    // Stop service
    memoryService.stop().get();
}
```

### Managing Lifecycle through ContextManager

When using `ContextManager`, service lifecycle is automatically managed:

```java
import io.agentscope.runtime.engine.memory.context.ContextManagerFactory;
import io.agentscope.runtime.engine.memory.context.ContextManager;
import java.util.Map;

ContextManager contextManager = ContextManagerFactory.createDefault();

try {
    // Start services (automatically starts memory service and session history service)
    contextManager.start().get();
    
    // Check service health status
    Map<String, Boolean> healthStatus = contextManager.healthCheck().get();
    System.out.println("Memory service health: " + healthStatus.get("memory"));
    System.out.println("Session service health: " + healthStatus.get("session"));
    
    // Use services...
    
} catch (Exception e) {
    e.printStackTrace();
} finally {
    // On exit, services automatically stop and clean up
    contextManager.stop().get();
}
```

---

## Selection Guide

### Development/Testing Environment
- **SessionHistoryService**: `InMemorySessionHistoryService`
- **MemoryService**: `InMemoryMemoryService`

### Production Environment (Small to Medium Scale)
- **SessionHistoryService**: `RedisSessionHistoryService`
- **MemoryService**: `RedisMemoryService`

### Production Environment (Large Scale, Alibaba Cloud)
- **SessionHistoryService**: `TableStoreSessionHistoryService`
- **MemoryService**: `TableStoreMemoryService`

### Production Environment (Requires Advanced Memory Management)
- **SessionHistoryService**: `RedisSessionHistoryService` or `TableStoreSessionHistoryService`
- **MemoryService**: `Mem0MemoryService`

### Custom Implementation

If you need a custom implementation, you can implement the corresponding interface:

```java
// Implement SessionHistoryService
public class CustomSessionHistoryService implements SessionHistoryService {
    // Implement interface methods...
}

// Implement MemoryService
public class CustomMemoryService implements MemoryService {
    // Implement interface methods...
}
```

Then pass the custom implementation when using:

```java
ContextManager contextManager = new ContextManager(
    ContextComposer.class,
    new CustomSessionHistoryService(),
    new CustomMemoryService()
);
```



