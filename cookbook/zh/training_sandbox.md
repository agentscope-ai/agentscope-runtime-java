# 训练用沙箱

```{note}
在阅读本节之前，建议先完成基础沙箱章节 {doc}`sandbox` 中关于 Docker 与 SandboxManager 的准备工作。
```

## 背景介绍

AgentScope Runtime Java 提供了针对多种公开评测数据集的训练型沙箱（Training Sandbox）。目前内置的训练沙箱包括：

- `APPWorldSandbox`（`SandboxType.APPWORLD`）：用于 APPWorld 多步骤任务评测。
- `BFCLSandbox`（`SandboxType.BFCL`）：用于 BFCL 函数调用与工具使用评测。
- `WebShopSandbox`（`SandboxType.WEBSHOP`）：用于 WebShop 购物对话任务评测。

每个沙箱都会在容器中启动对应的数据与工具服务，并暴露统一的实例管理 API：获取数据集切分、创建训练样本、执行一步（step）、评测（evaluate）以及释放实例。

## 准备工作

### 环境要求

- Java 17 或更高版本
- Maven 3.6+
- 本地 Docker 守护进程已启动，并允许当前用户访问
- （可选）Kubernetes 或 Alibaba AgentRun，用于托管训练沙箱（如果不使用 Docker）
- 根据数据集不同，需要的额外环境变量：
  - BFCL：`OPENAI_API_KEY`（必需），`DATASET_SUB_TYPE`（默认 `multi_turn`）

### 拉取训练沙箱镜像

训练沙箱镜像托管在阿里云容器镜像服务（ACR）。请根据本机架构拉取并重命名镜像，以便 SandboxManager 直接使用。

```bash
# APPWorld（x86_64）
docker pull agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-appworld:latest \
  && docker tag agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-appworld:latest \
     agentscope/runtime-sandbox-appworld:latest

# APPWorld（ARM64，可选）
docker pull agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-appworld:latest-arm64 \
  && docker tag agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-appworld:latest-arm64 \
     agentscope/runtime-sandbox-appworld:latest-arm64

# BFCL（x86_64）
docker pull agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-bfcl:latest \
  && docker tag agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-bfcl:latest \
     agentscope/runtime-sandbox-bfcl:latest

# BFCL（ARM64，可选）
docker pull agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-bfcl:latest-arm64 \
  && docker tag agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-bfcl:latest-arm64 \
     agentscope/runtime-sandbox-bfcl:latest-arm64

# WebShop（x86_64）
docker pull agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-webshop:latest \
  && docker tag agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-webshop:latest \
     agentscope/runtime-sandbox-webshop:latest
```

### （可选）从源码构建镜像

需要自定义镜像时，可使用仓库中的 Dockerfile 手动构建：

```bash
# 以 APPWorld 为例（在仓库根目录执行）
docker build \
  -f core/src/main/resources/training_box/environments/appworld/Dockerfile \
  -t agentscope/runtime-sandbox-appworld:latest .

# BFCL 的 Dockerfile 位于
# core/src/main/resources/training_box/environments/bfcl/Dockerfile
```

## Java API 概览

训练沙箱相关的核心类位于 `io.agentscope.runtime.sandbox.box` 包中：

| 类名 | 对应沙箱类型 | 说明 |
| --- | --- | --- |
| `APPWorldSandbox` | `SandboxType.APPWORLD` | APPWorld 训练环境 |
| `BFCLSandbox` | `SandboxType.BFCL` | BFCL 函数调用评测环境 |
| `WebShopSandbox` | `SandboxType.WEBSHOP` | WebShop 购物对话环境 |

所有训练沙箱都继承自 `TrainingSandbox`，并实现以下方法：

- `getEnvProfile(envType, split, params)`：获取数据集切分信息，默认 `split="train"`。
- `getTaskIds(envType, split, params)`：获取任务 ID 列表。
- `createInstance(envType, taskId, instanceId, params)`：创建训练实例，返回 JSON 字符串，包含 `info`（实例信息）与 `state`（输入消息）。
- `step(instanceId, action, params)`：执行一步，`action` 推荐遵循对话消息格式。
- `evaluate(instanceId, messages, params)`：评估当前实例，返回评测结果。
- `releaseInstance(instanceId)`：释放实例资源。

所有方法返回的都是 JSON 字符串，可使用 Jackson 的 `ObjectMapper` 解析。

## 初始化 SandboxManager

```java
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;

SandboxManager sandboxManager = new SandboxManager();
// 或自定义配置
// ManagerConfig config = ManagerConfig.builder()
//     .poolSize(0)
//     .build();
// SandboxManager sandboxManager = new SandboxManager(config);
```

SandboxManager 默认使用本地 Docker。如果需要 Kubernetes 或 AgentRun，请通过 `ManagerConfig` 传入对应的 `containerDeployment` 配置。

在应用退出时，请调用 `sandboxManager.cleanupAllSandboxes()` 以确保容器被释放。

## APPWorld 示例

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
            // 1. 查看数据集概要
            String profileJson = sandbox.getEnvProfile("appworld", "train", null);
            JsonNode profiles = MAPPER.readTree(profileJson);
            System.out.println("Profile sample: " + profiles.get(0));

            // 2. 创建训练实例
            String initJson = sandbox.createInstance("appworld", "82e2fac_1", null, null);
            JsonNode initNode = MAPPER.readTree(initJson);
            String instanceId = initNode.path("info").path("instance_id").asText();
            JsonNode queryState = initNode.path("state");
            System.out.println("Instance " + instanceId + " state: " + queryState);

            // 3. 执行动作
            Map<String, Object> action = Map.of(
                    "role", "assistant",
                    "content", "```python\nprint('hello appworld!!')\n```"
            );
            String stepJson = sandbox.step(instanceId, action, null);
            System.out.println("Step result: " + stepJson);

            // 4. 评测
            String evalJson = sandbox.evaluate(instanceId, Map.of(), Map.of("sparse", true));
            System.out.println("Evaluate result: " + evalJson);

            // 5. 释放实例
            String release = sandbox.releaseInstance(instanceId);
            System.out.println("Release status: " + release);
        } finally {
            sandboxManager.cleanupAllSandboxes();
        }
    }
}
```

## BFCL 示例

在使用 BFCL 之前，请设置必要的环境变量：

```bash
export OPENAI_API_KEY="your_openai_api_key"
export DATASET_SUB_TYPE="multi_turn"  # 可选: all、single_turn、python 等
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

            // 构建对话步骤
            Map<String, Object> action = Map.of(
                    "role", "assistant",
                    "content", "<tool_call>...工具调用示例...</tool_call>"
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

## WebShop 示例

WebShop 沙箱提供了电商对话场景的数据集。下面展示如何读取任务、创建实例并执行一次 step：

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
                    "content", "请列出商品详情。"
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

## 清理资源

- 沙箱实例实现了 `AutoCloseable`，推荐使用 try-with-resources 自动释放。
- 在所有沙箱操作完成后，调用 `sandboxManager.cleanupAllSandboxes()`，确保容器停止并回收。

## 下一步

- {doc}`sandbox`：了解通用沙箱与工具调用。
- {doc}`manager`：掌握 `ServiceManager` 与 `EnvironmentManager` 的整体架构。
- {doc}`context_manager`：在训练样本对话完成后，将数据写入上下文。
- {doc}`quickstart`：快速构建并部署一个完整的 Java 智能体。
