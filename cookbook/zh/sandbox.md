# 工具沙箱

AgentScope Runtime Java 的 Sandbox 提供了一个**安全**且**隔离**的环境，用于工具执行、浏览器自动化、文件系统操作、训练评测等功能。在本教程中，您将学习如何设置工具沙箱依赖项并在沙箱环境中运行工具。

## 前提条件

```{note}
当前的沙箱环境默认使用 Docker 进行隔离。此外，我们还支持 Kubernetes (K8s) 和 阿里云函数计算Agentrun 作为远程服务后端。未来，我们计划在即将发布的版本中加入更多第三方托管解决方案。
```

````{warning}
对于使用**苹果芯片**（如M1/M2）的设备，我们建议以下选项来运行**x86** Docker环境以获得最大兼容性：
* Docker Desktop：请参阅[Docker Desktop安装指南](https://docs.docker.com/desktop/setup/install/mac-install/)以启用Rosetta2，确保与x86_64镜像的兼容性。
* Colima：确保启用Rosetta 2支持。您可以使用以下命令启动[Colima](https://github.com/abiosoft/colima)以实现兼容性：`colima start --vm-type=vz --vz-rosetta --memory 8 --cpu 1`
````

- Docker
- （可选，仅支持远程模式）Kubernetes
- （可选）阿里云函数计算 Agentrun
- Java 8 或更高版本
- Maven 或 Gradle

## 安装

### 安装依赖项

首先，在您的 Maven 项目中添加 AgentScope Runtime 依赖：

```xml
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-runtime-core</artifactId>
    <version>0.1.0</version>
</dependency>
```

### 准备Docker镜像

沙箱为不同功能使用不同的Docker镜像。您可以只拉取需要的镜像，或者拉取所有镜像以获得完整功能：

#### 选项1：拉取所有镜像（推荐）

为了确保完整的沙箱体验并启用所有功能，请按照以下步骤从我们的仓库拉取并标记必要的Docker镜像：

```{note}
**镜像来源：阿里云容器镜像服务**

所有Docker镜像都托管在阿里云容器镜像服务(ACR)上，以在全球范围内实现可获取和可靠性。镜像从ACR拉取后使用标准名称重命名，以与AgentScope Runtime无缝集成。
```

```bash
# 基础镜像
docker pull agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-base:latest && docker tag agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-base:latest agentscope/runtime-sandbox-base:latest

# GUI镜像
docker pull agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-gui:latest && docker tag agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-gui:latest agentscope/runtime-sandbox-gui:latest

# 文件系统镜像
docker pull agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-filesystem:latest && docker tag agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-filesystem:latest agentscope/runtime-sandbox-filesystem:latest

# 浏览器镜像
docker pull agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-browser:latest && docker tag agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-browser:latest agentscope/runtime-sandbox-browser:latest
```

#### 选项2：拉取特定镜像

根据您的具体需求选择镜像：

| Image                | Purpose                   | When to Use                                                  |
| -------------------- | ------------------------- | ------------------------------------------------------------ |
| **Base Image**       | Python代码执行，shell命令 | 基本工具执行必需                                             |
| **GUI Image**        | 计算机操作                | 当你需要图形操作页面时                                       |
| **Filesystem Image** | 文件系统操作              | 当您需要文件读取/写入/管理时                                 |
| **Browser Image**    | Web浏览器自动化           | 当您需要网络爬取或浏览器控制时                               |
| **Training Image**   | 训练和评估智能体          | 当你需要在某些基准数据集上训练和评估智能体时 （详情请参考 {doc}`training_sandbox` ） |

### 验证安装

您可以通过创建并运行一个基础沙箱来验证一切设置是否正确：

```java
import io.agentscope.runtime.sandbox.box.BaseSandbox;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;
import io.agentscope.runtime.engine.service.EnvironmentManager;
import io.agentscope.runtime.engine.service.impl.DefaultEnvironmentManager;

public class SandboxVerification {
    public static void main(String[] args) {
        // 创建 SandboxManager
        ManagerConfig managerConfig = ManagerConfig.builder().build();
        SandboxManager sandboxManager = new SandboxManager(managerConfig);
        EnvironmentManager environmentManager = new DefaultEnvironmentManager(sandboxManager);
        
        // 创建基础沙箱并执行测试代码
        try (BaseSandbox sandbox = new BaseSandbox(sandboxManager, "test_user", "test_session")) {
            String result = sandbox.runIpythonCell("print('Setup successful!')");
            System.out.println(result);
        }
    }
}
```

### （可选）从头构建Docker镜像

如果您更倾向于在本地自己通过`Dockerfile`构建镜像或需要自定义修改，可以从头构建它们。请参阅{doc}`sandbox_advanced`了解详细说明。

## 沙箱使用

### 创建沙箱

在 Java 版本中，所有沙箱操作都需要先创建 `SandboxManager` 和 `EnvironmentManager`。然后通过沙箱类创建不同类型的沙箱实例。

**基础沙箱（Base Sandbox）**：用于在隔离环境中运行 **Python 代码** 或 **Shell 命令**。

```java
import io.agentscope.runtime.sandbox.box.BaseSandbox;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;
import io.agentscope.runtime.engine.service.EnvironmentManager;
import io.agentscope.runtime.engine.service.impl.DefaultEnvironmentManager;

ManagerConfig managerConfig = ManagerConfig.builder().build();
SandboxManager sandboxManager = new SandboxManager(managerConfig);
EnvironmentManager environmentManager = new DefaultEnvironmentManager(sandboxManager);

try (BaseSandbox sandbox = new BaseSandbox(sandboxManager, "user_1", "session_1")) {
    // 列出所有可用工具
    System.out.println(sandbox.listTools("all"));
    
    // 执行 Python 代码
    System.out.println(sandbox.runIpythonCell("print('hi')"));
    
    // 执行 Shell 命令
    System.out.println(sandbox.runShellCommand("echo hello"));
}
```

**GUI 沙箱 （GUI Sandbox）**： 提供**可视化桌面环境**，可执行鼠标、键盘以及屏幕相关操作。

<img src="https://img.alicdn.com/imgextra/i2/O1CN01df5SaM1xKFQP4KGBW_!!6000000006424-2-tps-2958-1802.png" alt="GUI Sandbox" width="800" height="500">

```java
import io.agentscope.runtime.sandbox.box.GuiSandbox;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;
import io.agentscope.runtime.engine.service.EnvironmentManager;
import io.agentscope.runtime.engine.service.impl.DefaultEnvironmentManager;
import java.util.ArrayList;
import java.util.List;

ManagerConfig managerConfig = ManagerConfig.builder().build();
SandboxManager sandboxManager = new SandboxManager(managerConfig);
EnvironmentManager environmentManager = new DefaultEnvironmentManager(sandboxManager);

try (GuiSandbox sandbox = new GuiSandbox(sandboxManager, "user_1", "session_1")) {
    // 列出所有可用工具
    System.out.println(sandbox.listTools("all"));
    
    // 获取桌面访问链接
    System.out.println("Desktop URL: " + sandbox.getDesktopUrl());
    
    // 获取鼠标位置
    System.out.println(sandbox.computerUse("get_cursor_position"));
    
    // 获取屏幕截图
    System.out.println(sandbox.computerUse("get_screenshot"));
}
```

**文件系统沙箱 （Filesystem Sandbox）**：基于 GUI 的隔离沙箱，可进行文件系统操作，如创建、读取和删除文件。

<img src="https://img.alicdn.com/imgextra/i4/O1CN01OIq1dD1gAJMcm0RFR_!!6000000004101-2-tps-2734-1684.png" alt="GUI Sandbox" width="800" height="500">

```java
import io.agentscope.runtime.sandbox.box.FilesystemSandbox;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;
import io.agentscope.runtime.engine.service.EnvironmentManager;
import io.agentscope.runtime.engine.service.impl.DefaultEnvironmentManager;

ManagerConfig managerConfig = ManagerConfig.builder().build();
SandboxManager sandboxManager = new SandboxManager(managerConfig);
EnvironmentManager environmentManager = new DefaultEnvironmentManager(sandboxManager);

try (FilesystemSandbox sandbox = new FilesystemSandbox(sandboxManager, "user_1", "session_1")) {
    // 列出所有可用工具
    System.out.println(sandbox.listTools("all"));
    
    // 获取桌面访问链接
    System.out.println("Desktop URL: " + sandbox.getDesktopUrl());
    
    // 创建目录
    System.out.println(sandbox.createDirectory("test"));
}
```

**浏览器沙箱（Browser Sandbox）**: 基于 GUI 的沙箱，可进行浏览器操作。

<img src="https://img.alicdn.com/imgextra/i4/O1CN01OIq1dD1gAJMcm0RFR_!!6000000004101-2-tps-2734-1684.png" alt="GUI Sandbox" width="800" height="500">

```java
import io.agentscope.runtime.sandbox.box.BrowserSandbox;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;
import io.agentscope.runtime.engine.service.EnvironmentManager;
import io.agentscope.runtime.engine.service.impl.DefaultEnvironmentManager;

ManagerConfig managerConfig = ManagerConfig.builder().build();
SandboxManager sandboxManager = new SandboxManager(managerConfig);
EnvironmentManager environmentManager = new DefaultEnvironmentManager(sandboxManager);

try (BrowserSandbox sandbox = new BrowserSandbox(sandboxManager, "user_1", "session_1")) {
    // 列出所有可用工具
    System.out.println(sandbox.listTools("all"));
    
    // 获取浏览器桌面访问链接
    System.out.println("Desktop URL: " + sandbox.getDesktopUrl());
    
    // 打开网页
    System.out.println(sandbox.navigate("https://www.google.com/"));
}
```

**TrainingSandbox**：训练评估沙箱，详情请参考：{doc}`training_sandbox`。

```java
import io.agentscope.runtime.sandbox.box.TrainingSandbox;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;
import io.agentscope.runtime.sandbox.manager.model.container.SandboxType;
import io.agentscope.runtime.engine.service.EnvironmentManager;
import io.agentscope.runtime.engine.service.impl.DefaultEnvironmentManager;

ManagerConfig managerConfig = ManagerConfig.builder().build();
SandboxManager sandboxManager = new SandboxManager(managerConfig);
EnvironmentManager environmentManager = new DefaultEnvironmentManager(sandboxManager);

// 创建训练评估用沙箱
try (TrainingSandbox sandbox = new TrainingSandbox(
        sandboxManager, 
        "user_1", 
        "session_1", 
        SandboxType.BASE)) {
    String profileList = sandbox.getEnvProfile("appworld", "train", null);
    System.out.println(profileList);
}
```

```{note}
我们很快会扩展更多类型的沙箱——敬请期待！
```

### 向沙箱添加MCP服务器

MCP（模型上下文协议）是一个标准化协议，使AI应用程序能够安全地连接到外部数据源和工具。通过将MCP服务器集成到您的沙箱中，您可以在不影响安全性的情况下使用专门的工具和服务扩展沙箱的功能。

沙箱支持通过`addMcpServers`方法集成MCP服务器。添加后，您可以使用`listTools`发现可用工具并使用`callTool`执行它们。以下是添加提供时区感知的MCP的示例：

```java
import io.agentscope.runtime.sandbox.box.BaseSandbox;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;
import io.agentscope.runtime.engine.service.EnvironmentManager;
import io.agentscope.runtime.engine.service.impl.DefaultEnvironmentManager;
import java.util.HashMap;
import java.util.Map;

ManagerConfig managerConfig = ManagerConfig.builder().build();
SandboxManager sandboxManager = new SandboxManager(managerConfig);
EnvironmentManager environmentManager = new DefaultEnvironmentManager(sandboxManager);

try (BaseSandbox sandbox = new BaseSandbox(sandboxManager, "user_1", "session_1")) {
    Map<String, Object> mcpServerConfigs = new HashMap<>();
    Map<String, Object> timeServer = new HashMap<>();
    timeServer.put("command", "uvx");
    timeServer.put("args", new String[]{
        "mcp-server-time",
        "--local-timezone=America/New_York"
    });
    
    Map<String, Object> mcpServers = new HashMap<>();
    mcpServers.put("time", timeServer);
    mcpServerConfigs.put("mcpServers", mcpServers);
    
    // 将MCP服务器添加到沙箱
    sandbox.addMcpServers(mcpServerConfigs, false);
    
    // 列出所有可用工具（现在包括MCP工具）
    System.out.println(sandbox.listTools("all"));
    
    // 使用MCP服务器提供的时间工具
    Map<String, Object> arguments = new HashMap<>();
    arguments.put("timezone", "America/New_York");
    System.out.println(sandbox.callTool("get_current_time", arguments));
}
```

### 连接到远程沙箱

```{note}
沙箱远程部署特别适用于：
* 将计算密集型任务分离到专用服务器
* 多个客户端共享同一沙箱环境
* 在资源受限的本地机器上开发，同时在高性能服务器上执行
* K8S集群部署沙盒服务

有关sandbox-server的更高级用法，请参阅{doc}`sandbox_advanced`了解详细说明。
```

要连接到远程沙箱服务，需要在 `ManagerConfig` 中配置 `baseUrl`：

```java
import io.agentscope.runtime.sandbox.box.BaseSandbox;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;
import io.agentscope.runtime.engine.service.EnvironmentManager;
import io.agentscope.runtime.engine.service.impl.DefaultEnvironmentManager;

// 配置远程沙箱服务器地址
ManagerConfig remoteConfig = ManagerConfig.builder()
        .baseUrl("http://your_IP_address:10001")
        .bearerToken("optional-token")  // 可选：如果需要认证
        .build();

SandboxManager remoteManager = new SandboxManager(remoteConfig);
EnvironmentManager environmentManager = new DefaultEnvironmentManager(remoteManager);

try (BaseSandbox sandbox = new BaseSandbox(remoteManager, "user_1", "session_1")) {
    System.out.println(sandbox.runIpythonCell("print('hi')"));
}
```

## 工具列表

* 基础工具（在所有沙箱类型中可用）
* 计算机操作工具（在`GuiSandbox`中可用）
* 文件系统工具（在`FilesystemSandbox`中可用）
* 浏览器工具（在`BrowserSandbox`中可用）

| 分类               | 工具名称                                                     | 描述                                                         |
| ------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
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
