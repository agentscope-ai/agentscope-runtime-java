# Manager Module

This module provides a unified interface for registering, starting, and stopping multiple services (such as session history services, memory services, and sandbox services) with automatic lifecycle management.

## Overview

- **ServiceManager**: Abstract base class for service management, providing common lifecycle and access APIs
- **ContextManager**: Focuses on context-related services (such as `SessionHistoryService` and `MemoryService`), extends `ServiceManager`
- **EnvironmentManager**: Focuses on environment/tool-related capabilities (through `SandboxManager`), is an interface rather than extending `ServiceManager`

### Service Interface Requirements

All services must implement the `Service` interface, which defines three core methods:

```java
import io.agentscope.runtime.engine.shared.Service;
import java.util.concurrent.CompletableFuture;

public interface Service {
    CompletableFuture<Void> start();
    CompletableFuture<Void> stop();
    CompletableFuture<Boolean> health();
}
```

Services can also be implemented by extending the `ServiceWithLifecycleManager` abstract class, which implements both the `Service` interface and the `AutoCloseable` interface:

```java
import io.agentscope.runtime.engine.shared.ServiceWithLifecycleManager;
import java.util.concurrent.CompletableFuture;

public class MockService extends ServiceWithLifecycleManager {
    private String name;
    private boolean started = false;
    private boolean stopped = false;

    public MockService(String name) {
        this.name = name;
    }

    @Override
    public CompletableFuture<Void> start() {
        return CompletableFuture.runAsync(() -> {
            started = true;
        });
    }

    @Override
    public CompletableFuture<Void> stop() {
        return CompletableFuture.runAsync(() -> {
            stopped = true;
        });
    }

    @Override
    public CompletableFuture<Boolean> health() {
        return CompletableFuture.completedFuture(started && !stopped);
    }
}
```

### Service Lifecycle Management

`ServiceManager` is an abstract base class. Subclasses need to implement the `registerDefaultServices()` method to register default services:

```java
import io.agentscope.runtime.engine.shared.ServiceManager;
import io.agentscope.runtime.engine.shared.Service;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class MyServiceManager extends ServiceManager {
    @Override
    protected void registerDefaultServices() {
        // Register default services
    }

    public static void main(String[] args) throws Exception {
        MyServiceManager manager = new MyServiceManager();
        
        // Register service class (via class registration, note: service class must have a no-argument constructor)
        manager.register(MockService.class, "service1");
        manager.register(MockService.class, "service2");
        
        // Or register service instance (via instance registration, recommended, can pass constructor parameters)
        manager.registerService("service3", new MockService("service3"));
        
        // Start all services
        manager.start().get();
        
        try {
            // Use services
            List<String> serviceNames = manager.listServices();
            Map<String, Boolean> health = manager.healthCheck().get();
            System.out.println("Services: " + serviceNames);
            System.out.println("Health: " + health);
        } finally {
            // Stop all services
            manager.stop().get();
            // Or use try-with-resources (because it implements AutoCloseable)
            // manager.close();
        }
    }
}
```

### Service Access Methods

`ServiceManager` provides the following methods to access services:

- Get service: `getService(String name)` - throws an exception if the service doesn't exist
- Get service (with default): `getService(String name, Service defaultService)` - returns default value if service doesn't exist
- Check if service exists: `hasService(String name)` - returns boolean
- List all service names: `listServices()` - returns list of service names
- Get all services: `getAllServices()` - returns map of service names to service instances
- Health check: `healthCheck()` - returns map of service names to health status

```java
// Get service
Service service1 = manager.getService("service1");

// Get service (with default)
Service service2 = manager.getService("service2", new MockService("default"));

// Check if service exists
if (manager.hasService("service1")) {
    Service service = manager.getService("service1");
}

// List all services
List<String> names = manager.listServices();

// Get all services
Map<String, Service> allServices = manager.getAllServices();

// Health check
Map<String, Boolean> health = manager.healthCheck().get();
```

## Context Manager

`ContextManager` extends `ServiceManager`, automatically assembles context services (`session`, `memory`), and provides context composition methods.

### Creating ContextManager

You can create `ContextManager` in multiple ways:

```java
import io.agentscope.runtime.engine.memory.context.ContextManager;
import io.agentscope.runtime.engine.memory.context.ContextComposer;
import io.agentscope.runtime.engine.memory.persistence.memory.service.InMemoryMemoryService;
import io.agentscope.runtime.engine.memory.persistence.session.InMemorySessionHistoryService;
import io.agentscope.runtime.engine.memory.service.MemoryService;
import io.agentscope.runtime.engine.memory.service.SessionHistoryService;
import java.util.concurrent.CompletableFuture;

// Method 1: Use default constructor (uses in-memory implementation)
ContextManager contextManager1 = new ContextManager();

// Method 2: Specify service implementations
SessionHistoryService sessionHistoryService = new InMemorySessionHistoryService();
MemoryService memoryService = new InMemoryMemoryService();
ContextManager contextManager2 = new ContextManager(
    ContextComposer.class,
    sessionHistoryService,
    memoryService
);

// Method 3: Use ContextManagerFactory (recommended)
import io.agentscope.runtime.engine.memory.context.ContextManagerFactory;

// Create default context manager
ContextManager defaultManager = ContextManagerFactory.createDefault();

// Create custom context manager
ContextManager customManager = ContextManagerFactory.createCustom(
    memoryService,
    sessionHistoryService
);
```

### Using ContextManager

```java
import io.agentscope.runtime.engine.memory.model.Message;
import io.agentscope.runtime.engine.memory.model.Session;
import java.util.ArrayList;
import java.util.List;

// Start services
contextManager.start().get();

try {
    // Get services
    SessionHistoryService sessionService = contextManager.getSessionHistoryService();
    MemoryService memoryService = contextManager.getMemoryService();
    
    // Or get by service name
    SessionHistoryService sessionService2 = (SessionHistoryService) contextManager.getService("session");
    MemoryService memoryService2 = (MemoryService) contextManager.getService("memory");
    
    // Compose session
    Session session = contextManager.composeSession("userId", "sessionId").get();
    
    // Compose context
    List<Message> requestInput = new ArrayList<>();
    contextManager.composeContext(session, requestInput).get();
    
    // Append messages
    List<Message> eventOutput = new ArrayList<>();
    contextManager.append(session, eventOutput).get();
    
} finally {
    // Stop services
    contextManager.stop().get();
}
```

For more details, see {doc}`context_manager`.

## Environment Manager

`EnvironmentManager` is an interface focused on environment/tool-related capabilities (through `SandboxManager`). `DefaultEnvironmentManager` is its default implementation.

### Creating EnvironmentManager

```java
import io.agentscope.runtime.engine.service.EnvironmentManager;
import io.agentscope.runtime.engine.service.impl.DefaultEnvironmentManager;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

// Method 1: Use default constructor (creates new SandboxManager)
EnvironmentManager envManager1 = new DefaultEnvironmentManager();

// Method 2: Specify SandboxManager
SandboxManager sandboxManager = new SandboxManager();
EnvironmentManager envManager2 = new DefaultEnvironmentManager(sandboxManager);
```

### Using EnvironmentManager

```java
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxType;
import io.agentscope.runtime.sandbox.manager.model.container.ContainerModel;

// Initialize environment
envManager.initializeEnvironment().get();

try {
    // Get SandboxManager (for managing sandbox environment)
    SandboxManager sandboxManager = envManager.getSandboxManager();
    
    // Create sandbox through SandboxManager
    ContainerModel container = sandboxManager.createFromPool(
        SandboxType.BASE, 
        "userId", 
        "sessionId"
    );
    
    // Release sandbox
    sandboxManager.releaseSandbox(SandboxType.BASE, "userId", "sessionId");
    
    // Get environment variable
    String value = envManager.getEnvironmentVariable("KEY");
    
    // Set environment variable
    envManager.setEnvironmentVariable("KEY", "value");
    
    // Get all environment variables
    Map<String, String> allVars = envManager.getAllEnvironmentVariables();
    
    // Check if environment is available
    boolean available = envManager.isEnvironmentAvailable();
    
    // Get environment information
    Map<String, Object> info = envManager.getEnvironmentInfo();
    
} finally {
    // Cleanup environment
    envManager.cleanupEnvironment().get();
}
```

For more details, see {doc}`environment_manager`.



