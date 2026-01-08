# 沙箱

AgentScope Runtime Java 的 Sandbox 提供了一个**安全**且**隔离**的环境，用于工具执行、浏览器自动化、文件系统操作、训练评测等功能。在本教程中，您将学习如何设置工具沙箱依赖项并在沙箱环境中运行工具。

## 前提条件

```{note}
当前的沙箱环境默认使用 Docker 进行隔离。此外，我们还支持 Kubernetes (K8s) 以及阿里云函数计算 AgentRun、FC 作为远程服务后端。未来，我们计划在即将发布的版本中加入更多第三方托管解决方案。
```


>对于使用**苹果芯片**（如M1/M2）的设备，我们建议以下选项来运行**x86** Docker环境以获得最大兼容性：
> * Docker Desktop：请参阅[Docker Desktop安装指南](https://docs.docker.com/desktop/setup/install/mac-install/)以启用Rosetta2，确保与x86_64镜像的兼容性。
> * Colima：确保启用Rosetta 2支持。您可以使用以下命令启动[Colima](https://github.com/abiosoft/colima)以实现兼容性：`colima start --vm-type=vz --vz-rosetta --memory 8 --cpu 1`


- Docker（默认）

以下几种部署后端均为扩展模块，使用时需要单独引入


- Kubernetes
- 阿里云函数计算 AgentRun
- 函数计算 FC

## 安装

### 安装依赖项

首先，安装 AgentScope Runtime：

### 准备 Docker 镜像

沙箱为不同功能使用不同的 Docker 镜像。您可以只拉取需要的镜像，或者拉取所有镜像以获得完整功能：

#### 选项1：拉取所有镜像（推荐）

为了确保完整的沙箱体验并启用所有功能，请按照以下步骤从我们的仓库拉取并标记必要的 Docker 镜像：

> **镜像来源：阿里云容器镜像服务**
>
> 所有Docker镜像都托管在阿里云容器镜像服务(ACR)上，以在全球范围内实现可获取和可靠性。镜像从ACR拉取后使用标准名称重命名，以与AgentScope Runtime无缝集成。

```bash
# 基础镜像
docker pull agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-base:latest && docker tag agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-base:latest agentscope/runtime-sandbox-base:latest

# GUI镜像
docker pull agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-gui:latest && docker tag agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-gui:latest agentscope/runtime-sandbox-gui:latest

# 文件系统镜像
docker pull agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-filesystem:latest && docker tag agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-filesystem:latest agentscope/runtime-sandbox-filesystem:latest

# 浏览器镜像
docker pull agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-browser:latest && docker tag agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-browser:latest agentscope/runtime-sandbox-browser:latest

# 移动端镜像
docker pull agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-mobile:latest && docker tag agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-mobile:latest agentscope/runtime-sandbox-mobile:latest
```

#### 选项2：拉取特定镜像

根据您的具体需求选择镜像：

| Image                | Purpose                   | When to Use                                                  |
| -------------------- | ------------------------- | ------------------------------------------------------------ |
| **Base Image**       | Python代码执行，shell命令 | 基本工具执行必需                                             |
| **GUI Image**        | 计算机操作                | 当你需要图形操作页面时                                       |
| **Filesystem Image** | 文件系统操作              | 当您需要文件读取/写入/管理时                                 |
| **Browser Image**    | Web浏览器自动化           | 当您需要网络爬取或浏览器控制时                               |
| **Mobile Image**     | 移动端操作                | 当您需要操作移动端设备时                                     |
| **Training Image**   | 训练和评估智能体          | 当你需要在某些基准数据集上训练和评估智能体时 （详情请参考 [训练用沙箱](training_sandbox.md)  ） |

### （可选）从头构建Docker镜像

如果您更倾向于在本地自己通过 `Dockerfile` 构建镜像或需要自定义修改，可以从头构建它们。请参阅 [工具沙箱高级用法](sandbox/advanced.md) 了解详细说明。

## 沙箱使用

### 创建沙箱

前面的部分介绍了以工具为中心的使用方法，而本节介绍以沙箱为中心的使用方法。

您可以通过 `sandbox` SDK创建不同类型的沙箱。通过 `SandboxService` 管理沙箱生命周期，支持会话管理和沙箱复用。


```java
import com.google.gson.Gson;
import io.agentscope.runtime.sandbox.box.BaseSandbox;
import io.agentscope.runtime.sandbox.box.Sandbox;
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
        try (Sandbox sandbox = new BaseSandbox(sandboxService, "userId", "sessionId")) {
            Gson gson = new Gson();
            String tools = gson.toJson(sandbox.listTools(""));
            System.out.println("Available tools: ");
            System.out.println(tools);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

返回的结果如下所示：
```json
{
  "generic": {
    "run_ipython_cell": {
      "json_schema": {
        "function": {
          "name": "run_ipython_cell",
          "description": "Run an IPython cell.",
          "parameters": {
            "type": "object",
            "properties": {
              "code": {
                "description": "IPython code to execute",
                "type": "string"
              }
            },
            "required": [
              "code"
            ]
          }
        },
        "type": "function"
      },
      "name": "run_ipython_cell"
    },
    "run_shell_command": {
      "json_schema": {
        "function": {
          "name": "run_shell_command",
          "description": "Run a shell command.",
          "parameters": {
            "type": "object",
            "properties": {
              "command": {
                "description": "Shell command to execute",
                "type": "string"
              }
            },
            "required": [
              "command"
            ]
          }
        },
        "type": "function"
      },
      "name": "run_shell_command"
    }
  }
}
```

可以看到基础沙箱中提供了**运行命令**和**执行python代码**两种工具


* **基础沙箱（Base Sandbox）**：用于在隔离环境中运行 **Python 代码** 或 **Shell 命令**。

```java
import io.agentscope.runtime.sandbox.box.BaseSandbox;
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
        try (BaseSandbox baseSandbox = new BaseSandbox(sandboxService, "userId", "sessionId")) {
            System.out.println(baseSandbox.listTools(""));
            String pythonResult = baseSandbox.runIpythonCell("print('Hello from the sandbox!')");
            System.out.println("Sandbox execution result: " + pythonResult);
            String shellResult = baseSandbox.runShellCommand("echo Hello, World!");
            System.out.println("Shell command result: " + shellResult);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

* **GUI 沙箱 （GUI Sandbox）**： 提供**可视化桌面环境**，可执行鼠标、键盘以及屏幕相关操作。

  <img src="https://img.alicdn.com/imgextra/i2/O1CN01df5SaM1xKFQP4KGBW_!!6000000006424-2-tps-2958-1802.png" alt="GUI Sandbox" width="800" height="500">

```java
import com.google.gson.Gson;
import io.agentscope.runtime.sandbox.box.GuiSandbox;
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
        try (GuiSandbox guiSandbox = new GuiSandbox(sandboxService, "userId", "sessionId")) {
            Gson gson = new Gson();
            String tools = gson.toJson(guiSandbox.listTools(""));
            System.out.println("Available tools: ");
            System.out.println(tools);

            String desktopUrl = guiSandbox.getDesktopUrl();
            System.out.println("GUI Desktop URL: " + desktopUrl);
            String cursorPosition = guiSandbox.computerUse("get_cursor_position");
            System.out.println("Cursor Position: " + cursorPosition);
            String screenShot = guiSandbox.computerUse("get_screenshot");
            System.out.println("Screenshot (base64): " + screenShot);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

* **文件系统沙箱 （Filesystem Sandbox）**：基于 GUI 的隔离沙箱，可进行文件系统操作，如创建、读取和删除文件。

  <img src="https://img.alicdn.com/imgextra/i3/O1CN01VocM961vK85gWbJIy_!!6000000006153-2-tps-2730-1686.png" alt="GUI Sandbox" width="800" height="500">

```java
import com.google.gson.Gson;
import io.agentscope.runtime.sandbox.box.FilesystemSandbox;
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
        try (FilesystemSandbox filesystemSandbox = new FilesystemSandbox(sandboxService, "userId", "sessionId")) {
            Gson gson = new Gson();
            String tools = gson.toJson(filesystemSandbox.listTools(""));
            System.out.println("Available tools: ");
            System.out.println(tools);

            String desktopUrl = filesystemSandbox.getDesktopUrl();
            System.out.println("GUI Desktop URL: " + desktopUrl);
            String cursorPosition = filesystemSandbox.createDirectory("test");
            System.out.println("Created directory 'test' at: " + cursorPosition);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

* **浏览器沙箱（Browser Sandbox）**: 基于 GUI 的沙箱，可进行浏览器操作。

  <img src="https://img.alicdn.com/imgextra/i4/O1CN01OIq1dD1gAJMcm0RFR_!!6000000004101-2-tps-2734-1684.png" alt="GUI Sandbox" width="800" height="500">

```java
import com.google.gson.Gson;
import io.agentscope.runtime.sandbox.box.BrowserSandbox;
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
        try (BrowserSandbox browserSandbox = new BrowserSandbox(sandboxService, "userId", "sessionId")) {
            Gson gson = new Gson();
            String tools = gson.toJson(browserSandbox.listTools(""));
            System.out.println("Available tools: ");
            System.out.println(tools);

            String desktopUrl = browserSandbox.getDesktopUrl();
            System.out.println("GUI Desktop URL: " + desktopUrl);
            String navigateResult = browserSandbox.navigate("https://cn.bing.com");
            System.out.println("Navigate Result: " + navigateResult);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

* **TrainingSandbox**：训练评估沙箱，详情请参考：[训练用沙箱](training_sandbox.md)。

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
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

* **云沙箱（Cloud Sandbox）**：基于云服务的沙箱环境，无需本地 Docker 容器。`CloudSandbox` 是云沙箱的基类，提供了云沙箱的统一接口

```java
// CloudSandbox 是抽象基类，通常不直接使用
// 请使用具体的云沙箱实现，如 AgentBaySandbox
```

* **AgentBay沙箱（AgentBay Sandbox）**：基于 AgentBay 云服务的沙箱实现，支持多种镜像类型（Linux、Windows、Browser、CodeSpace、Mobile等）

```java
import io.agentscope.runtime.sandbox.box.AgentBaySandbox;
import io.agentscope.runtime.sandbox.manager.ManagerConfig;
import io.agentscope.runtime.sandbox.manager.SandboxService;
import io.agentscope.runtime.sandbox.manager.client.container.BaseClientStarter;
import io.agentscope.runtime.sandbox.manager.client.container.docker.DockerClientStarter;

public class Main {
    public static void main(String[] args) {
//        创建并启动沙箱服务
        BaseClientStarter clientConfig = DockerClientStarter.builder().build();
        ManagerConfig managerConfig = ManagerConfig.builder()
                .agentBayApiKey(System.getenv("AGENTBAY_API_KEY"))
                .clientStarter(clientConfig)
                .build();
        SandboxService sandboxService = new SandboxService(managerConfig);
        sandboxService.start();

//        连接沙箱（沙箱会在执行后自动删除）
        try (AgentBaySandbox agentBaySandbox = new AgentBaySandbox(sandboxService, "user", "session", "linux_latest")) {
            System.out.println(agentBaySandbox.listTools());
            String pythonResult = agentBaySandbox.runIpythonCell("print('Hello from the sandbox!')");
            System.out.println("Sandbox execution result: " + pythonResult);
            String shellResult = agentBaySandbox.runShellCommand("echo Hello, World!");
            System.out.println("Shell command result: " + shellResult);
        }
    }
}
```

**AgentBay 沙箱特性：**

* 无需本地 Docker，完全基于云服务
* 支持多种环境类型
* 自动管理会话生命周期
* 通过 API 直接与云服务通信

> 更多沙箱类型正在开发中，敬请期待！

### 向沙箱添加MCP服务器

MCP（模型上下文协议）是一个标准化协议，使AI应用程序能够安全地连接到外部数据源和工具。通过将MCP服务器集成到您的沙箱中，您可以在不影响安全性的情况下使用专门的工具和服务扩展沙箱的功能。

沙箱支持通过`add_mcp_servers`方法集成MCP服务器。添加后，您可以使用`list_tools`发现可用工具并使用`call_tool`执行它们。

```java
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.agentscope.runtime.sandbox.box.BaseSandbox;
import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.manager.ManagerConfig;
import io.agentscope.runtime.sandbox.manager.SandboxService;
import io.agentscope.runtime.sandbox.manager.client.container.BaseClientStarter;
import io.agentscope.runtime.sandbox.manager.client.container.docker.DockerClientStarter;

import java.lang.reflect.Type;
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
        try (Sandbox sandbox = new BaseSandbox(sandboxService, "userID", "sessionID")) {
            String mcpServerConfig = """
                               {
                        "mcpServers": {
                            "time": {
                                "command": "uvx",
                                "args": [
                                    "mcp-server-time",
                                    "--local-timezone=America/New_York"
                                ]
                            }
                        }
                    }
                    """;

            Gson gson = new Gson();
            Type mcpServerType = new TypeToken<Map<String, Object>>() {
            }.getType();
            Map<String, Object> serverConfigMap = gson.fromJson(mcpServerConfig, mcpServerType);

//          	将MCP服务器添加到沙箱
            sandbox.addMcpServers(serverConfigMap);

//          	列出所有可用工具（现在包括MCP工具）
            String tools = gson.toJson(sandbox.listTools(""));
            System.out.println("Available tools: ");
            System.out.println(tools);

//          	使用MCP服务器提供的时间工具
            String result = sandbox.callTool("get_current_time", Map.of("timezone", "America/New_York"));
            System.out.println("Tool call result: ");
            System.out.println(result);
        }
    }
}
```

### 连接到远程沙箱

> 沙箱远程部署特别适用于：
> * 将计算密集型任务分离到专用服务器
> * 多个客户端共享同一沙箱环境
> * 在资源受限的本地机器上开发，同时在高性能服务器上执行
> * K8s 集群部署沙盒服务
>
> 有关sandbox-server的更高级用法，请参阅[工具沙箱高级用法](sandbox_advanced.md)了解详细说明。

您可以在本地机器或不同机器上启动沙箱服务器，以便于远程访问。您可以先启动一个runtime，作为远程沙箱管理器

要连接到远程沙箱服务，只需要在构建managerConfig的时候添加远程runtime的实际启动地址，其余操作和本地沙箱相同，在进行沙箱管理和工具调用的时候会自动将操作转发到远程runtime处理：

```java
ManagerConfig managerConfig = ManagerConfig.builder()
            .baseUrl("http://remote-host:port")
            .build();
```

## 沙箱服务

### 使用沙箱服务管理沙箱

`SandboxService` 提供了统一的沙箱管理接口，支持通过 `session_id` 和 `user_id` 来管理不同用户会话的沙箱环境。使用 `SandboxService` 可以让您更好地控制沙箱的生命周期，并实现沙箱的复用。

```java
import io.agentscope.runtime.sandbox.box.BaseSandbox;
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

        try {
//            连接到沙箱，指定需要的沙箱类型
            BaseSandbox baseSandbox = new BaseSandbox(sandboxService, "userId", "sessionId");
//            直接在沙箱实例上调用工具方法
            String pythonResult = baseSandbox.runIpythonCell("a=1");
            System.out.println("Sandbox execution result: " + pythonResult);
//            使用相同的 session_id 和 user_id 会复用同一个沙箱实例
            baseSandbox = new BaseSandbox(sandboxService, "userId", "sessionId");
            pythonResult = baseSandbox.runIpythonCell("print(a)");
            System.out.println("Sandbox execution result: " + pythonResult);
//            停止沙箱服务
            baseSandbox.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## 工具列表

* 基础工具（在所有沙箱类型中可用）
* 计算机操作工具（在`GuiSandbox`中可用）
* 文件系统工具（在`FilesystemSandbox`中可用）
* 浏览器工具（在`BrowserSandbox`中可用）


| 分类               | 工具名称                                                     | 描述                                                         |
| -------------------------------------- | ------------------------------------------- | ------------------------------------------------------------ |
| **基础工具**       | `runIpythonCell(code: String)`                               | 在IPython环境中执行Python代码                                |
|                    | `runShellCommand(command: String)`                           | 在沙箱中执行shell命令                                        |
| **文件系统工具**   | `readFile(path: String)`                                     | 读取文件的完整内容                                           |
|                    | `readMultipleFiles(paths: List<String>)`                     | 同时读取多个文件                                             |
|                    | `writeFile(path: String, content: String)`                   | 创建或覆盖文件内容                                           |
|                    | `editFile(path: String, edits: Object[], dryRun: boolean)`   | 对文本文件进行基于行的编辑                                   |
|                    | `createDirectory(path: String)`                               | 创建新目录                                                   |
|                    | `listDirectory(path: String)`                                 | 列出路径中的所有文件和目录                                   |
|                    | `directoryTree(path: String)`                                 | 获取目录结构的递归树视图                                     |
|                    | `moveFile(source: String, destination: String)`               | 移动或重命名文件和目录                                       |
|                    | `searchFiles(path: String, pattern: String, excludePatterns: String[])` | 搜索匹配模式的文件                                           |
|                    | `getFileInfo(path: String)`                                   | 获取文件或目录的详细元数据                                   |
|                    | `listAllowedDirectories()`                                    | 列出服务器可以访问的目录                                     |
| **浏览器工具**     | `navigate(url: String)`                                       | 导航到特定URL                                                |
|                    | `navigateBack()`                                              | 返回到上一页                                                 |
|                    | `navigateForward()`                                           | 前进到下一页                                                 |
|                    | `closeBrowser()`                                              | 关闭当前浏览器页面                                           |
|                    | `resize(width: Double, height: Double)`                       | 调整浏览器窗口大小                                           |
|                    | `click(element: String, ref: String)`                         | 点击Web元素                                                  |
|                    | `type(element: String, ref: String, text: String)`            | 在输入框中输入文本                                           |
|                    | `hover(element: String, ref: String)`                        | 悬停在Web元素上                                              |
|                    | `drag(startElement: String, startRef: String, endElement: String, endRef: String)` | 在元素之间拖拽                                               |
|                    | `selectOption(element: String, ref: String, values: String[])` | 在下拉菜单中选择选项                                         |
|                    | `pressKey(key: String)`                                       | 按键盘按键                                                   |
|                    | `fileUpload(paths: String[])`                                | 上传文件到页面                                               |
|                    | `snapshot()`                                                  | 捕获当前页面的可访问性快照                                   |
|                    | `takeScreenshot(raw: Boolean, filename: String, element: String, ref: String)` | 截取页面或元素的屏幕快照                                     |
|                    | `pdfSave(filename: String)`                                   | 将当前页面保存为PDF                                          |
|                    | `tabList()`                                                   | 列出所有打开的浏览器标签页                                   |
|                    | `tabNew(url: String)`                                         | 打开新标签页                                                 |
|                    | `tabSelect(index: Integer)`                                   | 切换到特定标签页                                             |
|                    | `tabClose(index: Integer)`                                    | 关闭标签页（如果未指定索引则关闭当前标签页）                 |
|                    | `waitFor(time: Double, text: String, textGone: String)`      | 等待条件或时间流逝                                           |
|                    | `consoleMessages()`                                           | 获取页面的所有控制台消息                                     |
|                    | `networkRequests()`                                           | 获取页面加载以来的所有网络请求                               |
|                    | `handleDialog(accept: Boolean, promptText: String)`           | 处理浏览器对话框（警告、确认、提示）                         |
| **计算机操作工具** | `computerUse(action: String, coordinate: List<Double>, text: String)` | 使用鼠标和键盘与桌面 GUI 互动，支持以下操作：移动光标、点击、输入文字以及截图 |
