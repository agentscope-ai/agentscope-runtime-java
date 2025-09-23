<div align="center">

# AgentScope Runtime for Java

[![License](https://img.shields.io/badge/license-Apache%202.0-red.svg?logo=apache&label=Liscnese)](LICENSE)
[![GitHub Stars](https://img.shields.io/github/stars/agentscope-ai/agentscope-runtime?style=flat&logo=github&color=yellow&label=Stars)](https://github.com/agentscope-ai/agentscope-runtime-java/stargazers)
[![GitHub Forks](https://img.shields.io/github/forks/agentscope-ai/agentscope-runtime?style=flat&logo=github&color=purple&label=Forks)](https://github.com/agentscope-ai/agentscope-runtime-java/network)
[![MCP](https://img.shields.io/badge/MCP-Model_Context_Protocol-purple.svg?logo=plug&label=MCP)](https://modelcontextprotocol.io/)
[![Maven central](https://maven-badges.herokuapp.com/maven-central/io.agentscope/agentscope-runtime/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.agentscope/agentscope-runtime)
[![DingTalk](https://img.shields.io/badge/DingTalk-Join_Us-orange.svg)](https://qr.dingtalk.com/action/joingroup?code=v1,k1,OmDlBXpjW+I2vWjKDsjvI9dhcXjGZi3bQiojOq3dlDw=&_dt_no_comment=1&origin=11)

[[Python Cookbook]](https://runtime.agentscope.io/)

**AgentScope Runtime Java**

This is the java implementation of [AgentScope Runtime](https://github.com/agentscope-ai/agentscope-runtime/). Please notice that this project is still experimental and under active development.

</div>

---

## ✨ Key Features

- **Deployment Infrastructure**: Built-in services for session management, memory, and sandbox environment control
- **Sandboxed Tool Execution**: Isolated sandboxes ensure safe tool execution without system compromise
- **Developer Friendly**: Simple deployment with powerful customization options
- :construction: **Framework Agnostic**: Not tied to any specific framework. Works seamlessly with popular open-source agent frameworks and custom implementations
- :construction: **Observability**: Comprehensive tracing and monitoring for runtime operations

---

## 💬 Contact

Welcome to join our community on

| DingTalk                                                     |
| ------------------------------------------------------------ |
| <img src="https://img.alicdn.com/imgextra/i1/O1CN01LxzZha1thpIN2cc2E_!!6000000005934-2-tps-497-477.png" width="100" height="100"> |

---

## 📋 Table of Contents

- [🚀 Quick Start](#-quick-start)
- [🔌 Agent Framework Integration](#-agent-framework-integration)
- [🏗️ Deployment](#️-deployment)
- [🤝 Contributing](#-contributing)
- [📄 License](#-license)

---

## 🚀 Quick Start

### Prerequisites
- Java 17+

### Build from Source

```shell
mvn clean install -DskipTests
```

### Add Dependency

```xml
<dependency>
	<groupId>io.agentscope</groupId>
	<artifactId>agentscope-runtime</artifactId>
	<version>0.1.0-SNAPSHOT</version>
</dependency>
```

### Basic Agent Usage Example
The source code of the following example can be found in the [examples](./examples) directory.

This example demonstrates how to delegate a simple Spring AI Alibaba ReactAgent using AgentScope Runtime Agent.

```java
public static void main(String[] args) {
	try {
		// Create Spring AI Alibaba ReactAgent Builder
		Builder builder = ReactAgent.builder()
			.name("saa_agent")
			.model(chatModel);

		// Create runtime agent proxy using the ReactAgent Builder
		SaaAgent saaAgent = SaaAgent.builder()
			.name("saa_agent_proxy")
			.description("An agent powered by Spring AI Alibaba ReactAgent")
			.reactAgentBuilder(builder)
			.build();

		// Create Runner with the SaaAgent
		Runner runner = new Runner(saaAgent, contextManager);

		// Create AgentRequest
		AgentRequest request = createAgentRequest("Hello, can you tell me a joke?");

		// Execute the agent and handle the response stream
		Flux<Event> eventStream = runner.streamQuery(request);

		eventStream.subscribe(
			event -> handleEvent(event),
			error -> System.err.println("Error occurred: " + error.getMessage()),
			() -> System.out.println("Conversation completed.")
		);

		// Wait a bit for async execution (in real applications, you'd handle this properly)
		Thread.sleep(5000);
	} catch (Exception e) {
		e.printStackTrace();
	}
}
```

> [!NOTE]
> Supporting for AgentScope and other agents coming soon ...

### Basic Sandbox Usage Example

Developers can tell the agent to use a specific tool and the execution of the tool will be delegated to the sandbox managed by AgentScope Runtime.

```java
SaaAgent saaAgent = SaaAgent.builder()
	.name("saa_agent_proxy")
	.tools(List.of("run_python"))
	.description("An agent powered by Spring AI Alibaba ReactAgent.")
	.reactAgentBuilder(builder) //
	.build();

Runner runner = new Runner(saaAgent, contextManager);
AgentRequest request = createAgentRequest("What is the 8th number of Fibonacci?");
Flux<Event> eventStream = runner.streamQuery(request);
```

> [!NOTE]
>
> Current version requires Docker to be installed and running on your system. Please refer to [this tutorial](https://runtime.agentscope.io/en/sandbox.html) for more details.

## 🔌 Agent Framework Integration

AgentScope Runtime Java implementation currently can automatically load Agents developed using Spring AI Alibaba. More agent framework integrations coming soon!

---

## 🏗️ Deployment

AgentScope Java can expose Agents on a port in the form of standard A2A protocol.

Change the port through the `application.yml` file:

```yaml
server:
  port: 8090
```

Run `io.agentscope.runtime.engine.deployer.LocalDeployer` to start the A2A server.

---

## 🤝 Contributing

We welcome contributions from the community! Here's how you can help:

### 🐛 Bug Reports
- Use GitHub Issues to report bugs
- Include detailed reproduction steps
- Provide system information and logs

### 💡 Feature Requests
- Discuss new ideas in GitHub Discussions
- Follow the feature request template
- Consider implementation feasibility

### 🔧 Code Contributions
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

For detailed contributing guidelines, please see  [CONTRIBUTE](CONTRIBUTING.md).

---
