# Training Sandbox

> This section introduces the usage of training sandboxes. We strongly recommend completing the Docker installation and prerequisites in the previous section [Sandbox](sandbox.md) before proceeding.

## Background

The Training Sandbox in AgentScope Runtime Java is primarily used for training and evaluation purposes. Training sandbox data is mainly based on public datasets (such as Appworld, Webshop, BFCL, etc.), providing data for Agent training, tool calls supplied by the dataset, and real-time reward validation.

Training sandboxes primarily implement high-concurrency data calls through Ray. After creating a sandbox, external Agents can create, execute, and evaluate instances for different samples in high concurrency.

+ [APPWorld](https://github.com/StonyBrookNLP/appworld): APPWorld is an efficient testing environment for testing and evaluating AI Agents' ability to perform complex multi-step tasks.
+ [BFCL](https://github.com/ShishirPatil/gorilla): BFCL is the first comprehensive and executable evaluation platform specifically designed to assess Large Language Models' (LLMs) function calling capabilities. Unlike previous evaluations, BFCL covers multiple forms of function calling, rich scenarios, and focuses on function call executability.

## Installation

### Appworld Case

#### Pull Required Images

Please follow these steps to pull and tag the necessary training sandbox Docker images from our repository:

> **Image Source: Alibaba Cloud Container Registry**
> 
> All Docker images are hosted on Alibaba Cloud Container Registry (ACR) for global availability and reliability. Images are pulled from ACR and then renamed with standard names for seamless integration with AgentScope Runtime Java.

```bash
# Pull Appworld image from DockerHub
docker pull agentscope/runtime-sandbox-appworld:latest

# Pull Appworld image from ACR and tag it
docker pull agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-appworld:latest && docker tag agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-appworld:latest agentscope/runtime-sandbox-appworld:latest
```

#### Verify Installation

You can verify that everything is set up correctly by calling `getEnvProfile`. If correct, it will return dataset IDs:

```java
import io.agentscope.runtime.sandbox.box.APPWorldSandbox;
import io.agentscope.runtime.sandbox.manager.ManagerConfig;
import io.agentscope.runtime.sandbox.manager.SandboxService;
import io.agentscope.runtime.sandbox.manager.client.container.BaseClientStarter;
import io.agentscope.runtime.sandbox.manager.client.container.docker.DockerClientStarter;

public class Main {
    public static void main(String[] args) {
        // Create and start the sandbox service
        BaseClientStarter clientConfig = DockerClientStarter.builder().build();
        ManagerConfig managerConfig = ManagerConfig.builder()
                .clientStarter(clientConfig)
                .build();
        SandboxService sandboxService = new SandboxService(managerConfig);
        sandboxService.start();

        // Connect to a sandbox (the sandbox will be automatically deleted after execution)
        try (APPWorldSandbox appWorldSandbox = new APPWorldSandbox(sandboxService, "userId", "sessionId")) {
            String profileList = appWorldSandbox.getEnvProfile("appworld", "train", null);
            System.out.println("Profile List: " + profileList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

#### (Optional) Build Docker Images from Scratch

If you prefer to build images locally through `Dockerfile` or need custom modifications, you can build them from scratch. Please refer to [Tool Sandbox Advanced Usage](advanced.md) for detailed instructions.

For training sandboxes, different datasets use different Dockerfiles, located in the Python version [AgentScope Runtime](https://github.com/agentscope-ai/agentscope-runtime) repository under `src/agentscope_runtime/sandbox/box/training_box/environments/{dataset_name}` directory.

Taking appworld as an example:

```bash
docker build -f src/agentscope_runtime/sandbox/box/training_box/environments/appworld/Dockerfile     -t agentscope/runtime-sandbox-appworld:latest     .
```

#### Training Sample Usage

You can create a specific training sandbox (defaults to `Appworld`), then create multiple different training samples in parallel, and execute and evaluate them separately.

#### View Dataset Samples

After building the Docker image, we can first view the dataset samples.

For example, we can use the `getEnvProfile` method to get a list of training IDs.

```java
try (APPWorldSandbox appWorldSandbox = new APPWorldSandbox(sandboxService, "userId", "sessionId")) {
    String profileList = appWorldSandbox.getEnvProfile("appworld", "train", null);
    System.out.println("Profile List: " + profileList);
}
```

#### Create Training Sample

Taking the first query in the training set as an example, you can create a training instance (Instance) through `createInstance`, which is assigned an instance ID (Instance ID).
One query can create multiple instances, and each instance uniquely corresponds to a training sample (based on the sample ID you specified when creating).
The prompt (`system prompt`) and actual question (`user prompt`) provided by the training set will be returned as a `Message List`, specifically located in the `state` of the return value.

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
        // Create and start the sandbox service
        BaseClientStarter clientConfig = DockerClientStarter.builder().build();
        ManagerConfig managerConfig = ManagerConfig.builder()
                .clientStarter(clientConfig)
                .build();
        SandboxService sandboxService = new SandboxService(managerConfig);
        sandboxService.start();

        // Connect to a sandbox (the sandbox will be automatically deleted after execution)
        try (APPWorldSandbox appWorldSandbox = new APPWorldSandbox(sandboxService, "userId", "sessionId")) {
            String profileList = appWorldSandbox.getEnvProfile("appworld", "train", null);
            System.out.println("Profile List: " + profileList);

            Gson gson = new Gson();
            Type listType = new TypeToken<List<String>>() {}.getType();
            List<String> profiles = gson.fromJson(profileList, listType);

            if (profiles.isEmpty()) {
                System.out.println("No profiles available.");
                return;
            }

            String initResponse = appWorldSandbox.createInstance("appworld", profiles.get(0));
            Type instanceType = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> instance = gson.fromJson(initResponse, instanceType);

            String instanceInfoStr = instance.get("info").toString();
            Type infoType = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> infoMap = gson.fromJson(instanceInfoStr, infoType);
            String instanceId = (String) infoMap.get("instance_id");

            String query = instance.get("state").toString();
            System.out.println("Created instance " + instanceId + " with query: " + query);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

#### Use Training Sample

Use the `step` method and specify the specific `instanceId` and `action` to get feedback results from the environment.
This method currently only supports Message format input, recommended to use `"role": "assistant"` format.

```java
Map<String, Object> action = Map.of(
        "role", "assistant",
        "content", "```python\\nprint('hello appworld!!')\\n```"
);
String result = appWorldSandbox.step(instanceId, action, null);
System.out.println("Step Result: " + result);
```

#### Evaluate Training Sample

Use the `evaluate` method to evaluate the state of an instance and get the `Reward`. Different datasets may have additional evaluation parameters, passed through `params`.

```java
String score = appWorldSandbox.evaluate(instanceId, Map.of(), Map.of("sparse", true));
System.out.println("Evaluation Score: " + score);
```

#### Release Training Sample

To reduce memory overhead, it is recommended to use the `releaseInstance` method after using the sample.
Additionally, during training sandbox operation, inactive instances are periodically cleared every 5 minutes.

```java
String success = appWorldSandbox.releaseInstance(instanceId);
System.out.println("Instance released: " + success);
```

### BFCL Case

#### Pull Required Images
Please follow these steps to pull and tag the necessary training sandbox Docker images from our repository:

> **Image Source: Alibaba Cloud Container Registry**
> 
> All Docker images are hosted on Alibaba Cloud Container Registry (ACR) for global availability and reliability. Images are pulled from ACR and then renamed with standard names for seamless integration with AgentScope Runtime Java.

```bash
# Pull BFCL image from DockerHub
docker pull agentscope/runtime-sandbox-bfcl:latest

# Pull BFCL image from ACR and tag it
docker pull agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-bfcl:latest && docker tag agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-bfcl:latest agentscope/runtime-sandbox-bfcl:latest
```

<details>
  <summary> (Optional) Build Your Own Docker Image</summary>
  Run the following code in the <a href="https://github.com/agentscope-ai/agentscope-runtime">AgentScope Runtime Python</a> root directory:
</details>


```bash
docker build -f src/agentscope_runtime/sandbox/box/training_box/environments/bfcl/Dockerfile     -t agentscope/runtime-sandbox-bfcl:latest .
```

</details>

#### Initialization
BFCL has multiple sub-databases: *all, all_scoring, multi_turn, single_turn, live, non_live, non_python, python*. Please select a database before initializing the sandbox, then fill in your openai_api_key.

**Note: You need to set the OPENAI_API_KEY and DATASET_SUB_TYPE environment variables, which default to "" and "multi_turn" respectively**


```java
try (Sandbox sandbox = sandboxService.connect("sessionId", "userId", BFCLSandbox.class)){
    if(sandbox instanceof BFCLSandbox bfclSandbox){
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
    } else {
        System.err.println("Failed to connect to TrainingSandbox.");
    }
}
```

#### Use Training Sample
Refer to the following simulated conversation:
<details><summary>Simulated Conversation</summary>

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

#### Evaluate Instance
```java
String score = bfclSandbox.evaluate(instanceId, Map.of("sparse", true), null);
System.out.println("[RESULT] sparse_score = " + score);
```
#### Release Instance
```java
bfclSandbox.releaseInstance(instanceId);
```

