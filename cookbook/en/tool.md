# Sandbox and Tools

In AgentScope Runtime Java, tools are key components for agents to implement business capabilities. Whether directly calling model services, executing browser automation, or integrating enterprise APIs, the tool system needs to be secure, controllable, and easy to extend. This chapter provides an overall approach and connects subsequent sub-chapters (ready-to-use tools, sandbox basics/advanced, training sandbox, sandbox troubleshooting) to help you choose the appropriate path based on your scenario.

## Tool Integration Modes

Runtime supports two common ways to integrate tools:

1. **Ready-to-Use Tools**: Provided directly by service providers or Runtime, such as RAG retrieval, which can be called with zero deployment.
2. **Sandbox Tools**: Run in a controlled manner through sandbox environments such as Browser/FileSystem.

## Sub-Chapter Guide

### Sandbox

Introduces the concepts, lifecycle, and common types of tool sandboxes (browser, file system, Python execution, etc.). You will learn how to:

- Create, connect, and release sandboxes through the `Sandbox` SDK.
- Reuse and isolate resources in multi-session scenarios.

For operational details, see [Sandbox](sandbox/sandbox.md).

#### Advanced Sandbox

In-depth discussion of advanced features such as multiple backends and remote sandbox services. Suitable for teams that need large-scale stable operation or meet enterprise security requirements. Content includes:

- More sandbox settings.
- Integration methods for Kubernetes and remote container clusters.
- Extension interfaces for custom sandbox types.

For the complete guide, see [Advanced Sandbox](sandbox/advanced.md).

#### Training Sandbox

Focuses on special sandbox capabilities for evaluation, training, or self-play scenarios:

For more content, refer to [Training Sandbox](sandbox/training_sandbox.md).

#### Sandbox Troubleshooting

Provides common problem identification and repair suggestions, such as sandbox startup failures, tool timeouts, insufficient permissions, etc., and provides checklists (logs, health checks, resource usage) and common error code descriptions.

For troubleshooting steps, see [Sandbox Troubleshooting](sandbox/troubleshooting.md).

## Recommended Path

1. Start with ready-to-use tools to determine the required calling methods.
2. Based on side effects and security requirements, choose whether to enable sandbox and its level.
3. Refer to advanced chapters to complete batch validation and production deployment.
4. When encountering stability issues, quickly consult the troubleshooting chapter to identify root causes.

Through the above steps, you can build a tool system that is both secure and reliable while having high extensibility, enabling agents to have continuously evolving capabilities.
