# Installation

Ready to get started with AgentScope Runtime Java? This guide will help you quickly set up and run **AgentScope Runtime Java** in just a few minutes.

## Prerequisites

- **Java 17** or higher
- **Maven 3.6** or higher
- **Docker** (optional, for sandbox tool execution)

## Installation Methods

### 1. Install via Maven Central (Recommended)

AgentScope Runtime Java has been published to Maven Central, and you can use it directly through Maven dependencies.

```{note}
Current stable version: 0.1.0

You can find and download all modules on [Maven Central](https://central.sonatype.com/artifact/io.agentscope/agentscope-runtime-core).
```

Add the corresponding dependencies to your `pom.xml`:

#### Core Runtime

Add the core runtime dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-runtime-core</artifactId>
    <version>0.1.0</version>
</dependency>
```

#### AgentScope Agent Integration

If you need to use AgentScope Agent:

```xml
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-runtime-agentscope</artifactId>
    <version>0.1.0</version>
</dependency>
```

#### Spring-AI-Alibaba Agent Support

If you need to use Spring AI Alibaba Agent (SAA):

```xml
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-runtime-saa</artifactId>
    <version>0.1.0</version>
</dependency>
```

#### One-Click Deployment (Web)

If you need to use one-click deployment:

```xml
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-runtime-web</artifactId>
    <version>0.1.0</version>
</dependency>
```

#### A2A Integration

If you need to use A2A (Agent-to-Agent) protocol:

```xml
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>spring-boot-starter-runtime-a2a</artifactId>
    <version>0.1.0</version>
</dependency>
```

### 2. Install from Source (Optional)

If you want to use the latest development version, test new features, or contribute to the project, you can install from source:

```bash
git clone https://github.com/agentscope-ai/agentscope-runtime-java.git

cd agentscope-runtime-java

mvn clean install -DskipTests
```

After installation, the dependencies will be installed in your local Maven repository, and you can use them in your projects.

```{note}
Installing from source will use SNAPSHOT versions, suitable for development and testing scenarios. Production environments are recommended to use stable versions from Maven Central.
```

## Verifying Your Installation

To verify the installation, you can check if Maven dependencies were successfully downloaded, or try compiling a simple Java class.

### Check Core Runtime

Create a simple Java class to test the core runtime:

```java
import io.agentscope.runtime.engine.memory.context.ContextManager;
import io.agentscope.runtime.engine.memory.context.ContextManagerFactory;

public class InstallCheck {
    public static void main(String[] args) {
        try {
            ContextManager manager = ContextManagerFactory.createDefault();
            System.out.println("✅ agentscope-runtime-core - Installation successful");
        } catch (Exception e) {
            System.out.println("❌ agentscope-runtime-core - Installation failed: " + e.getMessage());
            System.out.println("💡 Please ensure dependencies are correctly added to pom.xml");
        }
    }
}
```

### Check AgentScope Agent

```java
import io.agentscope.runtime.engine.agents.agentscope.AgentScopeAgent;

public class AgentScopeCheck {
    public static void main(String[] args) {
        try {
            Class<?> agentClass = AgentScopeAgent.class;
            System.out.println("✅ AgentScopeAgent - Import successful: " + agentClass.getName());
        } catch (NoClassDefFoundError e) {
            System.out.println("❌ AgentScopeAgent - Import failed: " + e.getMessage());
            System.out.println("💡 Please ensure agentscope-runtime-agentscope dependency is added");
        }
    }
}
```

### Check Spring-AI-Alibaba Agent

```java
import io.agentscope.runtime.engine.agents.saa.SaaAgent;

public class SaaAgentCheck {
    public static void main(String[] args) {
        try {
            Class<?> agentClass = SaaAgent.class;
            System.out.println("✅ SaaAgent - Import successful: " + agentClass.getName());
        } catch (NoClassDefFoundError e) {
            System.out.println("❌ SaaAgent - Import failed: " + e.getMessage());
            System.out.println("💡 Please ensure agentscope-runtime-saa dependency is added");
        }
    }
}
```

### Check Dependencies with Maven

You can also use Maven commands to check if dependencies are correctly resolved:

```bash
mvn dependency:tree | grep agentscope
```

This will display all agentscope-related dependencies and their versions.

### Compile and Run Check Code

To run the check code above, you need to:

1. Create a Maven project (if you haven't already)
2. Save the check code as a Java file
3. Run in the project root directory:

```bash
# Compile the project
mvn compile

# Run the check class (e.g., InstallCheck)
mvn exec:java -Dexec.mainClass="InstallCheck"
```

Or use an IDE (such as IntelliJ IDEA or Eclipse) to run the Java class directly.

## Installation Options Overview

This diagram shows the hierarchy of installation options, starting from the bottom core runtime (agentscope-runtime-core)—which **includes the Agent runtime framework and Sandbox dependencies**. Optional modules (such as saa, agentscope, web, a2a-starter, etc.) are stacked on top of the core, each adding specific functionality (such as multi-Agent framework support, automation) and requiring corresponding dependencies. For detailed information on all installation options, see the project's [pom.xml](https://github.com/agentscope-ai/agentscope-runtime-java/blob/main/pom.xml).

| **Component**                    | **Maven Coordinates**                        | **Purpose**            | **Dependencies**                                    |
| --------------------------- | ------------------------------------- | ------------------- | --------------------------------------------- |
| Core Runtime                  | `io.agentscope:agentscope-runtime-core`         | Core runtime environment        | Minimal dependencies, including Agent runtime framework and Sandbox dependencies |
| Spring-AI-Alibaba Agent Support | `io.agentscope:agentscope-runtime-saa`          | SAA Agent development support  | Spring AI Alibaba framework                        |
| AgentScope Agent Integration       | `io.agentscope:agentscope-runtime-agentscope`   | AgentScope development support | AgentScope framework                               |
| One-Click Deployment                    | `io.agentscope:agentscope-runtime-web`          | One-click external deployment        | SpringBoot framework                               |
| A2A Integration                    | `io.agentscope:spring-boot-starter-runtime-a2a` | A2A support         | A2A SDK                                       |

### Maven Dependency Examples

#### Minimal Installation (Core Runtime Only)

```xml
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-runtime-core</artifactId>
    <version>0.1.0</version>
</dependency>
```

#### Full Feature Installation (All Modules Included)

```xml
<dependencies>
    <!-- Core Runtime -->
    <dependency>
        <groupId>io.agentscope</groupId>
        <artifactId>agentscope-runtime-core</artifactId>
        <version>0.1.0</version>
    </dependency>
    
    <!-- AgentScope Agent Integration -->
    <dependency>
        <groupId>io.agentscope</groupId>
        <artifactId>agentscope-runtime-agentscope</artifactId>
        <version>0.1.0</version>
    </dependency>
    
    <!-- Spring-AI-Alibaba Agent Support -->
    <dependency>
        <groupId>io.agentscope</groupId>
        <artifactId>agentscope-runtime-saa</artifactId>
        <version>0.1.0</version>
    </dependency>
    
    <!-- One-Click Deployment -->
    <dependency>
        <groupId>io.agentscope</groupId>
        <artifactId>agentscope-runtime-web</artifactId>
        <version>0.1.0</version>
    </dependency>
    
    <!-- A2A Integration -->
    <dependency>
        <groupId>io.agentscope</groupId>
        <artifactId>spring-boot-starter-runtime-a2a</artifactId>
        <version>0.1.0</version>
    </dependency>
</dependencies>
```

## Version Information

- **Current Stable Version**: `0.1.0`
- **Release Location**: [Maven Central](https://central.sonatype.com/artifact/io.agentscope/agentscope-runtime-core)
- **GroupId**: `io.agentscope`

### Finding on Maven Central

You can search and view all available modules on Maven Central:

- [agentscope-runtime-core](https://central.sonatype.com/artifact/io.agentscope/agentscope-runtime-core)
- [agentscope-runtime-agentscope](https://central.sonatype.com/artifact/io.agentscope/agentscope-runtime-agentscope)
- [agentscope-runtime-saa](https://central.sonatype.com/artifact/io.agentscope/agentscope-runtime-saa)
- [agentscope-runtime-web](https://central.sonatype.com/artifact/io.agentscope/agentscope-runtime-web)
- [spring-boot-starter-runtime-a2a](https://central.sonatype.com/artifact/io.agentscope/spring-boot-starter-runtime-a2a)




