# Advanced Deployment

This section demonstrates three advanced deployment methods available in AgentScope Runtime Java, providing production-ready solutions for different scenarios: **Local Docker Packaging**, **Kubernetes Deployment**, and **AgentRun Deployment**.

## Deployment Method Overview

AgentScope Runtime provides three different deployment methods, each targeting specific use cases:

| Deployment Type | Use Case | Scalability | Management | Resource Isolation |
|---------|---------|--------|---------|---------|
| **Local Docker Packaging** | Development & Testing | Single Container | Manual | Container Level |
| **Kubernetes** | Enterprise & Cloud | K8s Engine Auto-orchestration | Orchestrated | Container Level |
| **AgentRun** | AgentRun Platform | Cloud Managed | Platform Managed | Container Level |

## Prerequisites

### 🔧 Installation Requirements

Add the **packaging dependency** provided by AgentScope Runtime Java:

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

### 🔑 Parameter Configuration

Configure deployment parameters in a yaml file. The yaml file path is read from the `configFile` path configured in the maven plugin.

```yaml
build:
  imageName: agentscope-use-example	# Name of the built image
  imageTag: latest  # Tag of the built image
  baseImage: eclipse-temurin:17-jre # Base image
  port: 10001  # Application internal port, i.e., the port number configured in AgentApp
  pushToRegistry: false # Whether to push the image to Docker Registry (must be true if deploying to K8s cluster)
  deployToK8s: false  # Whether to deploy to Kubernetes cluster
  deployToAgentRun: true  # Whether to deploy to Alibaba Cloud Function Compute AgentRun

# ============================================
# Docker image registry configuration. Note: if deploying to K8s later, ensure K8s has access to this registry
# ============================================
registry:
  url: "<REGISTRY_URL>" # Image registry address
  username: "<REGISTRY_USERNAME>" # Image registry username
  password: "<REGISTRY_PASSWORD>" # Image registry password
  namespace: "<REGISTRY_NAMESPACE>" # Image registry namespace

# ============================================
# K8s deployment configuration
# ============================================
kubernetes:
  replicas: 1 # Number of deployment replicas
  kubeconfigPath: "<KUBECONFIG_PATH>" # Kubeconfig file path
  namespace: "default"  # Deployment namespace

# ============================================
# OSS configuration (used for AgentRun deployment)
# ============================================
oss:
  region: cn-hangzhou # OSS region
  accessKeyId: "<YOUR_ACCESS_KEY_ID>" # OSS access key ID
  accessKeySecret: "<YOUR_ACCESS_KEY_SECRET>" # OSS access key secret
  bucket: "<YOUR_BUCKET_KEY_ID>"  # OSS bucket name

# ============================================
# Environment variables passed to the application
# ============================================
environment:
  AI_DASHSCOPE_API_KEY: "<DASHSCOPE API KEY>"
  SPRING_PROFILES_ACTIVE: production

# ============================================
# AgentRun deployment configuration. Note: if deploying to AgentRun, OSS must be configured first. OSS and AgentRun share the same access keys
# ============================================
agentrun:
  region: cn-hangzhou # AgentRun region
  runtimeNamePrefix: agentscope-use-example # Deployed runtime name prefix
  cpu: 2  # Number of CPU cores
  memorySize: 2048  # Memory size in MB
  sessionConcurrencyLimit: 1  # Session concurrency limit
  sessionIdleTimeoutSeconds: 600  # Session idle timeout in seconds
  networkMode: PUBLIC # Network mode, PUBLIC or VPC
```

### 📦 Prerequisites for Each Deployment Type

#### All Deployment Types

- **Java 17** or higher
- **Maven 3.6** or higher
- **Docker** (for image packaging)

#### Kubernetes Deployment
- **Kubernetes** cluster access
- Configured **kubectl**
- **Container registry** access (for pushing images)

#### AgentRun Deployment

* **AgentRun** access parameters

## Common Agent Configuration

All deployment methods share the same agent and endpoint configuration. Refer to [Simple Deployment](agent_app.md) to first build a web application.

## Method 1: Local Docker Image Packaging

**Best For**: Development, testing, and single-user scenarios requiring manual control of persistent services.

### Features
- One-click build of web application image
- Manual lifecycle management
- Interactive control and monitoring
- Direct resource sharing

### Usage

Using the agent and endpoints defined in [Common Agent Configuration](#common-agent-configuration), configure packaging parameters:

```yaml
build:
  imageName: agentscope-use-example	# Name of the built image
  imageTag: latest  # Tag of the built image
  baseImage: eclipse-temurin:17-jre # Base image
  port: 10001  # Application internal port, i.e., the port number configured in AgentApp
  
# ============================================
# Environment variables passed to the application
# ============================================
environment:
  AI_DASHSCOPE_API_KEY: "<DASHSCOPE API KEY>"
  SPRING_PROFILES_ACTIVE: production  
```

**Key Points**:

- Service will be packaged as the specified image
- Manually manage container lifecycle via `docker run` command
- Best for development and testing

### Testing Deployed Service

After deployment, you can test the endpoints using curl:

**Using curl:**

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
          "text": "Hello, please calculate the 10th Fibonacci number using Python",
          "kind": "text"
        }
      ],
      "messageId": "c4911b64c8404b7a8bf7200dd225b152"
    }
  }
}'
```


## Method 2: Kubernetes Deployment

**Best For**: Enterprise production environments requiring scalability, high availability, and cloud-native orchestration.

### Features
- Container-based deployment
- Horizontal scaling support
- Cloud-native orchestration
- Resource management and limits
- Health checks and auto-recovery

### Kubernetes Deployment Prerequisites

```bash
# Ensure Docker is running
docker --version

# Verify Kubernetes access
kubectl cluster-info

# Check registry access (using Alibaba Cloud as example)
docker login your-registry
```

### Usage

Using the agent and endpoints defined in [Common Agent Configuration](#common-agent-configuration), configure packaging parameters needed for K8s deployment:

```yaml
build:
  imageName: agentscope-use-example	# Name of the built image
  imageTag: latest  # Tag of the built image
  baseImage: eclipse-temurin:17-jre # Base image
  port: 10001  # Application internal port, i.e., the port number configured in AgentApp
  pushToRegistry: true # Whether to push the image to Docker Registry (must be true if deploying to K8s cluster)
  deployToK8s: true  # Whether to deploy to Kubernetes cluster

# ============================================
# Docker image registry configuration. Note: if deploying to K8s later, ensure K8s has access to this registry
# ============================================
registry:
  url: "<REGISTRY_URL>" # Image registry address
  username: "<REGISTRY_USERNAME>" # Image registry username
  password: "<REGISTRY_PASSWORD>" # Image registry password
  namespace: "<REGISTRY_NAMESPACE>" # Image registry namespace

# ============================================
# K8s deployment configuration
# ============================================
kubernetes:
  replicas: 1 # Number of deployment replicas
  kubeconfigPath: "<KUBECONFIG_PATH>" # Kubeconfig file path
  namespace: "default"  # Deployment namespace


# ============================================
# Environment variables passed to the application
# ============================================
environment:
  AI_DASHSCOPE_API_KEY: "<DASHSCOPE API KEY>"
  SPRING_PROFILES_ACTIVE: production
```

**Key Points**:

- Container-based deployment with auto-scaling support
- Configure resource limits and health checks
- Scale using `kubectl scale deployment`

## Method 3: Serverless Deployment: AgentRun

**Best For**: Alibaba Cloud users who need to deploy agents to AgentRun service, achieving automated build, upload, and deployment workflows.

### Features
- Managed deployment to Alibaba Cloud AgentRun service
- Automatic project build and packaging
- OSS integration for artifact storage
- Complete lifecycle management
- Automatic creation and management of runtime endpoints

### Usage

Using the agent and endpoints defined in [Common Agent Configuration](#common-agent-configuration), configure parameters needed for AgentRun deployment:

```yaml
build:
  imageName: agentscope-use-example	# Name of the built image
  imageTag: latest  # Tag of the built image
  baseImage: eclipse-temurin:17-jre # Base image
  port: 10001  # Application internal port, i.e., the port number configured in AgentApp
  deployToAgentRun: true  # Whether to deploy to Alibaba Cloud Function Compute AgentRun

# ============================================
# OSS configuration (used for AgentRun deployment, OSS repository stores build artifacts)
# ============================================
oss:
  region: cn-hangzhou # OSS region
  accessKeyId: "<YOUR_ACCESS_KEY_ID>" # OSS access key ID
  accessKeySecret: "<YOUR_ACCESS_KEY_SECRET>" # OSS access key secret
  bucket: "<YOUR_BUCKET_KEY_ID>"  # OSS bucket name

# ============================================
# Environment variables passed to the application
# ============================================
environment:
  AI_DASHSCOPE_API_KEY: "<DASHSCOPE API KEY>"
  SPRING_PROFILES_ACTIVE: production

# ============================================
# AgentRun deployment configuration. Note: if deploying to AgentRun, OSS must be configured first. OSS and AgentRun share the same access keys
# ============================================
agentrun:
  region: cn-hangzhou # AgentRun region
  runtimeNamePrefix: agentscope-use-example # Deployed runtime name prefix
  cpu: 2  # Number of CPU cores
  memorySize: 2048  # Memory size in MB
  sessionConcurrencyLimit: 1  # Session concurrency limit
  sessionIdleTimeoutSeconds: 600  # Session idle timeout in seconds
  networkMode: PUBLIC # Network mode, PUBLIC or VPC
```

**Key Points**:
- Automatically build project and package as jar file
- Upload artifacts to OSS
- Create and manage runtime in AgentRun service
- Automatically create public access endpoint

