# SAA Agent Usage Example

This is a simple SAA (Spring AI Alibaba) Agent usage example that demonstrates how to create and deploy an intelligent agent based on Spring AI Alibaba ReactAgent using the AgentScope Runtime Java framework.

## Features

- Build agents using Spring AI Alibaba's `ReactAgent`
- Integrate DashScope large language model (Qwen)
- Support tool calls: Python code execution, Shell command execution, browser navigation
- Deploy as an A2A application using local deployment manager
- Support sandbox environment management

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Environment variable `AI_DASHSCOPE_API_KEY` (Qwen API Key)

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

### 1. Set Environment Variables

```bash
export AI_DASHSCOPE_API_KEY="your-dashscope-api-key"
```

### 2. Build the Project

```bash
mvn clean compile
```

### 3. Run the Example

```bash
mvn exec:java -Dexec.mainClass="io.saa.SaaAgentSandboxToolDeployExample"
```

Or use the Spring Boot plugin to run:

```bash
mvn spring-boot:run
```

### 4. Test the Deployed Agent

After successful deployment, the Agent will run on `http://localhost:10001`. You can test it using the following curl command:

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

## Code Explanation

### Main Components

1. **ReactAgent**: Spring AI Alibaba's agent implementation
2. **SaaAgent**: Wraps ReactAgent as an AgentScope Runtime compatible agent
3. **ContextManager**: Manages conversation history and agent memory
4. **SandboxManager**: Manages code execution sandbox environment (defaults to Docker)
5. **Runner**: Execution engine for running agents
6. **LocalDeployManager**: Local deployment manager that deploys agents as A2A applications

### Tool Configuration

The example registers the following tools:

- `RunPythonCodeTool()`: Execute Python code in sandbox
- `RunShellCommandTool()`: Execute Shell commands in sandbox
- `BrowserNavigateBackTool()`: Browser navigation tool

### Sandbox Configuration

Defaults to Docker as the container management framework, can be customized via `ManagerConfig`:

- Deploy using Kubernetes (requires `KubernetesClientConfig` configuration)
- Configure container pool size
- Configure port range
- Configure file system storage (local file system or OSS)

## Dependencies

The project depends on the following components:

- **Spring AI Alibaba**: Alibaba Cloud implementation of Spring AI
- **Jackson**: JSON serialization/deserialization
- **AgentScope Runtime Modules**:
  - `agentscope-runtime-saa`: SAA compatibility layer
  - `agentscope-runtime-web`: Web service support
  - `agentscope-runtime-core`: Core runtime functionality

## Differences from AgentScope Example

| Feature | SAA Example | AgentScope Example |
|---------|-------------|-------------------|
| Agent Type | Spring AI Alibaba ReactAgent | AgentScope ReActAgent |
| Tool Initialization | `ToolcallsInit` | `ToolkitInit` |
| Model Configuration | DashScope ChatModel | AgentScope DashScopeChatModel |
| Use Case | Integration with Spring AI ecosystem | Using AgentScope native features |

## Notes

1. Ensure the `AI_DASHSCOPE_API_KEY` environment variable is properly set
2. Default deployment port is `10001`, can be adjusted in code if needed
3. Sandbox manager defaults to Docker, ensure Docker is installed and running
4. Example uses in-memory storage, production environments should use persistent storage (Redis, OSS, etc.)

## Extension Suggestions

- Add more custom tools (including MCP tools)
- Configure persistent storage (Redis, OSS, etc.)
- Deploy sandbox using Kubernetes
- Integrate more Spring AI features

## Related Documentation

- [AgentScope Runtime Java Main Documentation](../../README.md)
- [Complete Examples Documentation](../README.md)
- [Spring AI Alibaba Documentation](https://github.com/alibaba/spring-ai-alibaba)
