# 上下文管理器 (Context Manager)

## 概述

上下文管理器提供了一种方便的方式来管理上下文生命周期。它包含：

- **一组上下文服务**：会话历史服务（SessionHistoryService）和记忆服务（MemoryService）
- **ContextComposer**：用于编排历史和记忆更新的组合器

## 服务架构概览

上下文管理器支持两种核心服务类型，每种服务都有多个内置实现：

### SessionHistoryService（会话历史服务）

用于以会话级别粒度维护完整的对话历史，不会对记忆进行压缩。

**接口方法：**

- `createSession`：创建新会话
- `getSession`：获取会话
- `deleteSession`：删除会话
- `listSessions`：列出所有会话
- `appendMessage`：向历史中添加消息

**内置实现：**

| 实现类 | 存储方式 | 适用场景 | 说明 |
|--------|----------|----------|------|
| `InMemorySessionHistoryService` | 内存（Map） | 开发、测试、单机部署 | 数据存储在内存中，服务重启后数据丢失 |
| `RedisSessionHistoryService` | Redis | 生产环境、分布式部署 | 数据持久化到Redis，支持多实例共享 |
| `TableStoreSessionHistoryService` | 阿里云表格存储 | 生产环境、大规模部署 | 使用阿里云TableStore，支持海量数据存储 |

### MemoryService（记忆服务）

用于存储和检索长期记忆。在Agent中，记忆存储终端用户之前的对话信息，可以跨会话查询和使用。

**接口方法：**
- `addMemory`：向记忆服务添加记忆
- `searchMemory`：从记忆服务中搜索记忆
- `deleteMemory`：从记忆服务中删除记忆
- `listMemory`：列出所有记忆
- `getAllUsers`：获取所有用户列表

**内置实现：**

| 实现类 | 存储方式 | 适用场景 | 说明 |
|--------|----------|----------|------|
| `InMemoryMemoryService` | 内存（Map） | 开发、测试、单机部署 | 简单关键词搜索，数据存储在内存中 |
| `RedisMemoryService` | Redis | 生产环境、分布式部署 | 数据持久化到Redis，支持多实例共享 |
| `TableStoreMemoryService` | 阿里云表格存储 | 生产环境、大规模部署 | 使用阿里云TableStore KnowledgeStore，支持向量搜索 |
| `Mem0MemoryService` | Mem0 API | 生产环境、需要高级记忆管理 | 使用Mem0云服务，提供高级记忆管理和搜索能力 |

---

## SessionHistoryService（会话历史服务）

### 接口定义

`SessionHistoryService` 是管理会话历史的接口，用于以会话级别粒度维护完整记忆，不会对记忆进行压缩。

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

### 实现1：InMemorySessionHistoryService

**特点：**
- 数据存储在内存中的 `ConcurrentHashMap`
- 线程安全
- 服务重启后数据丢失
- 适用于开发、测试和小规模部署

**使用示例：**

```java
import io.agentscope.runtime.engine.memory.persistence.session.InMemorySessionHistoryService;
import io.agentscope.runtime.engine.memory.service.SessionHistoryService;
import io.agentscope.runtime.engine.memory.model.Session;
import java.util.Optional;

// 创建服务实例
SessionHistoryService sessionHistoryService = new InMemorySessionHistoryService();

// 启动服务
sessionHistoryService.start().get();

try {
    // 创建会话
    Session session = sessionHistoryService.createSession("userId", Optional.empty()).get();
    
    // 获取会话
    Optional<Session> retrievedSession = sessionHistoryService.getSession("userId", session.getId()).get();
    
    // 使用服务...
} finally {
    // 停止服务
    sessionHistoryService.stop().get();
}
```

**实现细节：**
- 数据结构：`Map<String, Map<String, Session>>` (user_id -> session_id -> Session)
- 如果未提供会话ID，会使用UUID自动生成
- 空的或仅包含空格的会话ID会被替换为自动生成的ID
- 使用ConcurrentHashMap保证线程安全

### 实现2：RedisSessionHistoryService

**特点：**
- 数据持久化到Redis
- 支持多实例共享数据
- 适用于生产环境和分布式部署
- 需要配置Redis连接

**使用示例：**

```java
import io.agentscope.runtime.engine.memory.persistence.session.RedisSessionHistoryService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

// 配置Redis连接
RedisConnectionFactory connectionFactory = new LettuceConnectionFactory("localhost", 6379);
connectionFactory.afterPropertiesSet();

RedisTemplate<String, String> redisTemplate = new StringRedisTemplate(connectionFactory);
redisTemplate.afterPropertiesSet();

// 创建服务实例
SessionHistoryService sessionHistoryService = new RedisSessionHistoryService(redisTemplate);

// 启动服务
sessionHistoryService.start().get();

try {
    // 使用服务...
    Session session = sessionHistoryService.createSession("userId", Optional.empty()).get();
} finally {
    sessionHistoryService.stop().get();
}
```

**实现细节：**
- 使用Redis Hash存储会话数据
- Key格式：`session:{userId}:{sessionId}`
- 使用Redis Set维护用户会话索引：`session_index:{userId}`
- 支持JSON序列化/反序列化

### 实现3：TableStoreSessionHistoryService

**特点：**
- 使用阿里云表格存储（TableStore）
- 支持海量数据存储
- 适用于生产环境和大规模部署
- 需要配置阿里云TableStore客户端

**使用示例：**

```java
import io.agentscope.runtime.engine.memory.persistence.session.TableStoreSessionHistoryService;
import com.alicloud.openservices.tablestore.SyncClient;
import com.aliyun.openservices.tablestore.agent.util.Pair;
import com.aliyun.openservices.tablestore.agent.model.MetaType;

// 配置TableStore客户端
String endpoint = "https://your-instance.cn-hangzhou.ots.aliyuncs.com";
String accessKeyId = "your-access-key-id";
String accessKeySecret = "your-access-key-secret";
String instanceName = "your-instance-name";

SyncClient client = new SyncClient(endpoint, accessKeyId, accessKeySecret, instanceName);

// 创建服务实例（使用默认表名）
SessionHistoryService sessionHistoryService = new TableStoreSessionHistoryService(client);

// 或者使用自定义表名和索引
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

// 启动服务
sessionHistoryService.start().get();

try {
    // 使用服务...
    Session session = sessionHistoryService.createSession("userId", Optional.empty()).get();
} finally {
    sessionHistoryService.stop().get();
}
```

**实现细节：**
- 使用TableStore MemoryStore API
- 默认表名：`agentscope_runtime_session` 和 `agentscope_runtime_message`
- 支持二级索引加速查询
- 自动处理表的创建和初始化

### 会话对象结构

每个会话由 `Session` 对象表示：

```java
import io.agentscope.runtime.engine.memory.model.Session;
import io.agentscope.runtime.engine.memory.model.Message;
import io.agentscope.runtime.engine.memory.model.MessageContent;
import io.agentscope.runtime.engine.memory.model.MessageType;
import java.util.ArrayList;
import java.util.List;

// 创建会话对象
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

### 核心功能示例

#### 创建会话

```java
import io.agentscope.runtime.engine.memory.persistence.session.InMemorySessionHistoryService;
import io.agentscope.runtime.engine.memory.model.Session;
import java.util.Optional;

InMemorySessionHistoryService sessionHistoryService = new InMemorySessionHistoryService();
sessionHistoryService.start().get();

try {
    String userId = "test_user";
    
    // 创建带自动生成ID的会话
    Session session = sessionHistoryService.createSession(userId, Optional.empty()).get();
    System.out.println("Created session: " + session.getId());
    
    // 创建带自定义ID的会话
    Session customSession = sessionHistoryService.createSession(
        userId,
        Optional.of("my_custom_session_id")
    ).get();
    System.out.println("Custom session ID: " + customSession.getId());
} finally {
    sessionHistoryService.stop().get();
}
```

#### 检索会话

```java
try {
    String userId = "u1";
    String sessionId = "s1";
    
    // 检索现有会话
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

#### 列出会话

```java
try {
    String userId = "u_list";
    
    // 创建多个会话
    Session session1 = sessionHistoryService.createSession(userId, Optional.empty()).get();
    Session session2 = sessionHistoryService.createSession(userId, Optional.empty()).get();
    
    // 列出所有会话
    List<Session> listedSessions = sessionHistoryService.listSessions(userId).get();
    System.out.println("Total sessions: " + listedSessions.size());
    
    for (Session s : listedSessions) {
        System.out.println("Session ID: " + s.getId());
    }
} finally {
    sessionHistoryService.stop().get();
}
```

#### 添加消息

```java
import io.agentscope.runtime.engine.memory.model.Message;
import io.agentscope.runtime.engine.memory.model.MessageContent;
import io.agentscope.runtime.engine.memory.model.MessageType;
import java.util.ArrayList;
import java.util.List;

try {
    String userId = "u_append";
    Session session = sessionHistoryService.createSession(userId, Optional.empty()).get();
    
    // 添加单个消息
    List<MessageContent> userContent = new ArrayList<>();
    userContent.add(new MessageContent("text", "Hello, world!"));
    Message message1 = new Message(MessageType.USER, userContent);
    
    List<Message> messages = new ArrayList<>();
    messages.add(message1);
    sessionHistoryService.appendMessage(session, messages).get();
    
    // 一次添加多个消息
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
    
    // 验证消息已添加
    Optional<Session> updatedSession = sessionHistoryService.getSession(userId, session.getId()).get();
    System.out.println("Total messages: " + updatedSession.get().getMessages().size());
} finally {
    sessionHistoryService.stop().get();
}
```

#### 删除会话

```java
try {
    String userId = "u_delete";
    Session sessionToDelete = sessionHistoryService.createSession(userId, Optional.empty()).get();
    String sessionId = sessionToDelete.getId();
    
    // 删除会话
    sessionHistoryService.deleteSession(userId, sessionId).get();
    System.out.println("Session deleted: " + sessionId);
} finally {
    sessionHistoryService.stop().get();
}
```

---

## MemoryService（记忆服务）

### 接口定义

`MemoryService` 是管理记忆的接口。在Agent中，记忆存储终端用户之前的对话信息，可以跨会话查询和使用。

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

### 实现1：InMemoryMemoryService

**特点：**
- 数据存储在内存中的 `ConcurrentHashMap`
- 简单的关键词搜索（基于文本匹配）
- 线程安全
- 服务重启后数据丢失
- 适用于开发、测试和小规模部署

**使用示例：**

```java
import io.agentscope.runtime.engine.memory.persistence.memory.service.InMemoryMemoryService;
import io.agentscope.runtime.engine.memory.service.MemoryService;
import io.agentscope.runtime.engine.memory.model.Message;
import io.agentscope.runtime.engine.memory.model.MessageContent;
import io.agentscope.runtime.engine.memory.model.MessageType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// 创建服务实例
MemoryService memoryService = new InMemoryMemoryService();

// 启动服务
memoryService.start().get();

try {
    String userId = "user1";
    
    // 添加记忆
    List<MessageContent> content = new ArrayList<>();
    content.add(new MessageContent("text", "My name is Alice"));
    Message message = new Message(MessageType.USER, content);
    
    List<Message> messages = new ArrayList<>();
    messages.add(message);
    memoryService.addMemory(userId, messages, Optional.empty()).get();
    
    // 搜索记忆
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

**实现细节：**
- 数据结构：`Map<String, Map<String, List<Message>>>` (user_id -> session_id -> messages)
- 未指定会话时使用默认会话ID ("default_session")
- 基于关键词的搜索不区分大小写
- 消息在每个会话中按时间顺序存储
- 使用ConcurrentHashMap保证线程安全

### 实现2：RedisMemoryService

**特点：**
- 数据持久化到Redis
- 支持多实例共享数据
- 简单的关键词搜索
- 适用于生产环境和分布式部署
- 需要配置Redis连接

**使用示例：**

```java
import io.agentscope.runtime.engine.memory.persistence.memory.service.RedisMemoryService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

// 配置Redis连接
RedisConnectionFactory connectionFactory = new LettuceConnectionFactory("localhost", 6379);
connectionFactory.afterPropertiesSet();

RedisTemplate<String, String> redisTemplate = new StringRedisTemplate(connectionFactory);
redisTemplate.afterPropertiesSet();

// 创建服务实例
MemoryService memoryService = new RedisMemoryService(redisTemplate);

// 启动服务
memoryService.start().get();

try {
    // 使用服务...
    String userId = "user1";
    List<Message> messages = new ArrayList<>();
    memoryService.addMemory(userId, messages, Optional.empty()).get();
} finally {
    memoryService.stop().get();
}
```

**实现细节：**
- 使用Redis Hash存储记忆数据
- Key格式：`user_memory:{userId}`
- 使用JSON序列化/反序列化消息
- 支持健康检查（通过Redis PING命令）

### 实现3：TableStoreMemoryService

**特点：**
- 使用阿里云表格存储（TableStore）KnowledgeStore
- 支持向量搜索和语义搜索
- 支持海量数据存储
- 适用于生产环境和大规模部署
- 需要配置阿里云TableStore客户端

**使用示例：**

```java
import io.agentscope.runtime.engine.memory.persistence.memory.service.TableStoreMemoryService;
import com.alicloud.openservices.tablestore.SyncClient;

// 配置TableStore客户端
String endpoint = "https://your-instance.cn-hangzhou.ots.aliyuncs.com";
String accessKeyId = "your-access-key-id";
String accessKeySecret = "your-access-key-secret";
String instanceName = "your-instance-name";

SyncClient client = new SyncClient(endpoint, accessKeyId, accessKeySecret, instanceName);

// 创建服务实例（使用默认表名和索引）
MemoryService memoryService = new TableStoreMemoryService(client);

// 或者使用自定义表名和索引
String tableName = "custom_memory_table";
String searchIndexName = "custom_search_index";
MemoryService customService = new TableStoreMemoryService(client, tableName, searchIndexName);

// 启动服务
memoryService.start().get();

try {
    // 使用服务...
    String userId = "user1";
    List<Message> messages = new ArrayList<>();
    memoryService.addMemory(userId, messages, Optional.empty()).get();
} finally {
    memoryService.stop().get();
}
```

**实现细节：**
- 使用TableStore KnowledgeStore API
- 默认表名：`agentscope_runtime_memory`
- 默认搜索索引：`agentscope_runtime_knowledge_search_index`
- 支持向量嵌入和语义搜索
- 自动处理表和索引的创建

### 实现4：Mem0MemoryService

**特点：**
- 使用Mem0云服务API
- 提供高级记忆管理和搜索能力
- 支持向量搜索和语义理解
- 适用于生产环境，需要高级记忆功能
- 需要Mem0 API密钥

**使用示例：**

```java
import io.agentscope.runtime.engine.memory.persistence.memory.service.Mem0MemoryService;

// 创建服务实例（需要API密钥）
String apiKey = System.getenv("MEM0_API_KEY");
MemoryService memoryService = new Mem0MemoryService(apiKey);

// 或者指定组织ID和项目ID
String orgId = "your-org-id";
String projectId = "your-project-id";
MemoryService memoryServiceWithIds = new Mem0MemoryService(apiKey, orgId, projectId);

// 启动服务
memoryService.start().get();

try {
    // 使用服务...
    String userId = "user1";
    List<Message> messages = new ArrayList<>();
    memoryService.addMemory(userId, messages, Optional.empty()).get();
    
    // 搜索记忆
    List<Message> searchQuery = new ArrayList<>();
    Map<String, Object> filters = new HashMap<>();
    filters.put("top_k", 5);
    List<Message> retrieved = memoryService.searchMemory(userId, searchQuery, Optional.of(filters)).get();
} finally {
    memoryService.stop().get();
}
```

**实现细节：**
- 使用Mem0 HTTP API (https://api.mem0.ai)
- 支持Mem0 v1和v2 API
- 自动处理HTTP请求和响应
- 支持组织级别和项目级别的记忆管理
- 需要有效的Mem0 API密钥

### 核心功能示例

#### 添加记忆

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
    
    // 不带会话ID添加记忆
    List<MessageContent> content = new ArrayList<>();
    content.add(new MessageContent("text", "My favorite color is blue"));
    Message message = new Message(MessageType.USER, content);
    
    List<Message> messages = new ArrayList<>();
    messages.add(message);
    memoryService.addMemory(userId, messages, Optional.empty()).get();
    
    // 带会话ID添加记忆
    memoryService.addMemory(userId, messages, Optional.of("session1")).get();
} finally {
    memoryService.stop().get();
}
```

#### 搜索记忆

```java
try {
    String userId = "user1";
    
    // 创建搜索查询
    List<MessageContent> queryContent = new ArrayList<>();
    queryContent.add(new MessageContent("text", "favorite color"));
    Message queryMessage = new Message(MessageType.USER, queryContent);
    
    List<Message> searchQuery = new ArrayList<>();
    searchQuery.add(queryMessage);
    
    // 搜索记忆（可以传入过滤器，如top_k）
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

#### 列出记忆

```java
try {
    String userId = "user1";
    
    // 列出记忆（带分页）
    Map<String, Object> filters = new HashMap<>();
    filters.put("page_size", 10);
    filters.put("page_num", 1);
    List<Message> memoryList = memoryService.listMemory(userId, Optional.of(filters)).get();
    
    System.out.println("Listed " + memoryList.size() + " memory items");
} finally {
    memoryService.stop().get();
}
```

#### 删除记忆

```java
try {
    String userId = "user1";
    String sessionId = "session1";
    
    // 删除特定会话的记忆
    memoryService.deleteMemory(userId, Optional.of(sessionId)).get();
    
    // 删除用户的所有记忆
    memoryService.deleteMemory(userId, Optional.empty()).get();
} finally {
    memoryService.stop().get();
}
```

#### 获取所有用户

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

## ContextManager（上下文管理器）

### 概述

`ContextManager` 继承自 `ServiceManager`，用于管理会话历史服务和记忆服务的生命周期，并提供上下文组合功能。

### 创建 ContextManager

可以通过多种方式创建 `ContextManager`：

```java
import io.agentscope.runtime.engine.memory.context.ContextManager;
import io.agentscope.runtime.engine.memory.context.ContextComposer;
import io.agentscope.runtime.engine.memory.context.ContextManagerFactory;
import io.agentscope.runtime.engine.memory.persistence.memory.service.InMemoryMemoryService;
import io.agentscope.runtime.engine.memory.persistence.session.InMemorySessionHistoryService;
import io.agentscope.runtime.engine.memory.service.MemoryService;
import io.agentscope.runtime.engine.memory.service.SessionHistoryService;

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
    SessionHistoryService session = (SessionHistoryService) contextManager.getService("session");
    MemoryService memory = (MemoryService) contextManager.getService("memory");
    
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

### ContextComposer

`ContextComposer` 是用于组合上下文的类。它将在上下文管理器创建上下文时被调用。

**功能：**
- 按顺序向 `SessionHistoryService` 和 `MemoryService` 更新智能体产生的消息
- 在向 Agent 输入消息之前进行搜索和拼接，构建输入上下文
- 从记忆服务中检索相关记忆并添加到会话中

**使用方式：**

```java
import io.agentscope.runtime.engine.memory.context.ContextComposer;
import io.agentscope.runtime.engine.memory.model.Session;
import io.agentscope.runtime.engine.memory.model.Message;
import java.util.List;
import java.util.Optional;

// ContextComposer.compose 方法
ContextComposer.compose(
    requestInput,           // 当前输入消息
    session,                // 会话对象
    Optional.ofNullable(memoryService),           // 记忆服务（可选）
    Optional.ofNullable(sessionHistoryService)    // 会话历史服务（可选）
).get();
```

---

## RAGService

`RAGService` 是一个接口，用于提供检索增强生成（RAG）功能。当最终用户提出请求时，代理会从知识库中检索相关信息。知识库可以是数据库或文档集合。

**接口方法：**
- `retrieve`：从知识库中检索相关信息

**注意：** 目前Java版本暂不支持RAGService，相关功能正在开发中。如需使用RAG功能，请参考Python版本的文档。

---

## 服务生命周期管理

所有服务都遵循标准的生命周期模式：

```java
import io.agentscope.runtime.engine.memory.persistence.memory.service.InMemoryMemoryService;
import java.util.concurrent.CompletableFuture;

InMemoryMemoryService memoryService = new InMemoryMemoryService();

try {
    // 启动服务
    memoryService.start().get();
    
    // 检查服务健康状态
    Boolean isHealthy = memoryService.health().get();
    System.out.println("Service health status: " + isHealthy);
    
    // 使用服务...
    
} catch (Exception e) {
    e.printStackTrace();
} finally {
    // 停止服务
    memoryService.stop().get();
}
```

### 通过 ContextManager 管理生命周期

使用 `ContextManager` 时，服务生命周期会自动管理：

```java
import io.agentscope.runtime.engine.memory.context.ContextManagerFactory;
import io.agentscope.runtime.engine.memory.context.ContextManager;
import java.util.Map;

ContextManager contextManager = ContextManagerFactory.createDefault();

try {
    // 启动服务（会自动启动记忆服务和会话历史服务）
    contextManager.start().get();
    
    // 检查服务健康状态
    Map<String, Boolean> healthStatus = contextManager.healthCheck().get();
    System.out.println("Memory service health: " + healthStatus.get("memory"));
    System.out.println("Session service health: " + healthStatus.get("session"));
    
    // 使用服务...
    
} catch (Exception e) {
    e.printStackTrace();
} finally {
    // 退出时，服务自动停止并清理
    contextManager.stop().get();
}
```

---

## 选择指南

### 开发/测试环境
- **SessionHistoryService**: `InMemorySessionHistoryService`
- **MemoryService**: `InMemoryMemoryService`

### 生产环境（中小规模）
- **SessionHistoryService**: `RedisSessionHistoryService`
- **MemoryService**: `RedisMemoryService`

### 生产环境（大规模，阿里云）
- **SessionHistoryService**: `TableStoreSessionHistoryService`
- **MemoryService**: `TableStoreMemoryService`

### 生产环境（需要高级记忆管理）
- **SessionHistoryService**: `RedisSessionHistoryService` 或 `TableStoreSessionHistoryService`
- **MemoryService**: `Mem0MemoryService`

### 自定义实现

如果需要自定义实现，可以实现相应的接口：

```java
// 实现 SessionHistoryService
public class CustomSessionHistoryService implements SessionHistoryService {
    // 实现接口方法...
}

// 实现 MemoryService
public class CustomMemoryService implements MemoryService {
    // 实现接口方法...
}
```

然后在使用时传入自定义实现：

```java
ContextManager contextManager = new ContextManager(
    ContextComposer.class,
    new CustomSessionHistoryService(),
    new CustomMemoryService()
);
```
