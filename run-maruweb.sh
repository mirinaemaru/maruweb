#!/bin/bash

#########################################
# Maruweb Local Development Server
# 로컬 개발 서버 실행 스크립트
#########################################

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "========================================="
echo "  Maruweb Server Startup Script"
echo "========================================="
echo ""

# Check if .env file exists
if [ ! -f .env ]; then
    echo -e "${RED}✗ Error: .env file not found!${NC}"
    echo "Please create .env file with required environment variables."
    echo "See .env.example for reference."
    exit 1
fi

echo -e "${GREEN}✓ Found .env file${NC}"

# Load environment variables
echo "Loading environment variables..."
export $(cat .env | grep -v '^#' | grep -v '^$' | xargs)
echo -e "${GREEN}✓ Environment variables loaded${NC}"

# Check if port 8090 is already in use
echo ""
echo "Checking port 8090..."
PID=$(lsof -ti:8090 2>/dev/null || true)

if [ ! -z "$PID" ]; then
    echo -e "${YELLOW}⚠ Port 8090 is already in use by process $PID${NC}"
    read -p "Do you want to kill the existing process? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "Killing process $PID..."
        kill -9 $PID 2>/dev/null || true
        sleep 2
        echo -e "${GREEN}✓ Process killed${NC}"
    else
        echo -e "${RED}✗ Aborted. Please stop the existing server manually.${NC}"
        exit 1
    fi
else
    echo -e "${GREEN}✓ Port 8090 is available${NC}"
fi

# Check MariaDB connection
echo ""
echo "Checking database connection..."
if mysql -u "$DB_USERNAME" -p"$DB_PASSWORD" -e "SELECT 1" maruweb &>/dev/null; then
    echo -e "${GREEN}✓ Database connection successful${NC}"
else
    echo -e "${RED}✗ Cannot connect to database${NC}"
    echo "Please check if MariaDB is running:"
    echo "  brew services start mariadb"
    exit 1
fi

# Start the server
echo ""
echo "========================================="
echo "  Starting Spring Boot Server"
echo "========================================="
echo ""
echo "Log file: /tmp/maruweb.log"
echo "Server URL: http://localhost:8090"
echo ""

# Start server in background
SPRING_PROFILES_ACTIVE=local \
DB_USERNAME="$DB_USERNAME" \
DB_PASSWORD="$DB_PASSWORD" \
DB_URL="$DB_URL" \
GOOGLE_CLIENT_ID="$GOOGLE_CLIENT_ID" \
GOOGLE_CLIENT_SECRET="$GOOGLE_CLIENT_SECRET" \
CALENDAR_ENCRYPTION_KEY="$CALENDAR_ENCRYPTION_KEY" \
TRADING_API_BASE_URL="$TRADING_API_BASE_URL" \
nohup ./mvnw spring-boot:run > /tmp/maruweb.log 2>&1 &

SERVER_PID=$!
echo "Server process ID: $SERVER_PID"

# Wait for server to start
echo ""
echo "Waiting for server to start..."
for i in {1..30}; do
    sleep 1
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8090/ 2>/dev/null || echo "000")
    if [ "$STATUS" = "200" ]; then
        echo -e "${GREEN}✓ Server started successfully!${NC}"
        echo ""
        echo "========================================="
        echo "  Server is ready!"
        echo "========================================="
        echo ""
        echo "  URL: http://localhost:8090"
        echo "  PID: $SERVER_PID"
        echo "  Logs: tail -f /tmp/maruweb.log"
        echo ""
        exit 0
    fi
    echo -n "."
done

# If we get here, server didn't start in time
echo ""
echo -e "${RED}✗ Server failed to start within 30 seconds${NC}"
echo ""
echo "Recent logs:"
echo "----------------------------------------"
tail -30 /tmp/maruweb.log
echo "----------------------------------------"
echo ""
echo "Full logs: /tmp/maruweb.log"
exit 1
