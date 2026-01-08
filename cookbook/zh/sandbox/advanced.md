# 工具沙箱高级用法

> 本节介绍沙箱的高级用法。我们强烈建议在继续之前先完成上一节的基础教程[沙箱](sandbox.md)。

## 沙箱管理器配置参考

#### ManagerConfig 配置

| Parameter            | Type                | Description                         | Default               | Notes                                                        |
| -------------------- | ------------------- | ----------------------------------- | --------------------- | ------------------------------------------------------------ |
| `bearerToken`        | `String`            | 调用远程runtime沙箱的身份验证令牌   | `null`                | 如果设置为 `null`，将在连接的时候不会进行身份验证            |
| `baseUrl`            | `String`            | 调用远程runtime沙箱的服务器绑定地址 | `null`                | 如果设置为 `null`，将默认使用本地沙箱管理                    |
| `clientStarter`      | `BaseClientStarter` | 容器运行时                          | `DockerClientStarter` | 目前支持  `Docker`、`K8s` 、`AgentRun`和`FC`                 |
| `sandboxMap`         | `SandboxMap`        | 容器管理                            | `InMemorySandboxMap`  | 容器的管理，默认使用本地存储，引入 `redis-extension` 可以使用 `redis` 作为管理后端 |
| `containerPrefixKey` | `String`            | 容器名称前缀                        | `sandbox_container_`  | 创建的容器名称前缀                                           |

#### Redis 配置

> **何时使用 Redis：**
>
> - **单个工作进程（`WORKERS=1`）**：Redis 是可选的。系统可以使用内存缓存来管理沙箱状态，这更简单且延迟更低。
> - **多个工作进程（`WORKERS>1`）**：需要 Redis 来在工作进程间共享沙箱状态并确保一致性。

Redis 为沙箱状态和状态管理提供缓存。如果只有一个工作进程，您可以使用内存缓存，您需要配置 `RedisManagerConfig` 并使用它创建一个 `RedisSandboxMap`，以下是 `RedisManagerConfig` 需要配置的参数：

| Parameter               | Description      | Default                                     | Notes            |
| ----------------------- | ---------------- | ------------------------------------------- | ---------------- |
| `redisServer`           | Redis 服务器地址 | localhost                                   | Redis 主机       |
| `redisPort`             | Redis 端口       | 6379                                        | 标准 Redis 端口  |
| `redisDb`               | Redis 数据库编号 | `0`                                         | 0-15             |
| `redisUser`             | Redis 用户名     | `null`                                      | 用于 Redis6+ ACL |
| `redisPassword`         | Redis 密码       | `null`                                      | 身份验证         |
| `redisPortKey`          | 端口跟踪键       | `_runtime_sandbox_container_occupied_ports` | 内部使用         |
| `redisContainerPoolKey` | 容器池键         | `_runtime_sandbox_container_container_pool` | 内部使用         |

#### FileSystemConfig 配置

目前只支持在本地 Docker 下实现文件目录的挂载，支持三种挂载：

* 拷贝挂载（推荐）：配置 `storageFolderPath`、`mountDir` 两个属性，在创建容器的时候，会首先将存储目录中的内容拷贝到挂载路径，并让容器挂载挂载路径，防止对原始文件造成污染，在容器关闭的时候，会将挂载路径的内容拷贝回去
* 只读挂载：配置 `readonlyMounts`，容器会直接挂载本地文件系统
* 可读写零拷贝挂载（谨慎使用）：配置 `nonCopyMount`，容器会直接挂载本地文件系统，并拥有对于该文件夹的读写权限

在使用的时候，您需要把 `FileSystemConfig` 传递给 `Sandbox` 的 `fileSystemConfig` 参数。

##### 存储后端

###### 本地存储

默认使用 `LocalFileSystemConfig`，本地条件下无需配置，使用 `oss` 情况下需配置

###### OSS 存储

使用[阿里云对象存储服务](https://www.aliyun.com/product/oss)进行分布式文件存储：

| Parameter            | Description      | Default | Notes           |
| -------------------- | ---------------- | ------- | --------------- |
| `ossEndpoint`        | OSS 端点URL      | `null`  | 区域端点        |
| `ossAccessKeyId`     | OSS 访问密钥 ID  | `null`  | 来自 OSS 控制台 |
| `ossAccessKeySecret` | OSS 访问密钥秘钥 | `null`  | 保持安全        |
| `ossBucketName`      | OSS 存储桶名称   | `null`  | 预创建的存储桶  |

#### ClientConfig 配置

默认使用本地 `Docker` 作为运行时环境，可以选择如下三种自定义配置方式

##### （可选）Docker 设置

要在沙盒服务器中配置特定 Docker 的设置，请在 `containerDeployment` 中传递 `DockerClientConfig` 参数 。可以考虑调整以下参数：

| Parameter   | Description                                          | Default          | Notes                                                        |
| ----------- | ---------------------------------------------------- | ---------------- | ------------------------------------------------------------ |
| `portRange` | 沙箱服务可分配的**动态端口范围**（用于暴露容器内服务 | `(49152, 59152)` | 必须是未被占用的高端口范围；避免与系统服务冲突               |
| `host`      | Docker 守护进程（Docker Daemon）的监听地址           | `localhost`      | 若 Docker 运行在远程主机或 Docker Desktop，需设为对应 IP 或 socket 路径 |
| `port`      | Docker 守护进程的 TCP 监听端口                       | `2375`           | ⚠️ 仅当 Docker 配置了 `tcp://0.0.0.0:2375`时使用；生产环境应禁用（不安全） |
| `certPath`  | **TLS 证书目录路径**（用于安全连接 Docker Daemon）   | `null`           | 若启用了 TLS（端口通常为 `2376`），需提供包含 `ca.pem`, `cert.pem`, `key.pem` 的目录 |

##### （可选）K8s 设置

要在沙盒服务器中配置特定于 Kubernetes 的设置，请在 `containerDeployment` 中传递 `KubernetesClientConfig` 参数。可以考虑调整以下参数：

| Parameter        | Description                                                  | Default   | Notes                                                        |
| ---------------- | ------------------------------------------------------------ | --------- | ------------------------------------------------------------ |
| `namespace`      | 沙箱 Pod、Service 等资源将被创建到的 **Kubernetes 命名空间** | `default` | 建议使用专用命名空间（如 `agentscope-sandbox`），便于隔离和清理 |
| `kubeConfigPath` | **kubeconfig 文件的本地路径**，用于认证和连接目标 Kubernetes 集群 | `None`    | 若未指定，将尝试使用 `~/.kube/config`                        |

##### （可选）AgentRun设置

AgentRun是阿里云推出的基于Serverless架构的智能Agent开发框架，提供了一套完整的工具集，帮助开发者快速构建、部署和管理AI Agent应用。您可将沙盒服务器部署到AgentRun上。

要在沙盒服务器中配置特定于 [AgentRun](https://functionai.console.aliyun.com/cn-hangzhou/agent/) 的设置，请在 `containerDeployment` 中传递 `AgentRunClientConfig` 参数。可以考虑调整以下参数：

| Parameter                 | Description    | Default               | Notes                                                        |
| ------------------------- | -------------- | --------------------- | ------------------------------------------------------------ |
| `agentRunAccountId`       | 阿里云账号ID   | `null`                | 阿里云主账号ID，登录阿里云[RAM控制台](https://ram.console.aliyun.com/profile/access-keys)获取阿里云账号ID和AK、SK |
| `agentRunAccessKeyId`     | 访问密钥ID     | `null`                | 阿里云AccessKey ID，需要`AliyunAgentRunFullAccess`权限       |
| `agentRunAccessKeySecret` | 访问密钥Secret | `null`                | 阿里云AccessKey Secret                                       |
| `agentRunRegionId`        | 部署区域ID     | `cn-hangzhou`         | Agentrun部署地域ID                                           |
| `agentRunCpu`             | CPU规格        | `2.0f`                | vCPU规格                                                     |
| `agentRunMemory`          | 内存规格       | `2048`                | 内存规格 (MB)                                                |
| `agentRunVpcId`           | VPC ID         | `null`                | VPC网络ID（可选）                                            |
| `agentRunVswitchIds`      | 交换机ID列表   | `null`                | VSwitch ID列表（可选）                                       |
| `agentRunSecurityGroupId` | 安全组ID       | `null`                | 安全组ID（可选）                                             |
| `agentRunPrefix`          | 资源名称前缀   | `agentscope-sandbox_` | 创建的资源名称前缀                                           |
| `agentrunLogProject`      | SLS日志项目    | `null`                | SLS日志项目名称（可选）                                      |
| `agentrunLogStore`        | SLS日志库      | `null`                | SLS日志库名称（可选）                                        |

##### （可选）函数计算（FC）设置

函数计算（Function Compute，简称FC）是一种事件驱动的全托管计算服务，开发者无需管理服务器等基础设施，只需编写并上传代码，函数计算便会自动准备计算资源，并以弹性、可靠的方式运行代码。您可将沙盒服务器部署到FC上。

要在沙盒服务器中配置特定于 [FC](https://fcnext.console.aliyun.com/) 的设置，请在 `containerDeployment` 中传递 `FcClientConfig` 参数。可以考虑调整以下参数：

| Parameter           | Description    | Default               | Notes                                                        |
| ------------------- | -------------- | --------------------- | ------------------------------------------------------------ |
| `FcAccessKeyId`     | 访问密钥ID     | `null`                | 阿里云AccessKey ID，需要`AliyunAgentRunFullAccess`权限       |
| `FcAccessKeySecret` | 访问密钥Secret | `null`                | 阿里云AccessKey Secret                                       |
| `FcAccountId`       | 阿里云账号ID   | `null`                | 阿里云主账号ID，登录阿里云[RAM控制台](https://ram.console.aliyun.com/profile/access-keys)获取阿里云账号ID和AK、SK |
| `FcRegionId`        | 部署区域ID     | `cn-hangzhou`         | Agentrun部署地域ID                                           |
| `FcCpu`             | CPU规格        | `2.0f`                | vCPU规格                                                     |
| `FcMemory`          | 内存规格       | `2048`                | 内存规格 (MB)                                                |
| `FcVpcId`           | VPC ID         | `null`                | VPC网络ID（可选）                                            |
| `FcVswitchIds`      | 交换机ID列表   | `null`                | VSwitch ID列表（可选）                                       |
| `FcSecurityGroupId` | 安全组ID       | `null`                | 安全组ID（可选）                                             |
| `FcPrefix`          | 资源名称前缀   | `agentscope-sandbox_` | 创建的资源名称前缀                                           |
| `FcLogProject`      | SLS日志项目    | `null`                | SLS日志项目名称（可选）                                      |
| `FcLogStore`        | SLS日志库      | `null`                | SLS日志库名称（可选）                                        |

### 导入自定义沙箱

除了默认提供的基础沙箱类型外，您还可以通过使用 `@RegisterSandbox` 注解并继承 `Sandbox` 类，实现自定义沙箱的功能，例如修改镜像、增加环境变量、定义超时时间等，应用会在启动时自动扫描所有带有该注解的类，并实现自动注册。（目前仅支持添加一个自定义沙箱，后续版本允许添加更多自定义沙箱）

#### 编写自定义沙箱扩展（例如 `CustomSandbox.java`）

参考[自定义沙箱类](###创建自定义沙箱类)

> - `@RegisterSandbox` 会将该类注册到沙箱管理器中，启动时可被识别和使用。
> - `environment` 字段可以向沙箱注入外部 API Key 或其他必要配置。
> - 类继承自 `Sandbox`，可覆盖其方法来实现更多自定义逻辑。

#### 添加 Java SPI 扫描配置

AgentScope Runtime Java 提供了 `SandboxProvider` 作为沙箱扫描工具基类，你需要在你的项目中继承 `SandboxProvider` 类，并将你的沙箱添加进去，如下所示：

```java
import io.agentscope.runtime.sandbox.manager.registry.SandboxProvider;

import java.util.Collection;
import java.util.Collections;

public class CustomSandboxProvider implements SandboxProvider {

    @Override
    public Collection<Class<?>> getSandboxClasses() {
 				// 注册自定义的 CustomSandbox 沙箱
        return Collections.singletonList(CustomSandbox.class);
    }
}
```

然后在 resources 文件夹中添加 META-INF.services 文件夹，并添加 `io.agentscope.runtime.sandbox.manager.registry.SandboxProvider` 文件，文件内添加继承的 SandboxProvider 全限定名，如`io.agentscope.CustomSandboxProvider`

## 自定义构建沙箱

虽然内置沙箱类型涵盖了常见用例，但您可能会遇到需要专门环境或独特工具组合的场景。创建自定义沙箱允许您根据特定需求定制执行环境。本节演示如何构建和注册您的自定义沙箱类型。

### 从源码安装（自定义沙箱必需）

要创建自定义沙箱，您需要使用Python版本 [AgentScope Runtime](https://github.com/agentscope-ai/agentscope-runtime)。以可编辑模式从源码安装 AgentScope Runtime，这允许您修改代码并立即看到更改：

```bash
git clone https://github.com/agentscope-ai/agentscope-runtime.git
cd agentscope-runtime
git submodule update --init --recursive
pip install -e .
```

> 创建自定义沙箱时，`-e`（可编辑）标志是必需的，因为它允许您：
>
> - 修改沙箱代码并立即看到更改而无需重新安装
> - 将您的自定义沙箱类添加到注册表中
> - 迭代开发和测试自定义工具

### 创建自定义沙箱类

您可以定义自定义沙箱类型并将其注册到系统中以满足特殊需求。只需继承 `Sandbox` 并使用 `RegisterSandbox`装饰器，然后将文件放在你的项目文件夹中:

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

### 准备Docker镜像

创建自定义沙箱还需要准备相应的 Docker镜像。镜像应包含您特定用例所需的所有依赖项、工具和配置。

**注意：构建镜像操作需要在 Python 代码仓库中进行**

> **配置选项：**
>
> - **简单 MCP 服务器更改**：要简单更改沙箱中的初始MCP 服务器，请修改 `mcp_server_configs.json` 文件
> - **高级定制**：对于更高级的用法和定制，您必须非常熟悉Dockerfile 语法和Docker 最佳实践

这里是一个自定义沙箱的Dockerfile 示例，它在一个沙箱中集成了文件系统、浏览器和一些有用的 MCP 工具：


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

### 构建您的自定义镜像

准备好Dockerfile 和自定义沙箱类后，使用内置构建器工具构建您的自定义沙箱镜像：

```bash
runtime-sandbox-builder my_custom_sandbox --dockerfile_path examples/custom_sandbox/Dockerfile --extension PATH_TO_YOUR_SANDBOX_MODULE
```

**命令参数：**

- `custom_sandbox`: 您的自定义沙箱镜像的名称/标签
- `--dockerfile_path`: 您的自定义Dockerfile 的路径
- `--extension`: 自定义沙箱模块的路径

构建完成后，您的自定义沙箱镜像将准备好与您定义的相应沙箱类一起使用。

#### 本地构建内置镜像

您也可以使用构建器在本地构建内置沙箱镜像：

```bash
# 构建所有内置镜像
runtime-sandbox-builder all

# 构建基础镜像（约1GB）
runtime-sandbox-builder base

# 构建GUI镜像（约2GB）
runtime-sandbox-builder gui

# 构建浏览器镜像（约2GB）
runtime-sandbox-builder browser

# 构建文件系统镜像（约2GB）
runtime-sandbox-builder filesystem

# 构建移动端镜像（约3GB）
runtime-sandbox-builder mobile
```

上述命令在以下情况下很有用：

- 在本地构建镜像而不是从Docker拉取
- 在构建自己的镜像之前定制基础镜像
- 确保您拥有内置镜像的最新版本
- 在网络隔离的环境中工作

### 更改 Sandbox 镜像相关配置

Sandbox 模块运行所用的 Docker 镜像由以下三个环境变量共同决定，你可以根据需要修改其中任意一个，来改变镜像的来源或版本。

| 环境变量                          | 作用                                                    | 默认值         | 修改示例                                                     |
| --------------------------------- | ------------------------------------------------------- | -------------- | ------------------------------------------------------------ |
| `RUNTIME_SANDBOX_REGISTRY`        | 镜像注册中心地址（Registry）。为空表示使用 Docker Hub。 | `""`           | `export RUNTIME_SANDBOX_REGISTRY="agentscope-registry.ap-southeast-1.cr.aliyuncs.com"` |
| `RUNTIME_SANDBOX_IMAGE_NAMESPACE` | 镜像命名空间（Namespace），类似账号名。                 | `"agentscope"` | `export RUNTIME_SANDBOX_IMAGE_NAMESPACE="my_namespace"`      |
| `RUNTIME_SANDBOX_IMAGE_TAG`       | 镜像版本标签（Tag）。                                   | `"latest"`     | `export RUNTIME_SANDBOX_IMAGE_TAG="my_custom"`               |