# Custom Sandbox Example

A complete example demonstrating the two layers of sandbox customization: **sandbox runtime backend** (alongside Docker / K8s / AgentRun / FC) and **sandbox type** (tool invocation, hooks, session reuse). All implementations are fake/stub for teaching purposes — no real runtime environment is needed.

## Architecture Overview

The sandbox system has two independent extension layers:

```
┌─────────────────────────────────────────────────────────────────┐
│                     Application Layer (Sandbox)                  │
│  Your code calls tools, runs Python/Shell through Sandbox       │
│                                                                 │
│  Built-in: BaseSandbox / BrowserSandbox / FilesystemSandbox ... │
│  Custom: extend Sandbox + @RegisterSandbox + SandboxProvider SPI│
├─────────────────────────────────────────────────────────────────┤
│              Infrastructure Layer (BaseClientStarter + BaseClient)│
│  Determines WHERE containers run and HOW their lifecycle is      │
│  managed                                                         │
│                                                                 │
│  Built-in: DockerClientStarter / KubernetesClientStarter /      │
│            AgentRunClientStarter / FcClientStarter               │
│  Custom: extend BaseClientStarter + BaseClient                  │
└─────────────────────────────────────────────────────────────────┘
```

**How the two layers relate:**
- **Infrastructure layer** answers "where does the sandbox run" — local Docker? K8s cluster? Cloud functions? ECS? Your own orchestration system?
- **Application layer** answers "what happens inside the sandbox" — which tools to run? What to do before/after calls? How to inject config?

The two layers are fully orthogonal. You can:
- Use fake/stub backends for both layers (this example)
- Run custom sandboxes on built-in backends (Docker, K8s, etc.)
- Run built-in sandboxes on a custom runtime backend
- Customize both layers

## What You'll Learn

**Infrastructure Layer — Custom Sandbox Runtime Backend:**
1. `BaseClientStarter`: Configure how to connect to the runtime backend
2. `BaseClient`: Implement the full sandbox instance lifecycle (create, start, stop, remove)

**Application Layer — Custom Sandbox Type:**
3. Extend `Sandbox` with `@RegisterSandbox` + SPI registration
4. Override `callTool()` to add before/after hooks
5. Wrap convenience methods like `runPython()` / `runShell()`
6. Session reuse and environment variable injection

## Prerequisites

- Java 17+
- Maven 3.6+
- Core modules installed (`mvn clean install -DskipTests` from root)

## Quick Start

### 1) Build

```bash
mvn clean compile
```

### 2) Run

```bash
mvn exec:java -Dexec.mainClass="io.agentscope.Main"
```

### 3) Expected Output

The example runs as a single end-to-end flow:

```
╔══════════════════════════════════════════════════════════════╗
║  Custom Sandbox Example — Full Lifecycle Demo               ║
╚══════════════════════════════════════════════════════════════╝

--- Step 1: Create CustomClientStarter ---
--- Step 3: Start SandboxService (triggers connect()) ---
[CustomClientStarter] Creating client (host=my-platform.example.com, port=443, label=demo)
[CustomClient] Connecting to platform at my-platform.example.com:443 ...
[CustomClient] Connected successfully (label=demo)

--- Step 4: Create CustomSandbox (triggers container lifecycle) ---
[CustomClient] Checking if image/template exists: ...
[CustomClient] Creating runtime instance: ...
[CustomClient] Instance created: id=fake-xxxxxxxxxxxx, ip=127.0.0.1, ports=[8080]
[CustomClient] Starting instance: fake-xxxxxxxxxxxx

--- Step 5: Run Python (hooks fire automatically) ---
[HOOK:before] Tool 'run_ipython_cell' called with args: {code=x = 42...}
[HOOK:after]  Tool 'run_ipython_cell' completed in 0ms
  Result: [fake output] Python executed: x = 42...

--- Step 6: Run Shell Command ---
[HOOK:before] Tool 'run_shell_command' called with args: {command=echo ...}
  Result: [fake output] Shell executed: echo ...

--- Step 8: Session Reuse (same userId + sessionId) ---
--- Step 9: Environment Variable Injection ---
--- Step 10: Close sandbox (triggers cleanup lifecycle) ---

╔══════════════════════════════════════════════════════════════╗
║  ✅ All steps completed successfully!                         ║
╚══════════════════════════════════════════════════════════════╝
```

---

## Part 1: Custom Sandbox Runtime Backend

> A custom implementation alongside Docker / Kubernetes / AgentRun / FC.
> The sandbox runtime backend can be any compute resource — a Docker container, an ECS instance, a VM, a K8s Pod, a serverless function, or your own orchestration platform.

This example uses a **fake/stub** implementation (`CustomClient` + `CustomClientStarter`) to demonstrate the lifecycle flow and what each method is responsible for. Replace the TODO stubs with your platform's API calls to make it real.

### Core Interfaces

```
BaseClientStarter              BaseClient
┌───────────────────┐          ┌──────────────────────────┐
│ + startClient()   │─creates─▶│ + connect()              │
│ + getContainerType│          │ + createContainer()      │
└───────────────────┘          │ + startContainer()       │
                               │ + stopContainer()        │
                               │ + removeContainer()      │
                               │ + getContainerStatus()   │
                               │ + imageExists()          │
                               │ + pullImage()            │
                               │ + inspectContainer()     │
                               │ + isConnected()          │
                               └──────────────────────────┘
```

### Existing Implementations

| Implementation | ClientStarter | BaseClient | Type | Use Case |
|---------------|---------------|------------|------|----------|
| Docker | `DockerClientStarter` | `DockerClient` | `DOCKER` | Local dev, single-machine |
| Kubernetes | `KubernetesClientStarter` | `KubernetesClient` | `KUBERNETES` | K8s clusters |
| AgentRun | `AgentRunClientStarter` | `AgentRunClient` | `AGENTRUN` | Alibaba Cloud AgentRun |
| FC | `FcClientStarter` | `FcClient` | `FC` | Alibaba Cloud Function Compute |
| **Yours** | `MyClientStarter` | `MyClient` | Custom | Your orchestration platform |

### Step 1: Implement BaseClient

`BaseClient` is the core abstraction for managing sandbox runtime instances. A runtime instance could be anything — a Docker container, a VM, a K8s Pod, or even an SSH-connected remote machine.

> **Note:** Although the API uses the term "container" (e.g. `createContainer`, `ContainerCreateResult`), it is a general-purpose abstraction — not limited to Docker containers.

The framework (`SandboxService`) calls these methods in a specific order:

```
connect() → imageExists() → pullImage() → createContainer() → startContainer()
  → (sandbox runs, tools called via HTTP) →
stopContainer() → removeContainer()
```

Each method has comments explaining what different platforms (Docker, K8s, ECS, Serverless) would do:

```java
public class CustomClient extends BaseClient {

    @Override
    public boolean connect() {
        // Docker:      Connect to Docker daemon via TCP or Unix socket
        // Kubernetes:  Load kubeconfig, connect to K8s API server
        // ECS/VM:      Initialize cloud SDK client, validate credentials
        // SSH:         Establish SSH connection pool to remote machines
        return true;
    }

    @Override
    public boolean imageExists(String imageName) {
        // Docker:      Check if image exists locally (docker images)
        // Kubernetes:  Usually true (K8s pulls on pod creation)
        // ECS/VM:      Check if VM image/AMI/snapshot exists in region
        return true;
    }

    @Override
    public ContainerCreateResult createContainer(String containerName, String imageName,
            List<String> ports, List<VolumeBinding> volumeBindings,
            Map<String, String> environment, Map<String, Object> runtimeConfig) {
        // Docker:      docker create with port bindings and volume mounts
        // Kubernetes:  Create Pod spec with containers, services, PVCs
        // ECS/VM:      Call RunInstances API with security groups, VPC, user-data
        // Serverless:  Create/update function with sandbox image

        String instanceId = "fake-" + UUID.randomUUID().toString().substring(0, 12);
        List<String> exposedPorts = List.of("8080");
        String instanceIp = "127.0.0.1";

        // The framework uses ip + ports to build the sandbox HTTP endpoint:
        //   http://{ip}:{port}/fastapi/tools/run_ipython_cell
        return new ContainerCreateResult(instanceId, exposedPorts, instanceIp);
    }

    @Override
    public void startContainer(String containerId) { /* start the instance */ }

    @Override
    public void stopContainer(String containerId) { /* stop the instance */ }

    @Override
    public void removeContainer(String containerId) { /* clean up resources */ }

    @Override
    public String getContainerStatus(String containerId) { return "running"; }

    @Override
    public boolean isConnected() { return true; }

    @Override
    public boolean pullImage(String imageName) { return true; }

    @Override
    public boolean inspectContainer(String containerIdOrName) { return true; }

    @Override
    public boolean containerNameExists(String containerName) { return false; }
}
```

> See [`CustomClient.java`](src/main/java/io/agentscope/CustomClient.java) for the full implementation with detailed per-method Javadoc.

**`ContainerCreateResult` — what the framework needs from you:**

| Field | Type | Description |
|-------|------|-------------|
| `containerId` | `String` | Unique instance identifier (container ID, VM instance ID, Pod name) |
| `ports` | `List<String>` | Exposed port numbers where the sandbox HTTP API listens |
| `ip` | `String` | IP/hostname where the instance is reachable |

### Step 2: Implement BaseClientStarter

`BaseClientStarter` is a factory that holds connection configuration and creates a `BaseClient`. It's called once during `SandboxService.start()`.

Change the configuration fields to match your platform's needs:

| Platform | Typical config fields |
|----------|----------------------|
| Docker | host, port, certPath |
| Kubernetes | kubeConfigPath, namespace |
| ECS/VM | accessKeyId, accessKeySecret, regionId, securityGroupId |
| SSH | sshHost, sshPort, sshUser, privateKeyPath |
| Custom API | apiEndpoint, apiToken, maxRetries |

```java
public class CustomClientStarter extends BaseClientStarter {

    private final String host;
    private final int port;
    private final String label;

    private CustomClientStarter(Builder builder) {
        // ContainerClientType determines SandboxService behavior:
        //   DOCKER:      pulls images locally, uses local volume mounts
        //   KUBERNETES:  skips image pull, uses PVCs
        //   AGENTRUN/FC: skips local volume mounts, uses absolute paths
        // Choose the type that best matches your platform.
        super(ContainerClientType.DOCKER);
        this.host = builder.host;
        this.port = builder.port;
        this.label = builder.label;
    }

    @Override
    public BaseClient startClient(PortManager portManager) {
        CustomClient client = new CustomClient(this);
        client.connect();
        return client;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String host = "localhost";
        private int port = 8080;
        private String label = "custom";

        public Builder host(String host) { this.host = host; return this; }
        public Builder port(int port) { this.port = port; return this; }
        public Builder label(String label) { this.label = label; return this; }
        public CustomClientStarter build() { return new CustomClientStarter(this); }
    }
}
```

> See [`CustomClientStarter.java`](src/main/java/io/agentscope/CustomClientStarter.java) for the full implementation.

> **About `ContainerClientType`:** The current `ContainerClientType` is an enum with only `DOCKER`, `KUBERNETES`, `AGENTRUN`, `FC`. If your custom runtime behaves like Docker (local execution, supports volume mounts), reuse `ContainerClientType.DOCKER`. If it behaves like a cloud service (no local image pull, no local volume mounts), use another type. To add a new enum value, modify the `ContainerClientType` source and submit a PR.

### Step 3: Wire It Up

```java
// Step 1: Create your custom BaseClientStarter
BaseClientStarter localStarter = CustomClientStarter.builder()
        .host("my-platform.example.com")
        .port(443)
        .label("demo")
        .build();

// Step 2: Plug into ManagerConfig (replaces DockerClientStarter)
ManagerConfig config = ManagerConfig.builder()
        .clientStarter(localStarter)
        .build();

// Step 3: Start SandboxService — triggers connect()
SandboxService service = new SandboxService(config);
service.start();

// Step 4: Create sandbox — triggers: imageExists → createContainer → startContainer
try (CustomSandbox sandbox = new CustomSandbox(service, "user1", "session1")) {
    sandbox.listTools("");  // triggers lazy initialization
}
```

> See [`Main.java`](src/main/java/io/agentscope/Main.java) Part 1 for the full runnable example.

### Class Hierarchy

```
ManagerConfig
  └── clientStarter: BaseClientStarter     ← choose or customize the runtime backend
        ├── DockerClientStarter            ← built-in: local Docker
        ├── KubernetesClientStarter        ← built-in: K8s cluster
        ├── AgentRunClientStarter          ← built-in: Alibaba Cloud AgentRun
        ├── FcClientStarter                ← built-in: Alibaba Cloud Function Compute
        └── CustomClientStarter             ← example: fake/stub for teaching
              └── startClient() → CustomClient extends BaseClient
                    ├── connect()
                    ├── createContainer()   → returns ContainerCreateResult
                    ├── startContainer()
                    ├── stopContainer()
                    ├── removeContainer()
                    └── ...
```

---

## Part 2: Custom Sandbox Type

> Define **what happens** inside the sandbox: tool invocation, hooks, session management

### Step 1: Define Your Custom Sandbox

Extend `Sandbox` and add the `@RegisterSandbox` annotation:

```java
@RegisterSandbox(
    imageName = "your-registry/your-sandbox-image:latest",
    sandboxType = "custom",
    securityLevel = "medium",
    timeout = 60,
    description = "Custom sandbox with tool execution hooks"
)
public class CustomSandbox extends Sandbox {

    public CustomSandbox(SandboxService managerApi, String userId, String sessionId) {
        super(managerApi, userId, sessionId, "custom");
    }
}
```

**Annotation parameters:**

| Parameter | Description | Default |
|-----------|-------------|---------|
| `imageName` | Container image for the sandbox | (required) |
| `sandboxType` | Unique type identifier | `"base"` |
| `securityLevel` | Security level: low / medium / high | `"medium"` |
| `timeout` | Operation timeout in seconds | `300` |
| `environment` | Environment variables (`KEY=VALUE`) | `{}` |

### Step 2: Add Before/After Hooks

Override `callTool()` to intercept every tool invocation:

```java
@Override
public String callTool(String name, Map<String, Object> arguments) {
    // ---- Before Hook ----
    long startTime = System.currentTimeMillis();
    beforeToolCall(name, arguments);

    String result;
    try {
        // ---- Actual Execution ----
        // In a REAL application, call super.callTool(name, arguments)
        // which sends HTTP POST to the sandbox instance.
        // For this example, we use a fake implementation:
        result = fakeToolExecution(name, arguments);
    } catch (Exception e) {
        // ---- Error Hook ----
        onToolError(name, arguments, e);
        throw e;
    }

    // ---- After Hook ----
    long elapsed = System.currentTimeMillis() - startTime;
    afterToolCall(name, arguments, result, elapsed);
    return result;
}

private String fakeToolExecution(String name, Map<String, Object> arguments) {
    switch (name) {
        case "run_ipython_cell":
            return "[fake output] Python executed: " + arguments.get("code");
        case "run_shell_command":
            return "[fake output] Shell executed: " + arguments.get("command");
        default:
            return "[fake output] Unknown tool: " + name;
    }
}
```

**What you can do in hooks:**
- Log or audit every tool call (who, what, when, args)
- Validate or transform arguments before execution
- Measure execution time for performance monitoring
- Intercept results for post-processing
- Implement rate limiting, access control, or retry logic

### Step 3: Add Convenience Methods

Wrap `callTool()` with domain-specific methods:

```java
public String runPython(String code) {
    Map<String, Object> arguments = new HashMap<>();
    arguments.put("code", code);
    return callTool("run_ipython_cell", arguments);
}

public String runShell(String command) {
    Map<String, Object> arguments = new HashMap<>();
    arguments.put("command", command);
    return callTool("run_shell_command", arguments);
}
```

### Step 4: Register via SPI

Create a `SandboxProvider` implementation:

```java
public class CustomSandboxProvider implements SandboxProvider {
    @Override
    public Collection<Class<?>> getSandboxClasses() {
        return Collections.singletonList(CustomSandbox.class);
    }
}
```

Register it in `META-INF/services/io.agentscope.runtime.sandbox.manager.registry.SandboxProvider`:

```
io.agentscope.CustomSandboxProvider
```

### Step 5: Use Your Sandbox

```java
// Create SandboxService with your custom backend
BaseClientStarter clientStarter = CustomClientStarter.builder()
        .host("my-platform.example.com").port(443).label("demo").build();
ManagerConfig managerConfig = ManagerConfig.builder()
        .clientStarter(clientStarter)
        .build();
SandboxService sandboxService = new SandboxService(managerConfig);
sandboxService.start();

// Use the custom sandbox
CustomSandbox sandbox = new CustomSandbox(sandboxService, "user1", "session1");

// Run Python — hooks fire automatically
String result = sandbox.runPython("print('Hello from sandbox!')");
System.out.println(result);

// Run Shell
sandbox.runShell("echo 'Hello World'");

// Direct low-level tool call
sandbox.callTool("run_ipython_cell", Map.of("code", "1 + 1"));
```

> See [`Main.java`](src/main/java/io/agentscope/Main.java) for the full runnable example.

### Session Reuse

Creating a new sandbox object with the **same `userId` + `sessionId`** reuses the existing container. Python variables persist across calls:

```java
// First sandbox creates the container
CustomSandbox s1 = new CustomSandbox(service, "user1", "session1");
s1.runPython("x = 42");

// Second sandbox reuses the same container — x is still there
CustomSandbox s2 = new CustomSandbox(service, "user1", "session1");
s2.runPython("print(x)");  // Output: 42
```

> **Note:** Calling `close()` (or exiting `try-with-resources`) stops and removes the container.

### Custom Environment Variables

Pass environment variables into the container at creation time:

```java
try (CustomSandbox sandbox = new CustomSandbox(
        sandboxService, "user1", "session1",
        Map.of("MY_API_KEY", "secret123", "APP_ENV", "production"))) {
    sandbox.runShell("echo $MY_API_KEY");  // Output: secret123
}
```

---

## Built-in Tool Reference

The `base` sandbox image provides these tools out of the box:

| Tool Name | Description | Parameters |
|-----------|-------------|------------|
| `run_ipython_cell` | Execute Python code (IPython) | `code`: Python code string |
| `run_shell_command` | Execute Shell commands | `command`: Shell command string |

## Project Structure

```
custom_sandbox_example/
├── src/main/java/io/agentscope/
│   ├── CustomClient.java            # Fake/stub BaseClient — teaches the lifecycle contract (Part 1)
│   ├── CustomClientStarter.java     # Custom BaseClientStarter — factory + config (Part 1)
│   ├── CustomSandbox.java          # Custom sandbox with hooks (Part 2)
│   ├── CustomSandboxProvider.java   # SPI provider for registration
│   └── Main.java                   # Entry point: full lifecycle demo
├── src/main/resources/META-INF/services/
│   └── io.agentscope.runtime.sandbox.manager.registry.SandboxProvider
└── pom.xml
```

## Related Documentation

- [Sandbox Guide (English)](../../../cookbook/en/sandbox/sandbox.md)
- [Sandbox Guide (中文)](../../../cookbook/zh/sandbox/sandbox.md)
- [AgentScope Runtime Java (root)](../../../README.md)
