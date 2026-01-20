# Sandbox Usage Best Practices

> **Prerequisite Reading**: This document assumes you are familiar with the basic concepts and usage of sandboxes. Before diving into the content below, it is strongly recommended to complete the previous tutorial on [ Sandbox Basics ](sandbox.md) to better understand the advanced deployment strategies discussed in this section.

In production environments, the deployment and management of sandboxes must be designed according to system scale, concurrency requirements, and resource isolation needs. Different Runtime deployment architectures present distinct challenges regarding sandbox lifecycle management, resource reuse mechanisms, and backend storage. The following sections introduce sandbox usage best practices from single-machine to distributed scenarios, categorized by hierarchy.

## Single Machine, Single Runtime Scenario

### Applicable Scenario

Suitable for development and debugging, lightweight services, or single-instance applications where only one Runtime process exists in the system, without the need to share sandboxes across processes.

### Recommended Architecture

*   **Sandbox Management**: Use an in-memory `SandboxMap` implementation for managing sandbox state.
*   **Container Backend**: Directly interface with the local Docker Daemon, using the basic Docker driver as the container runtime backend.

### Practice Recommendations

In this scenario, since there is no concurrent access or reuse of the same sandbox by multiple Runtime instances, there is no need to introduce external state storage. In-memory level sandbox mapping is sufficient to meet performance and consistency requirements. It is simple to deploy and starts quickly, making it ideal for rapid iteration and local verification.

> **Note**: This mode lacks scalability across processes or nodes and is not suitable for multi-instance deployment environments.

## Single Machine, Multiple Runtime Scenario

### Applicable Scenario

Suitable for high-concurrency, multi-tenant, or modular architecture deployments on a single machine, where multiple Runtime instances are launched to process tasks in parallel, sharing the resources of the same host.

### Core Challenge

Multiple Runtime instances may simultaneously attempt to access, reuse, or destroy the same sandbox instance. If local memory management is still used, it will lead to state inconsistency, resource contention, or duplicate creation issues.

### Recommended Architecture

*   **State Management**: **Redis must be introduced** as a globally shared sandbox metadata storage center to ensure all Runtime instances can consistently read and update sandbox states.
*   **Container Backend**: All Runtime instances access the backend through the same `containerClient` to achieve unified scheduling and reuse of sandbox instances.

### Practice Recommendations

Utilize the RedisSandboxMap provided by AgentScope Runtime Java to manage sandbox reference counting and lifecycle, avoiding race conditions.

## Multiple Machines, Multiple Runtime Scenario

### Applicable Scenario

Suitable for distributed systems, elastic scaling clusters, or microservice architectures where multiple Runtime instances are distributed across different hosts and need to collaboratively manage sandbox resources.

### Core Challenge

In addition to state consistency, the reachability of the container backend and network isolation issues must be addressed. Different nodes may not be able to directly access each other's container runtimes, leading to sandboxes that cannot be reused or managed effectively.

### Recommended Solutions

Based on whether all nodes can access a unified container backend, the following two deployment strategies are divided:

#### 1. All Nodes Can Access the Same Container Backend (Centralized Container Management)

*   **Architecture Description**: All Runtime nodes access the same remote container runtime (e.g., remote Docker Daemon or Kubernetes cluster) through the network (e.g., Docker TCP API or Kubernetes CRI).
*   **Management Method**: Consistent with the "Single Machine, Multiple Runtime" scenario, **use the RedisSandboxMap provided by AgentScope Runtime Java to centrally manage sandbox states**. All nodes operate containers through the shared backend.
*   **Advantages**: Unified architecture, simple management, and sandboxes can be reused by any node.
*   **Note**: Ensure the high availability and network stability of the container backend to avoid single points of failure.

#### 2. Nodes Cannot Access the Same Container Backend (Distributed Container Environment)

*   **Architecture Description**: Each node has its own independent container runtime (e.g., running Docker locally) and cannot directly operate containers on other nodes.
*   **Recommended Solution**: Adopt a **Remote Runtime architecture**.
*   **Implementation Method**:
    *   Deploy a dedicated **Runtime agent process** (Sandbox Manager) on a machine that can access the container backend.
    *   Configure the remaining nodes as **Remote Mode**. Connect to the agent through the built-in remote connection mode in **SandboxService**, delegating it to create, manage, and reuse sandboxes.
    *   All sandbox operation requests are forwarded to a unified entry point, achieving a management architecture that is logically centralized but physically distributed.

By reasonably selecting the above schemes, system complexity, performance, and scalability can be effectively balanced, providing a secure, efficient, and reusable sandbox runtime environment for applications of different scales.