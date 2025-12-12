# 安装

准备好开始使用 AgentScope Runtime Java 了吗？本指南将帮助您在几分钟内快速搭建和运行**AgentScope Runtime Java**。

## 前置要求

- **Java 17** 或更高版本
- **Maven 3.6** 或更高版本
- **Docker**（可选，用于沙箱工具执行）

## 安装方式

### 通过 Maven Central 安装（推荐）

AgentScope Runtime Java 已经发布到 Maven Central，您可以直接通过 Maven 依赖使用。

> 当前稳定版本：1.0.0
>
> 您可以在 [Maven Central](https://central.sonatype.com/artifact/io.agentscope/agentscope-runtime-core) 上查找和下载所有模块。

在您的 `pom.xml` 中添加相应的依赖即可使用：

#### 核心运行时 (Core)

在您的 `pom.xml` 中添加核心运行时依赖：

```xml
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-runtime-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### AgentScope Agent 集成

如果需要使用 AgentScope Agent：

```xml
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-runtime-agentscope</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### 一键部署 (Web)

如果需要使用一键部署功能：

```xml
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-runtime-web</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### 协议集成

如果需要使用 A2A (Agent-to-Agent) 协议：

```xml
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>spring-boot-starter-runtime-a2a</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### 自动化部署

如果想要自动将 Agent 应用打包为容器，并自动部署到 K8s 或 AgentRun 上，可以使用提供的插件：

```xml
<plugin>
    <groupId>io.agentscope</groupId>
    <artifactId>deployer-maven-plugin</artifactId>
    <version>1.0.0</version>
    <configuration>
        <configFile>deployer.yml</configFile>
        <port>8080</port>
    </configuration>
</plugin>
```

### （可选）从源码安装

如果您想要使用最新的开发版本或为项目做贡献，可以从源码安装：

```bash
git clone https://github.com/agentscope-ai/agentscope-runtime-java.git

cd agentscope-runtime-java

mvn clean install -Dskiptests
```

安装完成后，依赖项将安装在本地 Maven 仓库中，您可以在项目中使用它们。

> 从源码安装会使用 SNAPSHOT 版本，适合开发和测试场景。生产环境建议使用 Maven Central 上的稳定版本。

### 使用 Maven 检查依赖

您也可以使用 Maven 命令检查依赖是否正确解析：

```bash
mvn dependency:tree | grep agentscope
```

这将显示所有与 agentscope 相关的依赖及其版本。


## 安装选项说明

这个图展示了安装选项的层次结构，从底层核心运行时（agentscope-runtime-core）开始——其中 **包含 Agent 运行框架 和 Sandbox 依赖**。可选模块（例如 agentscope、web、a2a-starter等）堆叠在核心之上，每个模块都增加了特定的功能（如多Agent框架支持、自动化）。查看所有安装选项的详细信息，请参见项目的 [pom.xml](https://github.com/agentscope-ai/agentscope-runtime-java/blob/main/pom.xml)。

| **组件**              | **Maven 坐标**                                  | **用途**                                                     |
| --------------------- | ----------------------------------------------- | ------------------------------------------------------------ |
| 核心运行时            | `io.agentscope:agentscope-runtime-core`         | 最小依赖，提供沙箱管理、记忆管理等基础运行时能力             |
| AgentScope Agent 集成 | `io.agentscope:agentscope-runtime-agentscope`   | AgentScope 集成，支持原生 AgentScope Agent 到 Runtime Agent 的转换，并内置 Sandbox Tool 到 AgentScope Tool 的映射逻辑 |
| 一键启动              | `io.agentscope:agentscope-runtime-web`          | 通过 LocalDeployer 实现 Agent 应用的一键启动与本地运行       |
| 协议集成              | `io.agentscope:spring-boot-starter-runtime-a2a` | 在用户构建好的 Spring Boot 应用中自动注册 A2A（Agent-to-Agent）通信端点及 Responses API 接口 |
| 自动化部署            | `deployer-maven-plugin`                         | 将 Agent 应用打包为一个容器，并可选部署到 K8s 或 AgentRun 上 |

## 版本信息

- **当前稳定版本**：`1.0.0`
- **发布位置**：[Maven Central](https://central.sonatype.com/artifact/io.agentscope/agentscope-runtime-core)
- **GroupId**：`io.agentscope`

### 在 Maven Central 上查找

您可以在 Maven Central 上搜索和查看所有可用模块：

- [agentscope-runtime-core](https://central.sonatype.com/artifact/io.agentscope/agentscope-runtime-core)
- [agentscope-runtime-agentscope](https://central.sonatype.com/artifact/io.agentscope/agentscope-runtime-agentscope)
- [agentscope-runtime-web](https://central.sonatype.com/artifact/io.agentscope/agentscope-runtime-web)
- [spring-boot-starter-runtime-a2a](https://central.sonatype.com/artifact/io.agentscope/spring-boot-starter-runtime-a2a)