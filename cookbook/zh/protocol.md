# Agent API 协议规范

## 概述

本文档描述了与AI智能体通信的结构化JSON协议。该协议定义了支持以下功能的消息、请求和响应：

+ 流式内容传输
+ 工具/函数调用
+ 多模态内容（文本、图像、数据）
+ 全生命周期的状态跟踪
+ 错误处理

## 协议结构

### 1. 核心枚举和常量

**角色**：

在Java实现中，角色使用字符串常量表示，通常为 `"user"`、`"assistant"`、`"system"` 或 `"tool"`。角色值直接存储在 `Message` 类的 `role` 字段中。

**消息类型**：

```java
package io.agentscope.runtime.engine.memory.model;

public enum MessageType {
    CHUNK,
    MESSAGE,
    SYSTEM,
    USER,
    ASSISTANT,
    FUNCTION_CALL,
    FUNCTION_CALL_OUTPUT,
    PLUGIN_CALL,
    PLUGIN_CALL_OUTPUT,
    COMPONENT_CALL,
    COMPONENT_CALL_OUTPUT,
    MCP_LIST_TOOLS,
    MCP_APPROVAL_REQUEST,
    MCP_TOOL_CALL,
    MCP_APPROVAL_RESPONSE,
    HEARTBEAT,
    ERROR
}
```

**运行状态**：

```java
package io.agentscope.runtime.engine.schemas.agent;

public class RunStatus {
    public static final String CREATED = "created";
    public static final String IN_PROGRESS = "in_progress";
    public static final String COMPLETED = "completed";
    public static final String CANCELED = "canceled";
    public static final String FAILED = "failed";
    public static final String REJECTED = "rejected";
    public static final String UNKNOWN = "unknown";
    
    private RunStatus() {
        // Utility class, instantiation not allowed
    }
}
```

### 2. 内容模型

**内容类型常量**：

```java
package io.agentscope.runtime.engine.schemas.agent;

public class ContentType {
    public static final String TEXT = "text";
    public static final String DATA = "data";
    public static final String IMAGE = "image";
    public static final String AUDIO = "audio";
    
    private ContentType() {
        // Utility class, instantiation not allowed
    }
}
```

**基础内容模型**：

```java
package io.agentscope.runtime.engine.schemas.agent;

import java.util.Map;

public abstract class Content extends Event {
    private String type;
    private String object = "content";
    private Integer index;
    private Boolean delta = false;
    private String msgId;
    
    public Content() {
        super();
    }
    
    public Content(String type) {
        super();
        this.type = type;
    }
    
    /**
     * 内容部分的类型
     */
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    /**
     * 内容部分的标识
     */
    @Override
    public String getObject() {
        return object;
    }
    
    @Override
    public void setObject(String object) {
        this.object = object;
    }
    
    /**
     * 在消息内容列表中的索引位置
     */
    public Integer getIndex() {
        return index;
    }
    
    public void setIndex(Integer index) {
        this.index = index;
    }
    
    /**
     * 是否为增量内容
     */
    public Boolean getDelta() {
        return delta;
    }
    
    public void setDelta(Boolean delta) {
        this.delta = delta;
    }
    
    /**
     * 消息唯一ID
     */
    public String getMsgId() {
        return msgId;
    }
    
    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }
}
```

**专用内容类型**：

```java
package io.agentscope.runtime.engine.schemas.agent;

import java.util.Map;

public class TextContent extends Content {
    private String text;
    
    public TextContent() {
        super(ContentType.TEXT);
    }
    
    public TextContent(String text) {
        super(ContentType.TEXT);
        this.text = text;
    }
    
    public TextContent(Boolean delta, String text, Integer index) {
        super(ContentType.TEXT);
        this.setDelta(delta);
        this.text = text;
        this.setIndex(index);
    }
    
    /**
     * 文本内容
     */
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
}

public class ImageContent extends Content {
    private String imageUrl;
    
    public ImageContent() {
        super(ContentType.IMAGE);
    }
    
    public ImageContent(String imageUrl) {
        super(ContentType.IMAGE);
        this.imageUrl = imageUrl;
    }
    
    /**
     * 图片URL详情
     */
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}

public class DataContent extends Content {
    private Map<String, Object> data;
    
    public DataContent() {
        super(ContentType.DATA);
    }
    
    public DataContent(Map<String, Object> data) {
        super(ContentType.DATA);
        this.data = data;
    }
    
    public DataContent(Boolean delta, Map<String, Object> data, Integer index) {
        super(ContentType.DATA);
        this.setDelta(delta);
        this.data = data;
        this.setIndex(index);
    }
    
    /**
     * 数据内容
     */
    public Map<String, Object> getData() {
        return data;
    }
    
    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}
```

**注意**：Java实现中目前支持 `TextContent`、`ImageContent` 和 `DataContent`。`AudioContent`、`FileContent` 和 `RefusalContent` 暂未实现。

### 3. 事件模型

**基础事件类**：

```java
package io.agentscope.runtime.engine.schemas.agent;

public class Event {
    private Integer sequenceNumber;
    protected String object;
    protected String status;
    private java.lang.Error error;
    
    public Event() {
        this.status = RunStatus.CREATED;
    }
    
    /**
     * 序列号
     */
    public Integer getSequenceNumber() {
        return sequenceNumber;
    }
    
    public void setSequenceNumber(Integer sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }
    
    /**
     * 对象标识
     */
    public String getObject() {
        return object;
    }
    
    public void setObject(String object) {
        this.object = object;
    }
    
    /**
     * 状态：created, in_progress, completed, canceled, failed, rejected
     */
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    /**
     * 错误信息
     */
    public java.lang.Error getError() {
        return error;
    }
    
    public void setError(java.lang.Error error) {
        this.error = error;
    }
    
    /**
     * 设置为创建状态
     */
    public Event created() {
        this.status = RunStatus.CREATED;
        return this;
    }
    
    /**
     * 设置为进行中状态
     */
    public Event inProgress() {
        this.status = RunStatus.IN_PROGRESS;
        return this;
    }
    
    /**
     * 设置为完成状态
     */
    public Event completed() {
        this.status = RunStatus.COMPLETED;
        return this;
    }
    
    /**
     * 设置为拒绝状态
     */
    public Event rejected() {
        this.status = RunStatus.REJECTED;
        return this;
    }
    
    /**
     * 设置为取消状态
     */
    public Event canceled() {
        this.status = RunStatus.CANCELED;
        return this;
    }
}
```

### 4. 消息模型

```java
package io.agentscope.runtime.engine.schemas.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import io.agentscope.runtime.engine.schemas.message.MessageType;

public class Message extends Event {
    private String id;
    private String type = MessageType.MESSAGE.name();
    private String role;
    private List<Content> content;
    private String code;
    private String message;
    private Map<String, Object> usage;
    
    public Message() {
        super();
        this.id = "msg_" + UUID.randomUUID().toString();
        this.object = "message";
        this.status = RunStatus.CREATED;
        this.content = new ArrayList<>();
    }
    
    public Message(String type, String role) {
        this();
        this.type = type;
        this.role = role;
    }
    
    /**
     * 消息唯一ID
     */
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * 消息类型
     */
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    /**
     * 消息作者角色，应为 "user", "system", "assistant" 或 "tool"
     */
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    /**
     * 消息内容列表
     */
    public List<Content> getContent() {
        return content;
    }
    
    public void setContent(List<Content> content) {
        this.content = content != null ? content : new ArrayList<>();
    }
    
    /**
     * 消息错误代码
     */
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    /**
     * 消息错误描述
     */
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    /**
     * 使用情况统计
     */
    public Map<String, Object> getUsage() {
        return usage;
    }
    
    public void setUsage(Map<String, Object> usage) {
        this.usage = usage;
    }
}
```

**关键方法**：

+ `addDeltaContent(Content newContent)`: 向现有消息追加部分内容
+ `contentCompleted(int contentIndex)`: 标记内容片段为完成状态
+ `addContent(Content newContent)`: 添加完整的内容片段
+ `getTextContent()`: 获取消息中的文本内容
+ `getImageContent()`: 获取消息中的图片URL列表

### 5. 请求模型

**智能体请求**：

```java
package io.agentscope.runtime.engine.schemas.agent;

import java.util.List;

public class AgentRequest {
    private List<Message> input;
    private boolean stream = true;
    private String model;
    private Double topP;
    private Double temperature;
    private Double frequencyPenalty;
    private Double presencePenalty;
    private Integer maxTokens;
    private Object stop; // String or List<String>
    private Integer n = 1;
    private Integer seed;
    private List<Object> tools; // List<BaseSandboxTool> or List<Map>
    private String userId;
    private String sessionId;
    private String responseId;

    public AgentRequest() {}

    public AgentRequest(List<Message> input) {
        this.input = input;
    }
    
    /**
     * 输入消息列表
     */
    public List<Message> getInput() {
        return input;
    }

    public void setInput(List<Message> input) {
        this.input = input;
    }
    
    /**
     * 是否使用流式响应
     */
    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }
    
    /**
     * 模型名称
     */
    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
    
    // 其他getter和setter方法...
    public Double getTopP() { return topP; }
    public void setTopP(Double topP) { this.topP = topP; }
    
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    
    public Double getFrequencyPenalty() { return frequencyPenalty; }
    public void setFrequencyPenalty(Double frequencyPenalty) { this.frequencyPenalty = frequencyPenalty; }
    
    public Double getPresencePenalty() { return presencePenalty; }
    public void setPresencePenalty(Double presencePenalty) { this.presencePenalty = presencePenalty; }
    
    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
    
    public Object getStop() { return stop; }
    public void setStop(Object stop) { this.stop = stop; }
    
    public Integer getN() { return n; }
    public void setN(Integer n) { this.n = n; }
    
    public Integer getSeed() { return seed; }
    public void setSeed(Integer seed) { this.seed = seed; }
    
    public List<Object> getTools() { return tools; }
    public void setTools(List<Object> tools) { this.tools = tools; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getResponseId() { return responseId; }
    public void setResponseId(String responseId) { this.responseId = responseId; }
}
```

### 6. 响应模型

**智能体响应**：

```java
package io.agentscope.runtime.engine.schemas.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AgentResponse extends Event {
    private String id;
    private String object = "response";
    private Long createdAt;
    private Long completedAt;
    private List<Message> output;
    private Map<String, Object> usage;
    private String sessionId;
    
    public AgentResponse() {
        super();
        this.id = "response_" + UUID.randomUUID().toString();
        this.status = RunStatus.CREATED;
        this.createdAt = System.currentTimeMillis();
        this.output = new ArrayList<>();
    }
    
    /**
     * 响应唯一ID
     */
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * 响应对象标识
     */
    @Override
    public String getObject() {
        return object;
    }
    
    @Override
    public void setObject(String object) {
        this.object = object;
    }
    
    /**
     * 创建时间戳（毫秒）
     */
    public Long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
    
    /**
     * 完成时间戳（毫秒）
     */
    public Long getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(Long completedAt) {
        this.completedAt = completedAt;
    }
    
    /**
     * 输出消息列表
     */
    public List<Message> getOutput() {
        return output;
    }
    
    public void setOutput(List<Message> output) {
        this.output = output != null ? output : new ArrayList<>();
    }
    
    /**
     * 使用情况统计
     */
    public Map<String, Object> getUsage() {
        return usage;
    }
    
    public void setUsage(Map<String, Object> usage) {
        this.usage = usage;
    }
    
    /**
     * 会话ID
     */
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    /**
     * 添加新消息
     */
    public void addNewMessage(Message message) {
        if (output == null) {
            output = new ArrayList<>();
        }
        output.add(message);
    }
}
```

### 7. 错误模型

```java
package io.agentscope.runtime.engine.schemas.agent;

public class Error {
    private String code;
    private String message;
    
    public Error() {}
    
    public Error(String code, String message) {
        this.code = code;
        this.message = message;
    }
    
    /**
     * 错误代码
     */
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    /**
     * 错误消息
     */
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
```

## 协议流程

### 请求/响应生命周期

1. 客户端发送 `AgentRequest`，包含：
   - 输入消息
   - 生成参数
   - 工具定义
   - 会话上下文
2. 服务端响应 `AgentResponse` 对象流，包含：
   - 状态更新 (`created` → `in_progress` → `completed`)
   - 带内容片段的输出消息
   - 最终使用指标

### 内容流式传输

当请求中 `stream=True` 时：

+ 文本内容以 `delta=true` 片段增量发送
+ 每个片段包含指向目标内容槽的 `index`
+ 最终片段通过 `status=completed` 标记完成

**流式传输示例**：

```bash
{"status":"created","id":"response_...","object":"response"}
{"status":"created","id":"msg_...","object":"message","type":"assistant"}
{"status":"in_progress","type":"text","index":0,"delta":true,"text":"Hello","object":"content"}
{"status":"in_progress","type":"text","index":0,"delta":true,"text":", ","object":"content"}
{"status":"in_progress","type":"text","index":0,"delta":true,"text":"world","object":"content"}
{"status":"completed","type":"text","index":0,"delta":false,"text":"Hello, world!","object":"content"}
{"status":"completed","id":"msg_...","object":"message", ...}
{"status":"completed","id":"response_...","object":"response", ...}
```

### 状态转换

| 状态         | 描述                       |
| ------------- | -------------------------- |
| `created`     | 对象创建时的初始状态       |
| `in_progress` | 操作正在处理中             |
| `completed`   | 操作成功完成               |
| `failed`      | 操作因错误终止             |
| `rejected`    | 操作被系统拒绝             |
| `canceled`    | 操作被用户取消             |
| `unknown`     | 未知状态                   |


## 最佳实践

1. **流处理**：
   - 缓冲增量片段直到收到 `status=completed`
   - 使用 `msg_id` 关联内容与父消息
   - 尊重多片段消息的 `index` 顺序
2. **错误处理**：
   - 检查响应中的 `error` 字段
   - 监控 `failed` 状态转换
   - 对可恢复错误实施重试逻辑
3. **状态管理**：
   - 使用 `sessionId` 保持会话连续性
   - 跟踪 `createdAt`/`completedAt` 监控延迟（时间戳单位为毫秒）
   - 使用 `sequenceNumber` 排序（如已实现）

## 使用示例

**用户查询**：

```json
{
  "input": [{
    "role": "user",
    "content": [{"type": "text", "text": "描述这张图片"}],
    "type": "MESSAGE"
  }],
  "stream": true,
  "model": "gpt-4-vision"
}
```

**智能体响应流**：

```bash
{"id":"response_123","object":"response","status":"created"}
{"id":"msg_abc","object":"message","type":"MESSAGE","status":"created","role":"assistant"}
{"status":"in_progress","type":"text","index":0,"delta":true,"text":"这张","object":"content","msgId":"msg_abc"}
{"status":"in_progress","type":"text","index":0,"delta":true,"text":"图片显示...","object":"content","msgId":"msg_abc"}
{"status":"completed","type":"text","index":0,"delta":false,"text":"这张图片显示...","object":"content","msgId":"msg_abc"}
{"id":"msg_abc","status":"completed","object":"message"}
{"id":"response_123","status":"completed","object":"response"}
```

**Java代码示例**：

```java
import io.agentscope.runtime.engine.schemas.agent.*;
import java.util.ArrayList;
import java.util.List;

// 创建请求
AgentRequest request = new AgentRequest();
List<Message> input = new ArrayList<>();

Message userMessage = new Message();
userMessage.setRole("user");
userMessage.setType("MESSAGE");

TextContent textContent = new TextContent("描述这张图片");
List<Content> contents = new ArrayList<>();
contents.add(textContent);
userMessage.setContent(contents);

input.add(userMessage);
request.setInput(input);
request.setStream(true);
request.setModel("gpt-4-vision");

// 处理响应
AgentResponse response = new AgentResponse();
response.setSessionId("session_123");
response.created();

Message assistantMessage = new Message("MESSAGE", "assistant");
assistantMessage.created();

TextContent deltaContent = new TextContent(true, "这张", 0);
deltaContent.setMsgId(assistantMessage.getId());
deltaContent.inProgress();
assistantMessage.addDeltaContent(deltaContent);

// ... 继续添加内容 ...
```

## Agent API 协议使用方式

在Java实现中，可以直接使用类和方法来构建符合协议规范的流式响应数据。

### 1. 构建响应对象

**创建响应**：

```java
import io.agentscope.runtime.engine.schemas.agent.*;

// 创建响应对象
AgentResponse response = new AgentResponse();
response.setSessionId("session_123");
response.created();  // 设置为创建状态
```

**创建消息**：

```java
// 创建消息对象
Message message = new Message("MESSAGE", "assistant");
message.created();  // 设置为创建状态

// 添加到响应
response.addNewMessage(message);
```

### 2. 构建内容

**文本内容**：

```java
// 创建文本内容
TextContent textContent = new TextContent("Hello World");
textContent.setIndex(0);
textContent.completed();  // 设置为完成状态

// 添加到消息
message.addContent(textContent);
```

**增量文本内容（流式）**：

```java
// 创建增量文本内容
TextContent deltaContent1 = new TextContent(true, "Hello", 0);
deltaContent1.setMsgId(message.getId());
deltaContent1.inProgress();  // 设置为进行中状态
message.addDeltaContent(deltaContent1);

TextContent deltaContent2 = new TextContent(true, " World", 0);
deltaContent2.setMsgId(message.getId());
deltaContent2.inProgress();
message.addDeltaContent(deltaContent2);

// 完成内容
message.contentCompleted(0);
```

**图片内容**：

```java
// 创建图片内容
ImageContent imageContent = new ImageContent("https://example.com/image.jpg");
imageContent.setIndex(0);
imageContent.completed();

// 添加到消息
message.addContent(imageContent);
```

**数据内容**：

```java
import java.util.HashMap;
import java.util.Map;

// 创建数据内容
Map<String, Object> data = new HashMap<>();
data.put("type", "function_call");
data.put("name", "get_weather");
data.put("arguments", "{\"city\": \"Beijing\"}");

DataContent dataContent = new DataContent(data);
dataContent.setIndex(0);
dataContent.completed();

// 添加到消息
message.addContent(dataContent);
```

### 3. 完整使用示例

以下示例展示如何生成完整的流式响应序列：

```java
import io.agentscope.runtime.engine.schemas.agent.*;
import java.util.ArrayList;
import java.util.List;

public class StreamingResponseExample {
    public static void generateStreamingResponse(String sessionId, 
                                                  List<String> textTokens, 
                                                  String role) {
        // 创建响应对象
        AgentResponse response = new AgentResponse();
        response.setSessionId(sessionId);
        response.created();
        System.out.println(response);  // 输出响应创建事件
        
        // 创建消息对象
        Message message = new Message("MESSAGE", role);
        message.created();
        response.addNewMessage(message);
        System.out.println(message);  // 输出消息创建事件
        
        // 流式输出文本内容
        int index = 0;
        for (String token : textTokens) {
            TextContent deltaContent = new TextContent(true, token, index);
            deltaContent.setMsgId(message.getId());
            deltaContent.inProgress();
            message.addDeltaContent(deltaContent);
            System.out.println(deltaContent);  // 输出增量内容事件
        }
        
        // 完成内容
        message.contentCompleted(index);
        
        // 完成消息
        message.completed();
        System.out.println(message);  // 输出消息完成事件
        
        // 完成响应
        response.completed();
        response.setCompletedAt(System.currentTimeMillis());
        System.out.println(response);  // 输出响应完成事件
    }
}
```

### 4. 流式响应序列

标准的流式响应序列包括以下步骤：

1. **响应创建** (`response.created()`)
2. **消息创建** (`message.created()`)
3. **内容流式输出** (`content.inProgress()` with `delta=true`)
4. **内容完成** (`content.completed()`)
5. **消息完成** (`message.completed()`)
6. **响应完成** (`response.completed()`)

### 5. 支持的内容类型

Java实现中目前支持以下内容类型：

- **TextContent**: 文本内容，支持增量输出
- **ImageContent**: 图片内容，支持URL格式
- **DataContent**: 数据内容，支持任意Map数据

**注意**：`AudioContent`、`FileContent` 和 `RefusalContent` 暂未在Java实现中提供。

### 6. 最佳实践

1. **状态管理**: 确保按正确顺序设置状态（created → in_progress → completed）
2. **内容索引**: 为多内容消息正确设置index值
3. **增量输出**: 使用`addDeltaContent`方法实现流式文本输出
4. **错误处理**: 在构建过程中适当处理异常情况
5. **消息ID关联**: 使用`setMsgId`方法将内容与父消息关联

### 7. 高级用法

#### 多内容消息构建

```java
// 创建包含文本和图片的消息
Message message = new Message("MESSAGE", "assistant");

// 添加文本内容
TextContent textContent = new TextContent("这是一张图片：");
textContent.setIndex(0);
textContent.completed();
message.addContent(textContent);

// 添加图片内容
ImageContent imageContent = new ImageContent("https://example.com/image.jpg");
imageContent.setIndex(1);
imageContent.completed();
message.addContent(imageContent);

// 完成消息
message.completed();
```

#### 函数调用数据内容

```java
// 创建函数调用
FunctionCall functionCall = new FunctionCall(
    "call_123",
    "get_weather",
    "{\"city\": \"Beijing\"}"
);

// 转换为数据内容
DataContent dataContent = new DataContent(functionCall.toMap());
dataContent.setIndex(0);
dataContent.completed();

// 添加到消息
Message message = new Message("FUNCTION_CALL", "assistant");
message.addContent(dataContent);
```

通过直接使用Java类和方法，开发者可以构建符合协议规范的复杂流式响应，实现更好的用户体验和更灵活的响应控制。