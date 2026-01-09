# 训练用沙箱

> 本节介绍训练沙箱的用法，我们强烈建议在继续之前先完成上一节的基础教程（[沙箱](sandbox.md)） 中的 Docker 安装和注意事项。

## 背景介绍

AgentScope Runtime Java 的 Training Sandbox 主要用于训练评测的功能。训练沙箱的数据主要基于公开数据集（如 Appworld、 Webshop、 BFCL 等），提供用于 Agent 训练的数据供给、 Agent 使用数据集内供给的工具调用、实时的 Reward 验证。

训练用沙箱内主要通过 Ray 实现高并发的数据调用，在创建沙箱后，支持外部 Agent 高并发对不同样本的实例创建、执行、评测。

+ [APPWorld](https://github.com/StonyBrookNLP/appworld)：APPWorld 是一个高效的测试环境，用于测试和评估 AI Agent 在执行复杂多步骤任务的能力。
+ [BFCL](https://github.com/ShishirPatil/gorilla)：BFCL 是首个专门评估大语言模型（LLMs）函数调用能力的全面且可执行的评测平台。与以往的评测不同，BFCL涵盖了多种形式的函数调用、丰富的场景，并关注函数调用的可执行性。

## 安装

### Appworld 案例
#### 拉取所需的镜像

请按照以下步骤从我们的仓库拉取并标记必要的训练用沙盒Docker镜像：

> **镜像来源：阿里云容器镜像服务**
> 
> 所有 Docker 镜像都托管在阿里云容器镜像服务(ACR)上，以在全球范围内实现可获取和可靠性。镜像从ACR拉取后使用标准名称重命名，以与 AgentScope Runtime Java 无缝集成。

```bash
# 从 DockerHub 拉取 Appworld 镜像
docker pull agentscope/runtime-sandbox-appworld:latest

# 从 ACR 拉取 Appworld 镜像并打标签
docker pull agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-appworld:latest && docker tag agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-appworld:latest agentscope/runtime-sandbox-appworld:latest
```

#### 验证安装

您可以通过调用`getEnvProfile`来验证一切设置是否正确，如果正确将返回数据集ID：

```java
import io.agentscope.runtime.sandbox.box.APPWorldSandbox;
import io.agentscope.runtime.sandbox.manager.ManagerConfig;
import io.agentscope.runtime.sandbox.manager.SandboxService;
import io.agentscope.runtime.sandbox.manager.client.container.BaseClientStarter;
import io.agentscope.runtime.sandbox.manager.client.container.docker.DockerClientStarter;

public class Main {
    public static void main(String[] args) {
//        创建并启动沙箱服务
        BaseClientStarter clientConfig = DockerClientStarter.builder().build();
        ManagerConfig managerConfig = ManagerConfig.builder()
                .clientStarter(clientConfig)
                .build();
        SandboxService sandboxService = new SandboxService(managerConfig);
        sandboxService.start();

//        连接沙箱（沙箱会在执行后自动删除）
        try (APPWorldSandbox appWorldSandbox = new APPWorldSandbox(sandboxService, "userId", "sessionId")) {
            String profileList = appWorldSandbox.getEnvProfile("appworld", "train", null);
            System.out.println("Profile List: " + profileList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

#### （可选）从头构建Docker镜像

如果您更倾向于在本地自己通过`Dockerfile`构建镜像或需要自定义修改，可以从头构建它们。请参阅[工具沙箱高级用法](advanced.md)了解详细说明。

对于训练用沙箱，不同数据集使用不同的DockerFile，其路径在 Python 版本 [AgentScope Runtime](https://github.com/agentscope-ai/agentscope-runtime) 仓库`src/agentscope_runtime/sandbox/box/training_box/environments/{dataset_name}` 目录下

以appworld为例：

```bash
docker build -f src/agentscope_runtime/sandbox/box/training_box/environments/appworld/Dockerfile     -t agentscope/runtime-sandbox-appworld:latest     .
```

#### 训练样本使用

您可以创建某一个具体的训练用沙箱（默认为 `Appworld` ），随后可以并行创建多个不同的训练样本，并且分别执行、评测。

#### 查看数据集样本

构建 Docker 镜像后，我们可以首先查看数据集样本。

例如，我们可以使用 `getEnvProfile` 方法获取训练ID列表。

```java
try (APPWorldSandbox appWorldSandbox = new APPWorldSandbox(sandboxService, "userId", "sessionId")) {
    String profileList = appWorldSandbox.getEnvProfile("appworld", "train", null);
    System.out.println("Profile List: " + profileList);
}
```

#### 创建训练样本

以取训练集中的第1个query为例，可以通过`createInstance`创建1个训练实例（Instance)，并分配了一个实例ID（Instance ID）。
一个Query可以创建多个实例，一个实例唯一对应一个训练样本（基于您创建时，指定的样本ID）
其中，训练集提供的prompt (`system prompt`) 和 实际问题 (`user prompt`) 均会以`Message List`返回，具体位置于返回值的`state`
中

```java
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.agentscope.runtime.sandbox.box.APPWorldSandbox;
import io.agentscope.runtime.sandbox.manager.ManagerConfig;
import io.agentscope.runtime.sandbox.manager.SandboxService;
import io.agentscope.runtime.sandbox.manager.client.container.BaseClientStarter;
import io.agentscope.runtime.sandbox.manager.client.container.docker.DockerClientStarter;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
//        创建并启动沙箱服务
        BaseClientStarter clientConfig = DockerClientStarter.builder().build();
        ManagerConfig managerConfig = ManagerConfig.builder()
                .clientStarter(clientConfig)
                .build();
        SandboxService sandboxService = new SandboxService(managerConfig);
        sandboxService.start();

//        连接沙箱（沙箱会在执行后自动删除）
        try (APPWorldSandbox appWorldSandbox = new APPWorldSandbox(sandboxService, "userId", "sessionId")) {
            String profileList = appWorldSandbox.getEnvProfile("appworld","train", null);
            System.out.println("Profile List: " + profileList);

            Gson gson = new Gson();
            Type listType = new TypeToken<List<String>>(){}.getType();
            List<String> list = gson.fromJson(profileList, listType);
            String initResponse = appWorldSandbox.createInstance("appworld", list.get(0));
            Type instanceType = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> instance = gson.fromJson(initResponse, instanceType);
            String instanceInfo = instance.get("info").toString();
            Type infoType = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> infoMap = gson.fromJson(instanceInfo, infoType);
            String instanceId = (String) infoMap.get("instance_id");
            String query = instance.get("state").toString();
            System.out.println("Created instance " + instanceId + " with query: " + query);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

#### 使用训练样本

使用`step`方法，并指定具体的`instanceId`和`action`，可以得到环境内反馈结果。
该方法目前仅支持输入Message格式，建议以` "role": "assistant"` 方式输入。

```java
Map<String, Object> action = Map.of(
        "role", "assistant",
        "content", "```python\\nprint('hello appworld!!')\\n```"
);
String result = appWorldSandbox.step(instanceId, action, null);
System.out.println("Step Result: " + result);
```

#### 评测训练样本

使用`evaluate`方法，并评测某个实例的状态，并获取`Reward`。不同的数据集可能含有额外的测评参数，通过`params`传入。

```java
String score = appWorldSandbox.evaluate(instanceId, Map.of(), Map.of("sparse", true));
System.out.println("Evaluation Score: " + score);
```

#### 释放训练样本

为了减少内存开销，建议在使用完样本后使用`releaseInstance`方法。
同时，在训练用沙箱运行期间，每5分钟亦会定期清除非活跃实例。

```java
String success = appWorldSandbox.releaseInstance(instanceId);
System.out.println("Instance released: " + success);
```

### BFCL案例

#### 拉取所需的镜像
请按照以下步骤从我们的仓库拉取并标记必要的训练用沙盒Docker镜像：

> **镜像来源：阿里云容器镜像服务**
> 
> 所有 Docker 镜像都托管在阿里云容器镜像服务(ACR)上，以在全球范围内实现可获取和可靠性。镜像从 ACR 拉取后使用标准名称重命名，以与 AgentScope Runtime Java 无缝集成。

```bash
# 从 DockerHub 拉取 BFCL 镜像
docker pull agentscope/runtime-sandbox-bfcl:latest

# 从 ACR 拉取 BFCL 镜像并打标签
docker pull agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-bfcl:latest && docker tag agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-bfcl:latest agentscope/runtime-sandbox-bfcl:latest
```

<details>
  <summary> (可选) 建立自己的Docker镜像</summary>
  在 <a href="https://github.com/agentscope-ai/agentscope-runtime">AgentScope Runtime Python</a> 根目录运行以下代码：
</details>
```bash
docker build -f src/agentscope_runtime/sandbox/box/training_box/environments/bfcl/Dockerfile     -t agentscope/runtime-sandbox-bfcl:latest .
```

</details>

#### 初始化
BFCL 有多个子数据库 *all, all_scoring, multi_turn, single_turn, live, non_live, non_python, python*.在初始化沙盒前请选择一个数据库，然后填上自己的openai_api_key。

**注意：这里需要设置 OPENAI_API_KEY 以及 DATASET_SUB_TYPE 环境变量，二者默认分别为 "" 和 "multi_turn"**


```java
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.agentscope.runtime.sandbox.box.APPWorldSandbox;
import io.agentscope.runtime.sandbox.box.BFCLSandbox;
import io.agentscope.runtime.sandbox.manager.ManagerConfig;
import io.agentscope.runtime.sandbox.manager.SandboxService;
import io.agentscope.runtime.sandbox.manager.client.container.BaseClientStarter;
import io.agentscope.runtime.sandbox.manager.client.container.docker.DockerClientStarter;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
//        创建并启动沙箱服务
        BaseClientStarter clientConfig = DockerClientStarter.builder().build();
        ManagerConfig managerConfig = ManagerConfig.builder()
                .clientStarter(clientConfig)
                .build();
        SandboxService sandboxService = new SandboxService(managerConfig);
        sandboxService.start();

//        连接沙箱（沙箱会在执行后自动删除）
        try (BFCLSandbox bfclSandbox = new BFCLSandbox(sandboxService, "userId", "sessionId")) {
            String profileList = bfclSandbox.getEnvProfile("bfcl");
            System.out.println("Connected to BFCLSandbox. Profile List: " + profileList);

            Gson gson = new Gson();
            Type listType = new TypeToken<List<String>>(){}.getType();
            List<String> list = gson.fromJson(profileList, listType);
            String initResponse = bfclSandbox.createInstance("bfcl", list.get(0));
            Type instanceType = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> instance = gson.fromJson(initResponse, instanceType);
            String instanceInfo = instance.get("info").toString();
            Type infoType = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> infoMap = gson.fromJson(instanceInfo, infoType);
            String instanceId = (String) infoMap.get("instance_id");
            String query = instance.get("state").toString();
            System.out.println("Created instance " + instanceId + " with query: " + query);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

#### 使用训练样本
参考以下模拟的对话：
<details><summary>模拟对话</summary>

```java
List<Map<String, Object>> assistantMessages = List.of(
        Map.of("role", "assistant", "content", "'<tool_call>\\n{\"name\": \"cd\", \"arguments\": {\"folder\": \"document\"}}\\n</tool_call>\\n<tool_call>\\n{\"name\": \"mkdir\", \"arguments\": {\"dir_name\": \"temp\"}}\\n</tool_call>\\n<tool_call>\\n{\"name\": \"mv\", \"arguments\": {\"source\": \"final_report.pdf\", \"destination\": \"temp\"}}\\n</tool_call>'"),
        Map.of("role", "assistant", "content", "'ok.1'"),
        Map.of("role", "assistant", "content", "'<tool_call>\\n{\"name\": \"cd\", \"arguments\": {\"folder\": \"temp\"}}\\n</tool_call>\\n<tool_call>\\n{\"name\": \"grep\", \"arguments\": {\"file_name\": \"final_report.pdf\", \"pattern\": \"budget analysis\"}}\\n</tool_call>'"),
        Map.of("role", "assistant", "content", "'ok.2'"),
        Map.of("role", "assistant", "content", "'<tool_call>\\n{\"name\": \"sort\", \"arguments\": {\"file_name\": \"final_report.pdf\"}}\\n</tool_call>'"),
        Map.of("role", "assistant", "content", "'ok.2'"),
        Map.of("role", "assistant", "content", "'<tool_call>\\n{\"name\": \"cd\", \"arguments\": {\"folder\": \"..\"}}\\n</tool_call>\\n<tool_call>\\n{\"name\": \"mv\", \"arguments\": {\"source\": \"previous_report.pdf\", \"destination\": \"temp\"}}\\n</tool_call>\\n<tool_call>\\n{\"name\": \"cd\", \"arguments\": {\"folder\": \"temp\"}}\\n</tool_call>\\n<tool_call>\\n{\"name\": \"diff\", \"arguments\": {\"file_name1\": \"final_report.pdf\", \"file_name2\": \"previous_report.pdf\"}}\\n</tool_call>'"),
        Map.of("role", "assistant", "content", "'ok.2'")
);
```

</details>

```java
for (int i = 1; i <= assistantMessages.size(); ++i) {
    Map<String, Object> msg = assistantMessages.get(i);
    String response = bfclSandbox.step(instanceId, msg, null);
    Map<String, Object> responseMap = gson.fromJson(response, instanceType);
    System.out.println("[ TURN " + i + " term=" + responseMap.get("is_terminated") + " reward=" + responseMap.get("reward") + "\n state: " + responseMap.getOrDefault("state", ""));
    if((boolean) responseMap.get("is_terminated")){
        break;
    }
}
```

#### 评估实例
```java
String score = bfclSandbox.evaluate(instanceId, Map.of("sparse", true), null);
System.out.println("[RESULT] sparse_score = " + score);
```
#### 释放实例
```java
bfclSandbox.releaseInstance(instanceId);
```
