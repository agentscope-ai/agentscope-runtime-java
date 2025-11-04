#!/bin/bash

# Quick Start Script - Build and run both servers

set -e

echo "╔════════════════════════════════════════════════════════════╗"
echo "║  AgentScope Chatbot - Java Backend Quick Start            ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# Check Java
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed. Please install Java 17 or higher."
    exit 1
fi

# Check Maven
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven is not installed. Please install Maven 3.6 or higher."
    exit 1
fi

# Check API Key
if [ -z "$DASHSCOPE_API_KEY" ] && [ -z "$AI_DASHSCOPE_API_KEY" ]; then
    echo "❌ DASHSCOPE_API_KEY is not set"
    echo ""
    echo "Please set your API key:"
    echo "  export DASHSCOPE_API_KEY=your_api_key_here"
    echo ""
    exit 1
fi

echo "✅ Prerequisites check passed"
echo ""

# Build parent project
echo "════════════════════════════════════════════════════════════"
echo "Step 1/3: Building parent project..."
echo "════════════════════════════════════════════════════════════"
cd ../../..
if [ ! -f "target/agentscope-runtime-java-1.0.0-SNAPSHOT.jar" ]; then
    mvn clean install -DskipTests
    echo "✅ Parent project built"
else
    echo "✅ Parent project already built"
fi

# Build backend
echo ""
echo "════════════════════════════════════════════════════════════"
echo "Step 2/3: Building Java backend..."
echo "════════════════════════════════════════════════════════════"
cd examples/chatbot_fullstack_runtime/backend_java
mvn clean package -DskipTests
echo "✅ Backend built successfully"

# Copy database if needed
echo ""
echo "════════════════════════════════════════════════════════════"
echo "Step 3/3: Preparing database..."
echo "════════════════════════════════════════════════════════════"
if [ ! -f "ai_assistant.db" ]; then
    if [ -f "../backend/ai_assistant_example.db" ]; then
        cp ../backend/ai_assistant_example.db ai_assistant.db
        echo "✅ Database copied from example"
    else
        echo "⚠️  No example database found. Will create new database."
    fi
else
    echo "✅ Database already exists"
fi

echo ""
echo "════════════════════════════════════════════════════════════"
echo "Ready to run!"
echo "════════════════════════════════════════════════════════════"
echo ""
echo "To start the servers:"
echo ""
echo "Terminal 1 (Agent Server - port 8090):"
echo "  ./run-agent.sh"
echo ""
echo "Terminal 2 (Web Server - port 5100):"
echo "  ./run-web.sh"
echo ""
echo "Then start the frontend on port 3000"
echo ""

