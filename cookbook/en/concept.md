# Concepts

This chapter introduces the core concepts of AgentScope Runtime Java, which provides two main usage patterns:

- **Agent Deployment**: Use the Engine module for full-featured agent deployment, including runtime orchestration, context management, and production-ready services
- **Sandbox Tool Usage**: Use the Sandbox module independently for secure tool execution and integration in your own applications

## Engine Module Concepts

### Architecture

AgentScope Runtime uses a modular architecture with several key components:

<img src="/_static/agent_architecture.jpg" alt="Installation Options" style="zoom:25%;" />

- **Agent**: The core AI component that processes requests and generates responses (supports AgentScope Java, Spring AI Alibaba, and other Java Agent frameworks)
- **AgentApp**: Uses SpringBoot as the application implementation, responsible for providing external API interfaces, route registration, configuration loading, and delegating requests to Runner for execution
- **Runner**: Orchestrates agent execution at runtime and manages deployment
- **Context**: Contains all information needed for agent execution
- **Context & Env Manager**: Provides additional functional service management, such as session history management, long-term memory management, and sandbox management
- **Deployer**: Deploys Runner as a service

### Key Components

#### 1. Agent

`Agent` is the core component that processes requests and generates responses. It is an abstract base class that defines the interface for all agent types. We will use `AgentScopeAgent` as the main example, but the same deployment steps apply to all agent types.

#### 2. Runner

The `Runner` class provides a flexible and extensible runtime to orchestrate agent execution and provide deployment capabilities. It manages:

- Agent lifecycle
- Context manager and sandbox manager lifecycle
- Agent responses

#### 3. Context

The `Context` object contains all information needed for agent execution:

- Session information
- User requests
- Service instances

#### 4. Context & Env Manager

Includes `ContextManager` and `EnvironmentManager`:

* `ContextManager`: Provides session history management and long-term memory management
* `EnvironmentManager`: Provides sandbox lifecycle management

#### 5. Deployer

The `Deployer` system provides production-level deployment capabilities:

- Deploys `runner` as a service
- Health checks, monitoring, and lifecycle management
- Real-time response streaming using SSE
- Error handling, logging, and graceful shutdown

## Sandbox Module Concepts

### Architecture

The Sandbox module provides a **secure** and **isolated** execution environment for various operations, including MCP tool execution, browser automation, and file system operations. The architecture is built around three main components:

- **Sandbox**: Provides isolated and secure containerized execution environments
- **Tools**: Function-like interfaces executed within sandboxes

### Sandbox Types

The system supports multiple sandbox types, each optimized for specific use cases:

#### 1. BaseSandbox (Base Sandbox)

- **Purpose**: Basic Python code execution and shell commands
- **Use Cases**: Essential for basic tool execution and scripting
- **Capabilities**: IPython environment, shell command execution

#### 2. GuiSandbox (GUI Sandbox)

- **Purpose**: Graphical user interface (GUI) interaction and automation with secure access control
- **Use Cases**: User interface testing, desktop automation, interactive workflows
- **Capabilities**: Simulating user input (clicks, keyboard input), window management, screen capture, etc.

#### 3. FilesystemSandbox (File System Sandbox)

- **Purpose**: File system operations with secure access control
- **Use Cases**: File management, text processing, and data manipulation
- **Capabilities**: File read/write, directory operations, file search, and metadata, etc.

#### 4. BrowserSandbox (Browser Sandbox)

- **Purpose**: Web browser automation and control
- **Use Cases**: Web scraping, UI testing, and browser-based interactions
- **Capabilities**: Page navigation, element interaction, screenshot capture, etc.

#### 5. TrainingSandbox (Training Sandbox)

- **Purpose**: Agent training and evaluation environments
- **Use Cases**: Benchmarking and performance evaluation
- **Capabilities**: Environment analysis, training data management

### Tool Module

#### Function-like Interface

Tools are designed with an intuitive function-like interface that provides maximum flexibility while abstracting sandbox complexity:

- **Direct Execution**: Tools can be called directly, automatically creating temporary sandboxes
- **Sandbox Binding**: Tools can be bound to specific sandbox instances for persistent execution contexts
- **Schema Definition**: Each tool has a defined schema specifying input parameters and expected behavior

#### Tool Execution Priority

The tool module implements a three-level sandbox specification priority:

1. **Temporary Sandbox** (Highest Priority): Specified during function call
2. **Instance-bound Sandbox** (Second Priority): Specified through binding methods
3. **Dry-run Mode** (Lowest Priority, No Sandbox Specified): Automatically creates a temporary sandbox when no sandbox is specified, which will be released after tool execution

#### Immutable Binding Pattern

When a tool is bound to a specific sandbox, a new tool instance is created rather than modifying the original instance. This immutable binding pattern ensures thread safety and allows multiple sandbox-bound versions of the same tool to coexist without interfering with each other.




