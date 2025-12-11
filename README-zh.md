<div align="center">

# AgentScope Runtime for Java

[![License](https://img.shields.io/badge/license-Apache%202.0-red.svg?logo=apache&label=License)](LICENSE)
[![GitHub Stars](https://img.shields.io/github/stars/agentscope-ai/agentscope-runtime-java?style=flat&logo=github&color=yellow&label=Stars)](https://github.com/agentscope-ai/agentscope-runtime-java/stargazers)
[![GitHub Forks](https://img.shields.io/github/forks/agentscope-ai/agentscope-runtime-java?style=flat&logo=github&color=purple&label=Forks)](https://github.com/agentscope-ai/agentscope-runtime-java/network)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.agentscope/agentscope-runtime/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.agentscope/agentscope-runtime)
[![DingTalk](https://img.shields.io/badge/DingTalk-Join_Us-orange.svg)](https://qr.dingtalk.com/action/joingroup?code=v1,k1,OmDlBXpjW+I2vWjKDsjvI9dhcXjGZi3bQiojOq3dlDw=&_dt_no_comment=1&origin=11)

[Cookbook](./cookbook/zh)

**AgentScope Runtime Java**

这是 [AgentScope Runtime](https://github.com/agentscope-ai/agentscope-runtime/) 的 Java 实现。


</div>

---

## ✨ 核心特性

- **部署基础设施**：内置会话管理、内存和沙箱环境控制服务
- **沙箱化工具执行**：隔离的沙箱确保工具执行安全，不会危及系统
- **开发者友好**：简单部署，强大的自定义选项
- **框架无关**：不绑定任何特定框架。可与流行的开源 Agent 框架和自定义实现无缝协作
- 🚧 **可观测性**：全面追踪和可视化 Agent 操作

---

## 💬 社区

加入我们的钉钉社区：

| DingTalk                                                     |
| ------------------------------------------------------------ |
| <img src="https://img.alicdn.com/imgextra/i1/O1CN01LxzZha1thpIN2cc2E_!!6000000005934-2-tps-497-477.png" width="100" height="100"> |

---

## 📋 目录

- [快速开始](#-快速开始)
- [Agent 框架集成](#-agent-框架集成)
- [部署](#️-部署)
- [贡献](#-贡献)
- [许可证](#-许可证)

---

## 🚀 快速开始

### 前置要求

- Java 17 或更高版本
- Maven 3.6+

### 添加依赖

在您的 `pom.xml` 中添加以下依赖：
<!-- Add runtime starter dependency -->
```xml
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>spring-boot-starter-runtime-a2a</artifactId>
    <version>1.0.0-BETA1</version>
</dependency>

<!-- Add AgentScope Agent adapter dependency -->
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-runtime-agentscope</artifactId>
    <version>1.0.0-BETA1</version>
</dependency>
```

### 基础 Agent 使用示例

以下示例演示如何使用 AgentScope Runtime 委托一个 AgentScope ReactAgent。完整源代码可在 [examples](./examples) 目录中找到。

1. 创建 Agent Handler

通过扩展 `AgentScopeAgentHandler` 创建自定义 agent handler：

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

2. 初始化服务并部署

使用所需服务配置 agent handler，并使用 `AgentApp` 进行部署：

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
> 您也可以使用 **Kubernetes** 或阿里云 FC 平台的 **AgentRun** 来执行沙箱工具。更多详情请参考 [examples](./examples) 目录。
---

## 🔌 Agent 框架集成

AgentScope Runtime Java 实现可以轻松集成任何用 Java 开发的 Agent 框架。目前支持的框架包括：

- **AgentScope Java**
- **Spring AI Alibaba, Langchain4j 以及更多即将推出...**

---

## 🤝 贡献

我们欢迎社区贡献！以下是如何提供帮助：

### 🐛 错误报告

- 使用 [GitHub Issues](https://github.com/agentscope-ai/agentscope-runtime-java/issues) 报告错误
- 包含详细的复现步骤
- 提供系统信息和相关日志

### 💡 功能请求

- 在 [GitHub Discussions](https://github.com/agentscope-ai/agentscope-runtime-java/discussions) 中讨论新想法
- 遵循功能请求模板
- 考虑实现可行性

### 🔧 代码贡献

1. Fork 仓库
2. 创建功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 打开 Pull Request

详细的贡献指南，请参阅 [CONTRIBUTING.md](CONTRIBUTING.md)。

---

## 📄 许可证

本项目采用 Apache License 2.0 许可证。详情请参阅 [LICENSE](LICENSE) 文件。

---
