# SAA Agent Usage Example

A concise, end-to-end example showing how to build and deploy an intelligent agent with AgentScope Runtime Java using Spring AI Alibaba's ReactAgent, sandboxed tools, and local A2A deployment.

## Features

- Build agents using Spring AI Alibaba `ReactAgent`
- Integrate DashScope (Qwen) large language models
- Invoke tools: Python execution, Shell command execution, browser navigation and file
- Deploy locally as an A2A application
- Manage sandbox runtime environment

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Environment variable `AI_DASHSCOPE_API_KEY` (Qwen API key)
- AgentScope Runtime Java built and installed locally

## Project Structure

```
saa_use_example/
├── pom.xml
├── README.md
└── src/
    └── main/
        └── java/
            └── io/
                └── saa/
                    └── SaaAgentSandboxToolDeployExample.java
```

## Quick Start

### 1) Set Environment Variables

```bash
export AI_DASHSCOPE_API_KEY="your-dashscope-api-key"
```

### 2) Build the Project

```bash
mvn clean compile
```

### 3) Run the Example

```bash
mvn exec:java -Dexec.mainClass="io.saa.SaaAgentSandboxToolDeployExample"
```

Or run via Spring Boot:

```bash
mvn spring-boot:run
```

### 4) Test the Deployed Agent

After deployment, the agent listens on `http://localhost:10001`.

```bash
curl --location --request POST 'http://localhost:10001/a2a/' \
  --header 'Content-Type: application/json' \
  --data-raw '{
    "method": "message/stream",
    "id": "test-request-id",
    "jsonrpc": "2.0",
    "params": {
      "message": {
        "role": "user",
        "kind": "message",
        "contextId": "test-context",
        "metadata": {
          "userId": "test-user",
          "sessionId": "test-session"
        },
        "parts": [
          {
            "text": "Hello, please calculate the 10th Fibonacci number using Python",
            "kind": "text"
          }
        ],
        "messageId": "test-message-id"
      }
    }
  }'
```

## Code Overview

### Main Components

1. **ReactAgent**: Spring AI Alibaba agent that supports tool use and reasoning
2. **SaaAgent**: Wraps ReactAgent to make it compatible with AgentScope Runtime
3. **ContextManager**: Manages conversation history and memory
4. **SandboxManager**: Manages the sandbox environment (Docker by default)
5. **Runner**: Orchestrates agent execution
6. **LocalDeployManager**: Exposes the agent as an A2A application

### Tool Configuration

This example registers the following tools:

- `RunPythonCodeTool()`: Execute Python code in a sandbox
- `RunShellCommandTool()`: Execute shell commands in a sandbox
- `BrowserNavigateBackTool()`: Navigate the browser inside a sandbox

More built-in sandbox tools are available—see `ToolcallsInit.java` and related classes.

### Sandbox Configuration

The default container runtime is Docker. You can customize the runtime via `ManagerConfig`:

- Deploy using Kubernetes (requires `KubernetesClientConfig`)
- Configure container pool size
- Configure port range
- Configure file system storage (local file system or OSS)

## Dependencies

This example depends on:

- **Spring AI Alibaba**: Spring AI implementation for Alibaba Cloud
- **Jackson**: JSON serialization/deserialization
- **AgentScope Runtime Modules**:
  - `agentscope-runtime-saa`: SAA compatibility layer
  - `agentscope-runtime-web`: Web service support
  - `agentscope-runtime-core`: Core runtime functionality

## Differences from AgentScope Example

| Feature | SAA Example | AgentScope Example |
|--------|-------------|--------------------|
| Agent Type | Spring AI Alibaba ReactAgent | AgentScope ReActAgent |
| Tool Initialization | `ToolcallsInit` | `ToolkitInit` |
| Model Configuration | DashScope ChatModel | AgentScope DashScopeChatModel |
| Use Case | Spring AI ecosystem integration | AgentScope-native features |

## Notes

1. Ensure `AI_DASHSCOPE_API_KEY` is set
2. Default deployment port is `10001` (change in code if needed)
3. Sandbox manager defaults to Docker—ensure Docker is installed and running
4. The example uses in-memory storage; use persistent storage (Redis, OSS, etc.) in production

## Extension Ideas

- Add more custom tools (including MCP tools)
- Configure persistent storage (Redis, OSS, etc.)
- Use Kubernetes for sandbox deployment
- Integrate additional Spring AI features

## Related Documentation

- [AgentScope Runtime Java (root)](../../README.md)
- [Examples Overview](../README.md)
- [Spring AI Alibaba Documentation](https://github.com/alibaba/spring-ai-alibaba)
