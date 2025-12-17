# Tool Sandbox Advanced Usage

>This section introduces advanced sandbox usage. We strongly recommend completing the basic tutorial in the previous section [Sandbox](sandbox.md) before proceeding.

## Sandbox Manager Configuration Reference

#### ManagerConfig Configuration

| Parameter            | Type  | Description            | Default                    | Notes                                                        |
| ----------------------| ------------ | ---------------------- | -------------------------- | ------------------------------------------------------------ |
| `defaultSandboxType` | `List<SandboxType>` | Default sandbox type(s) (can be multiple) | `SandboxType.BASE`  | Can be a single type or a list of multiple types, enabling multiple independent sandbox warm-up pools. Valid values include `BASE`, `BROWSER`, `FILESYSTEM`, `GUI`, etc. |
| `bearerToken` | `String` | Authentication token for calling remote runtime sandbox | `null`                    | If set to `null`, no authentication will be performed when connecting |
| `baseUrl` | `String` | Server binding address for calling remote runtime sandbox | `null`                 | If set to `null`, local sandbox management will be used by default |
| `containerDeployment` | `BaseClientConfig` | Container runtime      | `DockerClientConfig` | Currently supports `Docker`, `K8s`, and `AgentRun`              |
| `poolSize` | `int` | Warm-up container pool size    | `0`       | Cached containers for faster startup. The `poolSize` parameter controls the number of pre-created containers cached in ready state. When a user requests a new sandbox, the system will first try to allocate from this warm-up pool, significantly reducing startup time compared to creating containers from scratch. For example, with `poolSize=10`, the system maintains 10 ready containers that can be immediately assigned to new requests |
| `fileSystemConfig` | `FileSystemConfig` | Container file system configuration   | `LocalFileSystemConfig` | Manages container file system download method, defaults to `local file system`, can also use `oss` |
| `redisConfig` | `RedisManagerConfig` | Redis support configuration | `null` | Enable Redis support, required for distributed deployment or when number of worker processes is greater than `1`, disabled by default |

#### Redis Configuration

> **When to use Redis:**
> - **Single worker process (`WORKERS=1`)**: Redis is optional. The system can use memory cache to manage sandbox state, which is simpler and has lower latency.
> - **Multiple worker processes (`WORKERS>1`)**: Redis is required to share sandbox state between worker processes and ensure consistency.

Redis provides caching for sandbox state and state management. If there is only one worker process, you can use memory cache:

| Parameter               | Description      | Default                                     | Notes            |
| ----------------------- | ---------------- | ------------------------------------------- | ---------------- |
| `redisServer`           | Redis server address | localhost                                   | Redis host       |
| `redisPort`             | Redis port       | 6379                                        | Standard Redis port  |
| `redisDb`               | Redis database number | `0`                                         | 0-15             |
| `redisUser`             | Redis username     | `null`                                      | For Redis6+ ACL |
| `redisPassword`         | Redis password       | `null`                                      | Authentication         |
| `redisPortKey`          | Port tracking key       | `_runtime_sandbox_container_occupied_ports` | Internal use         |
| `redisContainerPoolKey` | Container pool key         | `_runtime_sandbox_container_container_pool` | Internal use         |

#### FileSystemConfig Configuration

Defaults to `LocalFileSystemConfig`, no configuration needed for local conditions. Configuration is required when using `oss`.

##### OSS Configuration

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

### Importing Custom Sandbox

In addition to the default provided base sandbox types, you can also implement custom sandbox functionality by writing extension modules and loading them with the `--extension` parameter, such as modifying images, adding environment variables, defining timeouts, etc.

#### Writing Custom Sandbox Extension (e.g., `CustomSandbox.java`)

Refer to [Creating Custom Sandbox Class](#creating-custom-sandbox-class)

> - `@RegisterSandbox` will register this class to the sandbox manager and can be recognized and used at startup.
> - The `environment` field can inject external API Keys or other necessary configurations into the sandbox.
> - The class inherits from `Sandbox` and can override its methods to implement more custom logic.

## Custom Sandbox Building

While built-in sandbox types cover common use cases, you may encounter scenarios that require specialized environments or unique tool combinations. Creating custom sandboxes allows you to tailor the execution environment to specific needs. This section demonstrates how to build and register your custom sandbox type.

### Install from Source (Required for Custom Sandbox)

To create custom sandboxes, you need to use the Python version of [AgentScope Runtime](https://github.com/agentscope-ai/agentscope-runtime). Install AgentScope Runtime from source in editable mode, which allows you to modify code and see changes immediately:

```bash
git clone https://github.com/agentscope-ai/agentscope-runtime.git
cd agentscope-runtime
git submodule update --init --recursive
pip install -e .
```

> The `-e` (editable) flag is required when creating custom sandboxes because it allows you to:
> - Modify sandbox code and see changes immediately without reinstalling
> - Add your custom sandbox classes to the registry
> - Iterate on development and testing of custom tools

### Creating Custom Sandbox Class

You can define custom sandbox types and register them with the system to meet special requirements. Simply inherit from `Sandbox` and use the `SandboxRegistry.register` decorator, then place the file in `src/agentscope_runtime/sandbox/custom` (e.g., `src/agentscope_runtime/sandbox/custom/custom_sandbox.py`):

```python
import os

from typing import Optional

from agentscope_runtime.sandbox.utils import build_image_uri
from agentscope_runtime.sandbox.registry import SandboxRegistry
from agentscope_runtime.sandbox.enums import SandboxType
from agentscope_runtime.sandbox.box.sandbox import Sandbox

SANDBOXTYPE = "my_custom_sandbox"


@SandboxRegistry.register(
    build_image_uri(f"runtime-sandbox-{SANDBOXTYPE}"),
    sandbox_type=SANDBOXTYPE,
    security_level="medium",
    timeout=60,
    description="my sandbox",
    environment={
        "TAVILY_API_KEY": os.getenv("TAVILY_API_KEY", ""),
        "AMAP_MAPS_API_KEY": os.getenv("AMAP_MAPS_API_KEY", ""),
    },
)
class MyCustomSandbox(Sandbox):
    def __init__(
        self,
        sandbox_id: Optional[str] = None,
        timeout: int = 3000,
        base_url: Optional[str] = None,
        bearer_token: Optional[str] = None,
    ):
        super().__init__(
            sandbox_id,
            timeout,
            base_url,
            bearer_token,
            SandboxType(SANDBOXTYPE),
        )
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

