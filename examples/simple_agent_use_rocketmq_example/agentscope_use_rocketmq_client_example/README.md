# AgentScope Usage Example For Client

This directory contains example demonstrating the Agent-to-Agent (A2A) protocol implementation with RocketMQ integration in AgentScope.

## Prerequisites

- Java 17+
- Maven 3.6+

## Basic preliminary preparations

### Deploy Apache RocketMQ

Deploy the LiteTopic version of [Apache RocketMQ](http://rocketmq.apache.org/) (the open-source version is expected to be released by the end of December), or purchase a commercial RocketMQ instance that supports LiteTopic, and create the following resources:
- **1.1** Create a LiteTopic: `WorkerAgentResponse`
- **1.2** Create a bound Lite consumer group ID for WorkerAgentResponse: `CID_HOST_AGENT_LITE`

## Quick Start

| Parameter Name  | Description             | Required |
|-------|------------------|------|
| rocketMQNamespace | RocketMQ namespace     | No    |
| rocketMQAK | RocketMQ access key       | No    |
| rocketMQSK | RocketMQ secret key       | No    |
| workAgentResponseTopic | LiteTopic        | Yes    |
| workAgentResponseGroupID | LiteConsumer CID | Yes    |

### Build the Project

```bash
mvn clean compile
```

### Run the Client Example

```bash
mvn compile exec:java -Dexec.mainClass=io.agentscope.A2aAgentCallerExample -DrocketMQNamespace= -DworkAgentResponseTopic=WorkerAgentResponse -DworkAgentResponseGroupID=CID_HOST_AGENT_LITE -DrocketMQAK= -DrocketMQSK= 
```

## Contributing

If you'd like to contribute to these examples or report issues, please submit a pull request or open an issue in the repository.
