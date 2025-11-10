# 安装

准备好开始使用 AgentScope Runtime Java 了吗？本指南将帮助您在几分钟内快速搭建和运行**AgentScope Runtime Java**。

## 前置要求

- **Java 17** 或更高版本
- **Maven 3.6** 或更高版本
- **Docker**（可选，用于沙箱工具执行）

## 安装方式

### 1. 通过 Maven Central 安装（推荐）

AgentScope Runtime Java 已经发布到 Maven Central，您可以直接通过 Maven 依赖使用。

```{note}
当前稳定版本：0.1.0

您可以在 [Maven Central](https://central.sonatype.com/artifact/io.agentscope/agentscope-runtime-core) 上查找和下载所有模块。
```

在您的 `pom.xml` 中添加相应的依赖即可使用：

#### 核心运行时 (Core)

在您的 `pom.xml` 中添加核心运行时依赖：

```xml
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-runtime-core</artifactId>
    <version>0.1.0</version>
</dependency>
```

#### AgentScope Agent 集成

如果需要使用 AgentScope Agent：

```xml
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-runtime-agentscope</artifactId>
    <version>0.1.0</version>
</dependency>
```

#### Spring-AI-Alibaba Agent 支持

如果需要使用 Spring AI Alibaba Agent (SAA)：

```xml
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-runtime-saa</artifactId>
    <version>0.1.0</version>
</dependency>
```

#### 一键部署 (Web)

如果需要使用一键部署功能：

```xml
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-runtime-web</artifactId>
    <version>0.1.0</version>
</dependency>
```

#### A2A 集成

如果需要使用 A2A (Agent-to-Agent) 协议：

```xml
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>spring-boot-starter-runtime-a2a</artifactId>
    <version>0.1.0</version>
</dependency>
```

### 2. 从源码安装（可选）

如果您想要使用最新的开发版本、测试新功能或为项目做贡献，可以从源码安装：

```bash
git clone https://github.com/agentscope-ai/agentscope-runtime-java.git

cd agentscope-runtime-java

mvn clean install -DskipTests
```

安装完成后，依赖项将安装在本地 Maven 仓库中，您可以在项目中使用它们。

```{note}
从源码安装会使用 SNAPSHOT 版本，适合开发和测试场景。生产环境建议使用 Maven Central 上的稳定版本。
```

## 检查您的安装

要验证安装，您可以检查 Maven 依赖是否成功下载，或者尝试编译一个简单的 Java 类。

### 检查核心运行时

创建一个简单的 Java 类来测试核心运行时：

```java
import io.agentscope.runtime.engine.memory.context.ContextManager;
import io.agentscope.runtime.engine.memory.context.ContextManagerFactory;

public class InstallCheck {
    public static void main(String[] args) {
        try {
            ContextManager manager = ContextManagerFactory.createDefault();
            System.out.println("✅ agentscope-runtime-core - 安装成功");
        } catch (Exception e) {
            System.out.println("❌ agentscope-runtime-core - 安装失败: " + e.getMessage());
            System.out.println("💡 请确保已正确添加依赖到 pom.xml");
        }
    }
}
```

### 检查 AgentScope Agent

```java
import io.agentscope.runtime.engine.agents.agentscope.AgentScopeAgent;

public class AgentScopeCheck {
    public static void main(String[] args) {
        try {
            Class<?> agentClass = AgentScopeAgent.class;
            System.out.println("✅ AgentScopeAgent - 导入成功: " + agentClass.getName());
        } catch (NoClassDefFoundError e) {
            System.out.println("❌ AgentScopeAgent - 导入失败: " + e.getMessage());
            System.out.println("💡 请确保已添加 agentscope-runtime-agentscope 依赖");
        }
    }
}
```

### 检查 Spring-AI-Alibaba Agent

```java
import io.agentscope.runtime.engine.agents.saa.SaaAgent;

public class SaaAgentCheck {
    public static void main(String[] args) {
        try {
            Class<?> agentClass = SaaAgent.class;
            System.out.println("✅ SaaAgent - 导入成功: " + agentClass.getName());
        } catch (NoClassDefFoundError e) {
            System.out.println("❌ SaaAgent - 导入失败: " + e.getMessage());
            System.out.println("💡 请确保已添加 agentscope-runtime-saa 依赖");
        }
    }
}
```

### 使用 Maven 检查依赖

您也可以使用 Maven 命令检查依赖是否正确解析：

```bash
mvn dependency:tree | grep agentscope
```

这将显示所有与 agentscope 相关的依赖及其版本。

### 编译和运行检查代码

要运行上面的检查代码，您需要：

1. 创建一个 Maven 项目（如果还没有）
2. 将检查代码保存为 Java 文件
3. 在项目根目录运行：

```bash
# 编译项目
mvn compile

# 运行检查类（例如 InstallCheck）
mvn exec:java -Dexec.mainClass="InstallCheck"
```

或者使用 IDE（如 IntelliJ IDEA 或 Eclipse）直接运行 Java 类。

## 安装选项说明

这个图展示了安装选项的层次结构，从底层核心运行时（agentscope-runtime-core）开始——其中 **包含 Agent 运行框架 和 Sandbox 依赖**。可选模块（例如 saa、agentscope、web、a2a-starter等）堆叠在核心之上，每个模块都增加了特定的功能（如多Agent框架支持、自动化），并需要相应的依赖项。查看所有安装选项的详细信息，请参见项目的 [pom.xml](https://github.com/agentscope-ai/agentscope-runtime-java/blob/main/pom.xml)。

| **组件**                    | **Maven 坐标**                        | **用途**            | **依赖项**                                    |
| --------------------------- | ------------------------------------- | ------------------- | --------------------------------------------- |
| 核心运行时                  | `io.agentscope:agentscope-runtime-core`         | 核心运行环境        | 最小依赖，包括 Agent 运行框架 和 Sandbox 依赖 |
| Spring-AI-Alibaba Agent支持 | `io.agentscope:agentscope-runtime-saa`          | SAA Agent 开发支持  | Spring AI Alibaba 框架                        |
| AgentScope Agent 集成       | `io.agentscope:agentscope-runtime-agentscope`   | AgentScope 开发支持 | AgentScope 框架                               |
| 一键部署                    | `io.agentscope:agentscope-runtime-web`          | 一键对外部署        | SpringBoot 框架                               |
| A2A 集成                    | `io.agentscope:spring-boot-starter-runtime-a2a` | 引入A2A支持         | A2A SDK                                       |

### Maven 依赖示例

#### 最小化安装（仅核心运行时）

```xml
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-runtime-core</artifactId>
    <version>0.1.0</version>
</dependency>
```

#### 完整功能安装（包含所有模块）

```xml
<dependencies>
    <!-- 核心运行时 -->
    <dependency>
        <groupId>io.agentscope</groupId>
        <artifactId>agentscope-runtime-core</artifactId>
        <version>0.1.0</version>
    </dependency>
    
    <!-- AgentScope Agent 集成 -->
    <dependency>
        <groupId>io.agentscope</groupId>
        <artifactId>agentscope-runtime-agentscope</artifactId>
        <version>0.1.0</version>
    </dependency>
    
    <!-- Spring-AI-Alibaba Agent 支持 -->
    <dependency>
        <groupId>io.agentscope</groupId>
        <artifactId>agentscope-runtime-saa</artifactId>
        <version>0.1.0</version>
    </dependency>
    
    <!-- 一键部署 -->
    <dependency>
        <groupId>io.agentscope</groupId>
        <artifactId>agentscope-runtime-web</artifactId>
        <version>0.1.0</version>
    </dependency>
    
    <!-- A2A 集成 -->
    <dependency>
        <groupId>io.agentscope</groupId>
        <artifactId>spring-boot-starter-runtime-a2a</artifactId>
        <version>0.1.0</version>
    </dependency>
</dependencies>
```

## 版本信息

- **当前稳定版本**：`0.1.0`
- **发布位置**：[Maven Central](https://central.sonatype.com/artifact/io.agentscope/agentscope-runtime-core)
- **GroupId**：`io.agentscope`

### 在 Maven Central 上查找

您可以在 Maven Central 上搜索和查看所有可用模块：

- [agentscope-runtime-core](https://central.sonatype.com/artifact/io.agentscope/agentscope-runtime-core)
- [agentscope-runtime-agentscope](https://central.sonatype.com/artifact/io.agentscope/agentscope-runtime-agentscope)
- [agentscope-runtime-saa](https://central.sonatype.com/artifact/io.agentscope/agentscope-runtime-saa)
- [agentscope-runtime-web](https://central.sonatype.com/artifact/io.agentscope/agentscope-runtime-web)
- [spring-boot-starter-runtime-a2a](https://central.sonatype.com/artifact/io.agentscope/spring-boot-starter-runtime-a2a)
