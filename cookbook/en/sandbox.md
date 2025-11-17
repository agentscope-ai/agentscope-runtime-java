# Tool Sandbox

AgentScope Runtime Java's Sandbox provides a **secure** and **isolated** environment for tool execution, browser automation, file system operations, training evaluation, and more. In this tutorial, you will learn how to set up tool sandbox dependencies and run tools in a sandboxed environment.

## Prerequisites

```{note}
The current sandbox environment uses Docker for isolation by default. Additionally, we support Kubernetes (K8s) and Alibaba Cloud Function Compute AgentRun as remote service backends. In the future, we plan to add more third-party hosting solutions in upcoming releases.
```

````{warning}
For devices using **Apple Silicon** (such as M1/M2), we recommend the following options to run **x86** Docker environments for maximum compatibility:
* Docker Desktop: Please refer to the [Docker Desktop Installation Guide](https://docs.docker.com/desktop/setup/install/mac-install/) to enable Rosetta2, ensuring compatibility with x86_64 images.
* Colima: Ensure Rosetta 2 support is enabled. You can use the following command to start [Colima](https://github.com/abiosoft/colima) for compatibility: `colima start --vm-type=vz --vz-rosetta --memory 8 --cpu 1`
````

- Docker
- (Optional, remote mode only) Kubernetes
- (Optional) Alibaba Cloud Function Compute AgentRun
- Java 8 or higher
- Maven or Gradle

## Installation

### Install Dependencies

First, add AgentScope Runtime dependency to your Maven project:

```xml
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-runtime-core</artifactId>
    <version>0.1.1</version>
</dependency>
```

### Prepare Docker Images

Sandboxes use different Docker images for different functionalities. You can pull only the images you need, or pull all images for full functionality:

#### Option 1: Pull All Images (Recommended)

To ensure a complete sandbox experience and enable all features, follow these steps to pull and tag the necessary Docker images from our repository:

```{note}
**Image Source: Alibaba Cloud Container Registry**

All Docker images are hosted on Alibaba Cloud Container Registry (ACR) for global accessibility and reliability. Images are pulled from ACR and renamed with standard names for seamless integration with AgentScope Runtime.
```

```bash
# Base image
docker pull agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-base:latest && docker tag agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-base:latest agentscope/runtime-sandbox-base:latest

# GUI image
docker pull agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-gui:latest && docker tag agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-gui:latest agentscope/runtime-sandbox-gui:latest

# Filesystem image
docker pull agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-filesystem:latest && docker tag agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-filesystem:latest agentscope/runtime-sandbox-filesystem:latest

# Browser image
docker pull agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-browser:latest && docker tag agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-browser:latest agentscope/runtime-sandbox-browser:latest
```

#### Option 2: Pull Specific Images

Choose images based on your specific needs:

| Image                | Purpose                   | When to Use                                                  |
| -------------------- | ------------------------- | ------------------------------------------------------------ |
| **Base Image**       | Python code execution, shell commands | Essential for basic tool execution                                             |
| **GUI Image**        | Computer operations                | When you need graphical interface operations                                       |
| **Filesystem Image** | File system operations              | When you need file read/write/management                                 |
| **Browser Image**    | Web browser automation           | When you need web scraping or browser control                               |
| **Training Image**   | Training and evaluating agents          | When you need to train and evaluate agents on certain benchmark datasets (see {doc}`training_sandbox` for details) |

### Verify Installation

You can verify that everything is set up correctly by creating and running a base sandbox:

```java
import io.agentscope.runtime.sandbox.box.BaseSandbox;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;
import io.agentscope.runtime.engine.service.EnvironmentManager;
import io.agentscope.runtime.engine.service.impl.DefaultEnvironmentManager;

public class SandboxVerification {
    public static void main(String[] args) {
        // Create SandboxManager
        ManagerConfig managerConfig = ManagerConfig.builder().build();
        SandboxManager sandboxManager = new SandboxManager(managerConfig);
        EnvironmentManager environmentManager = new DefaultEnvironmentManager(sandboxManager);
        
        // Create base sandbox and execute test code
        try (BaseSandbox sandbox = new BaseSandbox(sandboxManager, "test_user", "test_session")) {
            String result = sandbox.runIpythonCell("print('Setup successful!')");
            System.out.println(result);
        }
    }
}
```

### (Optional) Build Docker Images from Scratch

If you prefer to build images locally using `Dockerfile` or need custom modifications, you can build them from scratch. See {doc}`sandbox_advanced` for detailed instructions.

## Sandbox Usage

### Creating Sandboxes

In the Java version, all sandbox operations require creating `SandboxManager` and `EnvironmentManager` first. Then create different types of sandbox instances through sandbox classes.

**Base Sandbox**: Used to run **Python code** or **Shell commands** in an isolated environment.

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
    // List all available tools
    System.out.println(sandbox.listTools("all"));
    
    // Execute Python code
    System.out.println(sandbox.runIpythonCell("print('hi')"));
    
    // Execute Shell command
    System.out.println(sandbox.runShellCommand("echo hello"));
}
```

**GUI Sandbox**: Provides a **visual desktop environment** for executing mouse, keyboard, and screen-related operations.

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
    // List all available tools
    System.out.println(sandbox.listTools("all"));
    
    // Get desktop access URL
    System.out.println("Desktop URL: " + sandbox.getDesktopUrl());
    
    // Get mouse position
    System.out.println(sandbox.computerUse("get_cursor_position"));
    
    // Get screenshot
    System.out.println(sandbox.computerUse("get_screenshot"));
}
```

**Filesystem Sandbox**: A GUI-based isolated sandbox for file system operations such as creating, reading, and deleting files.

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
    // List all available tools
    System.out.println(sandbox.listTools("all"));
    
    // Get desktop access URL
    System.out.println("Desktop URL: " + sandbox.getDesktopUrl());
    
    // Create directory
    System.out.println(sandbox.createDirectory("test"));
}
```

**Browser Sandbox**: A GUI-based sandbox for browser operations.

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
    // List all available tools
    System.out.println(sandbox.listTools("all"));
    
    // Get browser desktop access URL
    System.out.println("Desktop URL: " + sandbox.getDesktopUrl());
    
    // Open webpage
    System.out.println(sandbox.navigate("https://www.google.com/"));
}
```

**TrainingSandbox**: Training evaluation sandbox. For details, see: {doc}`training_sandbox`.

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

// Create training evaluation sandbox
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
We will soon expand to more types of sandboxes—stay tuned!
```

### Adding MCP Servers to Sandbox

MCP (Model Context Protocol) is a standardized protocol that enables AI applications to securely connect to external data sources and tools. By integrating MCP servers into your sandbox, you can extend sandbox functionality with specialized tools and services without compromising security.

Sandboxes support integrating MCP servers through the `addMcpServers` method. After adding, you can use `listTools` to discover available tools and `callTool` to execute them. Here's an example of adding an MCP that provides timezone awareness:

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
    
    // Add MCP servers to sandbox
    sandbox.addMcpServers(mcpServerConfigs, false);
    
    // List all available tools (now including MCP tools)
    System.out.println(sandbox.listTools("all"));
    
    // Use time tool provided by MCP server
    Map<String, Object> arguments = new HashMap<>();
    arguments.put("timezone", "America/New_York");
    System.out.println(sandbox.callTool("get_current_time", arguments));
}
```

### Connecting to Remote Sandbox

```{note}
Remote sandbox deployment is particularly suitable for:
* Separating compute-intensive tasks to dedicated servers
* Multiple clients sharing the same sandbox environment
* Developing on resource-constrained local machines while executing on high-performance servers
* K8S cluster deployment of sandbox services

For more advanced usage of sandbox-server, see {doc}`sandbox_advanced` for detailed instructions.
```

To connect to a remote sandbox service, configure `baseUrl` in `ManagerConfig`:

```java
import io.agentscope.runtime.sandbox.box.BaseSandbox;
import io.agentscope.runtime.sandbox.manager.SandboxManager;
import io.agentscope.runtime.sandbox.manager.model.ManagerConfig;
import io.agentscope.runtime.engine.service.EnvironmentManager;
import io.agentscope.runtime.engine.service.impl.DefaultEnvironmentManager;

// Configure remote sandbox server address
ManagerConfig remoteConfig = ManagerConfig.builder()
        .baseUrl("http://your_IP_address:10001")
        .bearerToken("optional-token")  // Optional: if authentication is required
        .build();

SandboxManager remoteManager = new SandboxManager(remoteConfig);
EnvironmentManager environmentManager = new DefaultEnvironmentManager(remoteManager);

try (BaseSandbox sandbox = new BaseSandbox(remoteManager, "user_1", "session_1")) {
    System.out.println(sandbox.runIpythonCell("print('hi')"));
}
```

## Tool List

* Base tools (available in all sandbox types)
* Computer operation tools (available in `GuiSandbox`)
* File system tools (available in `FilesystemSandbox`)
* Browser tools (available in `BrowserSandbox`)

| Category               | Tool Name                                                     | Description                                                         |
| ------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| **Base Tools**       | `runIpythonCell(code: String)`                               | Execute Python code in IPython environment                                |
|                    | `runShellCommand(command: String)`                           | Execute shell commands in sandbox                                        |
| **File System Tools**   | `readFile(path: String)`                                     | Read complete content of a file                                           |
|                    | `readMultipleFiles(paths: List<String>)`                     | Read multiple files simultaneously                                             |
|                    | `writeFile(path: String, content: String)`                   | Create or overwrite file content                                           |
|                    | `editFile(path: String, edits: Object[], dryRun: boolean)`   | Perform line-based editing on text files                                   |
|                    | `createDirectory(path: String)`                               | Create a new directory                                                   |
|                    | `listDirectory(path: String)`                                 | List all files and directories in a path                                   |
|                    | `directoryTree(path: String)`                                 | Get a recursive tree view of directory structure                                     |
|                    | `moveFile(source: String, destination: String)`               | Move or rename files and directories                                       |
|                    | `searchFiles(path: String, pattern: String, excludePatterns: String[])` | Search for files matching a pattern                                           |
|                    | `getFileInfo(path: String)`                                   | Get detailed metadata of a file or directory                                   |
|                    | `listAllowedDirectories()`                                    | List directories the server can access                                     |
| **Browser Tools**     | `navigate(url: String)`                                       | Navigate to a specific URL                                                |
|                    | `navigateBack()`                                              | Go back to the previous page                                                 |
|                    | `navigateForward()`                                           | Go forward to the next page                                                 |
|                    | `closeBrowser()`                                              | Close the current browser page                                           |
|                    | `resize(width: Double, height: Double)`                       | Resize browser window                                           |
|                    | `click(element: String, ref: String)`                         | Click a web element                                                  |
|                    | `type(element: String, ref: String, text: String)`            | Type text in an input box                                           |
|                    | `hover(element: String, ref: String)`                        | Hover over a web element                                              |
|                    | `drag(startElement: String, startRef: String, endElement: String, endRef: String)` | Drag between elements                                               |
|                    | `selectOption(element: String, ref: String, values: String[])` | Select options in a dropdown menu                                         |
|                    | `pressKey(key: String)`                                       | Press a keyboard key                                                   |
|                    | `fileUpload(paths: String[])`                                | Upload files to a page                                               |
|                    | `snapshot()`                                                  | Capture accessibility snapshot of current page                                   |
|                    | `takeScreenshot(raw: Boolean, filename: String, element: String, ref: String)` | Capture screenshot of page or element                                     |
|                    | `pdfSave(filename: String)`                                   | Save current page as PDF                                          |
|                    | `tabList()`                                                   | List all open browser tabs                                   |
|                    | `tabNew(url: String)`                                         | Open a new tab                                                 |
|                    | `tabSelect(index: Integer)`                                         | Switch to a specific tab                                             |
|                    | `tabClose(index: Integer)`                                    | Close a tab (closes current tab if index not specified)                 |
|                    | `waitFor(time: Double, text: String, textGone: String)`      | Wait for a condition or time to elapse                                           |
|                    | `consoleMessages()`                                           | Get all console messages from the page                                     |
|                    | `networkRequests()`                                           | Get all network requests since page load                               |
|                    | `handleDialog(accept: Boolean, promptText: String)`           | Handle browser dialogs (alert, confirm, prompt)                         |
| **Computer Operation Tools** | `computerUse(action: String, coordinate: List<Double>, text: String)` | Interact with desktop GUI using mouse and keyboard, supporting the following operations: move cursor, click, type text, and take screenshots |




