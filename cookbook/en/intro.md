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

## What is AgentScope Runtime Java?

**AgentScope Runtime Java** is a comprehensive agent runtime framework designed to address two key challenges: **efficient agent deployment** and **sandboxed tool execution**. It comes with built-in context management (long-term and short-term memory, external knowledge bases) and secure sandbox infrastructure, providing a framework-agnostic solution that works seamlessly with popular open-source agent frameworks and custom implementations. Whether you need to deploy agents at scale or ensure secure tool interactions, AgentScope Runtime provides the core infrastructure with full observability and developer-friendly deployment.

This guide will walk you through building service-level agent applications using **AgentScope Runtime Java**.

## Dual-Core Architecture

**⚙️ Agent Deployment Runtime (Engine)**

Infrastructure for deploying, managing, and running agent applications, with built-in context management (long-term and short-term memory, external knowledge bases) and environment sandbox control services.

**🛠️ Tool Execution Runtime (Sandbox)**

A secure, isolated environment that allows your agents to safely execute tools, control browsers, manage files, and integrate MCP tools—all without compromising your system's security.

## Why Choose AgentScope Runtime Java?

- **🏗️ Deployment Infrastructure**: Built-in session management, memory, and sandbox environment control services
- **🔒 Sandboxed Tool Execution**: Isolated sandboxes ensure tools execute safely without compromising the system
- **🔧 Framework-Agnostic**: Not tied to specific frameworks, works seamlessly with popular open-source agent frameworks and custom implementations
- **⚡ Developer-Friendly**: Simple deployment with powerful customization options
- **📊 Observability**: Comprehensive tracing and monitoring for runtime operations
- **☕ Java Runtime Advantages**: Built on mature, high-performance, and highly reliable Java ecosystem, with native support for cross-platform deployment, enterprise-grade stability, rich diagnostic tools (JFR/JMX), and powerful concurrency and memory management capabilities, making it particularly suitable for building long-running, highly available agent services.

Start deploying your agents with AgentScope Runtime Java and try out the tool sandbox now!



