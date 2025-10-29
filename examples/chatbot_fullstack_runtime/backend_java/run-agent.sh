#!/bin/bash

# Run Agent Server (port 8090)

echo "Starting Agent Server on port 8090..."

# Check if DASHSCOPE_API_KEY is set
if [ -z "$DASHSCOPE_API_KEY" ] && [ -z "$AI_DASHSCOPE_API_KEY" ]; then
    echo "Error: DASHSCOPE_API_KEY or AI_DASHSCOPE_API_KEY environment variable is not set"
    echo "Please set it using:"
    echo "  export DASHSCOPE_API_KEY=your_api_key_here"
    exit 1
fi

# Set environment variables
export SERVER_PORT=8090
export SERVER_ENDPOINT=agent

# Run with agent profile
mvn spring-boot:run -Dspring-boot.run.profiles=agent

