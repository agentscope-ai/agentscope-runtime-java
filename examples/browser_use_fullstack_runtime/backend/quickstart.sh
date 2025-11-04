#!/bin/bash

# Quick Start Script for Browser Use Java Backend
# This script helps you get started quickly with the Java backend

set -e  # Exit on error

echo "╔════════════════════════════════════════════════════════════╗"
echo "║  AgentScope Browser Use - Java Backend Quick Start        ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# Check Java version
echo "Checking Java installation..."
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed. Please install Java 17 or higher."
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F '.' '{print $1}')
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "❌ Java version must be 17 or higher. Current version: $JAVA_VERSION"
    exit 1
fi
echo "✅ Java version: $(java -version 2>&1 | head -n 1)"

# Check Maven
echo ""
echo "Checking Maven installation..."
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven is not installed. Please install Maven 3.6 or higher."
    exit 1
fi
echo "✅ Maven version: $(mvn -version | head -n 1)"

# Check Docker
echo ""
echo "Checking Docker installation..."
if ! docker ps &> /dev/null; then
    echo "❌ Docker is not running or not accessible"
    echo "   Please start Docker and try again"
    exit 1
fi
echo "✅ Docker is running"

# Check API Key
echo ""
echo "Checking API Key..."
if [ -z "$DASHSCOPE_API_KEY" ] && [ -z "$AI_DASHSCOPE_API_KEY" ]; then
    echo "❌ DASHSCOPE_API_KEY or AI_DASHSCOPE_API_KEY is not set"
    echo ""
    echo "Please set your API key:"
    echo "  export DASHSCOPE_API_KEY=your_api_key_here"
    echo ""
    exit 1
fi
echo "✅ API Key is set"

# Build parent project
echo ""
echo "════════════════════════════════════════════════════════════"
echo "Step 1/3: Building parent AgentScope Runtime project..."
echo "════════════════════════════════════════════════════════════"
cd ../../..
if [ ! -f "target/agentscope-runtime-java-1.0.0-SNAPSHOT.jar" ]; then
    echo "Building parent project (this may take a few minutes)..."
    mvn clean install -DskipTests
    echo "✅ Parent project built successfully"
else
    echo "✅ Parent project already built"
fi

# Build backend
echo ""
echo "════════════════════════════════════════════════════════════"
echo "Step 2/3: Building Java backend..."
echo "════════════════════════════════════════════════════════════"
cd examples/browser_use_fullstack_runtime/backend_java
mvn clean package -DskipTests
echo "✅ Java backend built successfully"

# Run the backend
echo ""
echo "════════════════════════════════════════════════════════════"
echo "Step 3/3: Starting Java backend..."
echo "════════════════════════════════════════════════════════════"
echo ""
echo "Backend will start on http://localhost:9000"
echo ""
echo "API Endpoints:"
echo "  - POST http://localhost:9000/chat/completions"
echo "  - GET  http://localhost:9000/env_info"
echo ""
echo "Press Ctrl+C to stop the server"
echo ""
echo "────────────────────────────────────────────────────────────"

mvn spring-boot:run

