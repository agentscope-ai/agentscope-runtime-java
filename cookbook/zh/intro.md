# 欢迎来到AgentScope Runtime Java Cookbook

[![License](https://img.shields.io/badge/license-Apache%202.0-red.svg?logo=apache&label=License)](LICENSE)
[![GitHub Stars](https://img.shields.io/github/stars/agentscope-ai/agentscope-runtime-java?style=flat&logo=github&color=yellow&label=Stars)](https://github.com/agentscope-ai/agentscope-runtime-java/stargazers)
[![GitHub Forks](https://img.shields.io/github/forks/agentscope-ai/agentscope-runtime-java?style=flat&logo=github&color=purple&label=Forks)](https://github.com/agentscope-ai/agentscope-runtime-java/network)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.agentscope/agentscope-runtime/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.agentscope/agentscope-runtime)
[![License](https://img.shields.io/badge/license-Apache%202.0-red.svg?logo=apache&label=License)](https://github.com/agentscope-ai/agentscope-runtime/blob/main/LICENSE)
[![Cookbook](https://img.shields.io/badge/📚_Cookbook-English|中文-teal.svg)](https://runtime.agentscope.io)
[![A2A](https://img.shields.io/badge/A2A-Agent_to_Agent-blue.svg?label=A2A)](https://a2a-protocol.org/)
[![MCP](https://img.shields.io/badge/MCP-Model_Context_Protocol-purple.svg?logo=plug&label=MCP)](https://modelcontextprotocol.io/)
[![DingTalk](https://img.shields.io/badge/DingTalk-Join_Us-orange.svg)](https://qr.dingtalk.com/action/joingroup?code=v1,k1,OmDlBXpjW+I2vWjKDsjvI9dhcXjGZi3bQiojOq3dlDw=&_dt_no_comment=1&origin=11)

## 什么是AgentScope Runtime Java？

**AgentScope Runtime Java** 是一个全面的智能体运行时框架，旨在解决两个关键挑战：**高效的智能体部署**和**沙箱工具执行**。它内置了上下文管理（长短期记忆、外部知识库）和安全沙箱基础设施，提供了一个框架无关的解决方案，可与流行的开源智能体框架和自定义实现配合使用。无论您需要大规模部署智能体还是确保安全的工具交互，AgentScope Runtime 都能提供具有完整可观测性和开发者友好部署的核心基础设施。

本指南将指导您使用 **AgentScope Runtime Java** 构建服务级的智能体应用程序。

## 双核心架构

**⚙️ 智能体部署运行时 (Engine)**

用于部署、管理和运行智能体应用程序的基础设施，内置上下文管理（长短期记忆、外部知识库）和环境沙箱控制服务。

**🛠️ 工具执行运行时 (Sandbox)**

安全隔离的环境，让您的智能体能够安全地执行工具、控制浏览器、管理文件并集成MCP 工具- 所有这些都不会危及您的系统安全。

## 为什么选择 AgentScope Runtime Java？

- **🏗️ 部署基础设施**：内置会话管理、内存和沙箱环境控制服务
- **🔒 沙箱工具执行**：隔离的沙箱确保工具安全执行，不会危及系统
- **🔧框架无关**：不绑定特定框架，与流行的开源智能体框架和自定义实现无缝配合
- ⚡ **开发者友好**：简单部署，功能强大的自定义选项
- **📊 可观测性**：针对运行时操作的全面追踪和监控
- **☕ Java 运行时优势**：依托成熟、高性能、高可靠性的 Java 生态，天然支持跨平台部署、企业级稳定性、丰富的诊断工具（JFR/JMX）以及强大的并发与内存管理能力，特别适合构建长期运行、高可用的智能体服务。

立即开始使用 AgentScope Runtime Java 部署你的智能体并尝试工具沙箱吧！
