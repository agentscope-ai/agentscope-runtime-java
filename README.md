<div align="center">

# AgentScope Runtime for Java

[![License](https://img.shields.io/badge/license-Apache%202.0-red.svg?logo=apache&label=License)](LICENSE)
[![GitHub Stars](https://img.shields.io/github/stars/agentscope-ai/agentscope-runtime-java?style=flat&logo=github&color=yellow&label=Stars)](https://github.com/agentscope-ai/agentscope-runtime-java/stargazers)
[![GitHub Forks](https://img.shields.io/github/forks/agentscope-ai/agentscope-runtime-java?style=flat&logo=github&color=purple&label=Forks)](https://github.com/agentscope-ai/agentscope-runtime-java/network)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.agentscope/agentscope-runtime/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.agentscope/agentscope-runtime)
[![DingTalk](https://img.shields.io/badge/DingTalk-Join_Us-orange.svg)](https://qr.dingtalk.com/action/joingroup?code=v1,k1,OmDlBXpjW+I2vWjKDsjvI9dhcXjGZi3bQiojOq3dlDw=&_dt_no_comment=1&origin=11)

[[Cookbook]](./cookbook/zh)
[[中文README]](./README_zh.md)
[[Examples]](./examples)

**AgentScope Runtime Java**

This is the Java implementation of [AgentScope Runtime](https://github.com/agentscope-ai/agentscope-runtime/). 


</div>

---

## ✨ Key Features

- **Deployment Infrastructure**: Built-in services for session management, memory, and sandbox environment control
- **Sandboxed Tool Execution**: Isolated sandboxes ensure safe tool execution without system compromise
- **Developer Friendly**: Simple deployment with powerful customization options
- **Framework Agnostic**: Not tied to any specific framework. Works seamlessly with popular open-source agent frameworks and custom implementations
- 🚧 **Observability**: Trace and visualize agent operations comprehensively (under development)

---

## 💬 Community

Join our community on DingTalk:

| DingTalk                                                     |
| ------------------------------------------------------------ |
| <img src="https://img.alicdn.com/imgextra/i1/O1CN01LxzZha1thpIN2cc2E_!!6000000005934-2-tps-497-477.png" width="100" height="100"> |

---

## 📋 Table of Contents

- [Quick Start](#-quick-start)
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
    <version>1.0.0</version>
</dependency>

<!-- Add AgentScope Agent adapter dependency -->
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-runtime-agentscope</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Basic Agent Usage Example

The following example demonstrates how to delegate a AgentScope ReactAgent using AgentScope Runtime. The complete source code can be found in the [examples](./examples) directory.

1. Create Agent Handler

Create a custom agent handler by extending `AgentScopeAgentHandler`:

```java
public class MyAgentScopeAgentHandler extends AgentScopeAgentHandler {
    
    @Override
    public Flux<io.agentscope.core.agent.Event> streamQuery(AgentRequest request, Object messages) {
        // Create Toolkit and register tools
        Toolkit toolkit = new Toolkit();
        if (sandboxService != null) {
            Sandbox sandbox = sandboxService.connect(
                request.getUserId(), 
                request.getSessionId(), 
                BaseSandbox.class
            );
            toolkit.registerTool(ToolkitInit.RunPythonCodeTool(sandbox));
        }
        
        // Create ReActAgent with tools
        ReActAgent agent = ReActAgent.builder()
            .name("Friday")
            .toolkit(toolkit)
            .model(DashScopeChatModel.builder()
                .apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
                .modelName("qwen-max")
                .stream(true)
                .formatter(new DashScopeChatFormatter())
                .build())
            .build();
        
        // Convert messages and stream agent responses
        // See examples/README.md for complete implementation
        return agent.stream(queryMessage, streamOptions);
    }
}
```

2. Initialize Services and Deploy

Configure the agent handler with required services and deploy using `AgentApp`:

```java
// Create and configure the agent handler
MyAgentScopeAgentHandler agentHandler = new MyAgentScopeAgentHandler();
agentHandler.setStateService(new InMemoryStateService());
agentHandler.setSessionHistoryService(new InMemorySessionHistoryService());
agentHandler.setMemoryService(new InMemoryMemoryService());
agentHandler.setSandboxService(new SandboxService(
    new SandboxManager(ManagerConfig.builder().build())
));

// Deploy using AgentApp
AgentApp agentApp = new AgentApp(agentHandler);
agentApp.run(8090); // Server will listen on port 8090
```

> [!NOTE]
> You can also use **Kubernetes** or Alibaba FC platform **AgentRun** to execute sandbox tools. Please refer to the [examples](./examples) directory for more details.
---

## 🔌 Agent Framework Integration

AgentScope Runtime Java implementation can be easily integrated with any agent frameworks developed in Java. Currently supported frameworks include:

- **AgentScope Java**
- **Spring AI Alibaba, Langchain4j and more coming soon...**

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
