# AgentScope Deployer Maven Plugin

A Maven plugin for automatically packaging AgentScope Runtime Java applications into Docker images and optionally deploying them to Kubernetes.

## Features

- ✅ Automatically generate Dockerfile
- ✅ Build Docker images
- ✅ Support pushing to Docker image registry
- ✅ Support deploying to Kubernetes
- ✅ Support YAML configuration files
- ✅ Support command-line parameter overrides

## Quick Start

### 1. Install the Plugin

First, build the plugin in the project root directory:

```bash
cd maven_plugin
mvn clean install
```

### 2. Use in Your Project

Add the plugin configuration to your Spring Boot project's `pom.xml`:

```xml
<plugin>
    <groupId>io.agentscope</groupId>
    <artifactId>deployer-maven-plugin</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <configuration>
        <configFile>deployer.yml</configFile>
        <port>8080</port>
    </configuration>
</plugin>
```

### 3. Create Configuration File

Create a `deployer.yml` configuration file in the project root directory:

```yaml
build:
  imageName: my-app
  imageTag: latest
  baseImage: eclipse-temurin:17-jre
  port: 8080
  pushToRegistry: false
  deployToK8s: false
  environment:
    MY_ENV_VAR: value

registry:
  url: registry.example.com
  namespace: myorg
  username: myuser
  password: mypass

kubernetes:
  namespace: default
  replicas: 1
```

### 4. Build Docker Image

```bash
mvn clean package deployer:build
```

Or use directly:

```bash
mvn clean package
```

(The plugin will automatically execute during the `package` phase)

## Configuration

### Configuration File (deployer.yml)

#### build Configuration

| Parameter | Description | Default Value |
|-----------|-------------|---------------|
| `imageName` | Docker image name | Project artifactId |
| `imageTag` | Docker image tag | Project version |
| `baseImage` | Base Docker image | `eclipse-temurin:17-jre` |
| `port` | Container port | `8080` |
| `pushToRegistry` | Whether to push to image registry | `false` |
| `deployToK8s` | Whether to deploy to Kubernetes | `false` |
| `deployToModelStudio` | Deploy to ModelStudio | `false` |
| `deployToAgentRun` | Deploy to AgentRun | `false` |
| `buildContextDir` | Build context directory | `${project.build.directory}/deployer` |
| `environment` | Environment variable mapping | `{}` |

#### registry Configuration

| Parameter | Description | Default Value |
|-----------|-------------|---------------|
| `url` | Image registry URL | - |
| `namespace` | Namespace/organization name | - |
| `username` | Username | - |
| `password` | Password | - |

#### kubernetes Configuration

| Parameter | Description | Default Value |
|-----------|-------------|---------------|
| `namespace` | Kubernetes namespace | `agentscope-runtime` |
| `kubeconfigPath` | kubeconfig file path | Use default configuration |
| `replicas` | Number of replicas | `1` |
| `runtimeConfig` | Runtime configuration | `{}` |

#### modelstudio Configuration

| Parameter | Description | Default Value |
|-----------|-------------|---------------|
| `region` | ModelStudio region | `cn-beijing` |
| `endpoint` | API endpoint | `bailian.cn-beijing.aliyuncs.com` |
| `workspaceId` | Workspace ID | `default` |
| `accessKeyId` | Bailian AK | - |
| `accessKeySecret` | Bailian SK | - |
| `dashscopeApiKey` | Optional DashScope key | - |
| `telemetryEnabled` | Enable wrapper telemetry | `true` |
| `deployName` | Agent name in Bailian | `agentscope-runtime` |
| `agentId` | Update existing agent ID | - |
| `agentDescription` | Description text | - |
| `serviceName` | Legacy field for name | `agentscope-runtime` |
| `functionName` | Legacy field for name | `agentscope-function` |
| `artifactBucket` | Artifact bucket (if needed) | `agentscope-runtime-fc-artifacts` |
| `memorySize` | Memory size in MB | `512` |
| `timeoutSeconds` | Timeout in seconds | `60` |
| `metadata` | Additional metadata map | `{}` |

#### agentrun Configuration

| Parameter | Description | Default Value |
|-----------|-------------|---------------|
| `region` | AgentRun region | `cn-beijing` |
| `endpoint` | API endpoint | `agentrun.<region>.aliyuncs.com` |
| `accessKeyId` | AgentRun AK | - |
| `accessKeySecret` | AgentRun SK | - |
| `runtimeNamePrefix` | Runtime name prefix | `agentscope-runtime` |
| `artifactBucket` | OSS bucket for code | `agentscope-runtime-agentrun-artifacts` |
| `cpu` | CPU cores | `2` |
| `memorySize` | Memory in MB | `4096` |
| `timeoutSeconds` | Timeout in seconds | `120` |
| `executionRoleArn` | Execution role ARN | - |
| `logProject` | Log service project | - |
| `logStore` | Log service logstore | - |
| `networkMode` | `PUBLIC` or `PUBLIC_AND_PRIVATE` | `PUBLIC` |
| `vpcId` | VPC ID when using private networks | - |
| `securityGroupId` | Security group ID | - |
| `vswitchIds` | List of vswitch IDs | `[]` |
| `sessionConcurrencyLimit` | Per-instance concurrency | `1` |
| `sessionIdleTimeoutSeconds` | Idle timeout seconds | `600` |
| `existingRuntimeId` | Update an existing runtime | - |
| `metadata` | Additional metadata map | `{}` |

### Command-Line Parameters

All configurations can be overridden via command-line parameters:

```bash
# Specify image name and tag
mvn deployer:build -Ddeployer.imageName=my-app -Ddeployer.imageTag=v1.0.0

# Push to registry
mvn deployer:build -Ddeployer.push=true

# Deploy to Kubernetes
mvn deployer:build -Ddeployer.deploy=true -Ddeployer.k8sNamespace=production

# Set environment variables
mvn deployer:build -Ddeployer.environment.AI_DASHSCOPE_API_KEY=your-key

# Deploy to ModelStudio
mvn deployer:build \
  -Ddeployer.deployToModelStudio=true \
  -Ddeployer.modelStudioAccessKeyId=xxx \
  -Ddeployer.modelStudioAccessKeySecret=yyy

# Deploy to AgentRun
mvn deployer:build \
  -Ddeployer.deployToAgentRun=true \
  -Ddeployer.agentRunAccessKeyId=xxx \
  -Ddeployer.agentRunAccessKeySecret=yyy \
  -Ddeployer.agentRunBucket=my-agentrun-bucket
```

## Usage Examples

### Example 1: Build Image Only

```bash
mvn clean package deployer:build
```

### Example 2: Build and Push to Registry

Configure in `deployer.yml`:

```yaml
build:
  pushToRegistry: true

registry:
  url: registry.example.com
  namespace: myorg
  username: myuser
  password: mypass
```

Then execute:

```bash
mvn clean package deployer:build
```

### Example 3: Build and Deploy to Kubernetes

Configure in `deployer.yml`:

```yaml
build:
  deployToK8s: true

kubernetes:
  namespace: production
  replicas: 3
```

Then execute:

```bash
mvn clean package deployer:build
```

### Example 4: Complete Workflow (Build, Push, Deploy)

```yaml
build:
  imageName: my-spring-boot-app
  imageTag: v1.0.0
  baseImage: eclipse-temurin:17-jre
  port: 8080
  pushToRegistry: true
  deployToK8s: true
  environment:
    SPRING_PROFILES_ACTIVE: production
    DATABASE_URL: jdbc:postgresql://db:5432/mydb

registry:
  url: registry.example.com
  namespace: myorg
  username: ${REGISTRY_USERNAME}
  password: ${REGISTRY_PASSWORD}

kubernetes:
  namespace: production
  replicas: 3
  runtimeConfig:
    resources.limits.cpu: "2"
    resources.limits.memory: "2Gi"
```

## Generated Dockerfile

The plugin will automatically generate a Dockerfile similar to the following:

```dockerfile
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy JAR file
COPY my-app-1.0.0.jar app.jar

# Environment variables
ENV SPRING_PROFILES_ACTIVE=production

# Expose port
EXPOSE 8080

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## Notes

1. **Docker Environment**: Ensure Docker is running and accessible
2. **Kubernetes Configuration**: Ensure kubeconfig is properly configured before deploying to K8s
3. **Image Registry Authentication**: Provide correct authentication information when pushing to private registries
4. **Environment Variables**: Sensitive information (such as passwords) should use environment variables instead of being written directly in configuration files

## Troubleshooting

### Docker Client Initialization Failed

Ensure Docker is running:

```bash
docker info
```

### Kubernetes Deployment Failed

Check kubeconfig configuration:

```bash
kubectl config view
```

### Image Push Failed

Check if the image registry authentication information is correct and ensure you have push permissions.

## Development

### Build the Plugin

```bash
cd maven_plugin
mvn clean install
```

### Test the Plugin

Test in the example project:

```bash
cd examples/simple_agent_use_examples/agentscope_use_example
mvn clean package deployer:build
```

## License

Apache License 2.0
