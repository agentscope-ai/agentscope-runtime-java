# Browser Use Backend - Java Implementation

This is the Java implementation of the browser use backend.

## Overview

This Java backend exposes the same HTTP API endpoints as the Python version. By including the spring-boot-starter-runtime-a2a dependency and registering runners as Spring beans, the application gains support for A2A protocol calls—enabling seamless interaction between the frontend and the AgentScope browser agent.

## Project Structure

```
backend/
├── src/
│   └── main/
│       ├── java/
│       │   └── io/
│       │       └── agentscope/
│       │           └── browser/
│       │               ├── BrowserAgentApplication.java   # Spring Boot main application
│       │               ├── agent/
│       │               │   └── AgentscopeBrowseruseAgent.java  # Agent implementation
│       │               ├── constants/
│       │               │   └── Prompts.java                    # System prompts
│       │               └── controller/
│       │                   └── ChatController.java              # REST API endpoints
│       └── resources/
│           └── application.yml                                  # Spring Boot configuration
└── pom.xml                                                      # Maven configuration
```

## Prerequisites

1. **Java 17 or higher** - Required for this project
2. **Maven 3.6+** - For building the project
3. **Docker** - Required for the browser sandbox
4. **DashScope API Key** - Set as environment variable

## Environment Variables

Set the following environment variable before running:

```bash
export DASHSCOPE_API_KEY=your_api_key_here
# or
export AI_DASHSCOPE_API_KEY=your_api_key_here
```

## Building

First, ensure the parent project is built:

```bash
cd agentscope-runtime-java
mvn clean install -DskipTests
```

Then build this backend:

```bash
cd examples/browser_use_fullstack_runtime/backend
mvn clean package
```

## Running

### Option 1: Using Maven

```bash
mvn spring-boot:run
```

### Option 2: Using the JAR file

```bash
java -jar target/browser-agent-backend-1.0.0.jar
```

The service will start on **port 8080** by default.

## API Endpoints

### 1. Chat Completions (Streaming)

**Endpoints:**
- `POST /v1/chat/completions`
- `POST /chat/completions`

**Request:**
```json
{
  "messages": [
    {
      "role": "user",
      "content": "Visit www.google.com and search for AgentScope"
    }
  ]
}
```

**Response:** Server-Sent Events (SSE) stream with OpenAI-compatible format

### 2. Browser Environment Info

**Endpoint:**
- `GET /env_info`

**Response:**
```json
{
  "url": "ws://localhost:xxxxx"
}
```

Returns the WebSocket URL for the browser visualization.

### 3. A2A Completions(Streaming)

**Endpoint:**

`POST a2a/`

**Request:**

```json
{
    "method": "message/stream",
    "id": "2d2b4dc8-8ea2-437b-888d-3aaf3a8239dc",
    "jsonrpc": "2.0",
    "params": {
        "message": {
            "role": "user",
            "kind": "message",
            "contextId": "testContext",
            "metadata":{
                "userId": "testUser",
                "sessionId": "testSession"
            },
            "parts": [
                {
                    "text": "Visit www.google.com and search for AgentScope",
                    "kind": "text"
                }
            ],
            "messageId": "c4911b64c8404b7a8bf7200dd225b152"
        }
    }
}
```

**Response:** Server-Sent Events (SSE) stream with A2A-protocol format



## Key Differences from Python Version

1. **Framework**: Uses Spring Boot instead of Quart
2. **Reactive**: Uses Project Reactor (Flux) for streaming instead of async generators
3. **Type Safety**: Full type checking with Java
4. **Configuration**: Uses Spring Boot application.yml instead of .env file
5. **Dependency Management**: Uses Maven instead of pip

## Integration with Frontend

The Java backend is **fully compatible** with the existing React frontend. No frontend changes are required. Simply:

1. Start the Java backend on port 8080
2. Start the frontend on port 3000
3. The frontend will automatically connect to the backend

## Logging

Logs are output to the console. You can adjust the logging level in `application.yml`:

```yaml
logging:
  level:
    io:
      agentscope:
        browser: DEBUG  # Change to INFO, WARN, ERROR as needed
```

## Troubleshooting

### Port Already in Use

If port 8080 is already in use, you can change it in `application.yml`:

```yaml
server:
  port: 9000  # Use a different port
```

Then update the frontend configuration accordingly.

### API Key Not Set

Make sure the environment variable is set:
```bash
echo $DASHSCOPE_API_KEY
```

### Docker Issues

Ensure Docker is running:
```bash
docker ps
```

## Comparison with Python Backend

| Feature | Python Backend | Java Backend                       |
|---------|---------------|------------------------------------|
| Framework | Quart (Async Flask) | Spring Boot                        |
| Language | Python 3.11+ | Java 17+                           |
| Streaming | Async generators | Reactor Flux                       |
| Port | 9000 | 9000                               |
| API Compatibility | OpenAI-like SSE | OpenAI-like SSE & A2A-protocol SSE |
| Browser Tools | agentscope_runtime | agentscope-runtime-java            |

Both implementations provide **identical HTTP APIs** and can be used interchangeably with the frontend.

## License

Apache 2.0

