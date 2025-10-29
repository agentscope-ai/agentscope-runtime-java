#!/bin/bash

# Script to run the Browser Agent Backend
# Usage: ./run.sh

# Check if DASHSCOPE_API_KEY is set
if [ -z "$DASHSCOPE_API_KEY" ] && [ -z "$AI_DASHSCOPE_API_KEY" ]; then
    echo "Error: DASHSCOPE_API_KEY or AI_DASHSCOPE_API_KEY environment variable is not set"
    echo "Please set it using:"
    echo "  export DASHSCOPE_API_KEY=your_api_key_here"
    exit 1
fi

# Check if Docker is running
if ! docker ps &> /dev/null; then
    echo "Error: Docker is not running or not accessible"
    echo "Please start Docker and try again"
    exit 1
fi

echo "Starting Browser Agent Backend..."
echo "Port: 9000"
echo ""

# Run the application
mvn spring-boot:run

