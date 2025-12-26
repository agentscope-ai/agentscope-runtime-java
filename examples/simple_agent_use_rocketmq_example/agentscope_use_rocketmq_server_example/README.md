# AgentScope Usage Example For Server

A minimal, end-to-end example showing how to build and deploy an intelligent agent with AgentScope Runtime Java using a ReAct-style agent, sandboxed tools, and local A2A deployment.

## Features

- Build agents using AgentScope's `ReActAgent`
- Integrate DashScope (Qwen) large language models
- Use RocketMQ for asynchronous communication between agents
- Call tools: Python execution, Shell command execution, browser navigation
- Deploy locally as an A2A application
- Stream responses and show thinking process
- One-click Docker image packaging

## Prerequisites

- Java 17+
- Maven 3.6+

## Basic preliminary preparations

### 1. Deploy Apache RocketMQ

Deploy the LiteTopic version of [Apache RocketMQ](http://rocketmq.apache.org/) (the open-source version is expected to be released by the end of December), or purchase a commercial RocketMQ instance that supports LiteTopic, and create the following resources:
- **1.1** Create a standard topic for the AI assistant: `AgentTask`
- **1.2** Create a standard consumer group ID for the AI assistant: `AgentTaskConsumerGroup`

### 2. Get Qwen API key
- Go to the Bailian platform to obtain the corresponding Qwen API key.

## Quick Start

| Parameter Name  | Description             | Required |
|-------|------------------|------|
| rocketMQEndpoint | RocketMQ service endpoint    | Yes    |
| rocketMQNamespace | RocketMQ namespace     | No    |
| bizTopic | Standard topic          | Yes    |
| bizConsumerGroup | Standard consumer group ID (CID)         | Yes    |
| rocketMQAK | RocketMQ access key       | No    |
| rocketMQSK | RocketMQ secret key       | No    |
| apiKey | API key for calling Bailian platform     | Yes    |

### 1.Build the Project

```bash
mvn clean compile
```

### 2.Run the Example

```bash
mvn exec:java -Dexec.mainClass=io.agentscope.AgentScopeDeployRocketMQExample -DrocketMQEndpoint= -DrocketMQNamespace= -DbizTopic=AgentTask -DbizConsumerGroup=AgentTaskConsumerGroup -DrocketMQAK= -DrocketMQSK= -DapiKey=
```

### 3.Test the Deployed Agent
After deployment, the agent listens on `http://localhost:10001`.
Query AgentCard Info, `http://localhost:10001/.well-known/agent-card.json`

## Notes

1. Default deployment port is `10001` (change in code if needed)
2. The sandbox manager uses default settings; customize via `ManagerConfig` as required
3. This example uses in-memory storage; use persistent storage for production

## Extension Ideas

- Add custom tools
- Configure persistent storage (Redis, OSS, etc.)
- Deploy sandboxes with Kubernetes
- Integrate additional MCP tools

## Related Documentation

- [AgentScope Runtime Java (root)](../../README.md)
- [Examples Overview](../README.md)
