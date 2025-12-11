# AgentScope Usage Example

A minimal, end-to-end example showing how to build and deploy an intelligent agent with AgentScope Runtime Java using a ReAct-style agent, sandboxed tools, and local A2A deployment.

## Features

- Build agents using AgentScope's `ReActAgent`
- Integrate DashScope (Qwen) large language models
- Call tools: Python execution, Shell command execution, browser navigation
- Deploy locally as an A2A application
- Stream responses and show thinking process
- One-click Docker image packaging

## Prerequisites

- Java 17+
- Maven 3.6+
- Environment variable `AI_DASHSCOPE_API_KEY` (Qwen API key)

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
mvn exec:java -Dexec.mainClass="io.agentscope.AgentScopeDeployExample"
```

Or run via Spring Boot plugin:

```bash
mvn spring-boot:run
```

### 4) Test the Deployed Agent

After deployment, the agent listens on `http://localhost:10001`.

```bash
curl --location --request POST 'http://localhost:10001/a2a/' \
--header 'User-Agent: Apifox/1.0.0 (https://apifox.com)' \
--header 'Content-Type: application/json' \
--header 'Accept: */*' \
--header 'Host: localhost:10001' \
--header 'Connection: keep-alive' \
--data-raw '{
  "method": "message/stream",
  "id": "2d2b4dc8-8ea2-437b-888d-3aaf3a8239dc",
  "jsonrpc": "2.0",
  "params": {
    "configuration": {
      "blocking": false
    },
    "message": {
      "role": "user",
      "kind": "message",
      "metadata": {
        "userId": "me",
        "sessionId": "test1"
      },
      "parts": [
        {
          "text": "Hello, please calculate the 10th Fibonacci number using Python.",
          "kind": "text"
        }
      ],
      "messageId": "c4911b64c8404b7a8bf7200dd225b152"
    }
  }
}'
```

## Notes

1. Ensure `AI_DASHSCOPE_API_KEY` is set
2. Default deployment port is `10001` (change in code if needed)
3. The sandbox manager uses default settings; customize via `ManagerConfig` as required
4. This example uses in-memory storage; use persistent storage for production

## Extension Ideas

- Add custom tools
- Configure persistent storage (Redis, OSS, etc.)
- Deploy sandboxes with Kubernetes
- Integrate additional MCP tools

## Related Documentation

- [AgentScope Runtime Java (root)](../../README.md)
- [Examples Overview](../README.md)
