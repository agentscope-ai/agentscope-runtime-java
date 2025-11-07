# AgentScope Usage Example

This is a simple AgentScope Agent usage example that demonstrates how to create and deploy an intelligent agent based on ReActAgent using the AgentScope Runtime Java framework.

## Features

- Build agents using AgentScope's `ReActAgent`
- Integrate DashScope large language model (Qwen)
- Support tool calls: Python code execution, Shell command execution, browser navigation
- Deploy as an A2A application using local deployment manager
- Support streaming responses and thinking process

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Environment variable `AI_DASHSCOPE_API_KEY` (Qwen API Key)

## Project Structure

```
agentscope_use_example/
├── pom.xml
├── README.md
└── src/
    └── main/
        └── java/
            └── io/
                └── agentscope/
                    └── AgentScopeDeployExample.java
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
mvn exec:java -Dexec.mainClass="io.agentscope.AgentScopeDeployExample"
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

1. **ReActAgent**: AgentScope's core agent that supports tool calls and reasoning
2. **AgentScopeAgent**: Wraps ReActAgent as an AgentScope Runtime compatible agent
3. **ContextManager**: Manages conversation history and agent memory
4. **SandboxManager**: Manages code execution sandbox environment
5. **Runner**: Execution engine for running agents
6. **LocalDeployManager**: Local deployment manager that deploys agents as A2A applications

### Tool Configuration

The example registers the following tools:

- `RunPythonCodeTool()`: Execute Python code in sandbox
- `RunShellCommandTool()`: Execute Shell commands in sandbox
- `BrowserNavigateTool()`: Browser navigation tool

### Model Configuration

Uses Qwen model (`qwen-plus`), supporting:
- Streaming responses
- Thinking process

## Dependencies

The project depends on the following AgentScope Runtime modules:

- `agentscope-runtime-agentscope`: AgentScope compatibility layer
- `agentscope-runtime-web`: Web service support
- `agentscope-runtime-core`: Core runtime functionality

## Notes

1. Ensure the `AI_DASHSCOPE_API_KEY` environment variable is properly set
2. Default deployment port is `10001`, can be adjusted in code if needed
3. Sandbox manager uses default configuration, can be customized with `ManagerConfig` as needed
4. Example uses in-memory storage, production environments should use persistent storage

## Extension Suggestions

- Add more custom tools
- Configure persistent storage (Redis, OSS, etc.)
- Deploy sandbox using Kubernetes
- Integrate more MCP tools

## Related Documentation

- [AgentScope Runtime Java Main Documentation](../../README.md)
- [Complete Examples Documentation](../README.md)
