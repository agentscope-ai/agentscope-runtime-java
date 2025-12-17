# Sandbox

AgentScope Runtime Java's Sandbox provides a **secure** and **isolated** environment for tool execution, browser automation, file system operations, training and evaluation, and more. In this tutorial, you will learn how to set up tool sandbox dependencies and run tools in a sandbox environment.

## Prerequisites

```{note}
The current sandbox environment uses Docker for isolation by default. Additionally, we support Kubernetes (K8s) and Alibaba Cloud Function Compute AgentRun as remote service backends. In the future, we plan to add more third-party hosting solutions in upcoming releases.
```

>For devices using **Apple Silicon** (such as M1/M2), we recommend the following options to run **x86** Docker environments for maximum compatibility:
> * Docker Desktop: Please refer to the [Docker Desktop Installation Guide](https://docs.docker.com/desktop/setup/install/mac-install/) to enable Rosetta2, ensuring compatibility with x86_64 images.
> * Colima: Ensure Rosetta 2 support is enabled. You can start [Colima](https://github.com/abiosoft/colima) with the following command for compatibility: `colima start --vm-type=vz --vz-rosetta --memory 8 --cpu 1`

- Docker (default)
- Kubernetes
- Alibaba Cloud Function Compute AgentRun

## Installation

### Install Dependencies

First, install AgentScope Runtime:

```bash
pip install agentscope-runtime
```

### Prepare Docker Images

Sandboxes use different Docker images for different functionalities. You can pull only the images you need, or pull all images for complete functionality:

#### Option 1: Pull All Images (Recommended)

To ensure a complete sandbox experience and enable all features, follow these steps to pull and tag the necessary Docker images from our repository:

> **Image Source: Alibaba Cloud Container Registry**
>
> All Docker images are hosted on Alibaba Cloud Container Registry (ACR) for global availability and reliability. Images are pulled from ACR and then renamed with standard names for seamless integration with AgentScope Runtime.

```bash
# Base Image
docker pull agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-base:latest && docker tag agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-base:latest agentscope/runtime-sandbox-base:latest

# GUI Image
docker pull agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-gui:latest && docker tag agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-gui:latest agentscope/runtime-sandbox-gui:latest

# Filesystem Image
docker pull agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-filesystem:latest && docker tag agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-filesystem:latest agentscope/runtime-sandbox-filesystem:latest

# Browser Image
docker pull agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-browser:latest && docker tag agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-browser:latest agentscope/runtime-sandbox-browser:latest

# Mobile Image
docker pull agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-mobile:latest && docker tag agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-mobile:latest agentscope/runtime-sandbox-mobile:latest
```

#### Option 2: Pull Specific Images

Choose images based on your specific needs:

| Image                | Purpose                   | When to Use                                                  |
| -------------------- | ------------------------- | ------------------------------------------------------------ |
| **Base Image**       | Python code execution, shell commands | Required for basic tool execution                                             |
| **GUI Image**        | Computer operations                | When you need graphical operation pages                                       |
| **Filesystem Image** | File system operations              | When you need file read/write/management                                 |
| **Browser Image**    | Web browser automation           | When you need web scraping or browser control                               |
| **Mobile Image**     | Mobile operations                | When you need to operate mobile devices                                     |
| **Training Image**   | Training and evaluating agents          | When you need to train and evaluate agents on certain benchmark datasets (see [Training Sandbox](training_sandbox.md) for details) |

### (Optional) Build Docker Images from Scratch

If you prefer to build images locally through `Dockerfile` or need custom modifications, you can build them from scratch. Please refer to [Tool Sandbox Advanced Usage](advanced.md) for detailed instructions.

## Sandbox Usage

### Creating Sandboxes

The previous section introduced tool-centric usage, while this section introduces sandbox-centric usage.

You can create different types of sandboxes through the `sandbox` SDK. Use `SandboxService` to manage sandbox lifecycle, supporting session management and sandbox reuse.

```java
import io.agentscope.runtime.engine.services.sandbox.SandboxService;
import io.agentscope.runtime.sandbox.box.BaseSandbox;
import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.client.config.BaseClientConfig;
import io.agentscope.runtime.sandbox.manager.client.config.DockerClientConfig;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;

import com.google.gson.Gson;

public class Main {
    public static void main(String[] args) {
        // Create and start sandbox service
        BaseClientConfig clientConfig = DockerClientConfig.builder().build();
        ManagerConfig managerConfig = ManagerConfig.builder()
                .containerDeployment(clientConfig)
                .build();
        SandboxService sandboxService = new SandboxService(
                new SandboxManager(managerConfig)
        );
        sandboxService.start();

        // Connect to sandbox (sandbox will be automatically deleted after execution)
        try (Sandbox sandbox = sandboxService.connect("sessionId", "userId", BaseSandbox.class)){
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

The returned result is as follows:
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

As you can see, the base sandbox provides two tools: **run command** and **execute python code**.

* **Base Sandbox**: Used to run **Python code** or **Shell commands** in an isolated environment.

```java
try (Sandbox sandbox = sandboxService.connect("sessionId", "userId", BaseSandbox.class)){
        System.out.println(sandbox.listTools(""));
        if(sandbox instanceof BaseSandbox baseSandbox) {
String pythonResult = baseSandbox.runIpythonCell("print('Hello from the sandbox!')");
        System.out.println("Sandbox execution result: " + pythonResult);
String shellResult = baseSandbox.runShellCommand("echo Hello, World!");
        System.out.println("Shell command result: " + shellResult);
    }
            }
```

* **GUI Sandbox**: Provides a **visual desktop environment** for mouse, keyboard, and screen-related operations.

  <img src="https://img.alicdn.com/imgextra/i2/O1CN01df5SaM1xKFQP4KGBW_!!6000000006424-2-tps-2958-1802.png" alt="GUI Sandbox" width="800" height="500">

```java
try (Sandbox sandbox = sandboxService.connect("sessionId", "userId", GuiSandbox.class)){
Gson gson = new Gson();
String tools = gson.toJson(sandbox.listTools(""));
System.out.println("Available tools: ");
System.out.println(tools);

if(sandbox instanceof GuiSandbox guiSandbox) {
String desktopUrl = guiSandbox.getDesktopUrl();
System.out.println("GUI Desktop URL: " + desktopUrl);
String cursorPosition = guiSandbox.computerUse("get_cursor_position");
System.out.println("Cursor Position: " + cursorPosition);
String screenShot = guiSandbox.computerUse("get_screenshot");
System.out.println("Screenshot (base64): " + screenShot);
}
}
```

* **Filesystem Sandbox**: A GUI-based isolated sandbox for file system operations such as creating, reading, and deleting files.

  <img src="https://img.alicdn.com/imgextra/i3/O1CN01VocM961vK85gWbJIy_!!6000000006153-2-tps-2730-1686.png" alt="GUI Sandbox" width="800" height="500">

```java
try (Sandbox sandbox = sandboxService.connect("sessionId", "userId", FilesystemSandbox.class)){
Gson gson = new Gson();
String tools = gson.toJson(sandbox.listTools(""));
    System.out.println("Available tools: ");
    System.out.println(tools);

    if(sandbox instanceof FilesystemSandbox filesystemSandbox) {
String desktopUrl = filesystemSandbox.getDesktopUrl();
        System.out.println("GUI Desktop URL: " + desktopUrl);
String cursorPosition = filesystemSandbox.createDirectory("test");
        System.out.println("Created directory 'test' at: " + cursorPosition);
    }
            }
```

* **Browser Sandbox**: A GUI-based sandbox for browser operations.

  <img src="https://img.alicdn.com/imgextra/i4/O1CN01OIq1dD1gAJMcm0RFR_!!6000000004101-2-tps-2734-1684.png" alt="GUI Sandbox" width="800" height="500">

```java
try (Sandbox sandbox = sandboxService.connect("sessionId", "userId", BrowserSandbox.class)){
Gson gson = new Gson();
String tools = gson.toJson(sandbox.listTools(""));
    System.out.println("Available tools: ");
    System.out.println(tools);

    if(sandbox instanceof BrowserSandbox browserSandbox) {
String desktopUrl = browserSandbox.getDesktopUrl();
        System.out.println("GUI Desktop URL: " + desktopUrl);
String navigateResult = browserSandbox.navigate("https://cn.bing.com");
        System.out.println("Navigate Result: " + navigateResult);
    }
            }
```

* **TrainingSandbox**: Training and evaluation sandbox. For details, please refer to: [Training Sandbox](training_sandbox.md).

```java
try (Sandbox sandbox = sandboxService.connect("sessionId", "userId", APPWorldSandbox.class)){
        if(sandbox instanceof APPWorldSandbox appWorldSandbox){
String profileList = appWorldSandbox.getEnvProfile("appworld","train",null);
        System.out.println("Profile List: " + profileList);
    } else {
            System.err.println("Failed to connect to TrainingSandbox.");
    }
            }
```

* **Cloud Sandbox**: A cloud service-based sandbox environment that doesn't require local Docker containers. `CloudSandbox` is the base class for cloud sandboxes, providing a unified interface for cloud sandboxes.

```java
// CloudSandbox is an abstract base class, typically not used directly
// Please use specific cloud sandbox implementations, such as AgentBaySandbox
```

* **AgentBay Sandbox**: A sandbox implementation based on AgentBay cloud service, supporting multiple image types (Linux, Windows, Browser, CodeSpace, Mobile, etc.)

```java
try (Sandbox sandbox = sandboxService.connect("sessionId", "userId", AgentBaySandbox.class)) {
    System.out.println(sandbox.listTools());
    if (sandbox instanceof AgentBaySandbox agentBaySandbox) {
        String pythonResult = agentBaySandbox.runIpythonCell("print('Hello from the sandbox!')");
        System.out.println("Sandbox execution result: " + pythonResult);
        String shellResult = agentBaySandbox.runShellCommand("echo Hello, World!");
        System.out.println("Shell command result: " + shellResult);
    }
}
```

**AgentBay Sandbox Features:**

* No local Docker required, completely cloud-based
* Supports multiple environment types
* Automatically manages session lifecycle
* Communicates directly with cloud services via API

> More sandbox types are under development, stay tuned!

### Adding MCP Servers to Sandbox

MCP (Model Context Protocol) is a standardized protocol that enables AI applications to securely connect to external data sources and tools. By integrating MCP servers into your sandbox, you can extend sandbox functionality with specialized tools and services without compromising security.

Sandboxes support MCP server integration through the `add_mcp_servers` method. After adding, you can use `list_tools` to discover available tools and use `call_tool` to execute them.

```java
try (Sandbox sandbox = sandboxService.connect("sessionId", "userId", BaseSandbox.class)) {
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

    // Add MCP server to sandbox
    sandbox.addMcpServers(serverConfigMap);

    // List all available tools (now includes MCP tools)
String tools = gson.toJson(sandbox.listTools(""));
    System.out.println("Available tools: ");
    System.out.println(tools);

    // Use the time tool provided by MCP server
String result = sandbox.callTool("get_current_time", Map.of("timezone", "America/New_York"));
    System.out.println("Tool call result: ");
    System.out.println(result);
}
```

### Connecting to Remote Sandbox

> Remote sandbox deployment is particularly suitable for:
> * Separating compute-intensive tasks to dedicated servers
> * Multiple clients sharing the same sandbox environment
> * Developing on resource-constrained local machines while executing on high-performance servers
> * K8s cluster sandbox service deployment
>
> For more advanced usage of sandbox-server, please see [Tool Sandbox Advanced Usage](advanced.md) for detailed instructions.

You can start a sandbox server on your local machine or a different machine for remote access. You can first start a runtime as a remote sandbox manager.

To connect to a remote sandbox service, simply add the actual startup address of the remote runtime when building managerConfig. Other operations are the same as local sandbox. Sandbox management and tool calls will automatically be forwarded to the remote runtime for processing:

```java
ManagerConfig managerConfig = ManagerConfig.builder()
            .baseUrl("http://remote-host:port")
            .build();
```

## Sandbox Service

### Managing Sandboxes with Sandbox Service

`SandboxService` provides a unified sandbox management interface, supporting management of sandbox environments for different user sessions through `session_id` and `user_id`. Using `SandboxService` gives you better control over sandbox lifecycle and enables sandbox reuse.

```java
import io.agentscope.runtime.engine.services.sandbox.SandboxService;
import io.agentscope.runtime.sandbox.box.BaseSandbox;
import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.client.config.BaseClientConfig;
import io.agentscope.runtime.sandbox.manager.client.config.DockerClientConfig;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;

public class Main {
    public static void main(String[] args) {
        // Create and start sandbox service
        BaseClientConfig clientConfig = DockerClientConfig.builder().build();
        ManagerConfig managerConfig = ManagerConfig.builder()
                .containerDeployment(clientConfig)
                .build();
        SandboxService sandboxService = new SandboxService(
                new SandboxManager(managerConfig)
        );
        sandboxService.start();

        try {
            // Connect to sandbox, specify the required sandbox type
            Sandbox sandbox = sandboxService.connect("sessionId", "userId", BaseSandbox.class);
            if(sandbox instanceof BaseSandbox baseSandbox) {
                // Call tool methods directly on sandbox instance
                String pythonResult = baseSandbox.runIpythonCell("a=1");
                System.out.println("Sandbox execution result: " + pythonResult);
            }
            // Using the same session_id and user_id will reuse the same sandbox instance
            sandbox = sandboxService.connect("sessionId", "userId", BaseSandbox.class);
            if(sandbox instanceof BaseSandbox baseSandbox) {
                // Variable a still exists because the same sandbox is reused
                String pythonResult = baseSandbox.runIpythonCell("print(a)");
                System.out.println("Sandbox execution result: " + pythonResult);
            }
            // Stop sandbox service
            sandbox.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### Adding MCP Servers with Sandbox Service

```java
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.agentscope.runtime.engine.services.sandbox.SandboxService;
import io.agentscope.runtime.sandbox.box.BaseSandbox;
import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.client.config.BaseClientConfig;
import io.agentscope.runtime.sandbox.manager.client.config.DockerClientConfig;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;

import java.lang.reflect.Type;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        BaseClientConfig clientConfig = DockerClientConfig.builder().build();
        ManagerConfig managerConfig = ManagerConfig.builder()
                .containerDeployment(clientConfig)
                .build();
        SandboxService sandboxService = new SandboxService(
                new SandboxManager(managerConfig)
        );
        sandboxService.start();

        try (Sandbox sandbox = sandboxService.connect("sessionId", "userId", BaseSandbox.class)) {
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

            // Add MCP server to sandbox
            sandbox.addMcpServers(serverConfigMap);

            // List all available tools (now includes MCP tools)
            String tools = gson.toJson(sandbox.listTools(""));
            System.out.println("Available tools: ");
            System.out.println(tools);

            // Use the time tool provided by MCP server
            String result = sandbox.callTool("get_current_time", Map.of("timezone", "America/New_York"));
            System.out.println("Tool call result: ");
            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### Connecting to Remote Sandbox with Sandbox Service

```java
import io.agentscope.runtime.engine.services.sandbox.SandboxService;
import io.agentscope.runtime.sandbox.box.BaseSandbox;
import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.client.config.BaseClientConfig;
import io.agentscope.runtime.sandbox.manager.client.config.DockerClientConfig;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;

public class Main {
    public static void main(String[] args) {
        // Create and start sandbox service
        ManagerConfig managerConfig = ManagerConfig.builder()
                .baseUrl("http://remote-host:port")
                .build();
        SandboxService sandboxService = new SandboxService(
                new SandboxManager(managerConfig)
        );
        sandboxService.start();

        try {
            // Connect to sandbox, specify the required sandbox type
            Sandbox sandbox = sandboxService.connect("sessionId", "userId", BaseSandbox.class);
            if(sandbox instanceof BaseSandbox baseSandbox) {
                // Call tool methods directly on sandbox instance
                String pythonResult = baseSandbox.runIpythonCell("a=1");
                System.out.println("Sandbox execution result: " + pythonResult);
            }
            // Stop sandbox service
            sandbox.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## Tool List

* Basic tools (available in all sandbox types)
* Computer operation tools (available in `GuiSandbox`)
* File system tools (available in `FilesystemSandbox`)
* Browser tools (available in `BrowserSandbox`)


| Category               | Tool Name                                                     | Description                                                         |
| -------------------------------------- | ------------------------------------------- | ------------------------------------------------------------ |
| **Basic Tools**       | `runIpythonCell(code: String)`                               | Execute Python code in IPython environment                                |
|                    | `runShellCommand(command: String)`                           | Execute shell command in sandbox                                        |
| **File System Tools**   | `readFile(path: String)`                                     | Read complete file content                                           |
|                    | `readMultipleFiles(paths: List<String>)`                     | Read multiple files simultaneously                                             |
|                    | `writeFile(path: String, content: String)`                   | Create or overwrite file content                                           |
|                    | `editFile(path: String, edits: Object[], dryRun: boolean)`   | Perform line-based edits on text files                                   |
|                    | `createDirectory(path: String)`                               | Create new directory                                                   |
|                    | `listDirectory(path: String)`                                 | List all files and directories in path                                   |
|                    | `directoryTree(path: String)`                                 | Get recursive tree view of directory structure                                     |
|                    | `moveFile(source: String, destination: String)`               | Move or rename files and directories                                       |
|                    | `searchFiles(path: String, pattern: String, excludePatterns: String[])` | Search for files matching pattern                                           |
|                    | `getFileInfo(path: String)`                                   | Get detailed metadata for file or directory                                   |
|                    | `listAllowedDirectories()`                                    | List directories the server can access                                     |
| **Browser Tools**     | `navigate(url: String)`                                       | Navigate to specific URL                                                |
|                    | `navigateBack()`                                              | Go back to previous page                                                 |
|                    | `navigateForward()`                                           | Go forward to next page                                                 |
|                    | `closeBrowser()`                                              | Close current browser page                                           |
|                    | `resize(width: Double, height: Double)`                       | Resize browser window                                           |
|                    | `click(element: String, ref: String)`                         | Click web element                                                  |
|                    | `type(element: String, ref: String, text: String)`            | Type text into input field                                           |
|                    | `hover(element: String, ref: String)`                        | Hover over web element                                              |
|                    | `drag(startElement: String, startRef: String, endElement: String, endRef: String)` | Drag between elements                                               |
|                    | `selectOption(element: String, ref: String, values: String[])` | Select options in dropdown menu                                         |
|                    | `pressKey(key: String)`                                       | Press keyboard key                                                   |
|                    | `fileUpload(paths: String[])`                                | Upload files to page                                               |
|                    | `snapshot()`                                                  | Capture accessibility snapshot of current page                                   |
|                    | `takeScreenshot(raw: Boolean, filename: String, element: String, ref: String)` | Take screenshot of page or element                                     |
|                    | `pdfSave(filename: String)`                                   | Save current page as PDF                                          |
|                    | `tabList()`                                                   | List all open browser tabs                                   |
|                    | `tabNew(url: String)`                                         | Open new tab                                                 |
|                    | `tabSelect(index: Integer)`                                   | Switch to specific tab                                             |
|                    | `tabClose(index: Integer)`                                    | Close tab (closes current tab if index not specified)                 |
|                    | `waitFor(time: Double, text: String, textGone: String)`      | Wait for condition or time to elapse                                           |
|                    | `consoleMessages()`                                           | Get all console messages from page                                     |
|                    | `networkRequests()`                                           | Get all network requests since page load                               |
|                    | `handleDialog(accept: Boolean, promptText: String)`           | Handle browser dialogs (alert, confirm, prompt)                         |
| **Computer Operation Tools** | `computerUse(action: String, coordinate: List<Double>, text: String)` | Interact with desktop GUI using mouse and keyboard, supporting: move cursor, click, type text, and screenshot |

