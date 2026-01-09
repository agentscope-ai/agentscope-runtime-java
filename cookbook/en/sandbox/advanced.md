# Tool Sandbox Advanced Usage

>This section introduces advanced sandbox usage. We strongly recommend completing the basic tutorial in the previous section [Sandbox](sandbox.md) before proceeding.

## Sandbox Manager Configuration Reference

#### ManagerConfig Configuration

| Parameter            | Type  | Description                                               | Default                    | Notes                                                                                                                                             |
| ----------------------| ------------ |-----------------------------------------------------------| -------------------------- |---------------------------------------------------------------------------------------------------------------------------------------------------|
| `bearerToken` | `String` | Authentication token for calling remote runtime sandbox   | `null`                    | If set to `null`, no authentication will be performed when connecting                                                                             |
| `baseUrl` | `String` | Server binding address for calling remote runtime sandbox | `null`                 | If set to `null`, local sandbox management will be used by default                                                                                |
| `clientStarter` | `BaseClientStarter` | Container runtime                                         | `DockerClientStarter` | Currently supports `Docker`, `K8s`, `AgentRun` and `FC`                                                                                           |
| `sandboxMap`         | `SandboxMap`        | Container Management                       | `InMemorySandboxMap`  | Container management uses local storage by default. Introducing the `redis-extension` allows using `redis` as the management backend. |
| `containerPrefixKey` | `String`            | Container name prefix                                                    | `sandbox_container_`  | Prefix for created container names                                                                                                                                         |


#### Redis Configuration

Currently, mounting file directories is only supported under local Docker, with three types of mounts available:

* Copy Mount (recommended): Configure the `storageFolderPath` and `mountDir` properties. When creating a container, the contents of the storage directory are first copied to the mount path, and the container mounts this mount path to prevent contamination of the original files. When the container shuts down, the contents of the mount path are copied back.
* Read-only Mount: Configure `readonlyMounts`. The container directly mounts the local file system in read-only mode.
* Read-write Zero-copy Mount (use with caution): Configure `nonCopyMount`. The container directly mounts the local file system and has full read-write permissions to the folder.

When using this feature, you need to pass a `FileSystemConfig` instance to the `fileSystemConfig` parameter of the `Sandbox`.

##### Storage Backend

###### Local Storage

Defaults to `LocalFileSystemConfig`, no configuration needed for local conditions. Configuration is required when using `oss`.

###### OSS Configuration

Use [Alibaba Cloud Object Storage Service](https://www.aliyun.com/product/oss) for distributed file storage:

| Parameter            | Description      | Default | Notes           |
| -------------------- | ---------------- | ------- | --------------- |
| `ossEndpoint`        | OSS endpoint URL      | `null`  | Region endpoint        |
| `ossAccessKeyId`     | OSS access key ID  | `null`  | From OSS console |
| `ossAccessKeySecret` | OSS access key secret | `null`  | Keep secure        |
| `ossBucketName`      | OSS bucket name   | `null`  | Pre-created bucket  |

#### ClientConfig Configuration

Defaults to local `Docker` as runtime environment, with three optional custom configuration methods:

##### (Optional) Docker Settings

To configure specific Docker settings in the sandbox server, pass `DockerClientConfig` parameter in `containerDeployment`. Consider adjusting the following parameters:

| Parameter         | Description                  | Default   | Notes                              |
| ----------------- | ---------------------------- | --------- | ---------------------------------- |
| `portRange` | **Dynamic port range** allocatable by sandbox service (for exposing services inside containers) | `(49152, 59152)` | Must be unused high port range; avoid conflicts with system services |
| `host` | Docker daemon listening address | `localhost` | If Docker runs on remote host or Docker Desktop, set to corresponding IP or socket path |
| `port` | Docker daemon TCP listening port | `2375` | ⚠️ Only use when Docker is configured with `tcp://0.0.0.0:2375`; should be disabled in production (insecure) |
| `certPath` | **TLS certificate directory path** (for secure Docker Daemon connection) | `null` | If TLS is enabled (port typically `2376`), provide directory containing `ca.pem`, `cert.pem`, `key.pem` |

##### (Optional) K8s Settings

To configure Kubernetes-specific settings in the sandbox server, pass `KubernetesClientConfig` parameter in `containerDeployment`. Consider adjusting the following parameters:

| Parameter        | Description                                                  | Default   | Notes                                                        |
| ---------------- | ------------------------------------------------------------ | --------- | ------------------------------------------------------------ |
| `namespace`      | **Kubernetes namespace** where sandbox Pods, Services, etc. will be created | `default` | Recommended to use dedicated namespace (e.g., `agentscope-sandbox`) for isolation and cleanup |
| `kubeConfigPath` | **Local path to kubeconfig file** for authenticating and connecting to target Kubernetes cluster | `None`    | If not specified, will try to use `~/.kube/config`                        |

##### (Optional) AgentRun Settings

AgentRun is an intelligent Agent development framework based on Serverless architecture launched by Alibaba Cloud, providing a complete toolset to help developers quickly build, deploy, and manage AI Agent applications. You can deploy the sandbox server to AgentRun.

To configure [AgentRun](https://functionai.console.aliyun.com/cn-hangzhou/agent/)-specific settings in the sandbox server, pass `AgentRunClientConfig` parameter in `containerDeployment`. Consider adjusting the following parameters:

| Parameter                     | Description              | Default                          | Notes                                                                                     |
|-------------------------------| ------------------------ |----------------------------------|-------------------------------------------------------------------------------------------|
| `agentRunAccountId` | Alibaba Cloud Account ID             | `null`                     | Alibaba Cloud main account ID, login to Alibaba Cloud [RAM Console](https://ram.console.aliyun.com/profile/access-keys) to get Alibaba Cloud Account ID and AK, SK |
| `agentRunAccessKeyId` | Access Key ID               | `null`     | Alibaba Cloud AccessKey ID, requires `AliyunAgentRunFullAccess` permission                                            |
| `agentRunAccessKeySecret` | Access Key Secret           | `null`   | Alibaba Cloud AccessKey Secret                                                                       |
| `agentRunRegionId` | Deployment region ID               | `cn-hangzhou` | Agentrun deployment region ID                                                                            |
| `agentRunCpu`    | CPU specification                  | `2.0f`                          | vCPU specification                                                                                    |
| `agentRunMemory` | Memory specification                 | `2048`                           | Memory specification (MB)                                                                                 |
| `agentRunVpcId` | VPC ID                   | `null`                        | VPC network ID (optional)                                                                               |
| `agentRunVswitchIds` | VSwitch ID list             | `null`                        | VSwitch ID list (optional)                                                                          |
| `agentRunSecurityGroupId` | Security group ID                 | `null`                        | Security group ID (optional)                                                                                 |
| `agentRunPrefix` | Resource name prefix             | `agentscope-sandbox_`            | Prefix for created resource names                                                                                 |
| `agentrunLogProject` | SLS log project              | `null`                        | SLS log project name (optional)                                                                             |
| `agentrunLogStore` | SLS log store                | `null`                        | SLS log store name (optional)                                                                              |

##### (Optional) Function Compute (FC) Settings

Function Compute (FC) is an event-driven fully managed compute service. Developers don't need to manage infrastructure such as servers; they just need to write and upload code, and Function Compute will automatically prepare compute resources and run code in an elastic and reliable manner. You can deploy the sandbox server to FC.

To configure [FC](https://fcnext.console.aliyun.com/)-specific settings in the sandbox server, pass `FcClientConfig` parameter in `containerDeployment`. Consider adjusting the following parameters:

| Parameter           | Description    | Default               | Notes                                                        |
| ------------------- | -------------- | --------------------- | ------------------------------------------------------------ |
| `FcAccessKeyId`     | Access Key ID     | `null`                | Alibaba Cloud AccessKey ID, requires `AliyunAgentRunFullAccess` permission       |
| `FcAccessKeySecret` | Access Key Secret | `null`                | Alibaba Cloud AccessKey Secret                                       |
| `FcAccountId`       | Alibaba Cloud Account ID   | `null`                | Alibaba Cloud main account ID, login to Alibaba Cloud [RAM Console](https://ram.console.aliyun.com/profile/access-keys) to get Alibaba Cloud Account ID and AK, SK |
| `FcRegionId`        | Deployment region ID     | `cn-hangzhou`         | Agentrun deployment region ID                                           |
| `FcCpu`             | CPU specification        | `2.0f`                | vCPU specification                                                     |
| `FcMemory`          | Memory specification       | `2048`                | Memory specification (MB)                                                |
| `FcVpcId`           | VPC ID         | `null`                | VPC network ID (optional)                                            |
| `FcVswitchIds`      | VSwitch ID list   | `null`                | VSwitch ID list (optional)                                       |
| `FcSecurityGroupId` | Security group ID       | `null`                | Security group ID (optional)                                             |
| `FcPrefix`          | Resource name prefix   | `agentscope-sandbox_` | Prefix for created resource names                                           |
| `FcLogProject`      | SLS log project    | `null`                | SLS log project name (optional)                                      |
| `FcLogStore`        | SLS log store      | `null`                | SLS log store name (optional)                                        |

### Importing Custom Sandboxes

In addition to the default built-in sandbox types, you can implement custom sandboxes by using the `@RegisterSandbox` annotation and extending the `Sandbox` class. This enables customization such as changing the container image, adding environment variables, defining timeout durations, and more. The application automatically scans all classes annotated with `@RegisterSandbox` at startup and registers them automatically. (Currently, only one custom sandbox is supported; future versions will allow multiple custom sandboxes.)

#### Implement a Custom Sandbox Extension (e.g., `CustomSandbox.java`)

Refer to [Custom Sandbox Class](###creating-a-custom-sandbox-class).

> - The `@RegisterSandbox` annotation registers the class with the sandbox manager, making it recognizable and usable at startup.
> - The `environment` field can inject external API keys or other required configurations into the sandbox.
> - By inheriting from `Sandbox`, you can override its methods to implement additional custom logic.

#### Add Java SPI Scan Configuration

AgentScope Runtime for Java provides `SandboxProvider` as the base class for sandbox discovery. You need to create a class in your project that implements `SandboxProvider` and includes your custom sandbox, as shown below:

```java
import io.agentscope.runtime.sandbox.manager.registry.SandboxProvider;

import java.util.Collection;
import java.util.Collections;

public class CustomSandboxProvider implements SandboxProvider {

    @Override
    public Collection<Class<?>> getSandboxClasses() {
        // Register the custom CustomSandbox
        return Collections.singletonList(CustomSandbox.class);
    }
}
```

Then, in the `resources` directory, create a folder named `META-INF/services`, and inside it, create a file named `io.agentscope.runtime.sandbox.manager.registry.SandboxProvider`. In this file, add the fully qualified class name of your `SandboxProvider` implementation, for example: `io.agentscope.CustomSandboxProvider`.

## Building Custom Sandboxes

Although the built-in sandbox types cover common use cases, you may encounter scenarios that require a specialized environment or a unique combination of tools. Creating a custom sandbox allows you to tailor the execution environment to your specific needs. This section demonstrates how to build and register your own custom sandbox type.

### Install from Source (Required for Custom Sandboxes)

To create a custom sandbox, you need to use the Python version of [AgentScope Runtime](https://github.com/agentscope-ai/agentscope-runtime). Install AgentScope Runtime from source in editable mode, which allows you to modify the code and immediately see the changes:

```bash
git clone https://github.com/agentscope-ai/agentscope-runtime.git
cd agentscope-runtime
git submodule update --init --recursive
pip install -e .
```

> The `-e` (editable) flag is required when creating custom sandboxes because it allows you to:
>
> - Modify sandbox code and immediately see changes without reinstalling
> - Add your custom sandbox class to the registry
> - Iteratively develop and test custom tools

### Create a Custom Sandbox Class

You can define a custom sandbox type and register it with the system to meet specific requirements. Simply inherit from `Sandbox` and use the `@RegisterSandbox` annotation, then place the file in your project directory:

```java
import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.manager.SandboxService;
import io.agentscope.runtime.sandbox.manager.registry.RegisterSandbox;


@RegisterSandbox(
        imageName = "agentscope-registry.ap-southeast-1.cr.aliyuncs.com/agentscope/runtime-sandbox-browser:latest",
        sandboxType = "custom",
        securityLevel = "medium",
        timeout = 30,
        description = "Base Sandbox"
)
public class CustomSandbox extends Sandbox {

    public CustomSandbox(
            SandboxService managerApi,
            String userId,
            String sessionId) {
        super(managerApi, userId, sessionId, "custom");
    }
}
```

### Prepare Docker Image

Creating custom sandboxes also requires preparing corresponding Docker images. Images should contain all dependencies, tools, and configurations needed for your specific use case.

**Note: Image building operations need to be performed in the Python code repository**

> **Configuration Options:**
>
> - **Simple MCP server changes**: To simply change the initial MCP servers in the sandbox, modify the `mcp_server_configs.json` file
> - **Advanced customization**: For more advanced usage and customization, you must be very familiar with Dockerfile syntax and Docker best practices

Here is an example Dockerfile for a custom sandbox that integrates file system, browser, and some useful MCP tools in one sandbox:

```dockerfile
FROM node:22-slim

# Set ENV variables
ENV NODE_ENV=production
ENV WORKSPACE_DIR=/workspace

ARG DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y --fix-missing \
    curl \
    python3 \
    python3-pip \
    python3-venv \
    build-essential \
    libssl-dev \
    git \
    supervisor \
    vim \
    nginx \
    gettext-base

WORKDIR /agentscope_runtime
RUN python3 -m venv venv
ENV PATH="/agentscope_runtime/venv/bin:$PATH"

# Copy application files
COPY src/agentscope_runtime/sandbox/box/shared/app.py ./
COPY src/agentscope_runtime/sandbox/box/shared/routers/ ./routers/
COPY src/agentscope_runtime/sandbox/box/shared/dependencies/ ./dependencies/
COPY src/agentscope_runtime/sandbox/box/shared/artifacts/ ./ext_services/artifacts/
COPY examples/custom_sandbox/box/third_party/markdownify-mcp/ ./mcp_project/markdownify-mcp/
COPY examples/custom_sandbox/box/third_party/steel-browser/ ./ext_services/steel-browser/
COPY examples/custom_sandbox/box/ ./

RUN pip install -r requirements.txt

# Install Google Chrome & fonts
RUN curl -fsSL https://dl.google.com/linux/linux_signing_key.pub | apt-key add - && \
    echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" > /etc/apt/sources.list.d/google-chrome.list && \
    apt-get update && apt-get install -y --fix-missing google-chrome-stable \
    google-chrome-stable \
    fonts-wqy-zenhei \
    fonts-wqy-microhei

# Install steel browser
WORKDIR /agentscope_runtime/ext_services/steel-browser
RUN npm ci --omit=dev \
    && npm install -g webpack webpack-cli \
    && npm run build -w api \
    && rm -rf node_modules/.cache

# Install artifacts backend
WORKDIR /agentscope_runtime/ext_services/artifacts
RUN npm install \
    && rm -rf node_modules/.cache

# Install mcp_project/markdownify-mcp
WORKDIR /agentscope_runtime/mcp_project/markdownify-mcp
RUN npm install -g pnpm \
    && pnpm install \
    && pnpm run build \
    && rm -rf node_modules/.cache

WORKDIR ${WORKSPACE_DIR}
RUN mv /agentscope_runtime/config/supervisord.conf /etc/supervisor/conf.d/supervisord.conf
RUN mv /agentscope_runtime/config/nginx.conf.template /etc/nginx/nginx.conf.template
RUN git init \
    && chmod +x /agentscope_runtime/scripts/start.sh

COPY .gitignore ${WORKSPACE_DIR}

# MCP required environment variables
ENV TAVILY_API_KEY=123
ENV AMAP_MAPS_API_KEY=123

# Cleanup to reduce image size
RUN pip cache purge \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/* \
    && rm -rf /tmp/* \
    && rm -rf /var/tmp/* \
    && npm cache clean --force \
    && rm -rf ~/.npm/_cacache

CMD ["/bin/sh", "-c", "envsubst '$SECRET_TOKEN' < /etc/nginx/nginx.conf.template > /etc/nginx/nginx.conf && /usr/bin/supervisord -c /etc/supervisor/conf.d/supervisord.conf"]
```

### Build Your Custom Image

After preparing the Dockerfile and custom sandbox class, build your custom sandbox image using the built-in builder tool:

```bash
runtime-sandbox-builder my_custom_sandbox --dockerfile_path examples/custom_sandbox/Dockerfile --extension PATH_TO_YOUR_SANDBOX_MODULE
```

**Command Parameters:**

- `custom_sandbox`: Name/tag for your custom sandbox image
- `--dockerfile_path`: Path to your custom Dockerfile
- `--extension`: Path to custom sandbox module

After building, your custom sandbox image will be ready to use with the corresponding sandbox class you defined.

#### Build Built-in Images Locally

You can also use the builder to build built-in sandbox images locally:

```bash
# Build all built-in images
runtime-sandbox-builder all

# Build base image (~1GB)
runtime-sandbox-builder base

# Build GUI image (~2GB)
runtime-sandbox-builder gui

# Build browser image (~2GB)
runtime-sandbox-builder browser

# Build filesystem image (~2GB)
runtime-sandbox-builder filesystem

# Build mobile image (~3GB)
runtime-sandbox-builder mobile
```

The above commands are useful when:

- Building images locally instead of pulling from Docker
- Customizing base images before building your own
- Ensuring you have the latest version of built-in images
- Working in network-isolated environments

### Changing Sandbox Image Related Configuration

The Docker image used by the Sandbox module is determined by the following three environment variables. You can modify any of them as needed to change the image source or version.

| Environment Variable                            | Purpose                                                    | Default         | Example Modification                                                     |
| --------------------------------- | ------------------------------------------------------- | -------------- | ------------------------------------------------------------ |
| `RUNTIME_SANDBOX_REGISTRY`     | Image registry address (Registry). Empty means using Docker Hub. | `""`           | `export RUNTIME_SANDBOX_REGISTRY="agentscope-registry.ap-southeast-1.cr.aliyuncs.com"` |
| `RUNTIME_SANDBOX_IMAGE_NAMESPACE` | Image namespace, similar to account name.                 | `"agentscope"` | `export RUNTIME_SANDBOX_IMAGE_NAMESPACE="my_namespace"`      |
| `RUNTIME_SANDBOX_IMAGE_TAG`   | Image version tag (Tag).                                   | `"latest"`     | `export RUNTIME_SANDBOX_IMAGE_TAG="my_custom"`               |

