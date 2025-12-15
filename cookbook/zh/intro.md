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

## AgentScope Runtime V1.0 发布

AgentScope Runtime Java V1.0 在高效智能体部署与安全沙箱执行的坚实基础上，推出了 **统一的 “Agent 作为 API” 开发体验**，覆盖完整智能体从本地开发到生产部署的生命周期，并扩展了更多沙箱类型、协议兼容性与更丰富的内置工具集。

同时，智能体服务的接入方式从过去的 **黑盒化模块替换** 升级为 ***白盒化适配器模式*** —— 开发者可以在保留原有智能体框架接口与行为的前提下，将状态管理、会话记录、工具注册等运行时能力按需嵌入应用生命周期，实现更灵活的定制与跨框架无缝集成。

**V1.0 主要改进：**

- **统一的开发/生产范式** —— 在开发环境与生产环境中 智能体功能性保持一致
- **原生多智能体支持** —— 完全兼容 AgentScope Java 的多智能体范式
- **主流 SDK 与协议集成** —— 支持 OpenAI Responses API SDK 与 Google A2A 协议
- **可视化 Web UI** —— 部署后即可立即体验的开箱即用 Web 聊天界面
- **扩展沙箱类型** —— GUI、浏览器、文件系统（大部分可通过 VNC 可视化）
- **更丰富的内置工具** —— 面向生产的搜索、RAG、AIGC、支付等模块
- **灵活的部署模式** —— 本地线程/进程、Docker、Kubernetes、或托管云端

更详细的变更说明，以及迁移指南请参考：[CHANGELOG](CHANGELOG.md)

## 什么是AgentScope Runtime Java？

**AgentScope Runtime Java** 是一个全面的智能体运行时框架，旨在解决两个关键挑战：**高效的智能体部署**和**沙箱执行**。它内置了基础服务（长短期记忆、智能体状态持久化）和安全沙箱基础设施。无论您需要大规模部署智能体还是确保安全的工具交互，AgentScope Runtime Java 都能提供具有完整可观测性和开发者友好部署的核心基础设施。

在 V1.0 中，这些运行时服务通过 **适配器模式** 对外开放，允许开发者在保留原有智能体框架接口与行为的基础上，将 AgentScope Java 的状态管理、会话记录、工具调用等模块按需嵌入到应用生命周期中。从过去的 “黑盒化替换” 变为 “白盒化集成”，开发者可以显式地控制服务初始化、工具注册与状态持久化流程，从而在不同框架间实现无缝整合，同时获得更高的扩展性与灵活性。

本指南将指导您使用 **AgentScope Runtime Java** 构建服务级的智能体应用程序。

## 核心架构

**⚙️ 智能体web应用部署 (Web)**

提供`AgentApp`作为智能体应用主入口，同时配备部署、管理和监控智能体应用的生产级基础设施，并内置了会话历史、长期记忆以及智能体状态等服务。

**🔒 沙箱执行运行时 (Sandbox)**

安全隔离的环境，让您的智能体能够安全地执行代码、控制浏览器、管理文件并集成MCP 工具——所有这些都不会危及您的系统安全。

**🛠️ 生产级工具服务 (Tool)**

基于可信第三方 API 能力（如搜索、RAG、AIGC、支付等），通过统一的 SDK 封装对外提供标准化调用接口，使智能体能够以一致的方式集成和使用这些服务，而无需关心底层 API 的差异与复杂性。

**🔌 适配器模式 (Adapter)**

将 Runtime 内的各类服务模块（状态管理、会话记录、工具执行等）适配到智能体框架的原生模块接口中，使开发者能够在保留原生行为的同时直接调用这些能力，实现无缝对接与灵活扩展。

## 为什么选择 AgentScope Runtime Java？

- 🤖 **AS原生运行时框架**：由 AgentScope 官方构建和维护，与其多智能体范式、适配器模式及工具使用深度集成，确保最佳兼容性与性能
- **🏗️ 部署基础设施**：内置长短期记忆、智能体状态和沙箱环境控制服务
- **🔒 沙箱执行**：隔离的沙箱确保工具安全执行，不会危及系统
- ⚡ **开发者友好**：简单部署，功能强大的自定义选项
- **📊 可观测性**：针对运行时操作的全面追踪和监控

立即开始使用 AgentScope Runtime Java 部署你的智能体并尝试工具沙箱吧！
