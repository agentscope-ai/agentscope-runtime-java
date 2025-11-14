<div align="center">

# AgentScope Runtime for Java

[![License](https://img.shields.io/badge/license-Apache%202.0-red.svg?logo=apache&label=License)](LICENSE)
[![GitHub Stars](https://img.shields.io/github/stars/agentscope-ai/agentscope-runtime-java?style=flat&logo=github&color=yellow&label=Stars)](https://github.com/agentscope-ai/agentscope-runtime-java/stargazers)
[![GitHub Forks](https://img.shields.io/github/forks/agentscope-ai/agentscope-runtime-java?style=flat&logo=github&color=purple&label=Forks)](https://github.com/agentscope-ai/agentscope-runtime-java/network)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.agentscope/agentscope-runtime/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.agentscope/agentscope-runtime)
[![DingTalk](https://img.shields.io/badge/DingTalk-Join_Us-orange.svg)](https://qr.dingtalk.com/action/joingroup?code=v1,k1,OmDlBXpjW+I2vWjKDsjvI9dhcXjGZi3bQiojOq3dlDw=&_dt_no_comment=1&origin=11)

[Python Cookbook](https://runtime.agentscope.io/)

**AgentScope Runtime Java**

This is the Java implementation of [AgentScope Runtime](https://github.com/agentscope-ai/agentscope-runtime/). 


</div>

---

## ✨ Key Features

- **Deployment Infrastructure**: Built-in services for session management, memory, and sandbox environment control
- **Sandboxed Tool Execution**: Isolated sandboxes ensure safe tool execution without system compromise
- **Developer Friendly**: Simple deployment with powerful customization options
- **Framework Agnostic**: Not tied to any specific framework. Works seamlessly with popular open-source agent frameworks and custom implementations
- 🚧 **Observability**: Trace and visualize agent operations comprehensively

---

## 💬 Community

Join our community on DingTalk:

| DingTalk                                                     |
| ------------------------------------------------------------ |
| <img src="https://img.alicdn.com/imgextra/i1/O1CN01LxzZha1thpIN2cc2E_!!6000000005934-2-tps-497-477.png" width="100" height="100"> |

---

## 📋 Table of Contents

- [Quick Start](#-quick-start)
- [Agent Framework Integration](#-agent-framework-integration)
- [Deployment](#️-deployment)
- [Contributing](#-contributing)
- [License](#-license)

---

## 🚀 Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.6+

### Add Dependency

Add the following dependency to your `pom.xml`:
<!-- Add runtime starter dependency -->
```xml
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>spring-boot-starter-runtime-a2a</artifactId>
    <version>0.1.0</version>
</dependency>

<!-- Add AgentScope Agent adapter dependency -->
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-runtime-agentscope</artifactId>
    <version>0.1.0</version>
</dependency>
```

### Basic Agent Usage Example

The following example demonstrates how to delegate a Spring AI Alibaba ReactAgent using AgentScope Runtime Agent. The complete source code can be found in the [examples](./examples) directory.

```java
public static void main(String[] args) {
    try {
        // Create Spring AI Alibaba ReactAgent Builder
        Builder builder = ReactAgent.builder()
            .name("saa Agent")
            .description("saa Agent")
            .model(chatModel);

        // Create runtime agent proxy using the ReactAgent Builder
        SaaAgent saaAgent = SaaAgent.builder()
            .agent(builder)
            .build();

        // Create Runner with the SaaAgent
        Runner runner = Runner.builder()
            .agent(saaAgent)
            .contextManager(contextManager)
            .build();

        // Create AgentRequest
        AgentRequest request = createAgentRequest("Hello, can you tell me a joke?");

        // Execute the agent and handle the response stream
        Flux<Event> eventStream = runner.streamQuery(request);

        CompletableFuture<Void> completionFuture = new CompletableFuture<>();
        
        eventStream.subscribe(
            this::handleEvent,
            error -> {
                System.err.println("Error occurred: " + error.getMessage());
                completionFuture.completeExceptionally(error);
            },
            () -> {
                System.out.println("Conversation completed.");
                completionFuture.complete(null);
            }
        );

        completionFuture
            .orTimeout(30, TimeUnit.SECONDS)
            .exceptionally(throwable -> {
                System.err.println("Operation failed or timed out: " + throwable.getMessage());
                return null;
            })
            .join();
    } catch (Exception e) {
        e.printStackTrace();
    }
}
```

> [!NOTE]
> The usage method for **AgentScope** is very similar. Please refer to the [examples](./examples) directory for more details.

### Basic Sandbox Usage Example

Developers can configure agents to use specific tools, and the tool execution will be delegated to the sandbox managed by AgentScope Runtime.

```java
Builder builder = ReactAgent.builder()
    .name("saa Agent")
    .description("saa Agent")
    .tools(List.of(ToolcallsInit.RunPythonCodeTool()))
    .model(chatModel);

SaaAgent saaAgent = SaaAgent.builder()
    .agent(builder)
    .build();

Runner runner = Runner.builder()
    .agent(saaAgent)
    .contextManager(contextManager)
    .environmentManager(environmentManager)
    .build();

AgentRequest request = createAgentRequest(
    "Calculate the 10th Fibonacci number using Python for me", 
    null, 
    null
);

Flux<Event> eventStream = runner.streamQuery(request);
```

> [!NOTE]
> You can also use **Kubernetes** or Alibaba FC platform **AgentRun** to execute sandbox tools. Please refer to [this tutorial](https://runtime.agentscope.io/en/sandbox.html) for more details.

---

## 🔌 Agent Framework Integration

AgentScope Runtime Java implementation can be easily integrated with agent frameworks developed in Java. Currently supported frameworks include:

- **AgentScope Java**
- **Spring AI Alibaba**

---

## 🏗️ Deployment

AgentScope Runtime Java can expose agents on a port using the standard A2A protocol or custom endpoints.

To change the port or host, deploy as follows:

```java
LocalDeployManager.builder()
    .port(10001)
    .build()
    .deploy(runner);
```

---

## 🤝 Contributing

We welcome contributions from the community! Here's how you can help:

### 🐛 Bug Reports

- Use [GitHub Issues](https://github.com/agentscope-ai/agentscope-runtime-java/issues) to report bugs
- Include detailed reproduction steps
- Provide system information and relevant logs

### 💡 Feature Requests

- Discuss new ideas in [GitHub Discussions](https://github.com/agentscope-ai/agentscope-runtime-java/discussions)
- Follow the feature request template
- Consider implementation feasibility

### 🔧 Code Contributions

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

For detailed contributing guidelines, please see [CONTRIBUTING.md](CONTRIBUTING.md).

---

## 📄 License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.

---
