# Agent API Protocol Specification

## Overview

This document describes a structured JSON protocol for communicating with AI agents. The protocol defines messages, requests, and responses that support the following features:

+ Streaming content transmission
+ Tool/function calls
+ Multimodal content (text, images, data)
+ Full lifecycle state tracking
+ Error handling

## Protocol Structure

### 1. Core Enumerations and Constants

**Roles**:

In the Java implementation, roles are represented using string constants, typically `"user"`, `"assistant"`, `"system"`, or `"tool"`. Role values are directly stored in the `role` field of the `Message` class.

**Message Types**:

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

**Run Status**:

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

### 2. Content Models

**Content Type Constants**:

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

**Base Content Model**:

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
     * Type of content part
     */
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    /**
     * Content part identifier
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
     * Index position in message content list
     */
    public Integer getIndex() {
        return index;
    }
    
    public void setIndex(Integer index) {
        this.index = index;
    }
    
    /**
     * Whether this is incremental content
     */
    public Boolean getDelta() {
        return delta;
    }
    
    public void setDelta(Boolean delta) {
        this.delta = delta;
    }
    
    /**
     * Message unique ID
     */
    public String getMsgId() {
        return msgId;
    }
    
    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }
}
```

**Specialized Content Types**:

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
     * Text content
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
     * Image URL details
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
     * Data content
     */
    public Map<String, Object> getData() {
        return data;
    }
    
    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}
```

**Note**: The Java implementation currently supports `TextContent`, `ImageContent`, and `DataContent`. `AudioContent`, `FileContent`, and `RefusalContent` are not yet implemented.

### 3. Event Models

**Base Event Class**:

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
     * Sequence number
     */
    public Integer getSequenceNumber() {
        return sequenceNumber;
    }
    
    public void setSequenceNumber(Integer sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }
    
    /**
     * Object identifier
     */
    public String getObject() {
        return object;
    }
    
    public void setObject(String object) {
        this.object = object;
    }
    
    /**
     * Status: created, in_progress, completed, canceled, failed, rejected
     */
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    /**
     * Error information
     */
    public java.lang.Error getError() {
        return error;
    }
    
    public void setError(java.lang.Error error) {
        this.error = error;
    }
    
    /**
     * Set to created status
     */
    public Event created() {
        this.status = RunStatus.CREATED;
        return this;
    }
    
    /**
     * Set to in-progress status
     */
    public Event inProgress() {
        this.status = RunStatus.IN_PROGRESS;
        return this;
    }
    
    /**
     * Set to completed status
     */
    public Event completed() {
        this.status = RunStatus.COMPLETED;
        return this;
    }
    
    /**
     * Set to rejected status
     */
    public Event rejected() {
        this.status = RunStatus.REJECTED;
        return this;
    }
    
    /**
     * Set to canceled status
     */
    public Event canceled() {
        this.status = RunStatus.CANCELED;
        return this;
    }
}
```

### 4. Message Model

```java
package io.agentscope.runtime.engine.schemas.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import io.agentscope.runtime.engine.memory.model.MessageType;

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
     * Message unique ID
     */
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * Message type
     */
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    /**
     * Message author role, should be "user", "system", "assistant", or "tool"
     */
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    /**
     * Message content list
     */
    public List<Content> getContent() {
        return content;
    }
    
    public void setContent(List<Content> content) {
        this.content = content != null ? content : new ArrayList<>();
    }
    
    /**
     * Message error code
     */
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    /**
     * Message error description
     */
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    /**
     * Usage statistics
     */
    public Map<String, Object> getUsage() {
        return usage;
    }
    
    public void setUsage(Map<String, Object> usage) {
        this.usage = usage;
    }
}
```

**Key Methods**:

+ `addDeltaContent(Content newContent)`: Append partial content to existing message
+ `contentCompleted(int contentIndex)`: Mark content fragment as completed
+ `addContent(Content newContent)`: Add complete content fragment
+ `getTextContent()`: Get text content in message
+ `getImageContent()`: Get list of image URLs in message

### 5. Request Model

**Agent Request**:

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
     * Input message list
     */
    public List<Message> getInput() {
        return input;
    }

    public void setInput(List<Message> input) {
        this.input = input;
    }
    
    /**
     * Whether to use streaming response
     */
    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }
    
    /**
     * Model name
     */
    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
    
    // Other getter and setter methods...
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

### 6. Response Model

**Agent Response**:

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
     * Response unique ID
     */
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * Response object identifier
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
     * Creation timestamp (milliseconds)
     */
    public Long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
    
    /**
     * Completion timestamp (milliseconds)
     */
    public Long getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(Long completedAt) {
        this.completedAt = completedAt;
    }
    
    /**
     * Output message list
     */
    public List<Message> getOutput() {
        return output;
    }
    
    public void setOutput(List<Message> output) {
        this.output = output != null ? output : new ArrayList<>();
    }
    
    /**
     * Usage statistics
     */
    public Map<String, Object> getUsage() {
        return usage;
    }
    
    public void setUsage(Map<String, Object> usage) {
        this.usage = usage;
    }
    
    /**
     * Session ID
     */
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    /**
     * Add new message
     */
    public void addNewMessage(Message message) {
        if (output == null) {
            output = new ArrayList<>();
        }
        output.add(message);
    }
}
```

### 7. Error Model

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
     * Error code
     */
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    /**
     * Error message
     */
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
```

## Protocol Flow

### Request/Response Lifecycle

1. Client sends `AgentRequest`, containing:
   - Input messages
   - Generation parameters
   - Tool definitions
   - Session context
2. Server responds with `AgentResponse` object stream, containing:
   - Status updates (`created` → `in_progress` → `completed`)
   - Output messages with content fragments
   - Final usage metrics

### Content Streaming

When `stream=True` in the request:

+ Text content is sent incrementally as fragments with `delta=true`
+ Each fragment contains an `index` pointing to the target content slot
+ Final fragment is marked as complete via `status=completed`

**Streaming Example**:

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

### Status Transitions

| Status         | Description                       |
| ------------- | -------------------------- |
| `created`     | Initial state when object is created       |
| `in_progress` | Operation is being processed             |
| `completed`   | Operation completed successfully               |
| `failed`      | Operation terminated due to error             |
| `rejected`    | Operation rejected by system             |
| `canceled`    | Operation canceled by user             |
| `unknown`     | Unknown status                   |


## Best Practices

1. **Stream Processing**:
   - Buffer incremental fragments until `status=completed` is received
   - Use `msg_id` to associate content with parent message
   - Respect `index` order for multi-fragment messages
2. **Error Handling**:
   - Check `error` field in responses
   - Monitor `failed` status transitions
   - Implement retry logic for recoverable errors
3. **State Management**:
   - Use `sessionId` to maintain session continuity
   - Track `createdAt`/`completedAt` to monitor latency (timestamp unit is milliseconds)
   - Use `sequenceNumber` for ordering (if implemented)

## Usage Examples

**User Query**:

```json
{
  "input": [{
    "role": "user",
    "content": [{"type": "text", "text": "Describe this image"}],
    "type": "MESSAGE"
  }],
  "stream": true,
  "model": "gpt-4-vision"
}
```

**Agent Response Stream**:

```bash
{"id":"response_123","object":"response","status":"created"}
{"id":"msg_abc","object":"message","type":"MESSAGE","status":"created","role":"assistant"}
{"status":"in_progress","type":"text","index":0,"delta":true,"text":"This","object":"content","msgId":"msg_abc"}
{"status":"in_progress","type":"text","index":0,"delta":true,"text":" image shows...","object":"content","msgId":"msg_abc"}
{"status":"completed","type":"text","index":0,"delta":false,"text":"This image shows...","object":"content","msgId":"msg_abc"}
{"id":"msg_abc","status":"completed","object":"message"}
{"id":"response_123","status":"completed","object":"response"}
```

**Java Code Example**:

```java
import io.agentscope.runtime.engine.schemas.agent.*;
import java.util.ArrayList;
import java.util.List;

// Create request
AgentRequest request = new AgentRequest();
List<Message> input = new ArrayList<>();

Message userMessage = new Message();
userMessage.setRole("user");
userMessage.setType("MESSAGE");

TextContent textContent = new TextContent("Describe this image");
List<Content> contents = new ArrayList<>();
contents.add(textContent);
userMessage.setContent(contents);

input.add(userMessage);
request.setInput(input);
request.setStream(true);
request.setModel("gpt-4-vision");

// Process response
AgentResponse response = new AgentResponse();
response.setSessionId("session_123");
response.created();

Message assistantMessage = new Message("MESSAGE", "assistant");
assistantMessage.created();

TextContent deltaContent = new TextContent(true, "This", 0);
deltaContent.setMsgId(assistantMessage.getId());
deltaContent.inProgress();
assistantMessage.addDeltaContent(deltaContent);

// ... continue adding content ...
```

## Agent API Protocol Usage

In the Java implementation, you can directly use classes and methods to build streaming response data that conforms to the protocol specification.

### 1. Building Response Objects

**Create Response**:

```java
import io.agentscope.runtime.engine.schemas.agent.*;

// Create response object
AgentResponse response = new AgentResponse();
response.setSessionId("session_123");
response.created();  // Set to created status
```

**Create Message**:

```java
// Create message object
Message message = new Message("MESSAGE", "assistant");
message.created();  // Set to created status

// Add to response
response.addNewMessage(message);
```

### 2. Building Content

**Text Content**:

```java
// Create text content
TextContent textContent = new TextContent("Hello World");
textContent.setIndex(0);
textContent.completed();  // Set to completed status

// Add to message
message.addContent(textContent);
```

**Incremental Text Content (Streaming)**:

```java
// Create incremental text content
TextContent deltaContent1 = new TextContent(true, "Hello", 0);
deltaContent1.setMsgId(message.getId());
deltaContent1.inProgress();  // Set to in-progress status
message.addDeltaContent(deltaContent1);

TextContent deltaContent2 = new TextContent(true, " World", 0);
deltaContent2.setMsgId(message.getId());
deltaContent2.inProgress();
message.addDeltaContent(deltaContent2);

// Complete content
message.contentCompleted(0);
```

**Image Content**:

```java
// Create image content
ImageContent imageContent = new ImageContent("https://example.com/image.jpg");
imageContent.setIndex(0);
imageContent.completed();

// Add to message
message.addContent(imageContent);
```

**Data Content**:

```java
import java.util.HashMap;
import java.util.Map;

// Create data content
Map<String, Object> data = new HashMap<>();
data.put("type", "function_call");
data.put("name", "get_weather");
data.put("arguments", "{\"city\": \"Beijing\"}");

DataContent dataContent = new DataContent(data);
dataContent.setIndex(0);
dataContent.completed();

// Add to message
message.addContent(dataContent);
```

### 3. Complete Usage Example

The following example demonstrates how to generate a complete streaming response sequence:

```java
import io.agentscope.runtime.engine.schemas.agent.*;
import java.util.ArrayList;
import java.util.List;

public class StreamingResponseExample {
    public static void generateStreamingResponse(String sessionId, 
                                                  List<String> textTokens, 
                                                  String role) {
        // Create response object
        AgentResponse response = new AgentResponse();
        response.setSessionId(sessionId);
        response.created();
        System.out.println(response);  // Output response creation event
        
        // Create message object
        Message message = new Message("MESSAGE", role);
        message.created();
        response.addNewMessage(message);
        System.out.println(message);  // Output message creation event
        
        // Stream text content
        int index = 0;
        for (String token : textTokens) {
            TextContent deltaContent = new TextContent(true, token, index);
            deltaContent.setMsgId(message.getId());
            deltaContent.inProgress();
            message.addDeltaContent(deltaContent);
            System.out.println(deltaContent);  // Output incremental content event
        }
        
        // Complete content
        message.contentCompleted(index);
        
        // Complete message
        message.completed();
        System.out.println(message);  // Output message completion event
        
        // Complete response
        response.completed();
        response.setCompletedAt(System.currentTimeMillis());
        System.out.println(response);  // Output response completion event
    }
}
```

### 4. Streaming Response Sequence

The standard streaming response sequence includes the following steps:

1. **Response Creation** (`response.created()`)
2. **Message Creation** (`message.created()`)
3. **Content Streaming** (`content.inProgress()` with `delta=true`)
4. **Content Completion** (`content.completed()`)
5. **Message Completion** (`message.completed()`)
6. **Response Completion** (`response.completed()`)

### 5. Supported Content Types

The Java implementation currently supports the following content types:

- **TextContent**: Text content, supports incremental output
- **ImageContent**: Image content, supports URL format
- **DataContent**: Data content, supports arbitrary Map data

**Note**: `AudioContent`, `FileContent`, and `RefusalContent` are not yet provided in the Java implementation.

### 6. Best Practices

1. **State Management**: Ensure states are set in the correct order (created → in_progress → completed)
2. **Content Indexing**: Correctly set index values for multi-content messages
3. **Incremental Output**: Use `addDeltaContent` method to implement streaming text output
4. **Error Handling**: Appropriately handle exceptions during construction
5. **Message ID Association**: Use `setMsgId` method to associate content with parent message

### 7. Advanced Usage

#### Multi-Content Message Construction

```java
// Create message containing text and image
Message message = new Message("MESSAGE", "assistant");

// Add text content
TextContent textContent = new TextContent("This is an image:");
textContent.setIndex(0);
textContent.completed();
message.addContent(textContent);

// Add image content
ImageContent imageContent = new ImageContent("https://example.com/image.jpg");
imageContent.setIndex(1);
imageContent.completed();
message.addContent(imageContent);

// Complete message
message.completed();
```

#### Function Call Data Content

```java
// Create function call
FunctionCall functionCall = new FunctionCall(
    "call_123",
    "get_weather",
    "{\"city\": \"Beijing\"}"
);

// Convert to data content
DataContent dataContent = new DataContent(functionCall.toMap());
dataContent.setIndex(0);
dataContent.completed();

// Add to message
Message message = new Message("FUNCTION_CALL", "assistant");
message.addContent(dataContent);
```

By directly using Java classes and methods, developers can build complex streaming responses that conform to the protocol specification, achieving better user experience and more flexible response control.



