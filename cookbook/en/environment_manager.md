# Environment Manager

## Overview

`EnvironmentManager` provides lifecycle and access capabilities for sandboxed environments and tools through `SandboxManager`.

The default implementation `DefaultEnvironmentManager` injects a `SandboxManager` instance to manage environment creation, connection, and release.

## Basic Usage

```java
// Create environment manager (creates SandboxManager internally)
EnvironmentManager environmentManager = new DefaultEnvironmentManager();

// Get sandbox manager
SandboxManager sandboxManager = environmentManager.getSandboxManager();

try {
    // Create sandbox (using try-with-resources for automatic management)
    try (BaseSandbox sandbox = new BaseSandbox(
        sandboxManager,
        "u1",  // userId
        "s1",  // sessionId
        300    // timeout in seconds
    )) {
        // Use sandbox
        // ...
    } // Sandbox automatically released

} catch (Exception e) {
    e.printStackTrace();
} finally {
    // Cleanup all sandboxes
    sandboxManager.cleanupAllSandboxes();
}
```

Or, if you manage the `SandboxManager` lifecycle yourself:

```java
// Create and manage SandboxManager
try (SandboxManager sandboxManager = new SandboxManager()) {
    // Create environment manager, passing SandboxManager
    EnvironmentManager environmentManager = new DefaultEnvironmentManager(sandboxManager);
    
    // Use environment manager
    try (BaseSandbox sandbox = new BaseSandbox(
        sandboxManager,
        "u1",
        "s1",
        300
    )) {
        // Use sandbox
        // ...
    }
} // SandboxManager automatically cleans up all resources
```

In the future, `EnvironmentManager` will not only support `SandboxManager`, but will also extend to other services that interact with the environment.

**Sandbox Manager** is designed to manage and provide access to sandboxed tool execution (see {doc}`sandbox` for details) for different users and sessions. Sandboxes are organized by a composite key of session ID and user ID, providing isolated execution contexts for each user session. The service supports multiple sandbox types, including BASE, BROWSER, FILESYSTEM, PYTHON, etc.

## Sandbox Manager Overview

The sandbox manager provides a unified interface for sandbox management, supporting different sandbox types such as code execution, file operations, and other specialized sandboxes. Here's an example of initializing the sandbox manager:

```java
// Create default sandbox manager
SandboxManager sandboxManager = new SandboxManager();

// Or use custom configuration
ManagerConfig config = ManagerConfig.builder()
    .poolSize(10)
    .build();
SandboxManager sandboxManager = new SandboxManager(config);

// Or use remote sandbox service
// ManagerConfig config = ManagerConfig.builder()
//   .poolSize(10)
//	 .baseUrl("remote_url")
//   .bearerToken("bearer_token")
//   .build();
// SandboxManager sandboxManager = new SandboxManager(config);
```

### Core Features

#### Creating Sandboxes

In Java, sandboxes are created through the `Sandbox` class constructor. The sandbox manager automatically retrieves or creates containers from the pool:

```java
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxType;
import io.agentscope.runtime.sandbox.box.BaseSandbox;

SandboxManager sandboxManager = new SandboxManager();
String sessionId = "session1";
String userId = "user1";

// Create base sandbox
BaseSandbox baseSandbox = new BaseSandbox(
    sandboxManager,
    userId,
    sessionId,
    300
)
```

#### Using Different Sandbox Types

The Java version supports multiple sandbox types. Each sandbox type has a corresponding implementation class:

```java
// Create base sandbox
BaseSandbox baseSandbox = new BaseSandbox(
    sandboxManager,
    userId,
    sessionId,
    300
);

// Create browser sandbox
BrowserSandbox browserSandbox = new BrowserSandbox(
    sandboxManager,
    userId,
    sessionId,
    300
);

// Create filesystem sandbox
FilesystemSandbox filesystemSandbox = new FilesystemSandbox(
    sandboxManager,
    userId,
    sessionId,
    300
);

// Create training sandbox
TrainingSandbox trainingSandbox = new TrainingSandbox(
    sandboxManager,
    userId,
    sessionId,
    SandboxType.TRAINING,
    300
);
```

#### Sandbox Reuse

The sandbox manager efficiently reuses existing sandboxes for the same user session and sandbox type:

```java
// First creation
BaseSandbox sandbox1 = new BaseSandbox(
    sandboxManager,
    userId,
    sessionId,
    SandboxType.BASE,
    300
);

// Second creation of the same type reuses existing container
BaseSandbox sandbox2 = new BaseSandbox(
    sandboxManager,
    userId,
    sessionId,
    SandboxType.BASE,
    300
);

// sandbox1 and sandbox2 use the same container instance
```

#### Automatic Release with try-with-resources

Java's `Sandbox` class implements the `AutoCloseable` interface, allowing automatic release with try-with-resources:

```java
try (BaseSandbox sandbox = new BaseSandbox(
    sandboxManager,
    userId,
    sessionId,
    SandboxType.BASE,
    300
)) {
    // Use sandbox
    // ...
} // Sandbox automatically released
```

#### Manual Sandbox Release

You can also manually release sandboxes:

```java
// Method 1: Call release() method
sandbox.release();

// Method 2: Release through SandboxManager
sandboxManager.releaseSandbox(SandboxType.BASE, userId, sessionId);

// Method 3: Cleanup all sandboxes
sandboxManager.cleanupAllSandboxes();
```

### Service Lifecycle

`SandboxManager` implements the `AutoCloseable` interface, allowing automatic resource management with try-with-resources:

```java
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;

// Use try-with-resources for automatic lifecycle management
try (SandboxManager sandboxManager = new SandboxManager()) {
    // Sandbox manager automatically initializes on creation
    
    // Use sandbox manager
    // ...
    
} // Automatically cleans up all sandboxes and closes resources
```

Or manually manage the lifecycle:

```java
SandboxManager sandboxManager = new SandboxManager();

try {
    // Use sandbox manager
    // ...
} catch (Exception e) {
    e.printStackTrace();
} finally {
    // Manually cleanup all sandboxes
    sandboxManager.cleanupAllSandboxes();
    
    // Close sandbox manager (automatically cleans up all resources)
    sandboxManager.close();
}
```

### Environment Manager Methods

The `EnvironmentManager` interface provides the following methods:

```java
EnvironmentManager envManager = new DefaultEnvironmentManager();

// Get sandbox manager
SandboxManager sandboxManager = envManager.getSandboxManager();

// Get environment variable
String value = envManager.getEnvironmentVariable("KEY");

// Set environment variable
envManager.setEnvironmentVariable("KEY", "VALUE");

// Get all environment variables
Map<String, String> allVars = envManager.getAllEnvironmentVariables();

// Check if environment is available
boolean available = envManager.isEnvironmentAvailable();

// Initialize environment
CompletableFuture<Void> initFuture = envManager.initializeEnvironment();
initFuture.get(); // Wait for initialization to complete

// Get environment information
Map<String, Object> envInfo = envManager.getEnvironmentInfo();

// Cleanup environment
CompletableFuture<Void> cleanupFuture = envManager.cleanupEnvironment();
cleanupFuture.get(); // Wait for cleanup to complete
```

### Supported Sandbox Types

The Java version supports the following sandbox types, each with a corresponding implementation class:

| Sandbox Type | Implementation Class | Description |
|---------|--------|------|
| `BASE` | `BaseSandbox` | Base sandbox, provides basic execution environment |
| `BROWSER` | `BrowserSandbox` | Browser sandbox, supports browser automation |
| `FILESYSTEM` | `FilesystemSandbox` | Filesystem sandbox, provides file operation capabilities |
| `GUI` | `GuiSandbox` | GUI sandbox, supports graphical interface operations |
| `TRAINING` | `TrainingSandbox` | Training sandbox, used for training scenarios |
| `APPWORLD` | `APPWorldSandbox` | App World sandbox, extends TrainingSandbox |
| `BFCL` | `BFCLSandbox` | BFCL sandbox, extends TrainingSandbox |
| `WEBSHOP` | `WebShopSandbox` | E-commerce sandbox, extends TrainingSandbox |

Other sandbox types (such as `PYTHON`, `NODE`, `JAVA`) can be created using `BaseSandbox` with the corresponding `SandboxType`.

### Configuring Sandbox Manager

You can use `ManagerConfig` to customize the sandbox manager's behavior:

```java
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;

ManagerConfig config = ManagerConfig.builder()
    .poolSize(10)  // Container pool size
    .portRange(new int[]{8000, 9000})  // Port range
    .redisConfig(redisConfig)	// Whether to enable Redis for sandbox management
  	.containerDeployment(clientConfig)	// Container deployment medium (Docker, K8s, Agentrun)
    .build();

SandboxManager sandboxManager = new SandboxManager(config);
```




