# 高级部署

章节演示了 AgentScope Runtime Java 中可用的三种高级部署方法，为不同场景提供生产就绪的解决方案：**本地Docker打包**、**Kubernetes部署**和**AgentRun部署**。

## 部署方法概述

AgentScope Runtime提供三种不同的部署方式，每种都针对特定的使用场景：

| 部署类型 | 使用场景 | 扩展性 | 管理方式 | 资源隔离 |
|---------|---------|--------|---------|---------|
| **本地Docker打包** | 开发与测试 | 单容器 | 手动 | 容器级 |
| **Kubernetes** | 企业与云端 | K8s 引擎自动编排 | 编排 | 容器级 |
| **AgentRun** | AgentRun平台 | 云端管理 | 平台管理 | 容器级 |

## 前置条件

### 🔧 安装要求

添加 AgentScope Runtime Java 提供的 **打包依赖**：

```xml
<plugin>
    <groupId>io.agentscope</groupId>
    <artifactId>deployer-maven-plugin</artifactId>
    <version>1.0.0</version>
    <configuration>
        <configFile>${project.basedir}/deployer.yml</configFile>
    </configuration>
    <executions>
        <execution>
            <id>deployer</id>
            <phase>package</phase>
            <goals>
                <goal>build</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### 🔑 参数配置

在 yaml 文件中配置部署参数，yaml 文件的读取路径为 maven 插件配置的 `configFile` 路径

```yaml
build:
  imageName: agentscope-use-example	# 构建的镜像名称
  imageTag: latest  # 构建的镜像标签
  baseImage: eclipse-temurin:17-jre # 基础镜像
  port: 10001  # 应用内部端口，即 AgentApp 配置的端口号
  pushToRegistry: false # 是否将镜像推送到 Docker Registry（如果设置部署到 K8s 集群，该项必须为 true）
  deployToK8s: false  # 是否部署到 Kubernetes 集群
  deployToAgentRun: true  # 是否部署到阿里云函数计算 AgentRun

# ============================================
# Docker 镜像仓库配置，注意，如果需要后续部署到 K8s，需要保证 K8s 具有该镜像仓库的访问权限
# ============================================
registry:
  url: "<REGISTRY_URL>" # 镜像仓库地址
  username: "<REGISTRY_USERNAME>" # 镜像仓库用户名
  password: "<REGISTRY_PASSWORD>" # 镜像仓库密码
  namespace: "<REGISTRY_NAMESPACE>" # 镜像仓库命名空间

# ============================================
# K8s 部署配置
# ============================================
kubernetes:
  replicas: 1 # 部署副本数
  kubeconfigPath: "<KUBECONFIG_PATH>" # Kubeconfig 文件路径
  namespace: "default"  # 部署命名空间

# ============================================
# OSS 配置（部署 AgentRun 使用）
# ============================================
oss:
  region: cn-hangzhou # OSS 所在区域
  accessKeyId: "<YOUR_ACCESS_KEY_ID>" # OSS 访问密钥 ID
  accessKeySecret: "<YOUR_ACCESS_KEY_SECRET>" # OSS 访问密钥 Secret
  bucket: "<YOUR_BUCKET_KEY_ID>"  # OSS 存储桶名称

# ============================================
# 传递给应用的环境变量
# ============================================
environment:
  AI_DASHSCOPE_API_KEY: "<DASHSCOPE API KEY>"
  SPRING_PROFILES_ACTIVE: production

# ============================================
# AgentRun 部署配置，注意，如果需要部署到 AgentRun，需要先配置 OSS，OSS 和 AgentRun 共享同一套访问密钥
# ============================================
agentrun:
  region: cn-hangzhou # AgentRun 所在区域
  runtimeNamePrefix: agentscope-use-example # 部署的运行时名称前缀
  cpu: 2  # CPU 核数
  memorySize: 2048  # 内存大小，单位 MB
  sessionConcurrencyLimit: 1  # 会话并发数限制
  sessionIdleTimeoutSeconds: 600  # 会话空闲超时时间，单位秒
  networkMode: PUBLIC # 网络模式，PUBLIC 或 VPC
```

### 📦 各部署类型的前置条件

#### 所有部署类型

- **Java 17** 或更高版本
- **Maven 3.6** 或更高版本
- **Docker**（用于镜像打包）

#### Kubernetes 部署
- **Kubernetes** 集群访问权限
- 已配置 **kubectl**
- **容器镜像仓库**访问权限（用于推送镜像）

#### AgentRun 部署

* **AgentRun** 访问参数

## 通用智能体配置

所有部署方法共享相同的智能体和端点配置。参照 [简单部署](agent_app.md) 首先构建一个 web 应用

## 方法1：打包本地 Docker 镜像

**最适合**：开发、测试和需要手动控制的持久服务的单用户场景。

### 特性
- 一键构建 web 应用镜像
- 手动生命周期管理
- 交互式控制和监控
- 直接资源共享

### 使用

使用 [通用智能体配置](###通用智能体配置) 部分定义的智能体和端点，配置打包参数：

```yaml
build:
  imageName: agentscope-use-example	# 构建的镜像名称
  imageTag: latest  # 构建的镜像标签
  baseImage: eclipse-temurin:17-jre # 基础镜像
  port: 10001  # 应用内部端口，即 AgentApp 配置的端口号
  
# ============================================
# 传递给应用的环境变量
# ============================================
environment:
  AI_DASHSCOPE_API_KEY: "<DASHSCOPE API KEY>"
  SPRING_PROFILES_ACTIVE: production  
```

**关键点**：

- 服务会被打包为指定镜像
- 通过 `docker run` 命令手动管理容器生命周期
- 最适合开发和测试

### 测试部署的服务

部署后，您可以使用 curl 测试端点：

**使用 curl：**

```bash
curl --location --request POST 'http://localhost:10001/a2a/' \
--header 'Content-Type: application/json' \
--header 'Accept: */*' \
--header 'Host: localhost:10001' \
--header 'Connection: keep-alive' \
--data-raw '{
  "method": "message/stream",
  "id": "2d2b4dc8-8ea2-437b-888d-3aaf3a8239dc",
  "jsonrpc": "2.0",
  "params": {
    "configuration": {
      "blocking": false
    },
    "message": {
      "role": "user",
      "kind": "message",
      "metadata": {
        "userId": "me",
        "sessionId": "test1"
      },
      "parts": [
        {
          "text": "你好，给我用python计算一下第10个斐波那契数",
          "kind": "text"
        }
      ],
      "messageId": "c4911b64c8404b7a8bf7200dd225b152"
    }
  }
}'
```


## 方法2：Kubernetes部署

**最适合**：需要扩展性、高可用性和云原生编排的企业生产环境。

### 特性
- 基于容器的部署
- 水平扩展支持
- 云原生编排
- 资源管理和限制
- 健康检查和自动恢复

### Kubernetes部署前置条件

```bash
# 确保Docker正在运行
docker --version

# 验证Kubernetes访问
kubectl cluster-info

# 检查镜像仓库访问（以阿里云为例）
docker login your-registry
```

### 使用

使用  [通用智能体配置](###通用智能体配置)  部分定义的智能体和端点，配置 K8s 部署需要使用到的打包参数：

```yaml
build:
  imageName: agentscope-use-example	# 构建的镜像名称
  imageTag: latest  # 构建的镜像标签
  baseImage: eclipse-temurin:17-jre # 基础镜像
  port: 10001  # 应用内部端口，即 AgentApp 配置的端口号
  pushToRegistry: true # 是否将镜像推送到 Docker Registry（如果设置部署到 K8s 集群，该项必须为 true）
  deployToK8s: true  # 是否部署到 Kubernetes 集群

# ============================================
# Docker 镜像仓库配置，注意，如果需要后续部署到 K8s，需要保证 K8s 具有该镜像仓库的访问权限
# ============================================
registry:
  url: "<REGISTRY_URL>" # 镜像仓库地址
  username: "<REGISTRY_USERNAME>" # 镜像仓库用户名
  password: "<REGISTRY_PASSWORD>" # 镜像仓库密码
  namespace: "<REGISTRY_NAMESPACE>" # 镜像仓库命名空间

# ============================================
# K8s 部署配置
# ============================================
kubernetes:
  replicas: 1 # 部署副本数
  kubeconfigPath: "<KUBECONFIG_PATH>" # Kubeconfig 文件路径
  namespace: "default"  # 部署命名空间


# ============================================
# 传递给应用的环境变量
# ============================================
environment:
  AI_DASHSCOPE_API_KEY: "<DASHSCOPE API KEY>"
  SPRING_PROFILES_ACTIVE: production
```

**关键点**：

- 容器化部署，支持自动扩展
- 配置资源限制和健康检查
- 可使用 `kubectl scale deployment` 进行扩展

## 方法3：Serverless部署：AgentRun

**最适合**：阿里云用户，需要将智能体部署到 AgentRun 服务，实现自动化的构建、上传和部署流程。

### 特性
- 阿里云 AgentRun 服务的托管部署
- 自动构建和打包项目
- OSS 集成用于制品存储
- 完整的生命周期管理
- 自动创建和管理运行时端点

### 使用

使用 [通用智能体配置](###通用智能体配置) 部分定义的智能体和端点，配置 AgentRun 部署需要使用到的参数：

```yaml
build:
  imageName: agentscope-use-example	# 构建的镜像名称
  imageTag: latest  # 构建的镜像标签
  baseImage: eclipse-temurin:17-jre # 基础镜像
  port: 10001  # 应用内部端口，即 AgentApp 配置的端口号
  deployToAgentRun: true  # 是否部署到阿里云函数计算 AgentRun

# ============================================
# OSS 配置（部署 AgentRun 使用，OSS 仓库中存放构建制品）
# ============================================
oss:
  region: cn-hangzhou # OSS 所在区域
  accessKeyId: "<YOUR_ACCESS_KEY_ID>" # OSS 访问密钥 ID
  accessKeySecret: "<YOUR_ACCESS_KEY_SECRET>" # OSS 访问密钥 Secret
  bucket: "<YOUR_BUCKET_KEY_ID>"  # OSS 存储桶名称

# ============================================
# 传递给应用的环境变量
# ============================================
environment:
  AI_DASHSCOPE_API_KEY: "<DASHSCOPE API KEY>"
  SPRING_PROFILES_ACTIVE: production

# ============================================
# AgentRun 部署配置，注意，如果需要部署到 AgentRun，需要先配置 OSS，OSS 和 AgentRun 共享同一套访问密钥
# ============================================
agentrun:
  region: cn-hangzhou # AgentRun 所在区域
  runtimeNamePrefix: agentscope-use-example # 部署的运行时名称前缀
  cpu: 2  # CPU 核数
  memorySize: 2048  # 内存大小，单位 MB
  sessionConcurrencyLimit: 1  # 会话并发数限制
  sessionIdleTimeoutSeconds: 600  # 会话空闲超时时间，单位秒
  networkMode: PUBLIC # 网络模式，PUBLIC 或 VPC
```

**关键点**：
- 自动构建项目并打包为 jar 文件
- 上传制品到 OSS
- 在 AgentRun 服务中创建和管理运行时
- 自动创建公共访问端点
