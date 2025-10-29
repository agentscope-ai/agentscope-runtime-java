# Chatbot Backend - Java Implementation

This is the Java implementation of the chatbot backend, equivalent to the Python version in the `backend` folder.

## Overview

This Java backend provides a complete chatbot system with:
- **User authentication** - Login system with password hashing
- **Conversation management** - Create, read, update, delete conversations
- **Message storage** - SQLite database for persistence
- **AI agent integration** - LLM-powered responses via AgentScope Runtime
- **RESTful API** - Compatible with the React frontend

## Architecture

The backend consists of two separate services:

### 1. Agent Server (Port 8090)
- Deploys the LLM agent as a service
- Handles agent queries and responses
- Provides streaming SSE responses
- Equivalent to `agent_server.py`

### 2. Web Server (Port 5100)
- REST API for frontend communication
- User authentication
- Conversation and message CRUD operations
- Calls Agent Server for AI responses
- Equivalent to `web_server.py`

## Project Structure

```
backend_java/
├── src/main/
│   ├── java/io/agentscope/chatbot/
│   │   ├── AgentServerApplication.java      # Agent service main class
│   │   ├── WebServerApplication.java        # Web API main class
│   │   ├── model/
│   │   │   ├── User.java                    # User entity
│   │   │   ├── Conversation.java            # Conversation entity
│   │   │   └── Message.java                 # Message entity
│   │   ├── repository/
│   │   │   ├── UserRepository.java
│   │   │   ├── ConversationRepository.java
│   │   │   └── MessageRepository.java
│   │   ├── service/
│   │   │   └── AgentService.java            # Service to call agent
│   │   └── controller/
│   │       └── WebServerController.java     # REST API endpoints
│   └── resources/
│       ├── application.yml                   # Default config
│       ├── application-agent.yml             # Agent server config
│       └── application-web.yml               # Web server config
├── pom.xml
├── run-agent.sh                              # Start agent server
├── run-web.sh                                # Start web server
├── quickstart.sh                             # Build and setup
└── README.md
```

## Prerequisites

- Java 17+
- Maven 3.6+
- DashScope API key
- SQLite (database will be created automatically)

## Environment Variables

```bash
export DASHSCOPE_API_KEY=your_api_key_here
# or
export AI_DASHSCOPE_API_KEY=your_api_key_here
```

## Quick Start

### 1. Build Everything

```bash
./quickstart.sh
```

This script will:
- Check prerequisites
- Build parent project (if needed)
- Build the backend
- Prepare the database

### 2. Start Agent Server (Terminal 1)

```bash
./run-agent.sh
```

The agent server will start on **port 8090**.

### 3. Start Web Server (Terminal 2)

```bash
./run-web.sh
```

The web server will start on **port 5100**.

### 4. Start Frontend (Terminal 3)

```bash
cd ../frontend
npm install
npm run start
```

The frontend will start on **port 3000**.

## Manual Setup

### Build Parent Project

```bash
cd ../../..
mvn clean install -DskipTests
```

### Build Backend

```bash
cd examples/chatbot_fullstack_runtime/backend_java
mvn clean package
```

### Prepare Database

If you have the Python example database:

```bash
cp ../backend/ai_assistant_example.db ai_assistant.db
```

Otherwise, the database will be created automatically with sample users.

### Run Agent Server

```bash
export DASHSCOPE_API_KEY=your_key
export SERVER_PORT=8090
export SERVER_ENDPOINT=agent

mvn spring-boot:run -Dspring-boot.run.profiles=agent -Dspring-boot.run.main-class=io.agentscope.chatbot.AgentServerApplication
```

### Run Web Server

```bash
export SERVER_HOST=localhost
export SERVER_PORT=8090
export SERVER_ENDPOINT=agent

mvn spring-boot:run -Dspring-boot.run.profiles=web -Dspring-boot.run.main-class=io.agentscope.chatbot.WebServerApplication
```

## API Endpoints

All endpoints are 100% compatible with the Python version.

### Authentication

- **POST** `/api/login` - User login

### Users

- **GET** `/api/users/{user_id}` - Get user info
- **GET** `/api/users/{user_id}/conversations` - Get all conversations
- **POST** `/api/users/{user_id}/conversations` - Create new conversation

### Conversations

- **GET** `/api/conversations/{conversation_id}` - Get conversation details
- **PUT** `/api/conversations/{conversation_id}` - Update conversation
- **DELETE** `/api/conversations/{conversation_id}` - Delete conversation
- **POST** `/api/conversations/{conversation_id}/messages` - Send message

## Default Users

The system comes with two default users:

| Username | Password | Name |
|----------|----------|------|
| user1 | password123 | Bruce |
| user2 | password456 | John |

## Database

The backend uses SQLite database (`ai_assistant.db`) with the following schema:

### Tables

- **user** - User accounts with hashed passwords
- **conversation** - Conversation metadata
- **message** - Chat messages (user and AI)

### Schema

```sql
CREATE TABLE user (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username VARCHAR(80) UNIQUE NOT NULL,
    password_hash VARCHAR(120) NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_at DATETIME NOT NULL
);

CREATE TABLE conversation (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title VARCHAR(200) NOT NULL,
    user_id INTEGER NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES user(id)
);

CREATE TABLE message (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    text TEXT NOT NULL,
    sender VARCHAR(20) NOT NULL,
    conversation_id INTEGER NOT NULL,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (conversation_id) REFERENCES conversation(id)
);
```

## Configuration

### Agent Server (application-agent.yml)

- Port: 8090
- Profile: agent
- Main class: `AgentServerApplication`

### Web Server (application-web.yml)

- Port: 5100
- Profile: web
- Main class: `WebServerApplication`
- Database: SQLite

## Key Differences from Python

| Aspect | Python | Java |
|--------|--------|------|
| Framework | Flask + SQLAlchemy | Spring Boot + JPA |
| Database Driver | sqlite3 | SQLite JDBC |
| Password Hashing | werkzeug | BCrypt (Spring Security) |
| Agent Deployment | LocalDeployManager (async) | LocalDeployManager (Spring) |
| HTTP Client | requests | WebClient (reactive) |
| Config | .env file | application.yml + env vars |

## Features

✅ User authentication with password hashing
✅ Multi-user support
✅ Session/conversation management
✅ Message persistence (SQLite)
✅ AI agent integration
✅ Streaming responses
✅ CORS enabled
✅ 100% API compatible with Python version

## Troubleshooting

### Port Already in Use

Change the port in the respective configuration file or export different port:

```bash
export SERVER_PORT=8091  # For agent server
# or in application-web.yml for web server
```

### Database Issues

Delete `ai_assistant.db` and restart - it will be recreated with sample users.

### API Key Not Set

```bash
echo $DASHSCOPE_API_KEY
# If empty, set it:
export DASHSCOPE_API_KEY=your_key
```

### Agent Server Not Responding

Make sure the agent server is running on port 8090 before starting the web server.

Check logs for errors:
```bash
# In the agent server terminal
# Look for "✅ Service deployed successfully!"
```

## Testing

### Test Agent Server

```bash
curl -X POST http://localhost:8090/agent \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{
    "input": [{"role": "user", "content": [{"type": "text", "text": "Hello"}]}],
    "session_id": "test123",
    "user_id": "test123"
  }'
```

### Test Web Server

```bash
# Login
curl -X POST http://localhost:5100/api/login \
  -H "Content-Type: application/json" \
  -d '{"username": "user1", "password": "password123"}'

# Get conversations
curl http://localhost:5100/api/users/1/conversations
```

## Logging

Logs are output to console. Adjust log level in configuration:

```yaml
logging:
  level:
    io.agentscope.chatbot: DEBUG
```

## License

Apache 2.0

## Notes

- Both servers must be running for the full system to work
- The web server calls the agent server for AI responses
- Frontend connects to web server (port 5100)
- Database is created automatically if it doesn't exist
- Sample users are created on first run

