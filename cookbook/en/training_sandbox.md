# Training Sandbox

```{note}
Before reading this section, it is recommended to complete the Docker and SandboxManager setup in the basic sandbox chapter {doc}`sandbox`.
```

## Background

AgentScope Runtime Java provides training sandboxes (Training Sandbox) for multiple public evaluation datasets. Currently, the built-in training sandboxes include:

- `APPWorldSandbox` (`SandboxType.APPWORLD`): For APPWorld multi-step task evaluation.
- `BFCLSandbox` (`SandboxType.BFCL`): For BFCL function calling and tool usage evaluation.
- `WebShopSandbox` (`SandboxType.WEBSHOP`): For WebShop shopping dialogue task evaluation.

Each sandbox starts the corresponding data and tool services in a container and exposes a unified instance management API: get dataset splits, create training samples, execute a step, evaluate, and release instances.

## Prerequisites

### Environment Requirements

- Java 17 or higher
- Maven 3.6+
- Local Docker daemon running and accessible by the current user
- (Optional) Kubernetes or Alibaba AgentRun for hosting training sandboxes (if not using Docker)
- Additional environment variables required depending on the dataset:
  - BFCL: `OPENAI_API_KEY` (required), `DATASET_SUB_TYPE` (default `multi_turn`)

### Pull Training Sandbox Images

Training sandbox images are hosted on Alibaba Cloud Container Registry (ACR). Pull and rename images according to your machine's architecture so SandboxManager can use them directly.

```bash
# APPWorld (x86_64)
docker pull agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-appworld:latest \
  && docker tag agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-appworld:latest \
     agentscope/runtime-sandbox-appworld:latest

# APPWorld (ARM64, optional)
docker pull agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-appworld:latest-arm64 \
  && docker tag agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-appworld:latest-arm64 \
     agentscope/runtime-sandbox-appworld:latest-arm64

# BFCL (x86_64)
docker pull agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-bfcl:latest \
  && docker tag agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-bfcl:latest \
     agentscope/runtime-sandbox-bfcl:latest

# BFCL (ARM64, optional)
docker pull agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-bfcl:latest-arm64 \
  && docker tag agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-bfcl:latest-arm64 \
     agentscope/runtime-sandbox-bfcl:latest-arm64

# WebShop (x86_64)
docker pull agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-webshop:latest \
  && docker tag agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-webshop:latest \
     agentscope/runtime-sandbox-webshop:latest
```

### (Optional) Build Images from Source

When you need to customize images, you can manually build them using the Dockerfiles in the repository:

```bash
# Using APPWorld as an example (run from repository root)
docker build \
  -f core/src/main/resources/training_box/environments/appworld/Dockerfile \
  -t agentscope/runtime-sandbox-appworld:latest .

# BFCL Dockerfile is located at
# core/src/main/resources/training_box/environments/bfcl/Dockerfile
```

## Java API Overview

Training sandbox-related core classes are located in the `io.agentscope.runtime.sandbox.box` package:

| Class Name | Corresponding Sandbox Type | Description |
| --- | --- | --- |
| `APPWorldSandbox` | `SandboxType.APPWORLD` | APPWorld training environment |
| `BFCLSandbox` | `SandboxType.BFCL` | BFCL function calling evaluation environment |
| `WebShopSandbox` | `SandboxType.WEBSHOP` | WebShop shopping dialogue environment |

All training sandboxes extend `TrainingSandbox` and implement the following methods:

- `getEnvProfile(envType, split, params)`: Get dataset split information, default `split="train"`.
- `getTaskIds(envType, split, params)`: Get task ID list.
- `createInstance(envType, taskId, instanceId, params)`: Create training instance, returns JSON string containing `info` (instance information) and `state` (input messages).
- `step(instanceId, action, params)`: Execute a step, `action` is recommended to follow conversation message format.
- `evaluate(instanceId, messages, params)`: Evaluate current instance, returns evaluation results.
- `releaseInstance(instanceId)`: Release instance resources.

All methods return JSON strings, which can be parsed using Jackson's `ObjectMapper`.

## Initialize SandboxManager

```java
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;

SandboxManager sandboxManager = new SandboxManager();
// Or custom configuration
// ManagerConfig config = ManagerConfig.builder()
//     .poolSize(0)
//     .build();
// SandboxManager sandboxManager = new SandboxManager(config);
```

SandboxManager defaults to using local Docker. If you need Kubernetes or AgentRun, pass the corresponding `containerDeployment` configuration through `ManagerConfig`.

When the application exits, call `sandboxManager.cleanupAllSandboxes()` to ensure containers are released.

## APPWorld Example

```java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.runtime.sandbox.box.APPWorldSandbox;
import io.agentscope.runtime.sandbox.manager.SandboxManager;

public class AppWorldExample {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        SandboxManager sandboxManager = new SandboxManager();
        try (APPWorldSandbox sandbox = new APPWorldSandbox(sandboxManager, "user1", "session1")) {
            // 1. View dataset overview
            String profileJson = sandbox.getEnvProfile("appworld", "train", null);
            JsonNode profiles = MAPPER.readTree(profileJson);
            System.out.println("Profile sample: " + profiles.get(0));

            // 2. Create training instance
            String initJson = sandbox.createInstance("appworld", "82e2fac_1", null, null);
            JsonNode initNode = MAPPER.readTree(initJson);
            String instanceId = initNode.path("info").path("instance_id").asText();
            JsonNode queryState = initNode.path("state");
            System.out.println("Instance " + instanceId + " state: " + queryState);

            // 3. Execute action
            Map<String, Object> action = Map.of(
                    "role", "assistant",
                    "content", "```python\nprint('hello appworld!!')\n```"
            );
            String stepJson = sandbox.step(instanceId, action, null);
            System.out.println("Step result: " + stepJson);

            // 4. Evaluate
            String evalJson = sandbox.evaluate(instanceId, Map.of(), Map.of("sparse", true));
            System.out.println("Evaluate result: " + evalJson);

            // 5. Release instance
            String release = sandbox.releaseInstance(instanceId);
            System.out.println("Release status: " + release);
        } finally {
            sandboxManager.cleanupAllSandboxes();
        }
    }
}
```

## BFCL Example

Before using BFCL, set the necessary environment variables:

```bash
export OPENAI_API_KEY="your_openai_api_key"
export DATASET_SUB_TYPE="multi_turn"  # Optional: all, single_turn, python, etc.
```

```java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.runtime.sandbox.box.BFCLSandbox;
import io.agentscope.runtime.sandbox.manager.SandboxManager;

public class BfclExample {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        SandboxManager sandboxManager = new SandboxManager();
        try (BFCLSandbox sandbox = new BFCLSandbox(sandboxManager, "bfcl-user", "bfcl-session")) {
            String profileJson = sandbox.getEnvProfile("bfcl", "train", null);
            JsonNode profiles = MAPPER.readTree(profileJson);
            String taskId = profiles.get(0).asText();

            String initJson = sandbox.createInstance("bfcl", taskId, null, null);
            JsonNode initNode = MAPPER.readTree(initJson);
            String instanceId = initNode.path("info").path("instance_id").asText();

            // Build conversation steps
            Map<String, Object> action = Map.of(
                    "role", "assistant",
                    "content", "<tool_call>...tool call example...</tool_call>"
            );
            String stepJson = sandbox.step(instanceId, action, null);
            System.out.println("Step result: " + stepJson);

            String evalJson = sandbox.evaluate(instanceId, Map.of(), Map.of("sparse", true));
            System.out.println("Evaluate result: " + evalJson);

            sandbox.releaseInstance(instanceId);
        } finally {
            sandboxManager.cleanupAllSandboxes();
        }
    }
}
```

## WebShop Example

The WebShop sandbox provides datasets for e-commerce dialogue scenarios. Below shows how to read tasks, create instances, and execute a step:

```java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.runtime.sandbox.box.WebShopSandbox;
import io.agentscope.runtime.sandbox.manager.SandboxManager;

public class WebShopExample {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        SandboxManager sandboxManager = new SandboxManager();
        try (WebShopSandbox sandbox = new WebShopSandbox(sandboxManager, "web-user", "web-session")) {
            String tasksJson = sandbox.getTaskIds("webshop", "train", null);
            JsonNode tasks = MAPPER.readTree(tasksJson);
            String taskId = tasks.get(0).asText();

            String initJson = sandbox.createInstance("webshop", taskId, null, null);
            JsonNode initNode = MAPPER.readTree(initJson);
            String instanceId = initNode.path("info").path("instance_id").asText();

            Map<String, Object> action = Map.of(
                    "role", "assistant",
                    "content", "Please list product details."
            );
            String stepJson = sandbox.step(instanceId, action, null);
            System.out.println("Step result: " + stepJson);

            sandbox.releaseInstance(instanceId);
        } finally {
            sandboxManager.cleanupAllSandboxes();
        }
    }
}
```

## Resource Cleanup

- Sandbox instances implement `AutoCloseable`, recommended to use try-with-resources for automatic release.
- After all sandbox operations are complete, call `sandboxManager.cleanupAllSandboxes()` to ensure containers are stopped and reclaimed.

## Next Steps

- {doc}`sandbox`: Learn about general sandbox and tool calling.
- {doc}`manager`: Master the overall architecture of `ServiceManager` and `EnvironmentManager`.
- {doc}`context_manager`: Write data to context after training sample conversations complete.
- {doc}`quickstart`: Quickly build and deploy a complete Java agent.




