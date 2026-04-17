#!/bin/bash
# Script to build all microservices

echo "Building all services..."

services=(
  "discovery-server"
  "identity-service"
  "campaign-service"
  "media-service"
  "chat-service"
  "payment-service"
  "notification-service"
  "api-gateway"
)

for service in "${services[@]}"; do
  echo "-----------------------------------"
  echo "Building $service..."
  cd $service
  mvn clean package -DskipTests
  cd ..
done

echo "Build complete! 🚀"
