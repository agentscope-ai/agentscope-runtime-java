<div align="center">

# AgentScope Runtime for Java

[![License](https://img.shields.io/badge/license-Apache%202.0-red.svg?logo=apache&label=License)](LICENSE)
[![GitHub Stars](https://img.shields.io/github/stars/agentscope-ai/agentscope-runtime-java?style=flat&logo=github&color=yellow&label=Stars)](https://github.com/agentscope-ai/agentscope-runtime-java/stargazers)
[![GitHub Forks](https://img.shields.io/github/forks/agentscope-ai/agentscope-runtime-java?style=flat&logo=github&color=purple&label=Forks)](https://github.com/agentscope-ai/agentscope-runtime-java/network)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.agentscope/agentscope-runtime/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.agentscope/agentscope-runtime)
[![DingTalk](https://img.shields.io/badge/DingTalk-Join_Us-orange.svg)](https://qr.dingtalk.com/action/joingroup?code=v1,k1,OmDlBXpjW+I2vWjKDsjvI9dhcXjGZi3bQiojOq3dlDw=&_dt_no_comment=1&origin=11)

[Cookbook](./cookbook/zh)

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
    <version>0.1.1</version>
</dependency>

<!-- Add AgentScope Agent adapter dependency -->
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-runtime-agentscope</artifactId>
    <version>0.1.1</version>
</dependency>
```

### Basic Agent Usage Example

The following example demonstrates how to delegate a AgentScope ReactAgent using AgentScope Runtime Agent. The complete source code can be found in the [examples](./examples) directory.

1. Create Agent

```java
import io.agentscope.core.ReActAgent;
import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.runtime.engine.agents.agentscope.AgentScopeAgent;
import io.agentscope.runtime.engine.agents.agentscope.tools.ToolkitInit;
import io.agentscope.core.model.DashScopeChatModel;

// Create ReActAgent
ReActAgent.Builder agentBuilder = ReActAgent.builder()
    .name("Friday")
    .sysPrompt("You're a helpful assistant named Friday.")
    .memory(new InMemoryMemory())
    .model(DashScopeChatModel.builder()
        .apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
        .modelName("qwen-turbo")
        .stream(true)
        .enableThinking(true)
        .formatter(new DashScopeChatFormatter())
        .build());

// Create Runtime AgentScopeAgent
AgentScopeAgent agentScopeAgent = AgentScopeAgent.builder()
    .agent(agentBuilder)
    .build();

System.out.println("✅ AgentScope agent created successfully");
```

2. Configure Sandbox Manager (Optional but Recommended)

```java
// Create Toolkit
Toolkit toolkit = new Toolkit();
toolkit.registerTool(ToolkitInit.RunPythonCodeTool());
toolkit.registerTool(ToolkitInit.RunShellCommandTool());

// Add Tools for Agent
// ...
agentBuilder.toolkit(toolkit)
// ...

// Create sandbox manager configuration (using default Docker configuration)
ManagerConfig managerConfig = ManagerConfig.builder().build();
// Create environment manager
EnvironmentManager environmentManager = new DefaultEnvironmentManager(new SandboxManager(managerConfig));
```

> [!NOTE]
> You can also use **Kubernetes** or Alibaba FC platform **AgentRun** to execute sandbox tools. Please refer to [this tutorial](https://runtime.agentscope.io/en/sandbox.html) for more details.

3. Create Runner

The Runner combines the agent, context manager, and environment manager:

```java
import io.agentscope.runtime.engine.Runner;

Runner runner = Runner.builder()
    .agent(agentScopeAgent)  // or saaAgent
    .contextManager(contextManager)
    .environmentManager(environmentManager)  // Required if using sandbox tools
    .build();

System.out.println("✅ Runner created successfully");
```

4. Deploy Agent

Use `LocalDeployManager` to deploy the agent as an A2A service:

```java
import io.agentscope.runtime.LocalDeployManager;

// Deploy agent (default port 8080)
LocalDeployManager.builder()
    .port(8090)
    .build()
    .deploy(runner);

System.out.println("✅ Agent deployed successfully on port 8090");
```

> [!NOTE]
> The usage method for **Spring AI Alibaba** is very similar. Please refer to the [examples](./examples) directory for more details.
---

## 🔌 Agent Framework Integration

AgentScope Runtime Java implementation can be easily integrated with any agent frameworks developed in Java. Currently supported frameworks include:

- **AgentScope Java**
- **Spring AI Alibaba**
- **Langchain4j and more coming soon...**
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
