# Welcome to AgentScope Runtime Java Cookbook

[![License](https://img.shields.io/badge/license-Apache%202.0-red.svg?logo=apache&label=License)](LICENSE)
[![GitHub Stars](https://img.shields.io/github/stars/agentscope-ai/agentscope-runtime-java?style=flat&logo=github&color=yellow&label=Stars)](https://github.com/agentscope-ai/agentscope-runtime-java/stargazers)
[![GitHub Forks](https://img.shields.io/github/forks/agentscope-ai/agentscope-runtime-java?style=flat&logo=github&color=purple&label=Forks)](https://github.com/agentscope-ai/agentscope-runtime-java/network)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.agentscope/agentscope-runtime/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.agentscope/agentscope-runtime)
[![License](https://img.shields.io/badge/license-Apache%202.0-red.svg?logo=apache&label=License)](https://github.com/agentscope-ai/agentscope-runtime/blob/main/LICENSE)
[![Cookbook](https://img.shields.io/badge/📚_Cookbook-English|中文-teal.svg)](https://runtime.agentscope.io)
[![A2A](https://img.shields.io/badge/A2A-Agent_to_Agent-blue.svg?label=A2A)](https://a2a-protocol.org/)
[![MCP](https://img.shields.io/badge/MCP-Model_Context_Protocol-purple.svg?logo=plug&label=MCP)](https://modelcontextprotocol.io/)
[![DingTalk](https://img.shields.io/badge/DingTalk-Join_Us-orange.svg)](https://qr.dingtalk.com/action/joingroup?code=v1,k1,OmDlBXpjW+I2vWjKDsjvI9dhcXjGZi3bQiojOq3dlDw=&_dt_no_comment=1&origin=11)

## AgentScope Runtime V1.0 Release

AgentScope Runtime Java V1.0 builds upon a solid foundation of efficient agent deployment and secure sandbox execution, introducing a **unified "Agent as API" development experience** that covers the complete agent lifecycle from local development to production deployment, and extends support for more sandbox types, protocol compatibility, and a richer built-in toolset.

At the same time, the agent service integration approach has been upgraded from the previous **black-box module replacement** to a ***white-box adapter pattern*** — developers can embed runtime capabilities such as state management, session recording, and tool registration into the application lifecycle as needed while preserving the original agent framework interfaces and behavior, enabling more flexible customization and seamless cross-framework integration.

**V1.0 Key Improvements:**

- **Unified Development/Production Paradigm** — Agent functionality remains consistent between development and production environments
- **Native Multi-Agent Support** — Fully compatible with AgentScope Java's multi-agent paradigm
- **Mainstream SDK and Protocol Integration** — Supports OpenAI Responses API SDK and Google A2A protocol
- **Visual Web UI** — Out-of-the-box web chat interface available immediately after deployment
- **Extended Sandbox Types** — GUI, browser, file system (most can be visualized via VNC)
- **Richer Built-in Tools** — Production-oriented modules for search, RAG, AIGC, payment, etc.
- **Flexible Deployment Modes** — Local thread/process, Docker, Kubernetes, or managed cloud

For more detailed change descriptions and migration guides, please refer to: [CHANGELOG](CHANGELOG.md)

## What is AgentScope Runtime Java?

**AgentScope Runtime Java** is a comprehensive agent runtime framework designed to address two key challenges: **efficient agent deployment** and **sandbox execution**. It includes built-in foundational services (long-term and short-term memory, agent state persistence) and secure sandbox infrastructure. Whether you need to deploy agents at scale or ensure secure tool interactions, AgentScope Runtime Java provides core infrastructure with full observability and developer-friendly deployment.

In V1.0, these runtime services are exposed through the **adapter pattern**, allowing developers to embed AgentScope Java's state management, session recording, tool invocation, and other modules into the application lifecycle as needed while preserving the original agent framework interfaces and behavior. Moving from "black-box replacement" to "white-box integration," developers can explicitly control service initialization, tool registration, and state persistence flows, enabling seamless integration across different frameworks while gaining higher extensibility and flexibility.

This guide will help you build service-level agent applications using **AgentScope Runtime Java**.

## Core Architecture

**⚙️ Agent Web Application Deployment (Web)**

Provides `AgentApp` as the main entry point for agent applications, along with production-grade infrastructure for deploying, managing, and monitoring agent applications, with built-in services such as session history, long-term memory, and agent state.

**🔒 Sandbox Execution Runtime (Sandbox)**

A secure, isolated environment that allows your agents to safely execute code, control browsers, manage files, and integrate MCP tools—all without compromising your system security.

**🛠️ Production-Grade Tool Services (Tool)**

Based on trusted third-party API capabilities (such as search, RAG, AIGC, payment, etc.), provides standardized calling interfaces through unified SDK encapsulation, enabling agents to integrate and use these services in a consistent manner without worrying about underlying API differences and complexity.

**🔌 Adapter Pattern (Adapter)**

Adapts various service modules within Runtime (state management, session recording, tool execution, etc.) to the native module interfaces of agent frameworks, enabling developers to directly call these capabilities while preserving native behavior, achieving seamless integration and flexible extension.

## Why Choose AgentScope Runtime Java?

- 🤖 **AS Native Runtime Framework**: Built and maintained by AgentScope official, deeply integrated with its multi-agent paradigm, adapter pattern, and tool usage, ensuring optimal compatibility and performance
- **🏗️ Deployment Infrastructure**: Built-in long-term and short-term memory, agent state, and sandbox environment control services
- **🔒 Sandbox Execution**: Isolated sandboxes ensure tools execute safely without compromising the system
- ⚡ **Developer Friendly**: Simple deployment with powerful customization options
- **📊 Observability**: Comprehensive tracking and monitoring for runtime operations

Start using AgentScope Runtime Java to deploy your agents and try the tool sandbox now!

