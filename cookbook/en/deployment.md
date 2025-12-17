# Deployment

This chapter focuses on how to deploy agents on AgentScope Runtime Java. After completing concepts and quick start, deployment is a key step to move experimental prototypes into stable operation. We will first provide deployment ideas and overall processes, then connect subsequent service, simple deployment, advanced deployment, and React Agent example sub-chapters to help you quickly locate the appropriate path.

## Why Deployment is Needed

- **Obtain stable operational capabilities**: Runtime provides standardized service lifecycle, health checks, and scaling capabilities, simplifying monitoring and rollback.
- **Reuse ecosystem capabilities**: Through unified deployment methods, you can reuse foundational services such as memory, sandbox, and state, avoiding reinventing the wheel.

## Deployment Path Overview

The deployment process typically includes the following phases:

1. **Preparation**: Install Runtime, prepare models and tools, configure environment variables and credentials.
2. **Start Basic Services**: Choose memory, session, sandbox, and other service implementations based on business needs.
3. **Agent App Definition**: Based on the `AgentApp` module, orchestrate agents, tools, and workflow logic to form a deployable application entry point.
4. **Run**: Start Runtime in local, container, or cloud-native environments.
5. **Upgrade and Extend**: Use advanced deployment and React Agent capabilities to implement multi-region deployment, hybrid orchestration, or UI interaction.

## Prerequisites Checklist

- Java environment (**JDK 17+**) and necessary dependencies.
- At least one available large model access (e.g., DashScope, OpenAI, or self-built inference service).
- Access and permissions for the target deployment platform (local, Docker, Kubernetes, etc.).
- Corresponding tool/sandbox permissions, such as browser automation, file system, or custom tool services.

## Sub-Chapter Guide

### Services

The `Service` chapter introduces Runtime's built-in foundational services such as session history, memory, sandbox, and state, as well as unified lifecycle interfaces. Reading this chapter will help you understand how to choose appropriate implementations (in-memory, Redis, Tablestore, etc.) and how to manage services through `start()`, `stop()`, and `health()` to ensure the deployment environment has stable support capabilities.
For detailed documentation, see [Services and Adapters](service/service.md).

### Simple Deployment

Runtime includes a simple deployment tool `AgentApp`. It is an application form that connects multiple agents, tools, and contexts. The sub-chapter will explain:

- How to define `AgentApp` configuration, routing, and session management.
- How to bind services, inject sandboxes, and expose HTTP/gRPC/CLI interfaces during deployment.
- How to write custom handlers and plugins to meet different business flows.

In actual deployment, Agent App usually serves as the main entry process, together with foundational services forming the runtime.
For complete examples, refer to [Simple Deployment](deployment/agent_app.md).

### Advanced Deployment

When higher availability and observability requirements need to be met, refer to the advanced deployment chapter, which includes:

- Using container orchestration (such as Docker, Kubernetes) to run multi-service topologies.
- Using Alibaba Cloud Function Compute AgentRun to run functionized Agents
- Configuring multi-region/multi-model redundancy, canary releases, and scaling strategies.

This chapter is suitable for readers with production scenarios or multi-team collaboration needs.
For more solutions, see [Advanced Deployment](deployment/advanced_deployment.md).

### Reference: Complete Deployment Example

The complete deployment example chapter introduces a complete Agent deployment example that includes sandbox services, including:

- Introducing browser sandbox
- Building AgentApp
- Service startup

If you want to review all aspects of deployment, this chapter is an important reference.
Please refer to [Reference: Complete Deployment Example](deployment/react_agent.md) for specific steps.

## Next Steps

After reading this chapter, you can proceed in the following order:

1. Go to the [Services](service/service.md) chapter to confirm the required infrastructure and implementations.
2. Complete business logic orchestration and local validation in [Simple Deployment](deployment/agent_app.md).
3. Choose the [Advanced Deployment](deployment/advanced_deployment.md) guide based on scale requirements to complete production configuration.
4. If a Web interaction layer is needed, continue reading the [Complete Deployment Example](deployment/react_agent.md) chapter and complete frontend deployment.

Through the above steps, you can progressively deploy agents from experimental environments to observable, maintainable, and scalable production systems.
