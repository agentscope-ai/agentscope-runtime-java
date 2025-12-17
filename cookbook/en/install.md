# Installation

Ready to start using AgentScope Runtime Java? This guide will help you quickly set up and run **AgentScope Runtime Java** in just a few minutes.

## Prerequisites

- **Java 17** or higher
- **Maven 3.6** or higher
- **Docker** (optional, for sandbox tool execution)

## Installation Methods

### Install via Maven Central (Recommended)

AgentScope Runtime Java has been published to Maven Central, and you can use it directly through Maven dependencies.

> Current stable version: 1.0.0
>
> You can find and download all modules on [Maven Central](https://central.sonatype.com/artifact/io.agentscope/agentscope-runtime-core).

Add the corresponding dependencies in your `pom.xml`:

#### Core Runtime

Add the core runtime dependency in your `pom.xml`:

```xml
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-runtime-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### AgentScope Agent Integration

If you need to use AgentScope Agent:

```xml
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-runtime-agentscope</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### One-Click Deployment (Web)

If you need to use one-click deployment functionality:

```xml
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-runtime-web</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### Protocol Integration

If you need to use A2A (Agent-to-Agent) protocol:

```xml
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>spring-boot-starter-runtime-a2a</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### Automated Deployment

If you want to automatically package Agent applications as containers and automatically deploy to K8s clusters or AgentRun, you can use the provided plugin:

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

### (Optional) Install from Source

If you want to use the latest development version or contribute to the project, you can install from source:

```bash
git clone https://github.com/agentscope-ai/agentscope-runtime-java.git

cd agentscope-runtime-java

mvn clean install -Dskiptests
```

After installation, the dependencies will be installed in your local Maven repository, and you can use them in your projects.

> Installing from source will use SNAPSHOT versions, suitable for development and testing scenarios. Production environments are recommended to use stable versions from Maven Central.

### Check Dependencies with Maven

You can also use Maven commands to check if dependencies are correctly resolved:

```bash
mvn dependency:tree | grep agentscope
```

This will display all agentscope-related dependencies and their versions.

## Installation Options Overview

This diagram shows the hierarchy of installation options, starting from the bottom-level core runtime (agentscope-runtime-core)—which **includes the Agent runtime framework and Sandbox dependencies**. Optional modules (such as agentscope, web, a2a-starter, etc.) are stacked on top of the core, with each module adding specific functionality (such as multi-agent framework support, automation). For detailed information on all installation options, please refer to the project's [pom.xml](https://github.com/agentscope-ai/agentscope-runtime-java/blob/main/pom.xml).

| **Component**              | **Maven Coordinates**                                  | **Purpose**                                                     |
| --------------------- | ----------------------------------------------- | ------------------------------------------------------------ |
| Core Runtime            | `io.agentscope:agentscope-runtime-core`         | Minimum dependency, provides basic runtime capabilities such as sandbox management, memory management             |
| AgentScope Agent Integration | `io.agentscope:agentscope-runtime-agentscope`   | AgentScope integration, supports native AgentScope Agent to Runtime Agent conversion, and includes built-in Sandbox Tool to AgentScope Tool mapping logic |
| One-Click Startup              | `io.agentscope:agentscope-runtime-web`          | One-click startup and local running of Agent applications through LocalDeployer       |
| Protocol Integration              | `io.agentscope:spring-boot-starter-runtime-a2a` | Automatically registers A2A (Agent-to-Agent) communication endpoints and Responses API interfaces in user-built Spring Boot applications |
| Automated Deployment            | `deployer-maven-plugin`                         | Packages Agent applications as containers, and optionally deploys to K8s or AgentRun |

## Version Information

- **Current Stable Version**: `1.0.0`
- **Release Location**: [Maven Central](https://central.sonatype.com/artifact/io.agentscope/agentscope-runtime-core)
- **GroupId**: `io.agentscope`

### Find on Maven Central

You can search and view all available modules on Maven Central:

- [agentscope-runtime-core](https://central.sonatype.com/artifact/io.agentscope/agentscope-runtime-core)
- [agentscope-runtime-agentscope](https://central.sonatype.com/artifact/io.agentscope/agentscope-runtime-agentscope)
- [agentscope-runtime-web](https://central.sonatype.com/artifact/io.agentscope/agentscope-runtime-web)
- [spring-boot-starter-runtime-a2a](https://central.sonatype.com/artifact/io.agentscope/spring-boot-starter-runtime-a2a)

