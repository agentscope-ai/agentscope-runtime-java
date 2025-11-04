#!/bin/bash

# Run Web Server (port 5100)

echo "Starting Web Server on port 5100..."

# Set environment variables
export SERVER_HOST=localhost
export SERVER_PORT=8090
export SERVER_ENDPOINT=agent

# Run with web profile
mvn spring-boot:run -Dspring-boot.run.profiles=web

