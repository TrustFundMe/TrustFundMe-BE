#!/bin/bash
# Mac/Linux: Open 7 Terminal windows to run all services
# Usage: ./run-all-services.sh

set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "Opening 7 Terminal windows to run services..."
echo "Note: Each service runs in a separate window"

# Open Discovery Server
osascript -e "tell application \"Terminal\" to do script \"cd '$ROOT'/discovery-server && echo 'Starting Discovery Server (port 8761)...' && mvn spring-boot:run\""
sleep 5

# Open API Gateway
osascript -e "tell application \"Terminal\" to do script \"cd '$ROOT'/api-gateway && echo 'Starting API Gateway (port 8080)...' && mvn spring-boot:run\""
sleep 5

# Open Identity Service (needs .env)
osascript -e "tell application \"Terminal\" to do script \"
  cd '$ROOT'
  [ -f .env ] && set -a && . .env && set +a
  cd identity-service
  echo 'Starting Identity Service (port 8081)...'
  mvn spring-boot:run
\""
sleep 5

# Open Campaign Service (needs .env)
osascript -e "tell application \"Terminal\" to do script \"
  cd '$ROOT'
  [ -f .env ] && set -a && . .env && set +a
  cd campaign-service
  echo 'Starting Campaign Service (port 8082)...'
  mvn spring-boot:run
\""
sleep 5

# Open Media Service (needs .env)
osascript -e "tell application \"Terminal\" to do script \"
  cd '$ROOT'
  [ -f .env ] && set -a && . .env && set +a
  cd media-service
  echo 'Starting Media Service (port 8083)...'
  mvn spring-boot:run
\""
sleep 5

# Open Feed Service (needs .env)
osascript -e "tell application \"Terminal\" to do script \"
  cd '$ROOT'
  [ -f .env ] && set -a && . .env && set +a
  cd feed-service
  echo 'Starting Feed Service (port 8084)...'
  mvn spring-boot:run
\""
sleep 5

# Open Chat Service (needs .env)
osascript -e "tell application \"Terminal\" to do script \"
  cd '$ROOT'
  [ -f .env ] && set -a && . .env && set +a
  cd chat-service
  echo 'Starting Chat Service (port 8086)...'
  mvn spring-boot:run
\""

echo ""
echo "Opened 7 Terminal windows!"
echo "  1. Discovery Server (port 8761)"
echo "  2. API Gateway (port 8080)"
echo "  3. Identity Service (port 8081)"
echo "  4. Campaign Service (port 8082)"
echo "  5. Media Service (port 8083)"
echo "  6. Feed Service (port 8084)"
echo "  7. Chat Service (port 8086)"
